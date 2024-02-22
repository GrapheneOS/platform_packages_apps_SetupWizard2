package app.grapheneos.setupwizard.view.activity

import android.content.Intent
import android.util.Log
import android.view.View
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.action.AppsActions
import app.grapheneos.setupwizard.action.SetupWizard

class AppsActivity : SetupWizardActivity(
    R.layout.activity_apps,
    R.drawable.apps_glif,
    R.string.install_apps,
    R.string.app_install_desc
) {
    companion object {
        private const val TAG = "AppsActivity"
    }

    private lateinit var skip: View
    private lateinit var next: View

    override fun bindViews() {
        skip = findViewById(R.id.skip)
        next = findViewById(R.id.next)
    }

    override fun setupActions() {
        skip.setOnClickListener { SetupWizard.next(this) }
        next.setOnClickListener { AppsActions.InstallApps(this) }
    }

    override fun onActivityResult(resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: $resultCode, data=$data")
        AppsActions.handleResult(this, resultCode)
        super.onActivityResult(resultCode, data)
    }
}
