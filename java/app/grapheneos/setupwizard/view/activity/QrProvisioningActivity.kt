package app.grapheneos.setupwizard.view.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.StringRes
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.action.QrProvisioningActions
import app.grapheneos.setupwizard.provisioning.ProvisioningQrParser
import app.grapheneos.setupwizard.utils.QrCodeScanner
import com.google.android.setupdesign.GlifLayout

class QrProvisioningActivity : SetupWizardActivity(
    R.layout.activity_qr_provisioning,
    header = R.string.qr_provisioning_title,
    description = R.string.qr_provisioning_desc,
) {
    private lateinit var scanner: QrCodeScanner
    private var provisioningStarted = false

    private val cameraPermission = registerForActivityResult(RequestPermission()) { granted ->
        Log.d(TAG, "camera permission granted: $granted")
        if (!granted) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        provisioningStarted =
            savedInstanceState?.getBoolean(KEY_PROVISIONING_STARTED) ?: false
        if (savedInstanceState == null && !hasCameraPermission()) {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun bindViews() {
        scanner = QrCodeScanner(
            this,
            requireViewById(R.id.camera_preview),
            onDecoded = ::onQrCode,
            onError = ::onScannerError,
        )

        secondaryButton.setText(this, R.string.cancel)
    }

    override fun setupActions() {
        secondaryButton.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()

        val canScan = !provisioningStarted && !isFinishing && hasCameraPermission()
        if (canScan) {
            scanner.start()
        }
    }

    override fun onPause() {
        scanner.stop()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_PROVISIONING_STARTED, provisioningStarted)
    }

    override fun onActivityResult(resultCode: Int, data: Intent?) {
        QrProvisioningActions.handleProvisioningResult(this, resultCode)
    }

    private fun onQrCode(contents: String) {
        if (provisioningStarted) {
            return
        }

        val intent = ProvisioningQrParser.parse(contents)

        if (intent == null) {
            showMessage(R.string.qr_provisioning_invalid_code)
            return
        }

        provisioningStarted = true
        scanner.stop()
        QrProvisioningActions.startProvisioning(this, intent)
    }

    private fun onScannerError(error: QrCodeScanner.Error) {
        val message = when (error) {
            QrCodeScanner.Error.NO_CAMERA -> R.string.qr_provisioning_no_camera
            QrCodeScanner.Error.CAMERA_DISABLED -> R.string.qr_provisioning_camera_disabled
            QrCodeScanner.Error.CAMERA_IN_USE,
            QrCodeScanner.Error.CAMERA_FAILURE -> R.string.qr_provisioning_camera_error
        }

        showMessage(message)
    }

    private fun showMessage(@StringRes message: Int) {
        requireViewById<GlifLayout>(R.id.glif_layout).setDescriptionText(message)
    }

    private fun hasCameraPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "QrProvisioningActivity"
        private const val KEY_PROVISIONING_STARTED = "provisioning_started"
    }
}
