package app.grapheneos.setupwizard.action

import app.grapheneos.setupwizard.OEM_UNLOCKED_ACK_TIMER
import app.grapheneos.setupwizard.appContext
import app.grapheneos.setupwizard.data.OemUnlockData
import app.grapheneos.setupwizard.view.activity.OemUnlockActivity
import app.grapheneos.setupwizard.view.activity.WelcomeActivity

object OemUnlockActions {
    private const val TAG = "OemUnlockActions"

    fun rebootToBootloader() {
        WelcomeActions.rebootToBootloader()
    }

    fun next(activity: OemUnlockActivity) {
        SetupWizard.next(activity, WelcomeActivity::class.java)
        return
    }

    fun startAckTimer(time: Int = OEM_UNLOCKED_ACK_TIMER) {
        if (time <= 0) return
        appContext.mainThreadHandler.postDelayed({
            OemUnlockData.ackTimer.value = time - 1
            startAckTimer(time - 1)
        }, 1_000)
    }
}
