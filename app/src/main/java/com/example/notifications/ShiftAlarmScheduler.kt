package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.ShiftPreferences
import com.example.domain.ShiftScheduleEngine

data class AlarmSchedulingResult(
    val scheduledCount: Int,
    val usedExactAlarms: Boolean,
    val shiftActive: Boolean
)

class ShiftAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun syncAlarms(
        preferences: ShiftPreferences,
        nowEpochMs: Long = System.currentTimeMillis()
    ): AlarmSchedulingResult {
        cancelAllAlarms()

        val canExact = canScheduleExactAlarms()
        if (!preferences.isShiftActive) {
            return AlarmSchedulingResult(
                scheduledCount = 0,
                usedExactAlarms = canExact,
                shiftActive = false
            )
        }

        val manager = alarmManager ?: return AlarmSchedulingResult(0, false, true)
        val alerts = ShiftScheduleEngine.computeScheduledAlerts(preferences, nowEpochMs)

        alerts.forEach { event ->
            val intent = Intent(context, ShiftAlarmReceiver::class.java).apply {
                action = ACTION_SHIFT_ALARM
                putExtra(EXTRA_REQUEST_CODE, event.requestCode)
                putExtra(EXTRA_SLOT_INDEX, event.slotIndex)
                putExtra(EXTRA_ASSIGNMENT_NAME, event.targetAssignment.name)
                putExtra(EXTRA_OFFSET_MINUTES, event.offsetMinutes)
                putExtra(EXTRA_TRANSITION_TIME_LABEL, event.transitionTimeLabel)
                putExtra(EXTRA_NEXT_CALENDAR_DAY, event.isNextCalendarDay)
                putExtra(EXTRA_IS_ONGOING_REFRESH, event.isSilentOngoingRefresh)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                event.requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (canExact) {
                try {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        event.triggerEpochMs,
                        pendingIntent
                    )
                } catch (_: SecurityException) {
                    manager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        event.triggerEpochMs,
                        pendingIntent
                    )
                }
            } else {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    event.triggerEpochMs,
                    pendingIntent
                )
            }
        }

        return AlarmSchedulingResult(
            scheduledCount = alerts.size,
            usedExactAlarms = canExact,
            shiftActive = true
        )
    }

    fun cancelAllAlarms() {
        val manager = alarmManager ?: return
        for (requestCode in ShiftScheduleEngine.allPossibleRequestCodes()) {
            val intent = Intent(context, ShiftAlarmReceiver::class.java).apply {
                action = ACTION_SHIFT_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                manager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    companion object {
        const val ACTION_SHIFT_ALARM = "com.example.action.SHIFT_ALARM"
        const val EXTRA_REQUEST_CODE = "extra_request_code"
        const val EXTRA_SLOT_INDEX = "extra_slot_index"
        const val EXTRA_ASSIGNMENT_NAME = "extra_assignment_name"
        const val EXTRA_OFFSET_MINUTES = "extra_offset_minutes"
        const val EXTRA_TRANSITION_TIME_LABEL = "extra_transition_time_label"
        const val EXTRA_NEXT_CALENDAR_DAY = "extra_next_calendar_day"
        const val EXTRA_IS_ONGOING_REFRESH = "extra_is_ongoing_refresh"
    }
}
