package app.grapheneos.setupwizard.action

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import app.grapheneos.setupwizard.appContext
import app.grapheneos.setupwizard.view.activity.QrProvisioningActivity
import app.grapheneos.setupwizard.view.activity.SetupWizardActivity

object QrProvisioningActions {

    private const val TAG = "QrProvisioningActions"

    private var finalized = false

    private val dpm: DevicePolicyManager
        get() = appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun isAvailable(): Boolean {
        val pm = appContext.packageManager
        return SetupWizard.isPrimaryUser
                && pm.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)
                && pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                && dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE)
    }

    fun launchScanner(activity: Activity) {
        if (!isAvailable()) {
            Log.d(TAG, "launchScanner: provisioning not allowed")
            return
        }
        SetupWizard.startActivity(activity, QrProvisioningActivity::class.java)
    }

    fun startProvisioning(context: SetupWizardActivity, intent: Intent) {
        Log.d(TAG, "startProvisioning")
        SetupWizard.startActivityForResult(context, intent)
    }

    // ManagedProvisioning's result code depends on the flow, so check the outcome directly
    fun handleProvisioningResult(context: Activity, resultCode: Int) {
        val provisioned = isManaged()
        Log.d(TAG, "handleProvisioningResult: resultCode=$resultCode provisioned=$provisioned")
        if (provisioned) WelcomeActions.next(context)
        context.finish()
    }

    private fun isManaged(): Boolean {
        return dpm.deviceOwnerComponentOnCallingUser != null
                || dpm.isOrganizationOwnedDeviceWithManagedProfile
    }

    fun shouldRunFinalization(): Boolean {
        return SetupWizard.isPrimaryUser && !finalized && isManaged()
    }

    fun launchFinalization(context: SetupWizardActivity) {
        Log.d(TAG, "launchFinalization")
        SetupWizard.startActivityForResult(
            context, Intent(DevicePolicyManager.ACTION_PROVISION_FINALIZATION))
    }

    fun handleFinalizationResult(context: Activity, resultCode: Int) {
        Log.d(TAG, "handleFinalizationResult: $resultCode")
        finalized = true
        SetupWizard.next(context)
    }
}
