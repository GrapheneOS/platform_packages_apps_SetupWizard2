package app.grapheneos.setupwizard.action

import android.ext.settings.GeocoderSettings.GEOCODER_DISABLED
import android.ext.settings.GeocoderSettings.GEOCODER_SERVER_GRAPHENEOS
import android.ext.settings.GeocoderSettings.GEOCODER_SETTING
import android.ext.settings.NetworkLocationSettings.NETWORK_LOCATION_DISABLED
import android.ext.settings.NetworkLocationSettings.NETWORK_LOCATION_GRAPHENEOS_APPLE_PROXY
import android.ext.settings.NetworkLocationSettings.NETWORK_LOCATION_SETTING
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.util.Log
import app.grapheneos.setupwizard.appContext
import app.grapheneos.setupwizard.data.LocationData

object LocationActions {
    private const val TAG = "LocationActions"

    init {
        refreshCurrentState()
    }

    fun setLocationEnabled(enabled: Boolean) {
        Log.d(TAG, "setLocationEnabled: $enabled")
        getLocationManager().setLocationEnabledForUser(enabled, appContext.user)
        refreshCurrentState()
    }

    fun setNetworkLocationEnabled(enabled: Boolean) {
        Log.d(TAG, "setNetworkLocationEnabled: $enabled")
        NETWORK_LOCATION_SETTING.put(
            appContext,
            if (enabled) NETWORK_LOCATION_GRAPHENEOS_APPLE_PROXY else NETWORK_LOCATION_DISABLED
        )
        refreshCurrentState()
    }

    fun setWifiScanningAlwaysAvailableEnabled(enabled: Boolean) {
        Log.d(TAG, "setWifiScanningAlwaysAvailableEnabled: $enabled")
        getWifiManager().isScanAlwaysAvailable = enabled
        refreshCurrentState()
    }

    fun setGeocoderEnabled(enabled: Boolean) {
        Log.d(TAG, "setGeocoderEnabled: $enabled")
        GEOCODER_SETTING.put(
            appContext,
            if (enabled) GEOCODER_SERVER_GRAPHENEOS else GEOCODER_DISABLED
        )
        refreshCurrentState()
    }

    private fun refreshCurrentState() {
        LocationData.locationEnabled.value = getLocationManager().isLocationEnabled
        Log.d(TAG, "refreshCurrentState: locationEnabled = ${LocationData.locationEnabled.value}")

        LocationData.networkLocationEnabled.value =
            NETWORK_LOCATION_SETTING.get(appContext) != NETWORK_LOCATION_DISABLED
        Log.d(
            TAG,
            "refreshCurrentState: networkLocationEnabled = ${LocationData.networkLocationEnabled.value}"
        )

        @Suppress("DEPRECATION") // WifiManager.isScanAlwaysAvailable is not deprecated for platform apps
        LocationData.wifiScanningAlwaysAvailableEnabled.value = getWifiManager().isScanAlwaysAvailable
        Log.d(
            TAG,
            "refreshCurrentState: wifiScanningAlwaysAvailableEnabled = ${LocationData.wifiScanningAlwaysAvailableEnabled.value}"
        )

        LocationData.geocoderEnabled.value =
            GEOCODER_SETTING.get(appContext) != GEOCODER_DISABLED
        Log.d(
            TAG,
            "refreshCurrentState: geocoderEnabled = ${LocationData.geocoderEnabled.value}"
        )
    }

    private fun getLocationManager(): LocationManager {
        return appContext.getSystemService(LocationManager::class.java)!!
    }

    private fun getWifiManager(): WifiManager {
        return appContext.getSystemService(WifiManager::class.java)!!
    }

}
