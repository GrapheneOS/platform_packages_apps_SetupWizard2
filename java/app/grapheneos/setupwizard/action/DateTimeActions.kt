package app.grapheneos.setupwizard.action

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.text.TimeZoneNames
import android.icu.util.TimeZone
import android.icu.util.ULocale
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.appContext
import app.grapheneos.setupwizard.data.DateTimeData
import java.util.Calendar
import com.google.android.setupdesign.R as SudR

object DateTimeActions {
    private const val TAG = "DateTimeActions"

    private val dateTimeReceiver = DateTimeReceiver()

    init {
        refreshCurrentState()
    }

    private fun refreshCurrentState() {
        val cal = Calendar.getInstance()
        DateTimeData.date.value = DateFormat.getDateFormat(appContext).format(cal.time)
        DateTimeData.time.value = DateFormat.getTimeFormat(appContext).format(cal.time)
        DateTimeData.timeZone.value = ZoneGetter.getZoneInfo(TimeZone.getDefault().id).standardName
    }

    fun showTimeZonePicker(context: Activity) {
        val zones = ZoneGetter.getZonesList()
        AlertDialog.Builder(context)
            .setTitle(R.string.select_time_zone)
            .setAdapter(ZoneGetter.getAdapter(context, zones)) { _, which ->
                Log.d(TAG, "showTimeZonePicker: ${zones[which]}")
                setTimeZone(zones[which].id)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    fun showDatePicker(context: Activity) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth -> setDate(year, month, dayOfMonth) },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showTimePicker(context: Activity) {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hourOfDay, min -> setTime(hourOfDay, min) },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            DateFormat.is24HourFormat(appContext)
        ).show()
    }

    private fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        setMillis(cal.timeInMillis)
    }

    private fun setTime(hourOfDay: Int, min: Int) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
        cal.set(Calendar.MINUTE, min)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        setMillis(cal.timeInMillis)
    }

    private fun setMillis(millis: Long) {
        getAlarmManager().setTime(millis)
        refreshCurrentState()
    }

    private fun setTimeZone(zoneId: String) {
        getAlarmManager().setTimeZone(zoneId)
        refreshCurrentState()
    }

    private fun getAlarmManager(): AlarmManager {
        return appContext.getSystemService(AlarmManager::class.java)!!
    }

    fun handleEntry() {
        refreshCurrentState()
        dateTimeReceiver.register()
    }

    fun handleExit() {
        dateTimeReceiver.unregister()
    }

    private class DateTimeReceiver : BroadcastReceiver() {
        private val intentFilter = IntentFilter()

        init {
            intentFilter.addAction(Intent.ACTION_TIME_TICK)
            intentFilter.addAction(Intent.ACTION_TIME_CHANGED)
            intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            refreshCurrentState()
        }

        fun register() {
            appContext.registerReceiver(this, intentFilter)
        }

        fun unregister() {
            appContext.unregisterReceiver(this)
        }
    }

    private object ZoneGetter {
        private const val TAG = "ZoneGetter"

        data class ZoneInfo(
            val id: String,
            val displayName: String, // EXEMPLAR_LOCATION
            val standardName: String, // LONG_GMT LONG_STANDARD
        )

        fun getZonesList(): List<ZoneInfo> {
            val zoneInfos = mutableListOf<ZoneInfo>()
            val zoneIds = appContext.resources.getStringArray(R.array.time_zones)
            val zoneNames = getZoneNamesInstance()
            zoneIds.forEach { id ->
                zoneInfos.add(getZoneInfo(id, zoneNames))
            }
            return zoneInfos
        }

        fun getZoneInfo(zoneId: String): ZoneInfo {
            return getZoneInfo(zoneId, getZoneNamesInstance())
        }

        private fun getZoneInfo(zoneId: String, zoneNames: TimeZoneNames): ZoneInfo {
            val timeZone = TimeZone.getTimeZone(zoneId)
            val displayName =
                zoneNames.getExemplarLocationName(TimeZone.getCanonicalID(zoneId) ?: zoneId)
                    ?: timeZone.displayName
            val longGmt = timeZone.getDisplayName(false, TimeZone.LONG_GMT)
            val longStandard = timeZone.getDisplayName(false, TimeZone.LONG)
            val standardName = "$longGmt $longStandard"
            Log.d(
                TAG, "getZoneInfo: zoneId=$zoneId, displayName=$displayName, " +
                        "standardName=$standardName"
            )
            return ZoneInfo(zoneId, displayName, standardName)
        }

        private fun getZoneNamesInstance(): TimeZoneNames {
            return TimeZoneNames.getInstance(ULocale.getDefault())
        }

        fun getAdapter(context: Activity, zones: List<ZoneInfo>): ArrayAdapter<ZoneInfo> {
            return object : ArrayAdapter<ZoneInfo>(
                context,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                zones
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val title = view.requireViewById<TextView>(android.R.id.text1)
                    val desc = view.requireViewById<TextView>(android.R.id.text2)
                    if (desc.tag == null) {
                        desc.setTextAppearance(SudR.style.TextAppearance_SudMaterialYouItemSummary)
                        desc.tag = Any()
                    }
                    title.text = zones[position].displayName
                    desc.text = zones[position].standardName
                    return view
                }
            }
        }
    }
}
