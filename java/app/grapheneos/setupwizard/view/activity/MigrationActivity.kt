package app.grapheneos.setupwizard.view.activity

import android.content.Intent
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!MigrationActions.isRestoreAvailable()) {
            Log.d(TAG, "onCreate: skipping, backup service is inactive")
            finish()
            SetupWizard.next(this)
        }
    }

    override fun bindViews() {
        primaryButton.setText(this, R.string.restore)
    }

    override fun setupActions() {
        secondaryButton.setOnClickListener { SetupWizard.next(this) }
        primaryButton.setOnClickListener { MigrationActions.launchMigration(this) }
    }

    override fun onActivityResult(resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: $resultCode, data=$data")
        MigrationActions.handleResult(this, resultCode)
        super.onActivityResult(resultCode, data)
    }
}
