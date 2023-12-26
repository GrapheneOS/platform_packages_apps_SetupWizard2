package org.grapheneos.setupwizard.action

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import com.android.internal.app.LocalePicker
import com.google.android.setupcompat.util.SystemBarHelper
import org.grapheneos.setupwizard.R
import org.grapheneos.setupwizard.data.WelcomeData

object WelcomeActions {
    private const val TAG = "WelcomeActions"
    private const val ACTION_ACCESSIBILITY = "android.settings.ACCESSIBILITY_SETTINGS_FOR_SUW"
    private const val ACTION_EMERGENCY = "com.android.phone.EmergencyDialer.DIAL"

    init {
        refreshCurrentLocale()
        Log.d(TAG, "init: currentLocale = ${WelcomeData.selectedLanguage}")
    }

    fun handleEntry(context: Activity) {
        SetupWizard.setStatusBarHidden(true)
        SystemBarHelper.setBackButtonVisible(context.window, false)
    }

    fun showLanguagePicker(activity: Activity) {
        val adapter = LocalePicker.constructAdapter(activity)
        AlertDialog.Builder(activity)
            .setTitle(R.string.choose_your_language)
            .setAdapter(adapter) { _, which ->
                updateLocale(
                    adapter.getItem(which)
                )
            }.create().show()
    }

    private fun updateLocale(locale: LocalePicker.LocaleInfo) {
        LocalePicker.updateLocale(locale.locale)
        refreshCurrentLocale()
    }

    private fun refreshCurrentLocale() {
        WelcomeData.selectedLanguage.value = LocalePicker.getLocales()[0]
    }

    fun accessibilitySettings(context: Activity) {
        SetupWizard.startActivity(context, Intent(ACTION_ACCESSIBILITY))
    }

    fun emergencyCall(context: Activity) {
        SetupWizard.startActivity(context, Intent(ACTION_EMERGENCY))
    }
}