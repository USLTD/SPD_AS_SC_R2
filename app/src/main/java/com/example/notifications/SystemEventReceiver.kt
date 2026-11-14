package com.example.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.ShiftPreferencesRepository
import com.example.domain.ShiftScheduleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val handledActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
        if (action !in handledActions) return

        val pendingResult: PendingResult? = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleSystemEvent(appContext)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        suspend fun handleSystemEvent(appContext: Context) {
            val repo = ShiftPreferencesRepository(appContext)
            val prefs = repo.getSnapshot()
            val nowMs = System.currentTimeMillis()
            val scheduler = ShiftAlarmScheduler(appContext)
            scheduler.syncAlarms(prefs, nowMs)

            val snapshot = ShiftScheduleEngine.evaluate(prefs, nowMs)
            ShiftNotificationHelper.syncOngoingNotification(appContext, prefs, snapshot)
        }
    }
}
