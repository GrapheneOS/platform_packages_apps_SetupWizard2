package app.grapheneos.setupwizard.view.activity

import android.view.View
import android.widget.CheckBox
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.action.LocationActions
import app.grapheneos.setupwizard.action.SetupWizard
import app.grapheneos.setupwizard.data.LocationData

class LocationActivity : SetupWizardActivity(
    R.layout.activity_location,
    R.drawable.baseline_location_on_glif,
    R.string.location_services
) {
    private lateinit var enabled: CheckBox
    private lateinit var next: View

    override fun bindViews() {
        enabled = requireViewById(R.id.enabled)
        next = requireViewById(R.id.next)
        LocationData.enabled.observe(this) { enabled.isChecked = it }
    }

    override fun setupActions() {
        requireViewById<View>(R.id.enabled_container).setOnClickListener {
            LocationActions.setEnabled(!enabled.isChecked)
        }
        enabled.setOnClickListener { LocationActions.setEnabled(enabled.isChecked) }
        next.setOnClickListener { SetupWizard.next(this) }
    }
}
