package app.grapheneos.setupwizard.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import app.grapheneos.setupwizard.action.QrProvisioningActions
import app.grapheneos.setupwizard.action.SetupWizard

class ProvisioningFinalizationActivity : ProxyActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!QrProvisioningActions.shouldRunFinalization()) {
            Log.d(TAG, "onCreate: skipping, not needed")
            finish()
            SetupWizard.next(this)
        }
    }

    override fun launchActual() {
        QrProvisioningActions.launchFinalization(this)
    }

    // finalization is done once, so back from the next step shouldn't return here
    override fun handleResult(resultCode: Int, data: Intent?) {
        finish()
        QrProvisioningActions.handleFinalizationResult(this, resultCode)
    }

    companion object {
        private const val TAG = "ProvisioningFinalizationActivity"
    }
}
