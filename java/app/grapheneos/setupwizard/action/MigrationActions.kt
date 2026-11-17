package app.grapheneos.setupwizard.action

import android.app.Activity
import android.app.backup.BackupManager
import android.content.Intent
import android.os.Process
import android.util.Log
import app.grapheneos.setupwizard.appContext
import app.grapheneos.setupwizard.view.activity.SetupWizardActivity

object MigrationActions {
    private const val TAG = "MigrationActions"
    private const val ACTION_SEEDVAULT_RESTORE = "com.stevesoltys.seedvault.RESTORE_BACKUP"

    init {
    }

    // a device owner deactivates backup
    fun isRestoreAvailable(): Boolean {
        return BackupManager(appContext).isBackupServiceActive(Process.myUserHandle())
    }

    fun launchMigration(context: SetupWizardActivity) {
        Log.d(TAG, "launchMigration")
        val intent = Intent(ACTION_SEEDVAULT_RESTORE)
        SetupWizard.startActivityForResult(context, intent)
    }

    fun handleResult(context: Activity, resultCode: Int) {
        if (resultCode != Activity.RESULT_CANCELED) {
            SetupWizard.next(context)
        }
    }
}
