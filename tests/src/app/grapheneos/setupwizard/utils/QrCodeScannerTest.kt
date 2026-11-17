package app.grapheneos.setupwizard.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraManager
import android.view.TextureView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QrCodeScannerTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val errors = LinkedBlockingQueue<QrCodeScanner.Error>()

    private lateinit var textureView: TextureView
    private lateinit var scanner: QrCodeScanner

    @Before
    fun setUp() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        assumeTrue(cameraManager.cameraIdList.isNotEmpty())
        assertThat(context.checkSelfPermission(Manifest.permission.CAMERA))
            .isEqualTo(PackageManager.PERMISSION_DENIED)

        onMain {
            textureView = TextureView(context)
            scanner = QrCodeScanner(
                context,
                textureView,
                onDecoded = {},
                onError = { errors.add(it) },
            )
        }
    }

    @After
    fun tearDown() {
        if (::scanner.isInitialized) {
            onMain { scanner.stop() }
        }
    }

    @Test
    fun synchronousOpenFailure_isReported() {
        startWithSurface()

        assertThat(nextError()).isEqualTo(QrCodeScanner.Error.CAMERA_FAILURE)
    }

    @Test
    fun synchronousOpenFailure_doesNotPreventRestart() {
        startWithSurface()
        assertThat(nextError()).isNotNull()

        startWithSurface()

        assertThat(nextError()).isNotNull()
    }

    private fun startWithSurface() {
        onMain {
            scanner.start()
            val listener = checkNotNull(textureView.surfaceTextureListener)
            listener.onSurfaceTextureAvailable(SurfaceTexture(false), 1080, 1920)
        }
    }

    private fun nextError(): QrCodeScanner.Error? {
        return errors.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private fun onMain(block: () -> Unit) {
        instrumentation.runOnMainSync(block)
    }

    private companion object {
        const val TIMEOUT_SECONDS = 5L
    }
}
