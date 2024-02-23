package app.grapheneos.setupwizard.view.activity

import android.view.View
import android.widget.CheckBox
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.action.FinishActions
import app.grapheneos.setupwizard.action.SetupWizard.isPrimaryUser
import app.grapheneos.setupwizard.data.FinishData
import app.grapheneos.setupwizard.data.FinishData.disableOemUnlockingVisible

class FinishActivity : SetupWizardActivity(
    R.layout.activity_finish,
    R.drawable.baseline_done_all_glif,
    R.string.you_re_all_set_now,
    if (isPrimaryUser) R.string.device_setup_done_desc else R.string.profile_setup_done_desc
) {
    private lateinit var finish: View
    private lateinit var disableOemUnlockingContainer: View
    private lateinit var disableOemUnlocking: CheckBox

    override fun bindViews() {
        finish = requireViewById(R.id.finish)
        disableOemUnlockingContainer = requireViewById(R.id.disable_oem_unlocking_container)
        disableOemUnlocking = requireViewById(R.id.disable_oem_unlocking)
        if (disableOemUnlockingVisible) {
            disableOemUnlocking.isChecked = FinishData.disableOemUnlockingChecked
            FinishData.oemUnlockingEnabled.observe(this) {
                disableOemUnlockingContainer.visibility = if (it) View.VISIBLE else View.GONE
            }
        } else {
            disableOemUnlockingContainer.visibility = View.GONE
            disableOemUnlocking.isChecked = false
        }
    }

    override fun setupActions() {
        disableOemUnlockingContainer.setOnClickListener {
            disableOemUnlocking.isChecked = !disableOemUnlocking.isChecked
        }
        finish.setOnClickListener {
            val disableOemUnlocking = disableOemUnlockingVisible && disableOemUnlocking.isChecked
            FinishActions.finish(this, disableOemUnlocking)
        }
    }
}
