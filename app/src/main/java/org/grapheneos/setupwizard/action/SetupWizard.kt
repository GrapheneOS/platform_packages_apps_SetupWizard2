package org.grapheneos.setupwizard.action

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.StyleRes
import com.google.android.setupcompat.partnerconfig.PartnerConfigHelper
import com.google.android.setupcompat.util.WizardManagerHelper
import com.google.android.setupdesign.util.PartnerStyleHelper
import com.google.android.setupdesign.util.ThemeHelper
import org.grapheneos.setupwizard.appContext
import org.grapheneos.setupwizard.view.activity.DateTimeActivity
import org.grapheneos.setupwizard.view.activity.MigrationActivity
import org.grapheneos.setupwizard.view.activity.FinishActivity
import org.grapheneos.setupwizard.view.activity.LocationActivity
import org.grapheneos.setupwizard.view.activity.SecurityActivity
import org.grapheneos.setupwizard.view.activity.WelcomeActivity
import java.lang.IllegalArgumentException

object SetupWizard {

    // it is assumed that activities are unrelated from each other
    // which means current activity has no info to pass to next activity
    // which means the launching of next activity will be a pure function
    private val activities = listOf<Class<out Activity>>(
        WelcomeActivity::class.java,
        DateTimeActivity::class.java,
        LocationActivity::class.java,
        SecurityActivity::class.java,
        MigrationActivity::class.java,
        FinishActivity::class.java
    )

    // launch next step
    fun next(current: Activity) {
        val index = activities.indexOf(current.javaClass)
        if (index == -1) throw IllegalArgumentException("unknown current step")
        if (index + 1 == activities.size) throw IllegalArgumentException("no more steps")
        val intent = Intent(current, activities[index + 1])
        current.startActivity(intent)
    }

    //////////////////////////////// common actions /////////////////////////

    private val statusBarManager = appContext.getSystemService(StatusBarManager::class.java)

    fun setStatusBarHidden(hidden: Boolean) {
        statusBarManager.setDisabledForSetup(hidden)
    }

    fun startActivity(context: Activity, intent: Intent) {
        prepareIntent(context, intent)
        context.startActivity(intent)
    }

    fun startActivityForResult(context: Activity, intent: Intent, requestCode: Int) {
        prepareIntent(context, intent)
        context.startActivityForResult(intent, requestCode)
    }

    /**
     * Commons stuffs for launching an activity.
     */
    private fun prepareIntent(context: Context, intent: Intent) {
        intent.putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true)
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
}