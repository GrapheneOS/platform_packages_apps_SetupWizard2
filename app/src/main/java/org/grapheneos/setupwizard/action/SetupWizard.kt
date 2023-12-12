package org.grapheneos.setupwizard.action

import android.app.Activity
import android.content.Intent
import org.grapheneos.setupwizard.view.activity.WelcomeActivity
import java.lang.IllegalArgumentException

object SetupWizard {

    // it is assumed that activities are unrelated from each other
    // which means current activity has no info to pass to next activity
    // which means the launching of next activity will be a pure function
    private val activities = listOf<Class<out Activity>>(
        WelcomeActivity::class.java
    )

    // launch next step
    fun next(current: Activity) {
        val index = activities.indexOf(current.javaClass)
        if (index == -1) throw IllegalArgumentException("unknown current step")
        if (index + 1 == activities.size) throw IllegalArgumentException("no more steps")
        val intent = Intent(current, activities[index + 1])
        current.startActivity(intent)
    }
}