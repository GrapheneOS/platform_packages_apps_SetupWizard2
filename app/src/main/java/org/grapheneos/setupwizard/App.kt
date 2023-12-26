package org.grapheneos.setupwizard

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
    }
}