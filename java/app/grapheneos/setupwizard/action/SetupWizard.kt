package app.grapheneos.setupwizard.action

import android.app.Activity
import android.app.ActivityOptions
import android.app.StatusBarManager
import android.content.Intent
import android.os.UserManager
import androidx.annotation.StyleRes
import com.google.android.setupcompat.partnerconfig.PartnerConfigHelper
import com.google.android.setupcompat.util.WizardManagerHelper
import com.google.android.setupdesign.R
import com.google.android.setupdesign.util.ThemeHelper
import app.grapheneos.setupwizard.appContext
import app.grapheneos.setupwizard.view.activity.DateTimeActivity
import app.grapheneos.setupwizard.view.activity.FinishActivity
import app.grapheneos.setupwizard.view.activity.GesturesActivity
import app.grapheneos.setupwizard.view.activity.LocationActivity
import app.grapheneos.setupwizard.view.activity.MigrationActivity
import app.grapheneos.setupwizard.view.activity.SecurityActivity
import app.grapheneos.setupwizard.view.activity.SetupWizardActivity
import app.grapheneos.setupwizard.view.activity.WelcomeActivity
import app.grapheneos.setupwizard.view.activity.WifiActivity

object SetupWizard {

    // it is assumed that activities are unrelated from each other
    // which means current activity has no info to pass to next activity
    // which means the launching of next activity will be a pure function
    private val primaryUserActivities = listOf<Class<out Activity>>(
        WelcomeActivity::class.java,
        WifiActivity::class.java,
        DateTimeActivity::class.java,
        LocationActivity::class.java,
        SecurityActivity::class.java,
        MigrationActivity::class.java,
        GesturesActivity::class.java,
        FinishActivity::class.java
    )
    private val secondaryUserActivities = listOf<Class<out Activity>>(
        WelcomeActivity::class.java,
        LocationActivity::class.java,
        SecurityActivity::class.java,
        MigrationActivity::class.java,
        FinishActivity::class.java
    )

    // launch next step
    fun next(current: Activity) {
        val activities = if (isPrimaryUser) primaryUserActivities else secondaryUserActivities
        val index = activities.indexOf(current.javaClass)
        if (index == -1) throw IllegalArgumentException("unknown current step")
        if (index + 1 == activities.size) throw IllegalArgumentException("no more steps")
        val intent = Intent(current, activities[index + 1])
        val options = ActivityOptions.makeCustomAnimation(
            current,
            R.anim.sud_slide_next_in,
            R.anim.sud_slide_next_out
        ).toBundle()
        current.startActivity(intent, options)
    }

    //////////////////////////////// common actions /////////////////////////

    private val statusBarManager = appContext.getSystemService(StatusBarManager::class.java)!!

    fun setStatusBarHidden(hidden: Boolean) {
        statusBarManager.setDisabledForSetup(hidden)
    }

    fun startActivity(context: Activity, intent: Intent) {
        prepareIntent(intent)
        context.startActivity(intent)
    }

    fun startActivityForResult(context: SetupWizardActivity, intent: Intent) {
        prepareIntent(intent)
        context.startActivityForResult(intent)
    }

    /**
     * Commons stuffs for launching an activity.
     */
    private fun prepareIntent(intent: Intent) {
        intent.putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true) // not needed on >= Q
        intent.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true)
        intent.putExtra(WizardManagerHelper.EXTRA_THEME, getDefaultThemeName())
    }

    @StyleRes
    fun getDefaultTheme(): Int {
        return ThemeHelper.getSuwDefaultTheme(appContext)
    }

    private fun getDefaultThemeName(): String? {
        return PartnerConfigHelper.getSuwDefaultThemeString(appContext)
    }

    val isPrimaryUser: Boolean by lazy {
        appContext.getSystemService(UserManager::class.java)!!.isSystemUser
    }
}
