package app.grapheneos.setupwizard.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.grapheneos.setupwizard.action.LocationActions

object LocationData : ViewModel() {
    val locationEnabled = MutableLiveData<Boolean>()
    val networkLocationEnabled = MutableLiveData<Boolean>()
    val wifiScanningAlwaysAvailableEnabled = MutableLiveData<Boolean>()

    init {
        LocationActions
    }
}
