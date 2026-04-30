package app.grapheneos.setupwizard.view.activity

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
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
import app.grapheneos.setupwizard.android.ConsecutiveTapsGestureDetector
import app.grapheneos.setupwizard.data.WelcomeData
import app.grapheneos.setupwizard.utils.DebugFlags

// TODO: explore Material 3.0 with JetPack compose
class WelcomeActivity : SetupWizardActivity(R.layout.activity_welcome) {
    companion object {
        private const val TAG = "WelcomeActivity"
    }

    private lateinit var oemUnlockedContainer: View
    private lateinit var language: TextView
    private lateinit var accessibility: View
    private lateinit var letsSetupText: TextView
    private var consecutiveTapsGestureDetector: ConsecutiveTapsGestureDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (WizardManagerHelper.isUserSetupComplete(this)
                && DebugFlags.getBool("allowLaunchAfterSetupCompleted") != true) {
            superOnCreateAtBaseClass(savedInstanceState)
            FinishActions.finish(this)
            return
        }
        WelcomeActions.handleEntry(this)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        consecutiveTapsGestureDetector?.resetCounter()
    }

    @MainThread
    override fun bindViews() {
        oemUnlockedContainer = requireViewById(R.id.oem_unlocked_container)
        language = requireViewById(R.id.language)
        accessibility = requireViewById(R.id.accessibility)
        letsSetupText = requireViewById(R.id.lets_setup_text)
        letsSetupText.setText(
            if (SetupWizard.isPrimaryUser) R.string.lets_setup_your_device
            else R.string.lets_setup_your_profile
        )
        secondaryButton.setText(this, R.string.emergency_call)
        WelcomeData.selectedLanguage.observe(this) {
            Log.d(TAG, "selectedLanguage: ${it.displayName}")
            this.language.text = it.displayName
        }
        WelcomeData.oemUnlocked.observe(this) {
            Log.d(TAG, "oemUnlocked: $it")
            oemUnlockedContainer.visibility = if (it) View.VISIBLE else View.GONE
        }
        consecutiveTapsGestureDetector = ConsecutiveTapsGestureDetector(
            onConsecutiveTapsListener,
            requireViewById(R.id.root_layout)
        )
    }

    @MainThread
    override fun setupActions() {
        language.setOnClickListener { WelcomeActions.showLanguagePicker(this) }
        accessibility.setOnClickListener { WelcomeActions.accessibilitySettings(this) }
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CALLING)) {
            secondaryButton.setOnClickListener { WelcomeActions.emergencyCall(this) }
        } else {
            secondaryButton.visibility = View.GONE
        }
        primaryButton.setOnClickListener { WelcomeActions.next(this) }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val handled = super.dispatchTouchEvent(ev)
        if (ev.action == MotionEvent.ACTION_UP) {
            consecutiveTapsGestureDetector?.onTouchEvent(ev)
        }
        return handled
    }

    private val onConsecutiveTapsListener =
        object : ConsecutiveTapsGestureDetector.OnConsecutiveTapsListener {
            override fun onConsecutiveTaps(welcomeTapCounter: Int) {
                WelcomeActions.handleConsecutiveTap(welcomeTapCounter, this@WelcomeActivity)
            }
        }
}
