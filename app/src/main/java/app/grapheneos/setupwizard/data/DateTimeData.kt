package app.grapheneos.setupwizard.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.grapheneos.setupwizard.action.DateTimeActions

object DateTimeData : ViewModel() {
    val date = MutableLiveData<String>()
    val time = MutableLiveData<String>()
    val timeZone = MutableLiveData<String>()

    init {
        DateTimeActions
    }
}
