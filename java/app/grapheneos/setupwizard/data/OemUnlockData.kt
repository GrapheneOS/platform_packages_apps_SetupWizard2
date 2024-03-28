package app.grapheneos.setupwizard.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.grapheneos.setupwizard.OEM_UNLOCKED_ACK_TIMER
import app.grapheneos.setupwizard.action.OemUnlockActions

object OemUnlockData : ViewModel() {
    val ackTimer = MutableLiveData(OEM_UNLOCKED_ACK_TIMER)

    init {
        OemUnlockActions
    }
}
