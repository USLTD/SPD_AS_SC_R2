package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.ShiftPreferencesRepository
import com.example.domain.AssignmentType
import com.example.domain.ShiftScheduleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShiftAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult: PendingResult? = goAsync()
        val appContext = context.applicationContext

        when (action) {
            ShiftNotificationHelper.ACTION_MARK_BRIEFING_COMPLETE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = ShiftPreferencesRepository(appContext)
                        val nowMs = System.currentTimeMillis()
                        repo.markBriefingComplete(nowMs)
                        val updated = repo.getSnapshot()
                        val snapshot = ShiftScheduleEngine.evaluate(updated, nowMs)
                        ShiftNotificationHelper.syncOngoingNotification(appContext, updated, snapshot)
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            ShiftNotificationHelper.ACTION_DISMISS_LIVE_UPDATE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = ShiftPreferencesRepository(appContext)
                        repo.setLiveUpdateDismissed(true)
                        ShiftNotificationHelper.cancelOngoingNotification(appContext)
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            ShiftAlarmScheduler.ACTION_SHIFT_ALARM -> {
                val slotIndex = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_SLOT_INDEX, -1)
                val assignmentName = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_ASSIGNMENT_NAME)
                val offsetMinutes = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_OFFSET_MINUTES, 0)
                val timeLabel =
                    intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TRANSITION_TIME_LABEL) ?: ""
                val isNextDay =
                    intent.getBooleanExtra(ShiftAlarmScheduler.EXTRA_NEXT_CALENDAR_DAY, false)
                val isOngoingRefreshOnly = intent.getBooleanExtra(
                    ShiftAlarmScheduler.EXTRA_IS_ONGOING_REFRESH,
                    false
                )
                val requestCode = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_REQUEST_CODE, 3001)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = ShiftPreferencesRepository(appContext)
                        val prefs = repo.getSnapshot()
                        if (!prefs.isShiftActive) {
                            ShiftNotificationHelper.cancelOngoingNotification(appContext)
                            return@launch
                        }

                        val nowMs = System.currentTimeMillis()
                        val snapshot = ShiftScheduleEngine.evaluate(prefs, nowMs)

                        if (!isOngoingRefreshOnly && assignmentName != null && slotIndex >= 0) {
                            val type = runCatching { AssignmentType.valueOf(assignmentName) }
                                .getOrDefault(AssignmentType.CHECKPOINT_VI)
                            ShiftNotificationHelper.postTransitionAlert(
                                context = appContext,
                                notificationId = requestCode,
                                slotIndex = slotIndex,
                                assignmentType = type,
                                offsetMinutes = offsetMinutes,
                                transitionTimeLabel = timeLabel,
                                isNextCalendarDay = isNextDay,
                                preferences = prefs
                            )
                        }

                        ShiftNotificationHelper.syncOngoingNotification(appContext, prefs, snapshot)
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            else -> pendingResult?.finish()
        }
    }
}
