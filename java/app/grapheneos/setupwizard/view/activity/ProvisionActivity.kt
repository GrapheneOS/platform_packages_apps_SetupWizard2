package app.grapheneos.setupwizard.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import app.grapheneos.setupwizard.action.ProvisionActions

class ProvisionActivity : ComponentActivity() {
    companion object {
        private const val TAG = "ProvisionActivity"
    }

    private lateinit var finalizationLauncher: ActivityResultLauncher<Intent>
    private lateinit var provisioningLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register the finalization launcher first so that the provisioning launcher can refer
        // to it from its callback.
        finalizationLauncher = registerForActivityResult(StartActivityForResult()) { result ->
            Log.d(TAG, "finalization result: ${ProvisionActions.resultCodeToString(result.resultCode)}")
            ProvisionActions.handleProvisioningStep2Result(this, result.resultCode)
        }
        provisioningLauncher = registerForActivityResult(StartActivityForResult()) { result ->
            Log.d(TAG, "provisioning result: ${ProvisionActions.resultCodeToString(result.resultCode)}")
            ProvisionActions.handleProvisioningStep1Result(this, result.resultCode, finalizationLauncher)
        }
        ProvisionActions.provisionDeviceOwner(this, provisioningLauncher)
    }
}
