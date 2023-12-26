package org.grapheneos.setupwizard.view.activity

import android.view.View
import android.widget.CheckBox
import org.grapheneos.setupwizard.R
import org.grapheneos.setupwizard.action.LocationActions
import org.grapheneos.setupwizard.action.SetupWizard
import org.grapheneos.setupwizard.data.LocationData

class LocationActivity : SetupWizardActivity(R.layout.activity_location) {
    private lateinit var enabled: CheckBox
    private lateinit var next: View

    override fun bindViews() {
        enabled = findViewById(R.id.enabled)
        next = findViewById(R.id.next)
        LocationData.enabled.observe(this) { enabled.isChecked = it }
    }

    override fun setupActions() {
        enabled.setOnClickListener { LocationActions.setEnabled(enabled.isChecked) }
        next.setOnClickListener { SetupWizard.next(this) }
    }
}