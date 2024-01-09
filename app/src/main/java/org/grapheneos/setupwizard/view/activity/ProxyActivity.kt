package org.grapheneos.setupwizard.view.activity

import android.app.Activity
import android.content.Intent
import android.util.Log

abstract class ProxyActivity : SetupWizardActivity() {
    companion object {
        private const val TAG = "ProxyActivity"
    }

    private var requestCode: Int? = null
    private var movingForward = false

    override fun onResume() {
        super.onResume()
        if (movingForward) {
            movingForward = false
            return
        }
        requestCode = launchActual()
    }

    protected fun setMovingForward() {
        movingForward = true
    }

    abstract fun launchActual(): Int

    abstract fun handleResult(resultCode: Int, data: Intent?)

    override fun bindViews() {
    }

    override fun setupActions() {
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: $requestCode, $resultCode, $data")
        if (requestCode != this.requestCode) return
        if (resultCode == Activity.RESULT_CANCELED) {
            finish()
            return
        }
        handleResult(resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}