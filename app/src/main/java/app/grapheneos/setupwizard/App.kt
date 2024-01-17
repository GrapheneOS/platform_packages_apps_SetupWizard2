package app.grapheneos.setupwizard

import android.app.Application
import com.google.android.material.color.DynamicColors

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}