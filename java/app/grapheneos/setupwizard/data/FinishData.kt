package app.grapheneos.setupwizard.data

import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.grapheneos.setupwizard.action.FinishActions
import app.grapheneos.setupwizard.action.SetupWizard

object FinishData : ViewModel() {
    val oemUnlockingEnabled = MutableLiveData<Boolean>() // bootloader unlocking is allowed

    // whether the "Disable OEM Unlocking" checkbox should be checked by default
    val disableOemUnlockingChecked = !Build.isDebuggable()

    // whether "Disable OEM Unlocking" checkbox should be visible
    //     visible if bootloader is locked and user is primary
    val disableOemUnlockingVisible =
        WelcomeData.oemUnlocked.value?.not() ?: false && SetupWizard.isPrimaryUser

    init {
        FinishActions
    }
}
