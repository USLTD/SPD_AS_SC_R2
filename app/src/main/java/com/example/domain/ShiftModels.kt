package com.example.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class AssignmentType {
    BRIEFING,
    BREAK,
    CHECKPOINT_VI,
    CHECKPOINT_III,
    ATTENTION
}

enum class ShiftPhase {
    INACTIVE,
    BEFORE_SHIFT,
    ACTIVE_BRIEFING,
    ACTIVE_TIMED,
    COMPLETED
}

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    GEORGIAN("ka");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

data class ScheduleSlot(
    val index: Int,
    val assignmentType: AssignmentType,
    val startDayOffset: Int,
    val startTime: LocalTime?,
    val endDayOffset: Int,
    val endTime: LocalTime?,
    val isPostBriefingBreak: Boolean = false
)

data class ScheduledAlertEvent(
    val requestCode: Int,
    val slotIndex: Int,
    val targetAssignment: AssignmentType,
    val offsetMinutes: Int,
    val triggerEpochMs: Long,
    val transitionEpochMs: Long,
    val transitionTimeLabel: String,
    val isNextCalendarDay: Boolean,
    val isSilentOngoingRefresh: Boolean = false
)

data class ShiftSnapshot(
    val phase: ShiftPhase,
    val nowEpochMs: Long,
    val zoneId: ZoneId,
    val currentTimeFormatted: String,
    val currentDateFormatted: String,
    val shiftStartDate: LocalDate,
    val currentSlot: ScheduleSlot?,
    val nextSlot: ScheduleSlot?,
    val activeSlotIndex: Int?,
    val nextTransitionEpochMs: Long?,
    val nextTransitionTimeLabel: String?,
    val countdownMillis: Long,
    val countdownFormatted: String,
    val progressFraction: Float?,
    val segmentElapsedFormatted: String?,
    val segmentTotalFormatted: String?,
    val canMarkBriefingComplete: Boolean,
    val briefingCompleted: Boolean,
    val briefingCompletedAtEpochMs: Long?,
    val shiftStartEpochMs: Long? = null,
    val shiftEndEpochMs: Long? = null,
    val elapsedShiftMinutes: Int = 0
)
