package com.example.domain

import com.example.data.ShiftPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ShiftScheduleEngine {

    val TBILISI_ZONE: ZoneId = ZoneId.of("Asia/Tbilisi")
    val ALL_REMINDER_OFFSETS: List<Int> = listOf(5, 10, 15, 30)
    const val TOTAL_SHIFT_MINUTES: Int = 1470

    private val TIME_HMS_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)
    private val TIME_HM_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val DATE_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    /**
     * Exact 24.5-hour schedule starting 09:30 on Day 0 and ending 10:00 on Day +1.
     * Note: Slot 0 (Briefing) has no known end time; Slot 1 (Break) starts only when briefing is
     * manually marked complete and ends at 12:00.
     */
    val slots: List<ScheduleSlot> = listOf(
        ScheduleSlot(
            index = 0,
            assignmentType = AssignmentType.BRIEFING,
            startDayOffset = 0,
            startTime = LocalTime.of(9, 30),
            endDayOffset = 0,
            endTime = null
        ),
        ScheduleSlot(
            index = 1,
            assignmentType = AssignmentType.BREAK,
            startDayOffset = 0,
            startTime = null,
            endDayOffset = 0,
            endTime = LocalTime.of(12, 0),
            isPostBriefingBreak = true
        ),
        ScheduleSlot(
            index = 2,
            assignmentType = AssignmentType.CHECKPOINT_VI,
            startDayOffset = 0,
            startTime = LocalTime.of(12, 0),
            endDayOffset = 0,
            endTime = LocalTime.of(13, 30)
        ),
        ScheduleSlot(
            index = 3,
            assignmentType = AssignmentType.CHECKPOINT_III,
            startDayOffset = 0,
            startTime = LocalTime.of(13, 30),
            endDayOffset = 0,
            endTime = LocalTime.of(15, 0)
        ),
        ScheduleSlot(
            index = 4,
            assignmentType = AssignmentType.BREAK,
            startDayOffset = 0,
            startTime = LocalTime.of(15, 0),
            endDayOffset = 0,
            endTime = LocalTime.of(18, 0)
        ),
        ScheduleSlot(
            index = 5,
            assignmentType = AssignmentType.CHECKPOINT_VI,
            startDayOffset = 0,
            startTime = LocalTime.of(18, 0),
            endDayOffset = 0,
            endTime = LocalTime.of(19, 30)
        ),
        ScheduleSlot(
            index = 6,
            assignmentType = AssignmentType.CHECKPOINT_III,
            startDayOffset = 0,
            startTime = LocalTime.of(19, 30),
            endDayOffset = 0,
            endTime = LocalTime.of(21, 0)
        ),
        ScheduleSlot(
            index = 7,
            assignmentType = AssignmentType.BREAK,
            startDayOffset = 0,
            startTime = LocalTime.of(21, 0),
            endDayOffset = 1,
            endTime = LocalTime.of(0, 0)
        ),
        ScheduleSlot(
            index = 8,
            assignmentType = AssignmentType.CHECKPOINT_VI,
            startDayOffset = 1,
            startTime = LocalTime.of(0, 0),
            endDayOffset = 1,
            endTime = LocalTime.of(3, 0)
        ),
        ScheduleSlot(
            index = 9,
            assignmentType = AssignmentType.CHECKPOINT_III,
            startDayOffset = 1,
            startTime = LocalTime.of(3, 0),
            endDayOffset = 1,
            endTime = LocalTime.of(6, 0)
        ),
        ScheduleSlot(
            index = 10,
            assignmentType = AssignmentType.BREAK,
            startDayOffset = 1,
            startTime = LocalTime.of(6, 0),
            endDayOffset = 1,
            endTime = LocalTime.of(8, 30)
        ),
        ScheduleSlot(
            index = 11,
            assignmentType = AssignmentType.ATTENTION,
            startDayOffset = 1,
            startTime = LocalTime.of(8, 30),
            endDayOffset = 1,
            endTime = LocalTime.of(10, 0)
        )
    )

    fun resolveZone(useDeviceTimezone: Boolean, deviceZone: ZoneId = ZoneId.systemDefault()): ZoneId =
        if (useDeviceTimezone) deviceZone else TBILISI_ZONE

    fun slotStartEpochMs(shiftStartDate: LocalDate, slot: ScheduleSlot, zoneId: ZoneId): Long? {
        val time = slot.startTime ?: return null
        return ZonedDateTime.of(shiftStartDate.plusDays(slot.startDayOffset.toLong()), time, zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun slotEndEpochMs(shiftStartDate: LocalDate, slot: ScheduleSlot, zoneId: ZoneId): Long? {
        val time = slot.endTime ?: return null
        return ZonedDateTime.of(shiftStartDate.plusDays(slot.endDayOffset.toLong()), time, zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun evaluate(
        preferences: ShiftPreferences,
        nowEpochMs: Long,
        deviceZone: ZoneId = ZoneId.systemDefault()
    ): ShiftSnapshot {
        val zoneId = resolveZone(preferences.useDeviceTimezone, deviceZone)
        val nowZdt = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId)
        val currentTimeFormatted = nowZdt.toLocalTime().format(TIME_HMS_FORMATTER)
        val currentDateFormatted = nowZdt.toLocalDate().format(DATE_FORMATTER)
        val startDate = preferences.shiftStartDate

        if (!preferences.isShiftActive) {
            return ShiftSnapshot(
                phase = ShiftPhase.INACTIVE,
                nowEpochMs = nowEpochMs,
                zoneId = zoneId,
                currentTimeFormatted = currentTimeFormatted,
                currentDateFormatted = currentDateFormatted,
                shiftStartDate = startDate,
                currentSlot = null,
                nextSlot = slots[0],
                activeSlotIndex = null,
                nextTransitionEpochMs = null,
                nextTransitionTimeLabel = null,
                countdownMillis = 0L,
                countdownFormatted = "00:00:00",
                progressFraction = null,
                segmentElapsedFormatted = null,
                segmentTotalFormatted = null,
                canMarkBriefingComplete = false,
                briefingCompleted = preferences.briefingCompleted,
                briefingCompletedAtEpochMs = preferences.briefingCompletedAtEpochMs
            )
        }

        val shiftStartMs = slotStartEpochMs(startDate, slots[0], zoneId)!!
        val noon12Ms = slotStartEpochMs(startDate, slots[2], zoneId)!!
        val shiftEndMs = slotEndEpochMs(startDate, slots.last(), zoneId)!!

        // Before 09:30 on Day 0
        if (nowEpochMs < shiftStartMs) {
            val remaining = (shiftStartMs - nowEpochMs).coerceAtLeast(0L)
            return ShiftSnapshot(
                phase = ShiftPhase.BEFORE_SHIFT,
                nowEpochMs = nowEpochMs,
                zoneId = zoneId,
                currentTimeFormatted = currentTimeFormatted,
                currentDateFormatted = currentDateFormatted,
                shiftStartDate = startDate,
                currentSlot = null,
                nextSlot = slots[0],
                activeSlotIndex = null,
                nextTransitionEpochMs = shiftStartMs,
                nextTransitionTimeLabel = slots[0].startTime?.format(TIME_HM_FORMATTER),
                countdownMillis = remaining,
                countdownFormatted = formatDurationHms(remaining),
                progressFraction = null,
                segmentElapsedFormatted = null,
                segmentTotalFormatted = null,
                canMarkBriefingComplete = false,
                briefingCompleted = preferences.briefingCompleted,
                briefingCompletedAtEpochMs = preferences.briefingCompletedAtEpochMs,
                shiftStartEpochMs = shiftStartMs,
                shiftEndEpochMs = shiftEndMs,
                elapsedShiftMinutes = 0
            )
        }

        val elapsedMinutes = ((nowEpochMs - shiftStartMs) / 60_000L).toInt().coerceIn(0, TOTAL_SHIFT_MINUTES)

        // Between 09:30 and 12:00 on Day 0 (open-ended briefing vs manual first break)
        if (nowEpochMs < noon12Ms) {
            val remainingToNoon = (noon12Ms - nowEpochMs).coerceAtLeast(0L)
            return if (!preferences.briefingCompleted) {
                val elapsedSinceStart = (nowEpochMs - shiftStartMs).coerceAtLeast(0L)
                ShiftSnapshot(
                    phase = ShiftPhase.ACTIVE_BRIEFING,
                    nowEpochMs = nowEpochMs,
                    zoneId = zoneId,
                    currentTimeFormatted = currentTimeFormatted,
                    currentDateFormatted = currentDateFormatted,
                    shiftStartDate = startDate,
                    currentSlot = slots[0],
                    nextSlot = slots[2],
                    activeSlotIndex = 0,
                    nextTransitionEpochMs = noon12Ms,
                    nextTransitionTimeLabel = "12:00",
                    countdownMillis = remainingToNoon,
                    countdownFormatted = formatDurationHms(remainingToNoon),
                    progressFraction = null,
                    segmentElapsedFormatted = formatDurationHms(elapsedSinceStart),
                    segmentTotalFormatted = null,
                    canMarkBriefingComplete = true,
                    briefingCompleted = false,
                    briefingCompletedAtEpochMs = null,
                    shiftStartEpochMs = shiftStartMs,
                    shiftEndEpochMs = shiftEndMs,
                    elapsedShiftMinutes = elapsedMinutes
                )
            } else {
                val completedAt = preferences.briefingCompletedAtEpochMs
                val validWindow = completedAt != null && completedAt in shiftStartMs until noon12Ms
                val progress = if (validWindow) {
                    val total = (noon12Ms - completedAt!!).toDouble()
                    val elapsed = (nowEpochMs - completedAt).coerceAtLeast(0L).toDouble()
                    (elapsed / total).coerceIn(0.0, 1.0).toFloat()
                } else {
                    null
                }
                val elapsedText = if (validWindow) {
                    formatDurationHms((nowEpochMs - completedAt!!).coerceAtLeast(0L))
                } else null
                val totalText = if (validWindow) {
                    formatDurationHms((noon12Ms - completedAt!!).coerceAtLeast(0L))
                } else null

                ShiftSnapshot(
                    phase = ShiftPhase.ACTIVE_TIMED,
                    nowEpochMs = nowEpochMs,
                    zoneId = zoneId,
                    currentTimeFormatted = currentTimeFormatted,
                    currentDateFormatted = currentDateFormatted,
                    shiftStartDate = startDate,
                    currentSlot = slots[1],
                    nextSlot = slots[2],
                    activeSlotIndex = 1,
                    nextTransitionEpochMs = noon12Ms,
                    nextTransitionTimeLabel = "12:00",
                    countdownMillis = remainingToNoon,
                    countdownFormatted = formatDurationHms(remainingToNoon),
                    progressFraction = progress,
                    segmentElapsedFormatted = elapsedText,
                    segmentTotalFormatted = totalText,
                    canMarkBriefingComplete = false,
                    briefingCompleted = true,
                    briefingCompletedAtEpochMs = completedAt,
                    shiftStartEpochMs = shiftStartMs,
                    shiftEndEpochMs = shiftEndMs,
                    elapsedShiftMinutes = elapsedMinutes
                )
            }
        }

        // Between 12:00 Day 0 and 10:00 Day +1
        if (nowEpochMs < shiftEndMs) {
            val activeSlot = slots.drop(2).first { slot ->
                val s = slotStartEpochMs(startDate, slot, zoneId)!!
                val e = slotEndEpochMs(startDate, slot, zoneId)!!
                nowEpochMs in s until e
            }
            val slotStart = slotStartEpochMs(startDate, activeSlot, zoneId)!!
            val slotEnd = slotEndEpochMs(startDate, activeSlot, zoneId)!!
            val nextSlot = slots.getOrNull(activeSlot.index + 1)
            val remaining = (slotEnd - nowEpochMs).coerceAtLeast(0L)
            val total = (slotEnd - slotStart).coerceAtLeast(1L)
            val elapsed = (nowEpochMs - slotStart).coerceIn(0L, total)
            val progress = (elapsed.toDouble() / total.toDouble()).coerceIn(0.0, 1.0).toFloat()

            return ShiftSnapshot(
                phase = ShiftPhase.ACTIVE_TIMED,
                nowEpochMs = nowEpochMs,
                zoneId = zoneId,
                currentTimeFormatted = currentTimeFormatted,
                currentDateFormatted = currentDateFormatted,
                shiftStartDate = startDate,
                currentSlot = activeSlot,
                nextSlot = nextSlot,
                activeSlotIndex = activeSlot.index,
                nextTransitionEpochMs = slotEnd,
                nextTransitionTimeLabel = activeSlot.endTime?.format(TIME_HM_FORMATTER),
                countdownMillis = remaining,
                countdownFormatted = formatDurationHms(remaining),
                progressFraction = progress,
                segmentElapsedFormatted = formatDurationHms(elapsed),
                segmentTotalFormatted = formatDurationHms(total),
                canMarkBriefingComplete = false,
                briefingCompleted = preferences.briefingCompleted,
                briefingCompletedAtEpochMs = preferences.briefingCompletedAtEpochMs,
                shiftStartEpochMs = shiftStartMs,
                shiftEndEpochMs = shiftEndMs,
                elapsedShiftMinutes = elapsedMinutes
            )
        }

        // After 10:00 Day +1
        return ShiftSnapshot(
            phase = ShiftPhase.COMPLETED,
            nowEpochMs = nowEpochMs,
            zoneId = zoneId,
            currentTimeFormatted = currentTimeFormatted,
            currentDateFormatted = currentDateFormatted,
            shiftStartDate = startDate,
            currentSlot = null,
            nextSlot = null,
            activeSlotIndex = null,
            nextTransitionEpochMs = null,
            nextTransitionTimeLabel = null,
            countdownMillis = 0L,
            countdownFormatted = "00:00:00",
            progressFraction = 1f,
            segmentElapsedFormatted = null,
            segmentTotalFormatted = null,
            canMarkBriefingComplete = false,
            briefingCompleted = preferences.briefingCompleted,
            briefingCompletedAtEpochMs = preferences.briefingCompletedAtEpochMs,
            shiftStartEpochMs = shiftStartMs,
            shiftEndEpochMs = shiftEndMs,
            elapsedShiftMinutes = TOTAL_SHIFT_MINUTES
        )
    }

    /**
     * Computes all future alarm triggers from confirmed transition timestamps.
     * Note: Slot 1 (post-briefing break) has an unknown start time so no lead-time or transition
     * alarm is ever fabricated for it.
     */
    fun computeScheduledAlerts(
        preferences: ShiftPreferences,
        nowEpochMs: Long,
        deviceZone: ZoneId = ZoneId.systemDefault()
    ): List<ScheduledAlertEvent> {
        if (!preferences.isShiftActive) return emptyList()
        val zoneId = resolveZone(preferences.useDeviceTimezone, deviceZone)
        val startDate = preferences.shiftStartDate

        // Confirmed transition boundaries (slotIndex 0 and 2..11 start times, plus terminal end at 10:00 Day +1)
        data class ConfirmedBoundary(
            val index: Int,
            val assignment: AssignmentType,
            val dayOffset: Int,
            val time: LocalTime
        )

        val boundaries = buildList {
            add(ConfirmedBoundary(0, AssignmentType.BRIEFING, 0, LocalTime.of(9, 30)))
            slots.drop(2).forEach { slot ->
                add(
                    ConfirmedBoundary(
                        index = slot.index,
                        assignment = slot.assignmentType,
                        dayOffset = slot.startDayOffset,
                        time = slot.startTime!!
                    )
                )
            }
            // Shift completion boundary at 10:00 on Day +1
            add(ConfirmedBoundary(12, AssignmentType.ATTENTION, 1, LocalTime.of(10, 0)))
        }

        val activeOffsets = buildSet {
            addAll(preferences.reminderOffsetsMinutes.filter { it in ALL_REMINDER_OFFSETS })
            if (preferences.notifyAtTransition) add(0)
        }

        val events = mutableListOf<ScheduledAlertEvent>()
        for (boundary in boundaries) {
            val transitionZdt = ZonedDateTime.of(
                startDate.plusDays(boundary.dayOffset.toLong()),
                boundary.time,
                zoneId
            )
            val transitionMs = transitionZdt.toInstant().toEpochMilli()
            val timeLabel = boundary.time.format(TIME_HM_FORMATTER)

            for (offset in activeOffsets) {
                val triggerMs = transitionMs - offset * 60_000L
                if (triggerMs > nowEpochMs) {
                    events += ScheduledAlertEvent(
                        requestCode = requestCodeFor(boundary.index, offset, isOngoingRefresh = false),
                        slotIndex = boundary.index,
                        targetAssignment = boundary.assignment,
                        offsetMinutes = offset,
                        triggerEpochMs = triggerMs,
                        transitionEpochMs = transitionMs,
                        transitionTimeLabel = timeLabel,
                        isNextCalendarDay = boundary.dayOffset > 0,
                        isSilentOngoingRefresh = false
                    )
                }
            }

            // If ongoing status notification is enabled and exact transition notifications are OFF,
            // schedule a silent refresh alarm at exact transition boundaries so the ongoing card updates.
            if (preferences.ongoingStatusNotification && !preferences.notifyAtTransition) {
                if (transitionMs > nowEpochMs) {
                    events += ScheduledAlertEvent(
                        requestCode = requestCodeFor(boundary.index, 0, isOngoingRefresh = true),
                        slotIndex = boundary.index,
                        targetAssignment = boundary.assignment,
                        offsetMinutes = 0,
                        triggerEpochMs = transitionMs,
                        transitionEpochMs = transitionMs,
                        transitionTimeLabel = timeLabel,
                        isNextCalendarDay = boundary.dayOffset > 0,
                        isSilentOngoingRefresh = true
                    )
                }
            }
        }

        return events.sortedBy { it.triggerEpochMs }
    }

    fun allPossibleRequestCodes(): List<Int> = buildList {
        val indices = listOf(0) + (2..12).toList()
        val offsets = listOf(0) + ALL_REMINDER_OFFSETS
        for (idx in indices) {
            for (offset in offsets) {
                add(requestCodeFor(idx, offset, isOngoingRefresh = false))
            }
            add(requestCodeFor(idx, 0, isOngoingRefresh = true))
        }
    }

    fun requestCodeFor(slotIndex: Int, offsetMinutes: Int, isOngoingRefresh: Boolean): Int {
        return if (isOngoingRefresh) {
            20_000 + slotIndex * 100
        } else {
            10_000 + slotIndex * 100 + offsetMinutes
        }
    }

    /**
     * Ceiling-second or floor-second formatting? Using whole-second boundary floor with ceiling on
     * sub-second boundary gives clean ticks: e.g., 59_999 ms -> 00:00:59 or 59_001 -> 00:00:59,
     * wait: if target is 12:00:00.000 and now is 11:59:59.050, remaining = 950ms -> 00:00:01 or 00:00:00?
     * Standard digital countdowns aligned to second boundaries:
     * Let's inspect: at 11:59:59.000, remaining = 1000ms -> 00:00:01! At 11:59:59.050, remaining = 950ms.
     * If we use ceiling to nearest second `(remainingMillis + 999L) / 1000L` OR `(remainingMillis / 1000L)`:
     * Wait: at 11:59:59.000 -> `1000 / 1000 = 1`, `(1000 + 999) / 1000 = 1`!
     * At 11:59:59.200 (0.2s after the boundary) -> `remaining = 800ms`: if floor, it shows 00:00:00 for 0.8s BEFORE 12:00:00! Whereas ceiling `(800 + 999) / 1000 = 1` shows `00:00:01` all through 11:59:59.xxx and flips cleanly to next assignment at 12:00:00.000!
     * AND at exact boundary 11:59:00.000 -> remaining = 60_000ms -> `(60000 + 999) / 1000 = 60` -> `00:01:00`!
     * Let's verify: 11:59:00.000 => 00:01:00; 11:59:01.000 => 00:00:59; ... 11:59:59.000 => 00:00:01; 12:00:00.000 => transitions to next slot!
     * Ceiling `(millis + 999L) / 1000L` (when `millis > 0`) matches exact second-boundary clocks AND avoids showing `00:00:00` prematurely for 999ms!
     */
    fun formatDurationHms(millis: Long): String {
        if (millis <= 0L) return "00:00:00"
        val totalSeconds = (millis + 999L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
