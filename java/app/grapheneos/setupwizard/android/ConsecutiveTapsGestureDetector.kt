package app.grapheneos.setupwizard.android

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration


class ConsecutiveTapsGestureDetector(
    private val mListener: OnConsecutiveTapsListener,
    private val mView: View
) {
    private val mConsecutiveTapTouchSlopSquare: Int
    private var mConsecutiveTapsCounter = 0
    private var mPreviousTapEvent: MotionEvent? = null

    interface OnConsecutiveTapsListener {
        fun onConsecutiveTaps(welcomeTapCounter: Int)
    }

    init {
        val doubleTapSlop: Int = ViewConfiguration.get(mView.context).getScaledDoubleTapSlop()
        mConsecutiveTapTouchSlopSquare = doubleTapSlop * doubleTapSlop
    }

    fun onTouchEvent(ev: MotionEvent) {
        if (ev.action != MotionEvent.ACTION_UP) {
            return
        }
        val viewRect = Rect()
        val leftTop = IntArray(2)
        mView.getLocationOnScreen(leftTop)
        viewRect.set(leftTop[0], leftTop[1], leftTop[0] + mView.width, leftTop[1] + mView.height)
        if (viewRect.contains(ev.x.toInt(), ev.y.toInt())) {
            if (isConsecutiveTap(ev)) {
                mConsecutiveTapsCounter++
            } else {
                mConsecutiveTapsCounter = 1
            }
            mListener.onConsecutiveTaps(mConsecutiveTapsCounter)
        } else {
            mConsecutiveTapsCounter = 0
        }
        if (mPreviousTapEvent != null) {
            mPreviousTapEvent!!.recycle()
        }
        mPreviousTapEvent = MotionEvent.obtain(ev)
    }

    fun resetCounter() {
        mConsecutiveTapsCounter = 0
    }

    private fun isConsecutiveTap(currentTapEvent: MotionEvent): Boolean {
        if (mPreviousTapEvent == null) {
            return false
        }
        val deltaX = (mPreviousTapEvent!!.x - currentTapEvent.x).toDouble()
        val deltaY = (mPreviousTapEvent!!.y - currentTapEvent.y).toDouble()
        return deltaX * deltaX + deltaY * deltaY <= mConsecutiveTapTouchSlopSquare.toDouble()
    }
}
