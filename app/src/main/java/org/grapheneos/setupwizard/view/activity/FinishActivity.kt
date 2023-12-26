package org.grapheneos.setupwizard.view.activity

import android.view.View
import org.grapheneos.setupwizard.R
import org.grapheneos.setupwizard.action.FinishActions

class FinishActivity : SetupWizardActivity(R.layout.activity_finish) {
    private lateinit var finish: View

    override fun bindViews() {
        finish = findViewById(R.id.finish)
    }

    override fun setupActions() {
        finish.setOnClickListener { FinishActions.finish(this) }
    }
}