package app.grapheneos.setupwizard.action

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.oemlock.OemLockManager
import android.util.Log
import app.grapheneos.setupwizard.appContext
import app.grapheneos.setupwizard.data.FinishData
import app.grapheneos.setupwizard.utils.DebugFlags

object FinishActions {
    private const val TAG = "FinishActions"

    init {
        refreshOemUnlockStatus()
    }

    fun finish(context: Activity, disableOemUnlocking: Boolean = false) {
        Log.d(TAG, "finish")
        // disable oem unlocking
        if (disableOemUnlocking) disableOemUnlockByUser()
        // mark completion
        if (SetupWizard.isPrimaryUser) {
            // not needed in case of secondary user
            Settings.Global.putInt(
                context.contentResolver,
                Settings.Global.DEVICE_PROVISIONED, 1
            )
        }
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.USER_SETUP_COMPLETE, 1
        )
        // cleanup tasks
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).appTasks.forEach {
            it.finishAndRemoveTask()
        }
        // finish activity
        context.finish()
        SetupWizard.setStatusBarHidden(false)
        // disable myself
        appContext.packageManager.setApplicationEnabledSetting(
            appContext.packageName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
    }

    private fun refreshOemUnlockStatus() {
        if (Build.isDebuggable()) {
            val flag = DebugFlags.getBool("isOemUnlockAllowedByUser_override")
            if (flag != null) {
                FinishData.oemUnlockingEnabled.value = flag
                return
            }
        }

        FinishData.oemUnlockingEnabled.value =
            getOemLockManager()?.isOemUnlockAllowedByUser ?: false
    }

    private fun disableOemUnlockByUser() {
        val olm = getOemLockManager()
        if (olm == null) {
            Log.d(TAG, "disableOemUnlockByUser: OemLockManager is null")
            return
        }

        try {
            olm.setOemUnlockAllowedByUser(false)
        } catch (e: SecurityException) {
            // See https://discuss.grapheneos.org/d/17678-setup-wizard-cannot-be-finished-after-grapheneos-installation/15
            if (!olm.isOemUnlockAllowed()) {
                Log.d(TAG, "setOemUnlockAllowedByUser(false) failed, OEM unlocking is not allowed", e)
            } else {
                // TODO: surface this error to the user
                Log.e(TAG, "", e)
            }
        }
    }

    private fun getOemLockManager(): OemLockManager? {
        return appContext.getSystemService(OemLockManager::class.java)
    }
}
