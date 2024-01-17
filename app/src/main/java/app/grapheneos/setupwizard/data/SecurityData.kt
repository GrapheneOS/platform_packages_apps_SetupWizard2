package app.grapheneos.setupwizard.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.grapheneos.setupwizard.action.SecurityActions

object SecurityData : ViewModel() {
    init {
        SecurityActions
    }

    val isDeviceSecure = MutableLiveData<Boolean>()
}