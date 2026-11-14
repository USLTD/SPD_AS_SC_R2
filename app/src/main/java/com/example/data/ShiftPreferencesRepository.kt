package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AppLanguage
import com.example.domain.ShiftScheduleEngine
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.shiftPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "shift_companion_prefs"
)

class ShiftPreferencesRepository(private val context: Context) {

    private object Keys {
        val IS_SHIFT_ACTIVE = booleanPreferencesKey("is_shift_active")
        val SHIFT_START_DATE = stringPreferencesKey("shift_start_date")
        val BRIEFING_COMPLETED = booleanPreferencesKey("briefing_completed")
        val BRIEFING_COMPLETED_AT = longPreferencesKey("briefing_completed_at")
        val REMINDER_OFFSETS = stringSetPreferencesKey("reminder_offsets")
        val NOTIFY_AT_TRANSITION = booleanPreferencesKey("notify_at_transition")
        val ONGOING_STATUS_NOTIFICATION = booleanPreferencesKey("ongoing_status_notification")
        val LIVE_UPDATE_DISMISSED = booleanPreferencesKey("live_update_dismissed")
        val USE_DEVICE_TIMEZONE = booleanPreferencesKey("use_device_timezone")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val preferencesFlow: Flow<ShiftPreferences> =
        context.shiftPreferencesDataStore.data.map { prefs -> mapPreferences(prefs) }

    suspend fun getSnapshot(): ShiftPreferences = preferencesFlow.first()

    suspend fun setShiftStartDate(date: LocalDate) {
        context.shiftPreferencesDataStore.edit { prefs ->
            prefs[Keys.SHIFT_START_DATE] = date.toString()
            prefs[Keys.BRIEFING_COMPLETED] = false
            prefs[Keys.LIVE_UPDATE_DISMISSED] = false
            prefs.remove(Keys.BRIEFING_COMPLETED_AT)
            Unit
        }
    }

    suspend fun activateShift(startDate: LocalDate, nowEpochMs: Long) {
        context.shiftPreferencesDataStore.edit { prefs ->
            val useDeviceTz = prefs[Keys.USE_DEVICE_TIMEZONE] ?: false
            val zoneId = ShiftScheduleEngine.resolveZone(useDeviceTz)
            val noonMs = ShiftScheduleEngine.slotStartEpochMs(
                startDate,
                ShiftScheduleEngine.slots[2],
                zoneId
            )!!
            // If activating mid-shift already past 12:00, mark briefing implicitly resolved
            val alreadyPastNoon = nowEpochMs >= noonMs
            prefs[Keys.SHIFT_START_DATE] = startDate.toString()
            prefs[Keys.IS_SHIFT_ACTIVE] = true
            prefs[Keys.BRIEFING_COMPLETED] = alreadyPastNoon
            prefs[Keys.LIVE_UPDATE_DISMISSED] = false
            prefs.remove(Keys.BRIEFING_COMPLETED_AT)
            Unit
        }
    }

    suspend fun deactivateShift() {
        context.shiftPreferencesDataStore.edit { prefs ->
            prefs[Keys.IS_SHIFT_ACTIVE] = false
            prefs[Keys.BRIEFING_COMPLETED] = false
            prefs[Keys.LIVE_UPDATE_DISMISSED] = false
            prefs.remove(Keys.BRIEFING_COMPLETED_AT)
            Unit
        }
    }

    suspend fun markBriefingComplete(completedAtEpochMs: Long) {
        context.shiftPreferencesDataStore.edit { prefs ->
            prefs[Keys.BRIEFING_COMPLETED] = true
            prefs[Keys.BRIEFING_COMPLETED_AT] = completedAtEpochMs
        }
    }

    suspend fun toggleReminderOffset(minutes: Int) {
        context.shiftPreferencesDataStore.edit { prefs ->
            val current = prefs[Keys.REMINDER_OFFSETS]?.mapNotNull { it.toIntOrNull() }?.toMutableSet()
                ?: mutableSetOf(15)
            if (current.contains(minutes)) {
                current.remove(minutes)
            } else {
                current.add(minutes)
            }
            prefs[Keys.REMINDER_OFFSETS] = current.map { it.toString() }.toSet()
        }
    }

    suspend fun setNotifyAtTransition(enabled: Boolean) {
        context.shiftPreferencesDataStore.edit { prefs ->
            prefs[Keys.NOTIFY_AT_TRANSITION] = enabled
        }
    }

    suspend fun setOngoingStatusNotification(enabled: Boolean) {
        context.shiftPreferencesDataStore.edit { prefs ->
            prefs[Keys.ONGOING_STATUS_NOTIFICATION] = enabled
            if (enabled) {
                prefs[Keys.LIVE_UPDATE_DISMISSED] = false
            }
            Unit
        }
    }

    suspend fun setLiveUpdateDismissed(dismissed: Boolean) {
        context.shiftPreferencesDataStore.edit { prefs ->
            prefs[Keys.LIVE_UPDATE_DISMISSED] = dismissed
            Unit
        }
    }

    suspend fun setUseDeviceTimezone(useDevice: Boolean) {
        context.shiftPreferencesDataStore.edit { prefs ->
            prefs[Keys.USE_DEVICE_TIMEZONE] = useDevice
        }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.shiftPreferencesDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = dark
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.shiftPreferencesDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    private fun mapPreferences(prefs: Preferences): ShiftPreferences {
        val useDeviceTz = prefs[Keys.USE_DEVICE_TIMEZONE] ?: false
        val defaultDate = LocalDate.now(
            if (useDeviceTz) ZoneId.systemDefault() else ShiftScheduleEngine.TBILISI_ZONE
        )
        val parsedDate = prefs[Keys.SHIFT_START_DATE]?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        } ?: defaultDate

        val offsets = prefs[Keys.REMINDER_OFFSETS]
            ?.mapNotNull { it.toIntOrNull() }
            ?.filter { it in ShiftScheduleEngine.ALL_REMINDER_OFFSETS }
            ?.toSet()
            ?: setOf(15)

        return ShiftPreferences(
            isShiftActive = prefs[Keys.IS_SHIFT_ACTIVE] ?: false,
            shiftStartDate = parsedDate,
            briefingCompleted = prefs[Keys.BRIEFING_COMPLETED] ?: false,
            briefingCompletedAtEpochMs = prefs[Keys.BRIEFING_COMPLETED_AT],
            reminderOffsetsMinutes = offsets,
            notifyAtTransition = prefs[Keys.NOTIFY_AT_TRANSITION] ?: true,
            ongoingStatusNotification = prefs[Keys.ONGOING_STATUS_NOTIFICATION] ?: true,
            liveUpdateDismissedForShift = prefs[Keys.LIVE_UPDATE_DISMISSED] ?: false,
            useDeviceTimezone = useDeviceTz,
            darkTheme = prefs[Keys.DARK_THEME] ?: false,
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE])
        )
    }
}
