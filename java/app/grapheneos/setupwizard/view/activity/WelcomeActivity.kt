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
        language = findViewById(R.id.language)
        accessibility = findViewById(R.id.accessibility)
        emergency = findViewById(R.id.emergency)
        next = findViewById(R.id.next)
        letsSetupText = findViewById(R.id.lets_setup_text)
        letsSetupText.setText(
            if (SetupWizard.isPrimaryUser) R.string.lets_setup_your_device
            else R.string.lets_setup_your_profile
        )
        WelcomeData.selectedLanguage.observe(this) {
            Log.d(TAG, "selectedLanguage: ${it.displayName}")
            this.language.text = it.displayName
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
        next.setOnClickListener { SetupWizard.next(this) }
    }
}
