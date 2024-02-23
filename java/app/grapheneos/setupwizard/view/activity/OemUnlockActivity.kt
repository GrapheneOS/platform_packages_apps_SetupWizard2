package app.grapheneos.setupwizard.view.activity

import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.MainThread
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.action.OemUnlockActions
import app.grapheneos.setupwizard.data.OemUnlockData

class OemUnlockActivity :
    SetupWizardActivity(
        R.layout.activity_oem_unlock,
        R.drawable.baseline_warning_amber_orange_glif,
        R.string.lock_your_bootloader
    ) {
    companion object {
        private const val TAG = "OemUnlockActivity"
    }

    private lateinit var acknowledgeRisksContainer: View
    private lateinit var acknowledgeRisks: CheckBox
    private lateinit var continueUnlocked: TextView
    private lateinit var rebootToBootloader: TextView

    @MainThread
    override fun bindViews() {
        acknowledgeRisksContainer = requireViewById(R.id.acknowledge_risks_container)
        acknowledgeRisks = requireViewById(R.id.acknowledge_risks)
        continueUnlocked = requireViewById(R.id.continue_unlocked)
        rebootToBootloader = requireViewById(R.id.reboot_to_bootloader)
        OemUnlockData.ackTimer.observe(this) {
            updateContinueButton(it)
        }
        OemUnlockActions.startAckTimer()
    }

    @MainThread
    override fun setupActions() {
        acknowledgeRisksContainer.setOnClickListener {
            acknowledgeRisks.isChecked = !acknowledgeRisks.isChecked
        }
        acknowledgeRisks.setOnCheckedChangeListener { _, _ ->
            if (OemUnlockData.ackTimer.value!! > 0) return@setOnCheckedChangeListener
            updateContinueButton(0)
        }
        continueUnlocked.setOnClickListener { OemUnlockActions.next(this) }
        rebootToBootloader.setOnClickListener { OemUnlockActions.rebootToBootloader() }
    }

    private fun updateContinueButton(timer: Int) {
        val text = if (timer == 0) {
            getString(R.string.continue_without_locking)
        } else {
            getString(R.string.continue_without_locking_timer, timer)
        }
        continueUnlocked.text = text
        continueUnlocked.isEnabled = timer == 0 && acknowledgeRisks.isChecked
    }
}
