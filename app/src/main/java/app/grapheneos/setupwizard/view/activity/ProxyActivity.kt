package app.grapheneos.setupwizard.view.activity

import android.app.Activity
import android.content.Intent
import android.util.Log

abstract class ProxyActivity : SetupWizardActivity() {
    companion object {
        private const val TAG = "ProxyActivity"
    }

    private var movingForward = false

    override fun onResume() {
        super.onResume()
        if (movingForward) {
            movingForward = false
            return
        }
        launchActual()
    }

    protected fun setMovingForward() {
        movingForward = true
    }

    abstract fun launchActual()

    abstract fun handleResult(resultCode: Int, data: Intent?)

    override fun bindViews() {
    }

    override fun setupActions() {
    }

    override fun onActivityResult(resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: $resultCode, $data")
        if (resultCode == Activity.RESULT_CANCELED) {
            finish()
            return
        }
        handleResult(resultCode, data)
        super.onActivityResult(resultCode, data)
    }
}