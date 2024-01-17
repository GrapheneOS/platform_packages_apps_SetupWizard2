package app.grapheneos.setupwizard.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.grapheneos.setupwizard.action.LocationActions
import app.grapheneos.setupwizard.action.WelcomeActions
import java.util.Locale

object LocationData : ViewModel() {
    val enabled = MutableLiveData<Boolean>()

    init {
        LocationActions
    }
}
