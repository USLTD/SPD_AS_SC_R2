package com.example

import com.example.data.ShiftPreferences
import com.example.domain.AssignmentType
import com.example.domain.ShiftPhase
import com.example.domain.ShiftScheduleEngine
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftScheduleEngineTest {

    private val tbilisi = ZoneId.of("Asia/Tbilisi")
    private val shiftDate = LocalDate.of(2026, 11, 13)

    private fun epoch(date: LocalDate, time: LocalTime, zone: ZoneId = tbilisi): Long =
        ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()

    @Test
    fun `inactive shift returns INACTIVE phase and zero countdown`() {
        val prefs = ShiftPreferences(isShiftActive = false, shiftStartDate = shiftDate)
        val snap = ShiftScheduleEngine.evaluate(prefs, epoch(shiftDate, LocalTime.of(12, 30)))

        assertEquals(ShiftPhase.INACTIVE, snap.phase)
        assertEquals("00:00:00", snap.countdownFormatted)
        assertNull(snap.currentSlot)
    }

    @Test
    fun `before shift start counts down to 09_30 briefing`() {
        val prefs = ShiftPreferences(isShiftActive = true, shiftStartDate = shiftDate)
        val now = epoch(shiftDate, LocalTime.of(8, 15, 30))
        val snap = ShiftScheduleEngine.evaluate(prefs, now)

        assertEquals(ShiftPhase.BEFORE_SHIFT, snap.phase)
        assertEquals("01:14:30", snap.countdownFormatted)
        assertEquals(AssignmentType.BRIEFING, snap.nextSlot?.assignmentType)
        assertFalse(snap.canMarkBriefingComplete)
    }

    @Test
    fun `open-ended briefing has unknown end time and switches manually to first break`() {
        val initialPrefs = ShiftPreferences(
            isShiftActive = true,
            shiftStartDate = shiftDate,
            briefingCompleted = false
        )
        val at1015 = epoch(shiftDate, LocalTime.of(10, 15, 0))
        val briefingSnap = ShiftScheduleEngine.evaluate(initialPrefs, at1015)

        assertEquals(ShiftPhase.ACTIVE_BRIEFING, briefingSnap.phase)
        assertEquals(AssignmentType.BRIEFING, briefingSnap.currentSlot?.assignmentType)
        assertNull(briefingSnap.progressFraction)
        assertNull(briefingSnap.segmentTotalFormatted)
        assertTrue(briefingSnap.canMarkBriefingComplete)
        assertEquals("01:45:00", briefingSnap.countdownFormatted)
        assertEquals(AssignmentType.CHECKPOINT_VI, briefingSnap.nextSlot?.assignmentType)

        // Mark briefing complete at 10:15 -> switches to post-briefing Break until 12:00
        val completedPrefs = initialPrefs.copy(
            briefingCompleted = true,
            briefingCompletedAtEpochMs = at1015
        )
        val at110730 = epoch(shiftDate, LocalTime.of(11, 7, 30))
        val breakSnap = ShiftScheduleEngine.evaluate(completedPrefs, at110730)

        assertEquals(ShiftPhase.ACTIVE_TIMED, breakSnap.phase)
        assertEquals(AssignmentType.BREAK, breakSnap.currentSlot?.assignmentType)
        assertTrue(breakSnap.currentSlot?.isPostBriefingBreak == true)
        assertFalse(breakSnap.canMarkBriefingComplete)
        assertEquals("00:52:30", breakSnap.countdownFormatted)
        assertEquals(0.5f, breakSnap.progressFraction!!, 0.01f)
    }

    @Test
    fun `midnight transition switches cleanly from Day 0 Break to Day 1 Checkpoint VI`() {
        val prefs = ShiftPreferences(
            isShiftActive = true,
            shiftStartDate = shiftDate,
            briefingCompleted = true
        )
        val nextDay = shiftDate.plusDays(1)

        // One second before midnight on Day 0
        val beforeMidnight = epoch(shiftDate, LocalTime.of(23, 59, 59))
        val snapBefore = ShiftScheduleEngine.evaluate(prefs, beforeMidnight)
        assertEquals(ShiftPhase.ACTIVE_TIMED, snapBefore.phase)
        assertEquals(AssignmentType.BREAK, snapBefore.currentSlot?.assignmentType)
        assertEquals(7, snapBefore.activeSlotIndex)
        assertEquals(AssignmentType.CHECKPOINT_VI, snapBefore.nextSlot?.assignmentType)
        assertEquals(1, snapBefore.nextSlot?.startDayOffset)
        assertEquals("00:00:01", snapBefore.countdownFormatted)

        // Exact midnight on Day +1
        val atMidnight = epoch(nextDay, LocalTime.of(0, 0, 0))
        val snapAt = ShiftScheduleEngine.evaluate(prefs, atMidnight)
        assertEquals(ShiftPhase.ACTIVE_TIMED, snapAt.phase)
        assertEquals(AssignmentType.CHECKPOINT_VI, snapAt.currentSlot?.assignmentType)
        assertEquals(8, snapAt.activeSlotIndex)
        assertEquals(1, snapAt.currentSlot?.startDayOffset)
        assertEquals("03:00:00", snapAt.countdownFormatted)
        assertEquals(AssignmentType.CHECKPOINT_III, snapAt.nextSlot?.assignmentType)
    }

    @Test
    fun `all post-midnight slots resolve on next calendar day through shift completion at 10_00`() {
        val prefs = ShiftPreferences(
            isShiftActive = true,
            shiftStartDate = shiftDate,
            briefingCompleted = true
        )
        val nextDay = shiftDate.plusDays(1)

        val at0330 = ShiftScheduleEngine.evaluate(prefs, epoch(nextDay, LocalTime.of(3, 30)))
        assertEquals(AssignmentType.CHECKPOINT_III, at0330.currentSlot?.assignmentType)
        assertEquals("02:30:00", at0330.countdownFormatted)

        val at0615 = ShiftScheduleEngine.evaluate(prefs, epoch(nextDay, LocalTime.of(6, 15)))
        assertEquals(AssignmentType.BREAK, at0615.currentSlot?.assignmentType)
        assertEquals("02:15:00", at0615.countdownFormatted)

        val at095959 = ShiftScheduleEngine.evaluate(prefs, epoch(nextDay, LocalTime.of(9, 59, 59)))
        assertEquals(AssignmentType.ATTENTION, at095959.currentSlot?.assignmentType)
        assertEquals("00:00:01", at095959.countdownFormatted)
        assertNull(at095959.nextSlot)

        // Shift complete at 10:00:00 Day +1
        val at100000 = ShiftScheduleEngine.evaluate(prefs, epoch(nextDay, LocalTime.of(10, 0, 0)))
        assertEquals(ShiftPhase.COMPLETED, at100000.phase)
        assertNull(at100000.currentSlot)
        assertEquals("00:00:00", at100000.countdownFormatted)
        assertEquals(1f, at100000.progressFraction)
    }

    @Test
    fun `scheduled alerts never fabricate first break transition and respect midnight next-day flag`() {
        val prefs = ShiftPreferences(
            isShiftActive = true,
            shiftStartDate = shiftDate,
            reminderOffsetsMinutes = setOf(15),
            notifyAtTransition = true,
            ongoingStatusNotification = false
        )
        val now = epoch(shiftDate, LocalTime.of(9, 0))
        val alerts = ShiftScheduleEngine.computeScheduledAlerts(prefs, now)

        // Slot 1 (post-briefing break) must NOT appear in scheduled alerts
        assertTrue(alerts.none { it.slotIndex == 1 })
        // Slot 8 (00:00 Checkpoint VI) must have isNextCalendarDay = true
        val midnightAlerts = alerts.filter { it.slotIndex == 8 }
        assertEquals(2, midnightAlerts.size) // 15m before + exact 00:00
        assertTrue(midnightAlerts.all { it.isNextCalendarDay })
    }

    @Test
    fun `Asia_Tbilisi default vs device timezone resolves shift clock appropriately`() {
        val deviceZone = ZoneId.of("UTC")
        val prefsTbilisi = ShiftPreferences(
            isShiftActive = true,
            shiftStartDate = shiftDate,
            useDeviceTimezone = false
        )
        val prefsDevice = prefsTbilisi.copy(useDeviceTimezone = true)

        // 08:00 UTC == 12:00 Asia/Tbilisi (UTC+4)
        val instantMs = epoch(shiftDate, LocalTime.of(8, 0), deviceZone)

        val tbilisiSnap = ShiftScheduleEngine.evaluate(prefsTbilisi, instantMs, deviceZone)
        assertEquals("12:00:00", tbilisiSnap.currentTimeFormatted)
        assertEquals(AssignmentType.CHECKPOINT_VI, tbilisiSnap.currentSlot?.assignmentType)

        val deviceSnap = ShiftScheduleEngine.evaluate(prefsDevice, instantMs, deviceZone)
        assertEquals("08:00:00", deviceSnap.currentTimeFormatted)
        assertEquals(ShiftPhase.BEFORE_SHIFT, deviceSnap.phase)
    }
}
