package app.grapheneos.setupwizard.view.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.View
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.action.MigrationActions
import app.grapheneos.setupwizard.action.SetupWizard

class MigrationActivity : SetupWizardActivity(
    R.layout.activity_migration,
    R.drawable.baseline_restore_glif,
    R.string.restore_apps_and_data,
    R.string.data_restore_desc
) {
    companion object {
        private const val TAG = "MigrationActivity"
        private const val ACTION_BACKUP_APP_RESTORE = "com.stevesoltys.seedvault.RESTORE_BACKUP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(ACTION_BACKUP_APP_RESTORE)
        if (intent.resolveActivity(packageManager) == null) {
            Log.d(TAG, "backup app intent cannot be resolved")
            hidebutton()
        }
    }

    private lateinit var skip: View
    private lateinit var next: View

    fun hidebutton() {
        next.background.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)
        next.isClickable = false
        next.setText("Backup app unavailable")
    }

    override fun bindViews() {
        skip = findViewById(R.id.skip)
        next = findViewById(R.id.next)
    }

    override fun setupActions() {
        skip.setOnClickListener { SetupWizard.next(this) }
        next.setOnClickListener { MigrationActions.launchMigration(this) }
    }

    override fun onActivityResult(resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: $resultCode, data=$data")
        MigrationActions.handleResult(this, resultCode)
        super.onActivityResult(resultCode, data)
    }
}
