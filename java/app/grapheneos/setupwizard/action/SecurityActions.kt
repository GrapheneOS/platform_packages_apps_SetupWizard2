package app.grapheneos.setupwizard.action

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import app.grapheneos.setupwizard.appContext
import app.grapheneos.setupwizard.data.SecurityData
import app.grapheneos.setupwizard.view.activity.SetupWizardActivity

object SecurityActions {
    private const val TAG = "SecurityActions"
    private const val ACTION_SETUP_LOCK_SCREEN = "com.android.settings.SETUP_LOCK_SCREEN"
    private const val ACTION_SETUP_BIOMETRIC = "android.settings.BIOMETRIC_ENROLL"

    init {
        refreshSecurityStatus()
    }

    fun launchSetup(context: SetupWizardActivity) {
        val intent = Intent(ACTION_SETUP_BIOMETRIC)
        SetupWizard.startActivityForResult(context, intent)
    }

    fun handleResult(context: Activity, resultCode: Int) {
        if (resultCode != Activity.RESULT_CANCELED) {
            SetupWizard.next(context)
        }
        refreshSecurityStatus()
    }

    fun refreshSecurityStatus() {
        SecurityData.isDeviceSecure.value = getKeyguardManager().isDeviceSecure
    }

    private fun getKeyguardManager(): KeyguardManager {
        return appContext.getSystemService(KeyguardManager::class.java)!!
    }
}
