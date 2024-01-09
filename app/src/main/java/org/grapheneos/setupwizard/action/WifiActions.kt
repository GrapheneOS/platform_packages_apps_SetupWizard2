package org.grapheneos.setupwizard.action

import android.app.Activity
import android.content.Intent

object WifiActions {
    private const val TAG = "WifiActions"

    private const val ACTION_SETUP_INTERNET = "android.settings.SETUP_INTERNET"
    private const val EXTRA_SHOW_BUTTONS = "extra_prefs_show_button_bar"
    private const val EXTRA_SHOW_SKIP = "extra_prefs_show_skip"
    private const val EXTRA_BACK_TEXT = "extra_prefs_set_back_text"
    private const val EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect"
    private const val EXTRA_MODE_WIFI = "setup_wizard_mode_wifi"
    private const val REQUEST_CODE = 301

    init {
    }

    fun launchSetup(context: Activity): Int {
        val intent = Intent(ACTION_SETUP_INTERNET)
        intent.putExtra(EXTRA_SHOW_BUTTONS, true)
        intent.putExtra(EXTRA_SHOW_SKIP, true)
        intent.putExtra(EXTRA_BACK_TEXT, null as String?)
        intent.putExtra(EXTRA_ENABLE_NEXT_ON_CONNECT, true)
        intent.putExtra(EXTRA_MODE_WIFI, true)
        SetupWizard.startActivityForResult(context, intent, REQUEST_CODE)
        return REQUEST_CODE
    }

    fun handleResult(context: Activity, resultCode: Int) {
        if (resultCode == Activity.RESULT_CANCELED) throw IllegalArgumentException()
        SetupWizard.next(context)
    }
}