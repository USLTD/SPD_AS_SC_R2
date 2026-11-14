package com.example

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.data.ShiftPreferences
import com.example.data.ShiftPreferencesRepository
import com.example.domain.AppLanguage
import com.example.domain.AssignmentType
import com.example.domain.ShiftPhase
import com.example.domain.ShiftScheduleEngine
import com.example.notifications.ShiftAlarmScheduler
import com.example.notifications.ShiftNotificationHelper
import com.example.notifications.SystemEventReceiver
import com.example.ui.RibbonTab
import com.example.ui.ShiftUiState
import com.example.ui.ShiftViewModel
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.ShiftCompanionTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h640dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val tbilisi = ZoneId.of("Asia/Tbilisi")
    private val shiftDate = LocalDate.of(2026, 11, 13)

    private fun epoch(date: LocalDate, time: LocalTime): Long =
        ZonedDateTime.of(date, time, tbilisi).toInstant().toEpochMilli()

    @Test
    fun `read string from context matches VI-III Shift`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VI–III Shift", appName)
    }

    @Test
    fun `permission changes switch between exact and inexact fallback alarms`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        val scheduler = ShiftAlarmScheduler(context)

        val prefs = ShiftPreferences(
            isShiftActive = true,
            shiftStartDate = shiftDate,
            reminderOffsetsMinutes = setOf(15),
            notifyAtTransition = true,
            ongoingStatusNotification = false
        )
        val now = epoch(shiftDate, LocalTime.of(11, 0))

        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val exactResult = scheduler.syncAlarms(prefs, now)
        assertTrue(exactResult.usedExactAlarms)
        assertTrue(exactResult.scheduledCount > 0)
        assertEquals(exactResult.scheduledCount, shadowAlarmManager.scheduledAlarms.size)
        assertTrue(shadowAlarmManager.scheduledAlarms.all { it.triggerAtMs > now })

        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val fallbackResult = scheduler.syncAlarms(prefs, now)
        assertFalse(fallbackResult.usedExactAlarms)
        assertEquals(exactResult.scheduledCount, fallbackResult.scheduledCount)
        assertEquals(fallbackResult.scheduledCount, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun `deactivating shift cancels all scheduled alarms`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        val scheduler = ShiftAlarmScheduler(context)

        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val activePrefs = ShiftPreferences(
            isShiftActive = true,
            shiftStartDate = shiftDate,
            reminderOffsetsMinutes = setOf(10, 30),
            notifyAtTransition = true
        )
        val now = epoch(shiftDate, LocalTime.of(11, 0))
        scheduler.syncAlarms(activePrefs, now)
        assertTrue(shadowAlarmManager.scheduledAlarms.isNotEmpty())

        scheduler.syncAlarms(activePrefs.copy(isShiftActive = false), now)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun `reboot broadcast restores future alarms from DataStore`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        val repo = ShiftPreferencesRepository(context)
        val futureDate = LocalDate.now(tbilisi).plusDays(1)
        repo.activateShift(futureDate, System.currentTimeMillis())

        ShiftAlarmScheduler(context).cancelAllAlarms()
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)

        val receiver = SystemEventReceiver()
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        Thread.sleep(400)

        assertTrue(
            "Expected future alarms to be restored after BOOT_COMPLETED",
            shadowAlarmManager.scheduledAlarms.isNotEmpty()
        )
        repo.deactivateShift()
    }

    @Test
    fun `foreground restoration recalculates countdown without drift after multi-hour jump`() =
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<Application>()
            val repo = ShiftPreferencesRepository(app)
            val scheduler = ShiftAlarmScheduler(app)

            var fakeNowMs = epoch(shiftDate, LocalTime.of(10, 15, 0))
            repo.activateShift(shiftDate, fakeNowMs)
            repo.markBriefingComplete(fakeNowMs)

            val viewModel = ShiftViewModel(
                application = app,
                repository = repo,
                alarmScheduler = scheduler,
                nowProvider = { fakeNowMs },
                notificationPermissionChecker = { true },
                liveUpdatesSupportedChecker = { true },
                liveUpdatesPermissionChecker = { true },
                ongoingSyncHandler = { _, _ -> },
                testNotificationDispatcher = { true },
                startTicker = false
            )

            val initialPrefs = repo.preferencesFlow.first()
            val initialSnap = ShiftScheduleEngine.evaluate(initialPrefs, fakeNowMs)
            assertEquals(AssignmentType.BREAK, initialSnap.currentSlot?.assignmentType)
            assertEquals("01:45:00", initialSnap.countdownFormatted)

            fakeNowMs = epoch(shiftDate, LocalTime.of(14, 15, 0))
            viewModel.refreshNowAndPermissions()

            val restoredSnap = ShiftScheduleEngine.evaluate(repo.getSnapshot(), fakeNowMs)
            assertEquals(ShiftPhase.ACTIVE_TIMED, restoredSnap.phase)
            assertEquals(AssignmentType.CHECKPOINT_III, restoredSnap.currentSlot?.assignmentType)
            assertEquals("00:45:00", restoredSnap.countdownFormatted)

            repo.deactivateShift()
        }

    @Test
    fun `Android 16 Live Update notification uses chronometer countdown ProgressStyle and respects dismissal`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Shadows.shadowOf(context.applicationContext as Application)
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = Shadows.shadowOf(nm)

        val prefs = ShiftPreferences(
            isShiftActive = true,
            shiftStartDate = shiftDate,
            briefingCompleted = false,
            ongoingStatusNotification = true,
            liveUpdateDismissedForShift = false
        )
        val now = epoch(shiftDate, LocalTime.of(10, 0, 0))
        val snapshot = ShiftScheduleEngine.evaluate(prefs, now)

        ShiftNotificationHelper.syncOngoingNotification(context, prefs, snapshot)
        val posted = shadowNm.getNotification(ShiftNotificationHelper.ONGOING_NOTIFICATION_ID)
        assertNotNull(posted)
        assertTrue((posted.flags and Notification.FLAG_ONGOING_EVENT) != 0)
        assertTrue(posted.extras.getBoolean("android.requestPromotedOngoing", false))
        assertTrue(posted.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false))
        assertTrue(posted.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false))
        assertEquals(snapshot.nextTransitionEpochMs, posted.`when`)
        assertEquals(1, posted.actions?.size ?: 0)

        // Respect dismissal for current shift
        ShiftNotificationHelper.syncOngoingNotification(
            context,
            prefs.copy(liveUpdateDismissedForShift = true),
            snapshot
        )
        assertNull(shadowNm.getNotification(ShiftNotificationHelper.ONGOING_NOTIFICATION_ID))
    }

    @Test
    fun `Shift tab fits countdown and Mark briefing complete in a single viewport without scrolling`() {
        val prefs = ShiftPreferences(
            isShiftActive = true,
            shiftStartDate = shiftDate,
            briefingCompleted = false,
            language = AppLanguage.ENGLISH
        )
        val now = epoch(shiftDate, LocalTime.of(10, 0, 0))
        val snapshot = ShiftScheduleEngine.evaluate(prefs, now)
        val uiState = ShiftUiState(preferences = prefs, snapshot = snapshot)

        composeTestRule.setContent {
            ShiftCompanionTheme(darkTheme = false, language = AppLanguage.ENGLISH) {
                ShiftCompanionScaffold(
                    uiState = uiState,
                    strings = AppStrings.forLanguage(AppLanguage.ENGLISH),
                    onSelectTab = {},
                    onToggleLanguage = {},
                    onToggleTheme = {},
                    onSelectDate = {},
                    onActivateShift = {},
                    onDeactivateShift = {},
                    onMarkBriefingComplete = {},
                    onToggleReminderOffset = {},
                    onSetNotifyAtTransition = {},
                    onSetOngoingNotification = {},
                    onSetUseDeviceTimezone = {},
                    onRequestPostNotifications = {},
                    onOpenExactAlarmSettings = {},
                    onOpenLiveUpdatesSettings = {},
                    onSendTestNotification = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("countdown_display").assertIsDisplayed()
        composeTestRule.onNodeWithText("02:00:00").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mark_briefing_complete_button").assertIsDisplayed()
    }

    @Test
    fun `Reminders tab fits all controls and Test Alert button in a single viewport without scrolling`() {
        val prefs = ShiftPreferences(
            isShiftActive = true,
            shiftStartDate = shiftDate,
            language = AppLanguage.ENGLISH
        )
        val now = epoch(shiftDate, LocalTime.of(10, 0, 0))
        val uiState = ShiftUiState(
            preferences = prefs,
            snapshot = ShiftScheduleEngine.evaluate(prefs, now),
            selectedTab = RibbonTab.REMINDERS
        )

        composeTestRule.setContent {
            ShiftCompanionTheme(darkTheme = false, language = AppLanguage.ENGLISH) {
                ShiftCompanionScaffold(
                    uiState = uiState,
                    strings = AppStrings.forLanguage(AppLanguage.ENGLISH),
                    onSelectTab = {},
                    onToggleLanguage = {},
                    onToggleTheme = {},
                    onSelectDate = {},
                    onActivateShift = {},
                    onDeactivateShift = {},
                    onMarkBriefingComplete = {},
                    onToggleReminderOffset = {},
                    onSetNotifyAtTransition = {},
                    onSetOngoingNotification = {},
                    onSetUseDeviceTimezone = {},
                    onRequestPostNotifications = {},
                    onOpenExactAlarmSettings = {},
                    onOpenLiveUpdatesSettings = {},
                    onSendTestNotification = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reminder_chip_15").assertIsDisplayed()
        composeTestRule.onNodeWithTag("live_updates_status").assertIsDisplayed()
        composeTestRule.onNodeWithTag("test_notification_button").assertIsDisplayed()
    }
}
