package app.grapheneos.setupwizard.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.grapheneos.setupwizard.action.SecurityActions

object SecurityData : ViewModel() {

    val isDeviceSecure = MutableLiveData<Boolean>()

    init {
        SecurityActions
    }
}
