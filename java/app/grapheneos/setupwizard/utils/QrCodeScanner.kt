package app.grapheneos.setupwizard.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.annotation.MainThread
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/**
 * Camera preview with QR code decoding. The hosting activity is expected to stay in its natural
 * orientation.
 *
 * Callbacks are invoked on the main thread. [onError] means that the scanner has stopped; it can
 * be started again.
 */
class QrCodeScanner(
    context: Context,
    private val textureView: TextureView,
    private val onDecoded: (String) -> Unit,
    private val onError: (Error) -> Unit,
) {
    enum class Error {
        NO_CAMERA,
        CAMERA_IN_USE,
        CAMERA_DISABLED,
        CAMERA_FAILURE,
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainExecutor = context.mainExecutor

    private var started = false
    private var camera: ScannerCamera? = null

    init {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                texture: SurfaceTexture,
                width: Int,
                height: Int,
            ) {
                if (started) {
                    openCamera()
                }
            }

            // TextureView resets the buffer size to the view size whenever the view is resized
            override fun onSurfaceTextureSizeChanged(
                texture: SurfaceTexture,
                width: Int,
                height: Int,
            ) {
                camera?.configurePreview(texture)
            }

            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                closeCamera()
                return true
            }

            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
        }
    }

    @MainThread
    fun start() {
        if (started) {
            return
        }

        started = true

        if (textureView.isAvailable) {
            openCamera()
        }
    }

    @MainThread
    fun stop() {
        if (!started) {
            return
        }

        started = false

        closeCamera()
    }

    private fun openCamera() {
        if (camera != null) {
            return
        }

        val cameraId: String?
        val characteristics: CameraCharacteristics?
        try {
            cameraId = findCamera()
            characteristics = cameraId?.let { cameraManager.getCameraCharacteristics(it) }
        } catch (e: CameraAccessException) {
            fail(errorOf(e), "querying cameras failed", e)
            return
        }

        val frameSize = characteristics?.let { chooseFrameSize(it) }
        if (cameraId == null || characteristics == null || frameSize == null) {
            fail(Error.NO_CAMERA, "no camera with a suitable YUV frame size")
            return
        }

        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        // open() can fail synchronously, which closes the camera via stop()
        val scannerCamera = ScannerCamera(frameSize, sensorOrientation)
        camera = scannerCamera
        scannerCamera.open(cameraId)
    }

    private fun closeCamera() {
        camera?.close()
        camera = null
    }

    private fun fail(error: Error, message: String, cause: Throwable? = null) {
        Log.e(TAG, "$message, reporting $error", cause)
        stop()
        // start() can fail synchronously, don't call back into the caller from within it
        mainHandler.post { onError(error) }
    }

    private fun findCamera(): String? {
        val ids = cameraManager.cameraIdList

        return ids
            .firstOrNull { id ->
                val facing = cameraManager
                    .getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING)

                facing == CameraCharacteristics.LENS_FACING_BACK
            }
            ?: ids.firstOrNull()
    }

    private fun chooseFrameSize(characteristics: CameraCharacteristics): Size? {
        val sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?.getOutputSizes(ImageFormat.YUV_420_888)
            ?: return null

        val pixels = { size: Size -> size.width * size.height }

        return sizes
            .filter { pixels(it) <= MAX_FRAME_PIXELS }
            .maxByOrNull(pixels)
            ?: sizes.minByOrNull(pixels)
    }

    /**
     * A single attempt to open the camera and stream frames from it. Everything except
     * [decodeLatestFrame] runs on the main thread.
     */
    private inner class ScannerCamera(
        private val frameSize: Size,
        private val sensorOrientation: Int,
    ) {
        private val decoderThread = HandlerThread(TAG).apply { start() }
        private val decoderHandler = Handler(decoderThread.looper)
        private val frameReader = ImageReader
            .newInstance(
                frameSize.width,
                frameSize.height,
                ImageFormat.YUV_420_888,
                MAX_FRAMES_IN_FLIGHT,
            )
            .apply {
                setOnImageAvailableListener({ decodeLatestFrame() }, decoderHandler)
            }

        private val qrReader = QRCodeReader()

        private var device: CameraDevice? = null
        private var session: CameraCaptureSession? = null
        private var closed = false

        private val deviceCallback = object : CameraDevice.StateCallback() {

            override fun onOpened(camera: CameraDevice) {
                if (closed) {
                    camera.close()
                    return
                }

                device = camera
                createSession(camera)
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                fail(Error.CAMERA_IN_USE, "camera disconnected")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                fail(errorOfDeviceError(error), "camera device error $error")
            }
        }

        @SuppressLint("MissingPermission")
        fun open(cameraId: String) {
            try {
                cameraManager.openCamera(cameraId, deviceCallback, mainHandler)
            } catch (e: CameraAccessException) {
                fail(errorOf(e), "openCamera failed", e)
            } catch (e: SecurityException) {
                fail(Error.CAMERA_FAILURE, "openCamera failed", e)
            }
        }

        fun close() {
            closed = true
            session?.close()
            device?.close()
            // closing the reader invalidates acquired images, so wait for the frame being decoded
            decoderHandler.post { frameReader.close() }
            decoderThread.quitSafely()
        }

        fun configurePreview(texture: SurfaceTexture) {
            texture.setDefaultBufferSize(frameSize.width, frameSize.height)
            applyCenterCrop()
        }

        private fun fail(error: Error, message: String, cause: Throwable? = null) {
            if (!closed) {
                this@QrCodeScanner.fail(error, message, cause)
            }
        }

        private fun createSession(camera: CameraDevice) {
            val texture = textureView.surfaceTexture
            if (texture == null) {
                fail(Error.CAMERA_FAILURE, "preview surface is gone")
                return
            }

            configurePreview(texture)

            val previewSurface = Surface(texture)
            val outputs = listOf(
                OutputConfiguration(previewSurface),
                OutputConfiguration(frameReader.surface),
            )

            val callback = object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (closed) {
                        session.close()
                        return
                    }

                    this@ScannerCamera.session = session
                    startRepeatingRequest(camera, session, previewSurface)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    fail(Error.CAMERA_FAILURE, "session configuration failed")
                }
            }

            val config = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputs,
                mainExecutor,
                callback,
            )

            try {
                camera.createCaptureSession(config)
            } catch (e: CameraAccessException) {
                fail(errorOf(e), "createCaptureSession failed", e)
            }
        }

        private fun startRepeatingRequest(
            camera: CameraDevice,
            session: CameraCaptureSession,
            previewSurface: Surface,
        ) {
            try {
                val request = camera
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    .apply {
                        addTarget(previewSurface)
                        addTarget(frameReader.surface)
                        set(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                        )
                    }
                    .build()

                session.setRepeatingRequest(request, null, null)
            } catch (e: CameraAccessException) {
                fail(errorOf(e), "setRepeatingRequest failed", e)
            }
        }

        // TextureView stretches the buffer to fill the view; scale it back to keep the aspect ratio
        private fun applyCenterCrop() {
            val viewWidth = textureView.width.toFloat()
            val viewHeight = textureView.height.toFloat()
            if (viewWidth == 0f || viewHeight == 0f) {
                return
            }

            val rotated = sensorOrientation % 180 != 0
            val bufferWidth = if (rotated) frameSize.height else frameSize.width
            val bufferHeight = if (rotated) frameSize.width else frameSize.height

            val scaleX = viewWidth / bufferWidth
            val scaleY = viewHeight / bufferHeight
            val scale = maxOf(scaleX, scaleY)
            textureView.setTransform(Matrix().apply {
                setScale(scale / scaleX, scale / scaleY, viewWidth / 2, viewHeight / 2)
            })
        }

        private fun decodeLatestFrame() {
            val text = try {
                frameReader.acquireLatestImage()?.use { image ->
                    // the Y plane alone is enough for decoding
                    val plane = image.planes[0]
                    val luminance = ByteArray(plane.buffer.remaining())
                        .also { plane.buffer.get(it) }

                    val source = PlanarYUVLuminanceSource(
                        luminance, plane.rowStride, image.height,
                        0, 0, image.width, image.height, false
                    )
                    qrReader.decode(BinaryBitmap(HybridBinarizer(source)), DECODE_HINTS).text
                }
            } catch (_: ReaderException) {
                null
            } catch (_: IllegalStateException) {
                // a frame became available after frameReader was closed
                null
            } ?: return

            mainHandler.post {
                if (!closed) {
                    onDecoded(text)
                }
            }
        }
    }

    private companion object {
        const val TAG = "QrCodeScanner"
        const val MAX_FRAME_PIXELS = 1920 * 1080
        const val MAX_FRAMES_IN_FLIGHT = 2

        val DECODE_HINTS = mapOf(DecodeHintType.TRY_HARDER to true)

        fun errorOf(e: CameraAccessException): Error = when (e.reason) {
            CameraAccessException.CAMERA_DISABLED -> Error.CAMERA_DISABLED
            CameraAccessException.CAMERA_IN_USE,
            CameraAccessException.MAX_CAMERAS_IN_USE,
            CameraAccessException.CAMERA_DISCONNECTED -> Error.CAMERA_IN_USE
            else -> Error.CAMERA_FAILURE
        }

        fun errorOfDeviceError(error: Int): Error = when (error) {
            CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> Error.CAMERA_DISABLED
            CameraDevice.StateCallback.ERROR_CAMERA_IN_USE,
            CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> Error.CAMERA_IN_USE
            else -> Error.CAMERA_FAILURE
        }
    }
}
