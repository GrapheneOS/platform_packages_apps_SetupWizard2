package org.grapheneos.setupwizard.action

import org.grapheneos.setupwizard.data.WifiData
import org.grapheneos.setupwizard.data.WifiData.WifiNetwork

object WifiActions {
    init {
        // TODO: ASYNC fetch the list of available networks from system WifiManager
        // trigger wifi scan
        // setup receiver to get discovery callback and keep adding to the networks list
        // we'll have to stop scanning and unregister receivers at some point
        WifiData.wifiNetworks = arrayOf()
    }

    fun connect(network: WifiNetwork) {}
}