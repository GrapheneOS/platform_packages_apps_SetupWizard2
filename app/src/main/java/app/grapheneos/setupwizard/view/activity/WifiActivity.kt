package app.grapheneos.setupwizard.view.activity

import android.content.Intent
import app.grapheneos.setupwizard.action.WifiActions

class WifiActivity : ProxyActivity() {
    companion object {
        private const val TAG = "WifiActivity"
    }

    override fun launchActual(): Int {
        return WifiActions.launchSetup(this)
    }

    override fun handleResult(resultCode: Int, data: Intent?) {
        setMovingForward()
        WifiActions.handleResult(this, resultCode)
    }
}