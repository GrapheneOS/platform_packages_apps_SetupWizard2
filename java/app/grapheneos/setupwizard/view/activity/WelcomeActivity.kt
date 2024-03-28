package app.grapheneos.setupwizard.view.activity

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.MainThread

import com.google.android.setupcompat.template.FooterButtonStyleUtils
import com.google.android.setupcompat.util.WizardManagerHelper
import com.google.android.setupdesign.GlifLayout

import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.action.FinishActions
import app.grapheneos.setupwizard.action.SetupWizard
import app.grapheneos.setupwizard.action.WelcomeActions
import app.grapheneos.setupwizard.data.WelcomeData

// TODO: explore Material 3.0 with JetPack compose
class WelcomeActivity : SetupWizardActivity(R.layout.activity_welcome) {
    companion object {
        private const val TAG = "WelcomeActivity"
    }

    private lateinit var oemUnlockedContainer: View
    private lateinit var language: TextView
    private lateinit var accessibility: View
    private lateinit var emergency: View
    private lateinit var next: View
    private lateinit var letsSetupText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        if (WizardManagerHelper.isUserSetupComplete(this)) {
            superOnCreateAtBaseClass(savedInstanceState)
            FinishActions.finish(this)
            return
        }
        WelcomeActions.handleEntry(this)
        super.onCreate(savedInstanceState)
    }

    @MainThread
    override fun bindViews() {
        oemUnlockedContainer = requireViewById(R.id.oem_unlocked_container)
        language = requireViewById(R.id.language)
        accessibility = requireViewById(R.id.accessibility)
        emergency = requireViewById(R.id.emergency)
        next = requireViewById(R.id.next)
        letsSetupText = requireViewById(R.id.lets_setup_text)
        letsSetupText.setText(
            if (SetupWizard.isPrimaryUser) R.string.lets_setup_your_device
            else R.string.lets_setup_your_profile
        )
        WelcomeData.selectedLanguage.observe(this) {
            Log.d(TAG, "selectedLanguage: ${it.displayName}")
            this.language.text = it.displayName
        }
        WelcomeData.oemUnlocked.observe(this) {
            Log.d(TAG, "oemUnlocked: $it")
            oemUnlockedContainer.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    @MainThread
    override fun setupActions() {
        language.setOnClickListener { WelcomeActions.showLanguagePicker(this) }
        accessibility.setOnClickListener { WelcomeActions.accessibilitySettings(this) }
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CALLING)) {
            emergency.setOnClickListener { WelcomeActions.emergencyCall(this) }
        } else {
            emergency.visibility = View.GONE
        }
        next.setOnClickListener { WelcomeActions.next(this) }
    }
}
