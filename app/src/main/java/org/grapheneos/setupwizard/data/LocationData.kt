package org.grapheneos.setupwizard.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.grapheneos.setupwizard.action.LocationActions
import org.grapheneos.setupwizard.action.WelcomeActions
import java.util.Locale

object LocationData : ViewModel() {
    val enabled = MutableLiveData<Boolean>()

    init {
        LocationActions
    }
}
