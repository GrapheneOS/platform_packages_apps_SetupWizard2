package org.grapheneos.setupwizard.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.grapheneos.setupwizard.action.SecurityActions

object SecurityData : ViewModel() {
    init {
        SecurityActions
    }

    val isDeviceSecure = MutableLiveData<Boolean>()
}