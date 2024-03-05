package app.grapheneos.setupwizard.view.activity

import android.view.View
import android.widget.TextView
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.action.DateTimeActions
import app.grapheneos.setupwizard.action.SetupWizard
import app.grapheneos.setupwizard.data.DateTimeData

class DateTimeActivity : SetupWizardActivity(
    R.layout.activity_datetime,
    R.drawable.baseline_today_glif,
    R.string.date_and_time,
    R.string.date_and_time_desc
) {
    companion object {
        private const val TAG = "DateTimeActivity"
    }

    private lateinit var timezoneContainer: View
    private lateinit var timezone: TextView
    private lateinit var dateContainer: View
    private lateinit var date: TextView
    private lateinit var timeContainer: View
    private lateinit var time: TextView
    private lateinit var next: View

    override fun onResume() {
        super.onResume()
        DateTimeActions.handleEntry()
    }

    override fun onPause() {
        super.onPause()
        DateTimeActions.handleExit()
    }

    override fun bindViews() {
        timezoneContainer = requireViewById(R.id.timezone_container)
        timezone = requireViewById(R.id.timezone)
        dateContainer = requireViewById(R.id.date_container)
        date = requireViewById(R.id.date)
        timeContainer = requireViewById(R.id.time_container)
        time = requireViewById(R.id.time)
        next = requireViewById(R.id.next)
        DateTimeData.timeZone.observe(this) { timezone.text = it }
        DateTimeData.date.observe(this) { date.text = it }
        DateTimeData.time.observe(this) { time.text = it }
    }

    override fun setupActions() {
        timezoneContainer.setOnClickListener { DateTimeActions.showTimeZonePicker(this) }
        dateContainer.setOnClickListener { DateTimeActions.showDatePicker(this) }
        timeContainer.setOnClickListener { DateTimeActions.showTimePicker(this) }
        next.setOnClickListener { SetupWizard.next(this) }
    }
}
