package app.grapheneos.setupwizard.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log

abstract class ProxyActivity : SetupWizardActivity() {
    companion object {
        private const val TAG = "ProxyActivity"
        private const val KEY_AWAITING_RESULT = "awaiting_result"
    }

    private var movingForward = false

    // survives recreation so that the still running target isn't launched a second time
    private var awaitingResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        awaitingResult = savedInstanceState?.getBoolean(KEY_AWAITING_RESULT) ?: false
    }

    override fun onResume() {
        super.onResume()
        if (movingForward) {
            movingForward = false
            return
        }

        if (awaitingResult) {
            return
        }

        awaitingResult = true
        launchActual()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(KEY_AWAITING_RESULT, awaitingResult)
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
        awaitingResult = false
        if (resultCode == RESULT_CANCELED) {
            finish()
            return
        }
        handleResult(resultCode, data)
        super.onActivityResult(resultCode, data)
    }
}
