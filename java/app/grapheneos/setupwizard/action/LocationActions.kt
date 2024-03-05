package app.grapheneos.setupwizard.action

import android.location.LocationManager
import android.util.Log
import app.grapheneos.setupwizard.appContext
import app.grapheneos.setupwizard.data.LocationData

object LocationActions {
    private const val TAG = "LocationActions"

    init {
        refreshCurrentState()
    }

    fun setEnabled(enabled: Boolean) {
        Log.d(TAG, "setEnabled: $enabled")
        getLocationManager().setLocationEnabledForUser(enabled, appContext.user)
        refreshCurrentState()
    }

    private fun refreshCurrentState() {
        LocationData.enabled.value = getLocationManager().isLocationEnabled
        Log.d(TAG, "refreshCurrentState: enabled = ${LocationData.enabled.value}")
    }

    private fun getLocationManager(): LocationManager {
        return appContext.getSystemService(LocationManager::class.java)!!
    }

}
