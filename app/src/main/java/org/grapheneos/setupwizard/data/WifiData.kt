package org.grapheneos.setupwizard.data

object WifiData {
    data class WifiNetwork(val identifier: String, val name: String)

    // TODO: should use androidx livedata
    var wifiNetworks: Array<WifiNetwork> = arrayOf()
    var connectingNetwork: WifiNetwork? = null
    var connectionStatus: String? = null // TODO: define enums paired with user message string res
}
