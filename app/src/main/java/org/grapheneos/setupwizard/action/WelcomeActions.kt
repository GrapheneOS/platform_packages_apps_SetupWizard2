package org.grapheneos.setupwizard.action

import org.grapheneos.setupwizard.data.WelcomeData

object WelcomeActions {
    init {
        // TODO: ASYNC fetch the list of languages from system
        WelcomeData.languages = arrayOf()
    }

    fun changeLanguage() {}
    fun accessibilitySettings() {}
    fun emergencyCall() {}
}