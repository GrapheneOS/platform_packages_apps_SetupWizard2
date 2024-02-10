package app.grapheneos.setupwizard.action

import android.app.Activity
import android.content.Intent
import android.util.Log
import app.grapheneos.setupwizard.view.activity.SetupWizardActivity

object GesturesActions {
    private const val TAG = "GesturesActions"
    private const val ACTION_LAUNCH_TUTORIAL = "com.android.quickstep.action.GESTURE_SANDBOX"

    init {
    }

    fun launchTutorial(context: SetupWizardActivity) {
        Log.d(TAG, "launchTutorial")
        val intent = Intent(ACTION_LAUNCH_TUTORIAL)
        SetupWizard.startActivityForResult(context, intent)
    }

    fun handleResult(context: Activity, resultCode: Int) {
        if (resultCode != Activity.RESULT_CANCELED) {
            SetupWizard.next(context)
        }
    }
}
