package org.grapheneos.setupwizard.action

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import org.grapheneos.setupwizard.appContext
import org.grapheneos.setupwizard.data.SecurityData

object SecurityActions {
    private const val TAG = "SecurityActions"
    private const val ACTION_SETUP_LOCK_SCREEN = "com.android.settings.SETUP_LOCK_SCREEN"
    private const val ACTION_SETUP_BIOMETRIC = "android.settings.BIOMETRIC_ENROLL"
    private const val REQUEST_CODE = 101

    init {
        refreshSecurityStatus()
    }

    fun launchSetup(context: Activity) {
        val intent = Intent(ACTION_SETUP_BIOMETRIC)
        intent.putExtra("title", "Titan")
        intent.putExtra("details", "Delta Force")
        SetupWizard.startActivityForResult(context, intent, REQUEST_CODE)
    }

    fun handleResult(context: Activity, requestCode: Int, resultCode: Int) {
        if (requestCode != REQUEST_CODE) {
            return
        }
        if (resultCode != Activity.RESULT_CANCELED) {
            SetupWizard.next(context)
        }
        refreshSecurityStatus()
    }

    private fun refreshSecurityStatus() {
        SecurityData.isDeviceSecure.value = getKeyguardManager().isDeviceSecure
    }

    private fun getKeyguardManager(): KeyguardManager {
        return appContext.getSystemService(KeyguardManager::class.java)
    }
}
