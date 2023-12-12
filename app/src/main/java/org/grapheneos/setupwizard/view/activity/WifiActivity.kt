package org.grapheneos.setupwizard.view.activity

import android.view.View
import android.widget.ListView
import androidx.annotation.MainThread
import org.grapheneos.setupwizard.R
import org.grapheneos.setupwizard.action.SetupWizard

// TODO: explore Material 3.0 with JetPack compose // and study trade-offs with regards to this project
class WifiActivity : SetupWizardActivity(R.layout.activity_welcome) {

    // TODO: probably use view binders
    private lateinit var wifiList: ListView
    private lateinit var skip: View
    private lateinit var connect: View

    @MainThread
    override fun bindViews() {
        wifiList = findViewById(R.id.wifi_list)
        // TODO: setup wifi list view adapter
        skip = findViewById(R.id.wifi_skip)
        connect = findViewById(R.id.connect)
        // TODO: attach data change listeners
    }

    @MainThread
    override fun setupActions() {
        skip.setOnClickListener { SetupWizard.next(this) }
        connect.setOnClickListener {
            // TODO: ideally the connect button shouldn't be there!
            // instead user should be able to trigger the connection process by clicking on the
            // wifi networks list view item
            //
            // WifiActions.connect()
        }
    }
}