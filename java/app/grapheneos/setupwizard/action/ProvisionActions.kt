package app.grapheneos.setupwizard.action

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_TRIGGER
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PersistableBundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.view.activity.FinishActivity
import app.grapheneos.setupwizard.view.activity.ProvisionActivity
import app.grapheneos.setupwizard.view.activity.WelcomeActivity

object ProvisionActions {
    private const val TAG = "ProvisionActions"
    private const val PROVISIONING_TRIGGER_QR_CODE = 2

    // Copied from ManagedProvisioning app, as they're hidden.
    private const val PROVISION_FINALIZATION_INSIDE_SUW =
        "android.app.action.PROVISION_FINALIZATION_INSIDE_SUW"
    private const val RESULT_CODE_PROFILE_OWNER_SET = 122
    const val RESULT_CODE_DEVICE_OWNER_SET = 123

    fun provisionDeviceOwner(
        activity: ProvisionActivity,
        launcher: ActivityResultLauncher<Intent>
    ) {
        val provisionIntent = Intent(ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE)
        provisionIntent.putExtra(EXTRA_PROVISIONING_TRIGGER, PROVISIONING_TRIGGER_QR_CODE)
        provisionIntent.putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
            activity.intent.getParcelableExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                ComponentName::class.java
            )
        )
        provisionIntent.putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM,
            activity.intent.getStringExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM)
        )
        val systemAppsEnabled = activity.intent.getBooleanExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED, false
        )
        if (systemAppsEnabled) {
            provisionIntent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED, true)
        }
        val skipEncryption = activity.intent.getBooleanExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, false
        )
        if (skipEncryption) {
            provisionIntent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, true)
        }
        val extrasBundle: PersistableBundle? = activity.intent.getParcelableExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
            PersistableBundle::class.java
        )
        if (extrasBundle != null) {
            provisionIntent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, extrasBundle)
        }
        launcher.launch(provisionIntent)
    }

    fun handleProvisioningStep1Result(
        activity: ProvisionActivity,
        resultCode: Int,
        finalizationLauncher: ActivityResultLauncher<Intent>
    ) {
        when (resultCode) {
            RESULT_CODE_PROFILE_OWNER_SET, RESULT_CODE_DEVICE_OWNER_SET -> {
                val intent = Intent(PROVISION_FINALIZATION_INSIDE_SUW)
                    .addCategory(Intent.CATEGORY_DEFAULT)
                Log.i(TAG, "Finalizing DPC with $intent")
                finalizationLauncher.launch(intent)
            }
            else -> {
                factoryReset(
                    activity,
                    "invalid response from the provisioning engine: ${resultCodeToString(resultCode)}"
                )
            }
        }
    }

    fun handleProvisioningStep2Result(activity: ProvisionActivity, resultCode: Int) {
        // Set provisioning state before launching FinishActivity. The DPC may not remove the back
        // button on its own, so the wizard needs to mark the device as provisioned first.
        setProvisioningState(activity)
        if (resultCode != Activity.RESULT_OK) {
            factoryReset(
                activity,
                "invalid response from the provisioning engine: ${resultCodeToString(resultCode)}"
            )
            return
        }
        Log.i(TAG, "Device owner mode provisioned")
        SetupWizard.startActivity(activity, FinishActivity::class.java)
        disableSelfAndFinish(activity)
    }

    fun resultCodeToString(resultCode: Int): String {
        val name = when (resultCode) {
            Activity.RESULT_OK -> "RESULT_OK"
            Activity.RESULT_CANCELED -> "RESULT_CANCELED"
            Activity.RESULT_FIRST_USER -> "RESULT_FIRST_USER"
            RESULT_CODE_PROFILE_OWNER_SET -> "RESULT_CODE_PROFILE_OWNER_SET"
            RESULT_CODE_DEVICE_OWNER_SET -> "RESULT_CODE_DEVICE_OWNER_SET"
            else -> "UNKNOWN_CODE"
        }
        return "$name($resultCode)"
    }

    private fun factoryReset(activity: ProvisionActivity, reason: String) {
        AlertDialog.Builder(activity)
            .setMessage("Device provisioning failed ($reason) and device must be factory reset")
            .setPositiveButton(activity.getString(R.string.button_reset)) { _: DialogInterface?, _: Int ->
                sendFactoryResetIntent(activity, reason)
            }
            .setOnDismissListener {
                sendFactoryResetIntent(activity, reason)
            }
            .show()
    }

    private fun sendFactoryResetIntent(activity: ProvisionActivity, reason: String) {
        Log.e(TAG, "Factory resetting: $reason")
        val intent = Intent(Intent.ACTION_FACTORY_RESET)
        intent.setPackage("android")
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        intent.putExtra(Intent.EXTRA_REASON, reason)
        activity.sendBroadcast(intent)

        // Just in case the factory reset request fails, mark the device as provisioned and shut
        // ourselves down so the user is not stuck in the wizard.
        setProvisioningState(activity)
        disableSelfAndFinish(activity)
    }

    private fun setProvisioningState(activity: Activity) {
        Log.i(TAG, "Setting provisioning state")
        Settings.Global.putInt(activity.contentResolver, Settings.Global.DEVICE_PROVISIONED, 1)
        Settings.Secure.putInt(activity.contentResolver, Settings.Secure.USER_SETUP_COMPLETE, 1)
    }

    // Disable the wizard's WelcomeActivity component so the launcher does not re-enter setup
    // after the MDM admin has taken over. We only disable the entry-point activity (not the
    // entire app) so the package is still around if the factory-reset fallback path runs.
    private fun disableSelfAndFinish(activity: Activity) {
        val pm: PackageManager = activity.packageManager
        val component = ComponentName(activity.packageName, WelcomeActivity::class.java.name)
        Log.i(TAG, "Disabling component $component")
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        activity.finish()
    }
}
