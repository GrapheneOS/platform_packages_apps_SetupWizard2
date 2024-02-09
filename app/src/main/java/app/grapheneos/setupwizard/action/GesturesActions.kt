package app.grapheneos.setupwizard.action

import android.app.Activity
import android.content.Intent
import android.util.Log

object GesturesActions {
    private const val TAG = "GesturesActions"
    private const val ACTION_LAUNCH_TUTORIAL = "com.android.quickstep.action.GESTURE_SANDBOX"
    private const val REQUEST_CODE = 501

    init {
    }

    fun launchTutorial(context: Activity) {
        Log.d(TAG, "launchTutorial")
        val intent = Intent(ACTION_LAUNCH_TUTORIAL)
        SetupWizard.startActivityForResult(context, intent, REQUEST_CODE)
    }

    fun handleResult(context: Activity, requestCode: Int, resultCode: Int) {
        if (requestCode != REQUEST_CODE) {
            return
        }
        if (resultCode != Activity.RESULT_CANCELED) {
            SetupWizard.next(context)
        }
    }
}