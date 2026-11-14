package com.example.data

import com.example.domain.AppLanguage
import java.time.LocalDate

data class ShiftPreferences(
    val isShiftActive: Boolean = false,
    val shiftStartDate: LocalDate = LocalDate.now(),
    val briefingCompleted: Boolean = false,
    val briefingCompletedAtEpochMs: Long? = null,
    val reminderOffsetsMinutes: Set<Int> = setOf(15),
    val notifyAtTransition: Boolean = true,
    val ongoingStatusNotification: Boolean = true,
    val liveUpdateDismissedForShift: Boolean = false,
    val useDeviceTimezone: Boolean = false,
    val darkTheme: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH
)
