package app.grapheneos.setupwizard.action

import android.app.Activity
import android.content.Intent
import android.location.LocationManager
import android.os.UserHandle
import android.util.Log
import app.grapheneos.setupwizard.appContext
import app.grapheneos.setupwizard.data.LocationData

object MigrationActions {
    private const val TAG = "MigrationActions"
    private const val ACTION_SEEDVAULT_RESTORE = "com.stevesoltys.seedvault.RESTORE_BACKUP"
    private const val REQUEST_CODE = 201

    init {
    }

    fun launchMigration(context: Activity) {
        Log.d(TAG, "launchMigration")
        val intent = Intent(ACTION_SEEDVAULT_RESTORE)
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