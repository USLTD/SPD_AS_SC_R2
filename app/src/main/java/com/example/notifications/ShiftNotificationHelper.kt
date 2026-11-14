package com.example.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ShiftPreferences
import com.example.domain.AppLanguage
import com.example.domain.AssignmentType
import com.example.domain.ShiftPhase
import com.example.domain.ShiftScheduleEngine
import com.example.domain.ShiftSnapshot
import com.example.ui.i18n.AppStrings

object ShiftNotificationHelper {

    const val CHANNEL_ALERTS = "shift_transition_alerts"
    const val CHANNEL_ONGOING = "shift_ongoing_status"

    const val ONGOING_NOTIFICATION_ID = 9001
    const val TEST_NOTIFICATION_ID = 9002

    const val EXTRA_OPEN_SHIFT = "extra_open_shift"
    const val EXTRA_SHIFT_DATE = "extra_shift_date"

    const val ACTION_MARK_BRIEFING_COMPLETE = "com.example.action.MARK_BRIEFING_COMPLETE"
    const val ACTION_DISMISS_LIVE_UPDATE = "com.example.action.DISMISS_LIVE_UPDATE"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_alerts_desc)
                enableVibration(true)
            }

            val ongoingChannel = NotificationChannel(
                CHANNEL_ONGOING,
                context.getString(R.string.channel_ongoing_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_ongoing_desc)
                setShowBadge(false)
            }

            manager.createNotificationChannel(alertsChannel)
            manager.createNotificationChannel(ongoingChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isLiveUpdatesApiSupported(): Boolean = Build.VERSION.SDK_INT >= 36

    fun canPostPromotedNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 36) return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.canPostPromotedNotifications()
    }

    fun createOpenShiftPendingIntent(context: Context, shiftDateIso: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SHIFT, true)
            putExtra(EXTRA_SHIFT_DATE, shiftDateIso)
        }
        return PendingIntent.getActivity(
            context,
            8001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createBriefingCompletePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ShiftAlarmReceiver::class.java).apply {
            action = ACTION_MARK_BRIEFING_COMPLETE
        }
        return PendingIntent.getBroadcast(
            context,
            8002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createDismissPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ShiftAlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_LIVE_UPDATE
        }
        return PendingIntent.getBroadcast(
            context,
            8003,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun postTransitionAlert(
        context: Context,
        notificationId: Int,
        slotIndex: Int,
        assignmentType: AssignmentType,
        offsetMinutes: Int,
        transitionTimeLabel: String,
        isNextCalendarDay: Boolean,
        preferences: ShiftPreferences
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val strings = AppStrings.forLanguage(preferences.language)
        val assignmentLabel = strings.assignmentName(assignmentType)
        val dayNote = if (isNextCalendarDay) " (+1)" else ""

        val title: String
        val body: String
        when {
            slotIndex == 12 && offsetMinutes == 0 -> {
                title = strings.notificationShiftCompleteTitle
                body = "${strings.shiftCompletedTitle} ($transitionTimeLabel$dayNote)"
            }
            slotIndex == 12 && offsetMinutes > 0 -> {
                title = "${strings.notificationReminderTitle} ($offsetMinutes ${strings.minutesBeforeSuffix})"
                body = "${strings.shiftEndsAtLabel}$dayNote"
            }
            offsetMinutes == 0 -> {
                title = "${strings.notificationTransitionTitle}: $assignmentLabel"
                body = "${strings.transitionAtPrefix} $transitionTimeLabel$dayNote"
            }
            else -> {
                title = "$assignmentLabel ($offsetMinutes ${strings.minutesBeforeSuffix})"
                body = "${strings.transitionAtPrefix} $transitionTimeLabel$dayNote"
            }
        }

        val tapIntent = createOpenShiftPendingIntent(context, preferences.shiftStartDate.toString())
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.fc_alarm_bell)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun postTestNotification(context: Context, preferences: ShiftPreferences): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)

        val strings = AppStrings.forLanguage(preferences.language)
        val tapIntent = createOpenShiftPendingIntent(context, preferences.shiftStartDate.toString())
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.fc_bell)
            .setContentTitle(strings.notificationTestTitle)
            .setContentText(strings.notificationTestBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .build()

        NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
        return true
    }

    @SuppressLint("MissingPermission")
    fun syncOngoingNotification(
        context: Context,
        preferences: ShiftPreferences,
        snapshot: ShiftSnapshot
    ) {
        val compatManager = NotificationManagerCompat.from(context)
        val shouldShow = preferences.isShiftActive &&
            preferences.ongoingStatusNotification &&
            !preferences.liveUpdateDismissedForShift &&
            (snapshot.phase == ShiftPhase.ACTIVE_BRIEFING || snapshot.phase == ShiftPhase.ACTIVE_TIMED)

        if (!shouldShow || !hasNotificationPermission(context)) {
            compatManager.cancel(ONGOING_NOTIFICATION_ID)
            return
        }

        ensureChannels(context)
        val strings = AppStrings.forLanguage(preferences.language)
        val currentType = snapshot.currentSlot?.assignmentType ?: AssignmentType.BRIEFING
        val currentLabel = strings.assignmentName(currentType)
        val nextAt = snapshot.nextTransitionTimeLabel ?: "10:00"
        val nextName = snapshot.nextSlot?.let { strings.assignmentName(it.assignmentType) }
            ?: strings.completedBadge
        val contentLine = if (snapshot.phase == ShiftPhase.ACTIVE_BRIEFING) {
            "${strings.briefingUnknownEndHint} • $nextName @ $nextAt"
        } else {
            "${strings.nextAssignmentTitle}: $nextName @ $nextAt"
        }

        val notification = if (Build.VERSION.SDK_INT >= 36) {
            buildAndroid16LiveUpdate(
                context = context,
                preferences = preferences,
                snapshot = snapshot,
                currentType = currentType,
                currentLabel = currentLabel,
                contentLine = contentLine,
                strings = strings
            )
        } else {
            buildLegacyChronometerOngoing(
                context = context,
                preferences = preferences,
                snapshot = snapshot,
                currentLabel = currentLabel,
                contentLine = contentLine,
                strings = strings
            )
        }

        compatManager.notify(ONGOING_NOTIFICATION_ID, notification)
    }

    @RequiresApi(36)
    private fun buildAndroid16LiveUpdate(
        context: Context,
        preferences: ShiftPreferences,
        snapshot: ShiftSnapshot,
        currentType: AssignmentType,
        currentLabel: String,
        contentLine: String,
        strings: AppStrings
    ): Notification {
        val tapIntent = createOpenShiftPendingIntent(context, preferences.shiftStartDate.toString())
        val deleteIntent = createDismissPendingIntent(context)
        val trackerRes = iconForAssignment(currentType)

        val firstBlockColor = if (snapshot.briefingCompleted) {
            Color.rgb(0xB7, 0x79, 0x1F)
        } else {
            Color.rgb(0x4B, 0x4E, 0x8A)
        }
        val cpViColor = Color.rgb(0x1D, 0x5F, 0xB8)
        val cpIiiColor = Color.rgb(0x14, 0x7D, 0x73)
        val breakColor = Color.rgb(0xB7, 0x79, 0x1F)
        val attnColor = Color.rgb(0xB8, 0x32, 0x32)

        val progressStyle = Notification.ProgressStyle()
            .setStyledByProgress(false)
            .setProgress(snapshot.elapsedShiftMinutes.coerceIn(0, ShiftScheduleEngine.TOTAL_SHIFT_MINUTES))
            .setProgressTrackerIcon(Icon.createWithResource(context, trackerRes))
            .setProgressSegments(
                listOf(
                    Notification.ProgressStyle.Segment(150).setColor(firstBlockColor), // 09:30–12:00
                    Notification.ProgressStyle.Segment(90).setColor(cpViColor),       // 12:00–13:30
                    Notification.ProgressStyle.Segment(90).setColor(cpIiiColor),      // 13:30–15:00
                    Notification.ProgressStyle.Segment(180).setColor(breakColor),     // 15:00–18:00
                    Notification.ProgressStyle.Segment(90).setColor(cpViColor),       // 18:00–19:30
                    Notification.ProgressStyle.Segment(90).setColor(cpIiiColor),      // 19:30–21:00
                    Notification.ProgressStyle.Segment(180).setColor(breakColor),     // 21:00–00:00
                    Notification.ProgressStyle.Segment(180).setColor(cpViColor),      // 00:00–03:00
                    Notification.ProgressStyle.Segment(180).setColor(cpIiiColor),     // 03:00–06:00
                    Notification.ProgressStyle.Segment(150).setColor(breakColor),     // 06:00–08:30
                    Notification.ProgressStyle.Segment(90).setColor(attnColor)        // 08:30–10:00
                )
            )
            .setProgressPoints(
                listOf(
                    Notification.ProgressStyle.Point(150).setColor(cpViColor),
                    Notification.ProgressStyle.Point(870).setColor(cpViColor),
                    Notification.ProgressStyle.Point(1380).setColor(attnColor)
                )
            )

        val chipText = shortChipLabel(currentType, preferences.language)
        val promoExtras = Bundle().apply {
            putBoolean("android.requestPromotedOngoing", true)
        }

        val builder = Notification.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.fc_clock_play)
            .setContentTitle(currentLabel)
            .setContentText(contentLine)
            .setSubText(strings.notificationOngoingPrefix)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(tapIntent)
            .setDeleteIntent(deleteIntent)
            .setShortCriticalText(chipText)
            .addExtras(promoExtras)
            .setStyle(progressStyle)

        val nextMs = snapshot.nextTransitionEpochMs
        if (nextMs != null && nextMs > snapshot.nowEpochMs) {
            builder
                .setWhen(nextMs)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        }

        if (snapshot.canMarkBriefingComplete) {
            val action = Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.fc_tick),
                strings.markBriefingComplete,
                createBriefingCompletePendingIntent(context)
            ).build()
            builder.addAction(action)
        }

        return builder.build()
    }

    private fun buildLegacyChronometerOngoing(
        context: Context,
        preferences: ShiftPreferences,
        snapshot: ShiftSnapshot,
        currentLabel: String,
        contentLine: String,
        strings: AppStrings
    ): Notification {
        val tapIntent = createOpenShiftPendingIntent(context, preferences.shiftStartDate.toString())
        val deleteIntent = createDismissPendingIntent(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.fc_clock_play)
            .setContentTitle("${strings.notificationOngoingPrefix}: $currentLabel")
            .setContentText(contentLine)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(tapIntent)
            .setDeleteIntent(deleteIntent)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(
                shortChipLabel(
                    snapshot.currentSlot?.assignmentType ?: AssignmentType.BRIEFING,
                    preferences.language
                )
            )

        val nextMs = snapshot.nextTransitionEpochMs
        if (nextMs != null && nextMs > snapshot.nowEpochMs) {
            builder
                .setWhen(nextMs)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        }

        val progress = snapshot.progressFraction
        if (progress != null) {
            builder.setProgress(100, (progress * 100f).toInt().coerceIn(0, 100), false)
        } else {
            builder.setProgress(0, 0, true)
        }

        if (snapshot.canMarkBriefingComplete) {
            builder.addAction(
                R.drawable.fc_tick,
                strings.markBriefingComplete,
                createBriefingCompletePendingIntent(context)
            )
        }

        return builder.build()
    }

    private fun iconForAssignment(type: AssignmentType): Int = when (type) {
        AssignmentType.BRIEFING -> R.drawable.fc_comment
        AssignmentType.BREAK -> R.drawable.fc_cup
        AssignmentType.CHECKPOINT_VI -> R.drawable.fc_shield
        AssignmentType.CHECKPOINT_III -> R.drawable.fc_shield_go
        AssignmentType.ATTENTION -> R.drawable.fc_exclamation
    }

    /**
     * Kept <= 7 characters so Android 16 status-bar chips always render the full text.
     */
    fun shortChipLabel(type: AssignmentType, language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> when (type) {
            AssignmentType.BRIEFING -> "Brief"
            AssignmentType.BREAK -> "Break"
            AssignmentType.CHECKPOINT_VI -> "CP VI"
            AssignmentType.CHECKPOINT_III -> "CP III"
            AssignmentType.ATTENTION -> "Attn"
        }
        AppLanguage.GEORGIAN -> when (type) {
            AssignmentType.BRIEFING -> "ინსტრ."
            AssignmentType.BREAK -> "შესვ."
            AssignmentType.CHECKPOINT_VI -> "სგპ VI"
            AssignmentType.CHECKPOINT_III -> "სგპ III"
            AssignmentType.ATTENTION -> "ყურად."
        }
    }

    fun cancelOngoingNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(ONGOING_NOTIFICATION_ID)
    }
}
