package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ShiftPreferences
import com.example.data.ShiftPreferencesRepository
import com.example.domain.AppLanguage
import com.example.domain.ShiftPhase
import com.example.domain.ShiftScheduleEngine
import com.example.domain.ShiftSnapshot
import com.example.notifications.AlarmSchedulingResult
import com.example.notifications.ShiftAlarmScheduler
import com.example.notifications.ShiftNotificationHelper
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RibbonTab {
    SHIFT,
    TIMETABLE,
    REMINDERS,
    ABOUT
}

data class PermissionStatus(
    val postNotificationsGranted: Boolean = true,
    val exactAlarmsGranted: Boolean = true,
    val liveUpdatesSupported: Boolean = false,
    val liveUpdatesGranted: Boolean = false
)

data class ShiftUiState(
    val preferences: ShiftPreferences = ShiftPreferences(),
    val snapshot: ShiftSnapshot = ShiftScheduleEngine.evaluate(
        ShiftPreferences(),
        System.currentTimeMillis()
    ),
    val permissions: PermissionStatus = PermissionStatus(),
    val schedulingResult: AlarmSchedulingResult = AlarmSchedulingResult(0, true, false),
    val selectedTab: RibbonTab = RibbonTab.SHIFT,
    val testNotificationMessage: String? = null
)

class ShiftViewModel(
    application: Application,
    private val repository: ShiftPreferencesRepository,
    private val alarmScheduler: ShiftAlarmScheduler,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val notificationPermissionChecker: () -> Boolean = {
        ShiftNotificationHelper.hasNotificationPermission(application)
    },
    private val liveUpdatesSupportedChecker: () -> Boolean = {
        ShiftNotificationHelper.isLiveUpdatesApiSupported()
    },
    private val liveUpdatesPermissionChecker: () -> Boolean = {
        ShiftNotificationHelper.canPostPromotedNotifications(application)
    },
    private val ongoingSyncHandler: (ShiftPreferences, ShiftSnapshot) -> Unit = { prefs, snap ->
        ShiftNotificationHelper.syncOngoingNotification(application, prefs, snap)
    },
    private val testNotificationDispatcher: (ShiftPreferences) -> Boolean = { prefs ->
        ShiftNotificationHelper.postTestNotification(application, prefs)
    },
    startTicker: Boolean = true
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        repository = ShiftPreferencesRepository(application.applicationContext),
        alarmScheduler = ShiftAlarmScheduler(application.applicationContext)
    )

    private val nowEpochMs = MutableStateFlow(nowProvider())
    private val selectedTab = MutableStateFlow(RibbonTab.SHIFT)
    private val permissions = MutableStateFlow(readPermissions())
    private val schedulingResult = MutableStateFlow(
        AlarmSchedulingResult(0, alarmScheduler.canScheduleExactAlarms(), false)
    )
    private val testMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ShiftUiState> = combine(
        repository.preferencesFlow,
        nowEpochMs,
        permissions,
        schedulingResult,
        selectedTab,
        testMessage
    ) { values ->
        val prefs = values[0] as ShiftPreferences
        val now = values[1] as Long
        val perms = values[2] as PermissionStatus
        val sched = values[3] as AlarmSchedulingResult
        val tab = values[4] as RibbonTab
        val msg = values[5] as String?

        val snapshot = ShiftScheduleEngine.evaluate(prefs, now)
        ShiftUiState(
            preferences = prefs,
            snapshot = snapshot,
            permissions = perms,
            schedulingResult = sched,
            selectedTab = tab,
            testNotificationMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ShiftUiState()
    )

    init {
        if (startTicker) {
            viewModelScope.launch {
                while (isActive) {
                    val current = nowProvider()
                    nowEpochMs.value = current
                    val nextBoundaryDelay = 1000L - (current % 1000L)
                    delay(nextBoundaryDelay.coerceIn(50L, 1000L))
                }
            }
        }

        // Sync alarms whenever scheduling-relevant preferences change
        viewModelScope.launch {
            repository.preferencesFlow
                .distinctUntilChanged { old, new ->
                    old.isShiftActive == new.isShiftActive &&
                        old.shiftStartDate == new.shiftStartDate &&
                        old.reminderOffsetsMinutes == new.reminderOffsetsMinutes &&
                        old.notifyAtTransition == new.notifyAtTransition &&
                        old.ongoingStatusNotification == new.ongoingStatusNotification &&
                        old.useDeviceTimezone == new.useDeviceTimezone
                }
                .collect { prefs ->
                    val now = nowProvider()
                    schedulingResult.value = alarmScheduler.syncAlarms(prefs, now)
                    val snap = ShiftScheduleEngine.evaluate(prefs, now)
                    ongoingSyncHandler(prefs, snap)
                }
        }

        // Refresh ongoing status notification when active slot, briefing status, or language changes
        viewModelScope.launch {
            uiState
                .map { state ->
                    Triple(
                        state.snapshot.phase,
                        state.snapshot.activeSlotIndex,
                        state.preferences.language to state.preferences.ongoingStatusNotification
                    )
                }
                .distinctUntilChanged()
                .collect {
                    val current = uiState.value
                    if (current.preferences.isShiftActive &&
                        current.snapshot.phase != ShiftPhase.INACTIVE
                    ) {
                        ongoingSyncHandler(current.preferences, current.snapshot)
                    }
                }
        }
    }

    private fun readPermissions(): PermissionStatus = PermissionStatus(
        postNotificationsGranted = notificationPermissionChecker(),
        exactAlarmsGranted = alarmScheduler.canScheduleExactAlarms(),
        liveUpdatesSupported = liveUpdatesSupportedChecker(),
        liveUpdatesGranted = liveUpdatesPermissionChecker()
    )

    fun refreshNowAndPermissions() {
        val now = nowProvider()
        nowEpochMs.value = now
        val newPerms = readPermissions()
        val exactChanged = newPerms.exactAlarmsGranted != permissions.value.exactAlarmsGranted
        permissions.value = newPerms

        val prefs = uiState.value.preferences
        if (exactChanged || prefs.isShiftActive) {
            schedulingResult.value = alarmScheduler.syncAlarms(prefs, now)
        }
        val snap = ShiftScheduleEngine.evaluate(prefs, now)
        ongoingSyncHandler(prefs, snap)
    }

    fun selectTab(tab: RibbonTab) {
        selectedTab.value = tab
    }

    fun openShiftTabFromIntent() {
        selectedTab.value = RibbonTab.SHIFT
        refreshNowAndPermissions()
    }

    fun setShiftStartDate(date: LocalDate) {
        viewModelScope.launch {
            repository.setShiftStartDate(date)
        }
    }

    fun activateShift() {
        viewModelScope.launch {
            val prefs = uiState.value.preferences
            repository.activateShift(prefs.shiftStartDate, nowProvider())
        }
    }

    fun deactivateShift() {
        viewModelScope.launch {
            repository.deactivateShift()
        }
    }

    fun markBriefingComplete() {
        viewModelScope.launch {
            repository.markBriefingComplete(nowProvider())
        }
    }

    fun toggleReminderOffset(minutes: Int) {
        viewModelScope.launch {
            repository.toggleReminderOffset(minutes)
        }
    }

    fun setNotifyAtTransition(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotifyAtTransition(enabled)
        }
    }

    fun setOngoingStatusNotification(enabled: Boolean) {
        viewModelScope.launch {
            repository.setOngoingStatusNotification(enabled)
        }
    }

    fun setUseDeviceTimezone(useDevice: Boolean) {
        viewModelScope.launch {
            repository.setUseDeviceTimezone(useDevice)
        }
    }

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch {
            repository.setDarkTheme(dark)
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (uiState.value.preferences.language == AppLanguage.ENGLISH) {
                AppLanguage.GEORGIAN
            } else {
                AppLanguage.ENGLISH
            }
            repository.setLanguage(next)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setLanguage(language)
        }
    }

    fun sendTestNotification(successMessage: String, deniedMessage: String) {
        refreshNowAndPermissions()
        val sent = testNotificationDispatcher(uiState.value.preferences)
        testMessage.value = if (sent) successMessage else deniedMessage
    }

    fun clearTestNotificationMessage() {
        testMessage.value = null
    }
}
