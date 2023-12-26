package org.grapheneos.setupwizard.view.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.MainThread
import org.grapheneos.setupwizard.R
import org.grapheneos.setupwizard.action.SetupWizard
import org.grapheneos.setupwizard.action.WelcomeActions
import org.grapheneos.setupwizard.data.WelcomeData

// TODO: explore Material 3.0 with JetPack compose
class WelcomeActivity : SetupWizardActivity(R.layout.activity_welcome) {
    companion object {
        private const val TAG = "WelcomeActivity"
    }

    private lateinit var language: View
    private lateinit var languageText: TextView
    private lateinit var accessibility: View
    private lateinit var emergency: View
    private lateinit var next: View

    override fun onCreate(savedInstanceState: Bundle?) {
        WelcomeActions.handleEntry(this)
        super.onCreate(savedInstanceState)
    }

    @MainThread
    override fun bindViews() {
        language = findViewById(R.id.language)
        languageText = findViewById(R.id.languageText)
        accessibility = findViewById(R.id.accessibility)
        emergency = findViewById(R.id.emergency)
        next = findViewById(R.id.next)
        WelcomeData.selectedLanguage.observe(this) {
            Log.d(TAG, "selectedLanguage: ${it.displayName}")
            languageText.text = it.displayName
        }
    }

    @MainThread
    override fun setupActions() {
        language.setOnClickListener { WelcomeActions.showLanguagePicker(this) }
        accessibility.setOnClickListener { WelcomeActions.accessibilitySettings(this) }
        emergency.setOnClickListener { WelcomeActions.emergencyCall(this) }
        next.setOnClickListener { SetupWizard.next(this) }
    }
}