package app.grapheneos.setupwizard.view.activity

import android.content.Intent
import android.util.Log
import android.view.View
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.action.GesturesActions
import app.grapheneos.setupwizard.action.SetupWizard

class GesturesActivity : SetupWizardActivity(
    R.layout.activity_gestures,
    R.drawable.baseline_gesture_glif,
    R.string.swipe_gestures_title,
    R.string.swipe_gestures_desc
) {
    companion object {
        private const val TAG = "GesturesActivity"
    }

    private lateinit var skip: View
    private lateinit var tryIt: View

    override fun bindViews() {
        skip = requireViewById(R.id.skip)
        tryIt = requireViewById(R.id.try_it)
    }

    override fun setupActions() {
        skip.setOnClickListener { SetupWizard.next(this) }
        tryIt.setOnClickListener { GesturesActions.launchTutorial(this) }
    }

    override fun onActivityResult(resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: $resultCode, data=$data")
        GesturesActions.handleResult(this, resultCode)
        super.onActivityResult(resultCode, data)
    }
}
