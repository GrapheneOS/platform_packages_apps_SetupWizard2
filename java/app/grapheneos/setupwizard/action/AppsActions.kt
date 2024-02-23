package app.grapheneos.setupwizard.action

import android.app.Activity
import android.content.Intent
import android.util.Log
import app.grapheneos.setupwizard.view.activity.SetupWizardActivity

object AppsActions {
    private const val TAG = "AppsActions"
    private const val ACTION_APP_INSTALL = "app.grapheneos.apps.MAIN"

    init {
    }

    fun InstallApps(context: SetupWizardActivity) {
        Log.d(TAG, "launchAppInstaller")
        val intent = Intent(ACTION_APP_INSTALL)
        intent.putExtra("SuW", true)
        SetupWizard.startActivityForResult(context, intent)
    }

    fun handleResult(context: Activity, resultCode: Int) {
        if (resultCode != Activity.RESULT_CANCELED) {
            SetupWizard.next(context)
        }
    }
}
