package app.grapheneos.setupwizard.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.PersistableBundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.action.AppInstaller
import app.grapheneos.setupwizard.action.MdmInstallActions
import app.grapheneos.setupwizard.view.activity.MdmInstallActivity

class MdmInstallViewModel : ViewModel() {
    val spinnerVisible = MutableLiveData<Boolean>()
    val progressVisible = MutableLiveData<Boolean>()
    val message = MutableLiveData<String>()
    val downloadProgress = MutableLiveData<Int>()
    val downloadProgressLegend = MutableLiveData<String>()
    val error = MutableLiveData<String?>()
    val complete = MutableLiveData<Boolean>()

    var adminComponentName: String? = null
    var downloadLocation: String? = null
    var packageChecksum: String? = null
    var skipEncryption: Boolean = false
    var systemAppsEnabled: Boolean = false
    var extrasBundle: PersistableBundle? = null
    var calculatedPackageChecksum: String? = null

    private var started: Boolean = false

    val appInstallReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent!!.getIntExtra(PackageInstaller.EXTRA_STATUS, 0)
            when (status) {
                PackageInstaller.STATUS_SUCCESS -> {
                    spinnerVisible.postValue(false)
                    message.postValue(context?.getString(R.string.install_successful))
                    complete.postValue(true)
                }
                else -> {
                    val extraMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    val statusMessage = AppInstaller.getPackageInstallerStatusMessage(status)
                    var errorText = context?.getString(R.string.install_failed) + statusMessage
                    if (!extraMessage.isNullOrEmpty()) {
                        errorText += ", extra: $extraMessage"
                    }
                    error.postValue(errorText)
                }
            }
        }
    }

    fun start(activity: MdmInstallActivity, intent: Intent) {
        if (started) return
        started = true
        MdmInstallActions.handleEntry(activity, this, intent)
    }
}
