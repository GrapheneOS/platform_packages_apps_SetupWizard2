package org.grapheneos.setupwizard.view.activity

import android.content.Intent
import android.util.Log
import android.view.View
import org.grapheneos.setupwizard.R
import org.grapheneos.setupwizard.action.SecurityActions
import org.grapheneos.setupwizard.action.SetupWizard

class SecurityActivity : SetupWizardActivity(R.layout.activity_security) {
    private lateinit var skip: View
    private lateinit var setup: View

    companion object {
        private const val TAG = "SecurityActivity"
    }

    override fun bindViews() {
        skip = findViewById(R.id.skip)
        setup = findViewById(R.id.setup)
    }

    override fun setupActions() {
        skip.setOnClickListener { SetupWizard.next(this) }
        setup.setOnClickListener { SecurityActions.launchSetup(this) }
    }

    @Deprecated("Deprecated in Java") // TODO: use the new interface
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: $resultCode, data=$data")
        SecurityActions.handleResult(this, requestCode, resultCode)
        super.onActivityResult(requestCode, resultCode, data)
    }
}