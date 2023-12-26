package org.grapheneos.setupwizard.action

import android.app.Activity
import android.app.ActivityManager
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.google.android.setupcompat.util.SystemBarHelper
import org.grapheneos.setupwizard.appContext

object FinishActions {
    private const val TAG = "FinishActions"

    init {
    }

    fun finish(context: Activity) {
        Log.d(TAG, "finish")
        // mark completion
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DEVICE_PROVISIONED, 1
        )
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.USER_SETUP_COMPLETE, 1
        )
        SystemBarHelper.hideSystemBars(context.window)
        // cleanup tasks
        context.getSystemService(ActivityManager::class.java).appTasks.forEach {
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
}