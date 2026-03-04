package app.grapheneos.setupwizard.view.activity

import android.view.View
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.action.LocationActions
import app.grapheneos.setupwizard.action.SetupWizard
import app.grapheneos.setupwizard.data.LocationData
import com.google.android.setupdesign.GlifRecyclerLayout
import com.google.android.setupdesign.items.ItemGroup
import com.google.android.setupdesign.items.RecyclerItemAdapter
import com.google.android.setupdesign.items.SwitchItem

class LocationActivity : SetupWizardActivity(
    R.layout.activity_location,
    R.drawable.baseline_location_on_glif,
    R.string.location_services
) {
    private lateinit var locationEnabled: SwitchItem
    // only initialized when in primary user
    private lateinit var networkLocationEnabled: SwitchItem
    // only initialized when in primary user
    private lateinit var wifiScanningAlwaysAvailableEnabled: SwitchItem
    // only initialized when in primary user
    private lateinit var geocoderEnabled: SwitchItem

    override fun bindViews() {
        val layout = requireViewById<GlifRecyclerLayout>(R.id.glif_layout)
        val itemGroup = ItemGroup()

        locationEnabled = SwitchItem().apply {
            id = View.generateViewId()
            title = getString(R.string.location_access_title)
            summary = getString(R.string.location_access_desc)
        }
        itemGroup.addChild(locationEnabled)
        LocationData.locationEnabled.observe(this) { locationEnabled.isChecked = it }

        // Network location, Wi-Fi scanning, and Geocoder are global settings.
        if (SetupWizard.isPrimaryUser) {
            networkLocationEnabled = SwitchItem().apply {
                id = View.generateViewId()
                title = getString(R.string.network_location_enabled_title)
                summary = getString(R.string.network_location_enabled_desc)
            }
            itemGroup.addChild(networkLocationEnabled)
            LocationData.networkLocationEnabled.observe(this) {
                networkLocationEnabled.isChecked = it
            }

            wifiScanningAlwaysAvailableEnabled = SwitchItem().apply {
                id = View.generateViewId()
                title = getString(R.string.wifi_scanning_always_available_enabled_title)
                summary = getString(R.string.wifi_scanning_always_available_enabled_desc)
            }
            itemGroup.addChild(wifiScanningAlwaysAvailableEnabled)
            LocationData.wifiScanningAlwaysAvailableEnabled.observe(this) {
                wifiScanningAlwaysAvailableEnabled.isChecked = it
            }

            geocoderEnabled = SwitchItem().apply {
                id = View.generateViewId()
                title = getString(R.string.geocoder_enabled_title)
                summary = getString(R.string.geocoder_enabled_desc)
            }
            itemGroup.addChild(geocoderEnabled)
            LocationData.geocoderEnabled.observe(this) {
                geocoderEnabled.isChecked = it
            }
        }

        val adapter = RecyclerItemAdapter(itemGroup)

        adapter.setOnItemSelectedListener { item ->
            if (item is SwitchItem) {
                item.toggle(findViewById(item.viewId))
            }
        }

        layout.adapter = adapter
    }

    override fun setupActions() {
        locationEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (SetupWizard.isPrimaryUser) {
                val areDependentsEnabled = isChecked
                networkLocationEnabled.isEnabled = areDependentsEnabled
                wifiScanningAlwaysAvailableEnabled.isEnabled = areDependentsEnabled
            }
            LocationActions.setLocationEnabled(isChecked)
        }

        // Network location, Wi-Fi scanning, and Geocoder are global settings.
        if (SetupWizard.isPrimaryUser) {
            networkLocationEnabled.run {
                setOnCheckedChangeListener { _, isChecked ->
                    LocationActions.setNetworkLocationEnabled(isChecked)
                }
            }

            wifiScanningAlwaysAvailableEnabled.run {
                setOnCheckedChangeListener { _, isChecked ->
                    LocationActions.setWifiScanningAlwaysAvailableEnabled(isChecked)
                }
            }

            geocoderEnabled.run {
                setOnCheckedChangeListener { _, isChecked ->
                    LocationActions.setGeocoderEnabled(isChecked)
                }
            }
        }

        primaryButton.setOnClickListener { SetupWizard.next(this) }
    }
}
