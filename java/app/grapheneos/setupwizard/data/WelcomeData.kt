package app.grapheneos.setupwizard.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.grapheneos.setupwizard.action.WelcomeActions
import java.util.Locale

object WelcomeData : ViewModel() {
    val selectedLanguage = MutableLiveData<Locale>()
    val oemUnlocked = MutableLiveData<Boolean>() // bootloader unlocked

    init {
        WelcomeActions
    }
}
