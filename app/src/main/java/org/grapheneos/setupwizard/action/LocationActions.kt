package org.grapheneos.setupwizard.action

import android.location.LocationManager
import android.os.UserHandle
import android.util.Log
import org.grapheneos.setupwizard.appContext
import org.grapheneos.setupwizard.data.LocationData

object LocationActions {
    private const val TAG = "LocationActions"

    init {
        refreshCurrentState()
    }

    fun setEnabled(enabled: Boolean) {
        Log.d(TAG, "setEnabled: $enabled")
        getLocationManager().setLocationEnabledForUser(enabled, UserHandle.CURRENT)
        refreshCurrentState()
    }

    private fun refreshCurrentState() {
        LocationData.enabled.value = getLocationManager().isLocationEnabled
        Log.d(TAG, "refreshCurrentState: enabled = ${LocationData.enabled.value}")
    }

    private fun getLocationManager(): LocationManager {
        return appContext.getSystemService(LocationManager::class.java)
    }

}