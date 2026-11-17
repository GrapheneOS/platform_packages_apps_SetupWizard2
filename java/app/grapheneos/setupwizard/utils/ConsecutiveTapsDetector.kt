package app.grapheneos.setupwizard.utils

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

/**
 * Calls [onDetected] after [count] taps on [view], each close in time and position to the
 * previous one. Feed it every event from Activity.dispatchTouchEvent.
 *
 * Events are observed rather than handled by a listener on [view], so that the view doesn't become
 * clickable and isn't announced as an action by accessibility services such as TalkBack.
 */
class ConsecutiveTapsDetector(
    private val view: View,
    private val count: Int,
    private val onDetected: () -> Unit,
) {
    private val slopSquare = ViewConfiguration.get(view.context).scaledDoubleTapSlop.let { it * it }
    private val timeout = ViewConfiguration.getDoubleTapTimeout()
    private val rect = Rect()
    private val location = IntArray(2)
    private var taps = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastTime = 0L

    fun onTouchEvent(ev: MotionEvent) {
        if (ev.actionMasked != MotionEvent.ACTION_UP) {
            return
        }

        view.getLocationOnScreen(location)
        rect.set(location[0], location[1], location[0] + view.width, location[1] + view.height)

        if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
            taps = 0
            return
        }

        val dx = ev.rawX - lastX
        val dy = ev.rawY - lastY

        val consecutive = taps > 0 && ev.eventTime - lastTime <= timeout
                && dx * dx + dy * dy <= slopSquare

        taps = if (consecutive) taps + 1 else 1
        lastX = ev.rawX
        lastY = ev.rawY
        lastTime = ev.eventTime

        if (taps == count) {
            taps = 0
            onDetected()
        }
    }
}
