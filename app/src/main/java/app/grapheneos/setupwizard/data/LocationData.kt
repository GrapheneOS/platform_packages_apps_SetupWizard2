package app.grapheneos.setupwizard.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.grapheneos.setupwizard.action.LocationActions

object LocationData : ViewModel() {
    val enabled = MutableLiveData<Boolean>()

    init {
        LocationActions
    }
}
