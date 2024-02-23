package app.grapheneos.setupwizard.action

import android.app.Activity
import android.app.ActivityManager
import android.content.pm.PackageManager
import android.provider.Settings
import android.service.oemlock.OemLockManager
import android.util.Log
import app.grapheneos.setupwizard.appContext
import app.grapheneos.setupwizard.data.FinishData

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
        context.getSystemService(ActivityManager::class.java)!!.appTasks.forEach {
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
        FinishData.oemUnlockingEnabled.value =
            getOemLockManager()?.isOemUnlockAllowedByUser ?: false
    }

    private fun disableOemUnlockByUser() {
        getOemLockManager()?.isOemUnlockAllowedByUser = false
    }

    private fun getOemLockManager(): OemLockManager? {
        return appContext.getSystemService(OemLockManager::class.java)
    }
}
