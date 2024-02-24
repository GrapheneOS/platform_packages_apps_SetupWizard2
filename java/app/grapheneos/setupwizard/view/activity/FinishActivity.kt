package app.grapheneos.setupwizard.view.activity

import android.view.View
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.action.FinishActions
import app.grapheneos.setupwizard.action.SetupWizard.isPrimaryUser

class FinishActivity : SetupWizardActivity(
    R.layout.activity_finish,
    R.drawable.baseline_done_all_glif,
    R.string.you_re_all_set_now,
    if (isPrimaryUser) R.string.device_setup_done_desc else R.string.profile_setup_done_desc
) {
    private lateinit var finish: View

    override fun bindViews() {
        finish = findViewById(R.id.finish)
    }

    override fun setupActions() {
        finish.setOnClickListener { FinishActions.finish(this) }
    }
}
