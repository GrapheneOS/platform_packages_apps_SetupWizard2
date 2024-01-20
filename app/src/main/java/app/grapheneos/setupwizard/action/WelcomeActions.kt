package app.grapheneos.setupwizard.action

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.ArrayAdapter
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.appContext
import app.grapheneos.setupwizard.data.WelcomeData
import com.android.internal.app.LocalePicker
import com.android.internal.app.LocalePicker.LocaleInfo
import com.google.android.setupcompat.util.SystemBarHelper

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
        val adapter = constructLocaleAdapter(activity)
        AlertDialog.Builder(activity)
            .setTitle(R.string.choose_your_language)
            .setAdapter(adapter) { _, which ->
                updateLocale(
                    adapter.getItem(which)
                )
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    private fun updateLocale(locale: LocaleInfo) {
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

    private fun constructLocaleAdapter(activity: Activity): ArrayAdapter<LocaleInfo> {
        val adapter = LocalePicker.constructAdapter(activity)
        val simLocale =
            appContext.getSystemService(TelephonyManager::class.java).simLocale ?: return adapter
        var localeInfo: LocaleInfo? = null
        for (index in 0..adapter.count) {
            val item = adapter.getItem(index)
            if (!item?.locale?.toLanguageTag().equals(simLocale.toLanguageTag())) continue
            Log.d(TAG, "constructLocaleAdapter: found simLocale $simLocale")
            localeInfo = item
            adapter.remove(localeInfo)
            break
        }
        if (localeInfo != null) adapter.insert(localeInfo, 0)
        return adapter
    }
}
