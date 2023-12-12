package org.grapheneos.setupwizard.view.activity

import android.view.View
import androidx.annotation.MainThread
import org.grapheneos.setupwizard.R
import org.grapheneos.setupwizard.action.SetupWizard
import org.grapheneos.setupwizard.action.WelcomeActions

// TODO: explore Material 3.0 with JetPack compose
class WelcomeActivity : SetupWizardActivity(R.layout.activity_welcome) {

    // TODO: probably use view binders
    private lateinit var language: View // TODO: should be a Spinner
    private lateinit var accessibility: View
    private lateinit var emergency: View
    private lateinit var next: View

    @MainThread
    override fun bindViews() {
        language = findViewById(R.id.language)
        accessibility = findViewById(R.id.accessibility)
        emergency = findViewById(R.id.emergency)
        next = findViewById(R.id.next)
        // TODO: attach data change listeners
    }

    @MainThread
    override fun setupActions() {
        language.setOnClickListener { WelcomeActions.changeLanguage() }
        accessibility.setOnClickListener { WelcomeActions.accessibilitySettings() }
        emergency.setOnClickListener { WelcomeActions.emergencyCall() }
        next.setOnClickListener { SetupWizard.next(this) }
    }
}