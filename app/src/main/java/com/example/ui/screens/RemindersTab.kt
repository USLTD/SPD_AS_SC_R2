package com.example.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ShiftScheduleEngine
import com.example.ui.ShiftUiState
import com.example.ui.components.AeroBevelButton
import com.example.ui.components.AeroPanel
import com.example.ui.components.AeroStatusBadge
import com.example.ui.components.FatCowIcon
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalAeroPalette

@Composable
fun RemindersTab(
    uiState: ShiftUiState,
    strings: AppStrings,
    onToggleReminderOffset: (Int) -> Unit,
    onSetNotifyAtTransition: (Boolean) -> Unit,
    onSetOngoingNotification: (Boolean) -> Unit,
    onSetUseDeviceTimezone: (Boolean) -> Unit,
    onRequestPostNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenLiveUpdatesSettings: () -> Unit,
    onSendTestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAeroPalette.current
    val prefs = uiState.preferences
    val perms = uiState.permissions
    val sched = uiState.schedulingResult

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Card 1: Lead times, exact transition, Live Update, and Time Zone
        AeroPanel(
            title = strings.reminderLeadTimesTitle,
            iconRes = R.drawable.fc_bell,
            contentPadding = 8.dp,
            headerAction = {
                val modeText = if (perms.exactAlarmsGranted) {
                    strings.exactModeLabel
                } else {
                    strings.inexactFallbackModeLabel
                }
                AeroStatusBadge(
                    text = "${strings.scheduledAlertsSummaryPrefix} ${sched.scheduledCount} • $modeText",
                    backgroundColor = palette.panelAltBg,
                    textColor = palette.textPrimary,
                    borderColor = palette.borderDark
                )
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShiftScheduleEngine.ALL_REMINDER_OFFSETS.forEach { minutes ->
                    val selected = minutes in prefs.reminderOffsetsMinutes
                    AeroToggleTile(
                        label = "${minutes}m",
                        iconRes = null,
                        selected = selected,
                        onClick = { onToggleReminderOffset(minutes) },
                        modifier = Modifier.weight(1f),
                        testTag = "reminder_chip_$minutes"
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AeroToggleTile(
                    label = strings.notifyAtExactTransition,
                    iconRes = if (prefs.notifyAtTransition) R.drawable.fc_tick else R.drawable.fc_alarm_bell,
                    selected = prefs.notifyAtTransition,
                    onClick = { onSetNotifyAtTransition(!prefs.notifyAtTransition) },
                    modifier = Modifier.weight(1f),
                    testTag = "notify_exact_switch"
                )
                AeroToggleTile(
                    label = strings.ongoingNotificationTitle,
                    iconRes = if (prefs.ongoingStatusNotification) R.drawable.fc_tick else R.drawable.fc_clock_play,
                    selected = prefs.ongoingStatusNotification,
                    onClick = { onSetOngoingNotification(!prefs.ongoingStatusNotification) },
                    modifier = Modifier.weight(1f),
                    testTag = "ongoing_notification_switch"
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AeroBevelButton(
                    text = strings.timezoneTbilisi,
                    iconRes = if (!prefs.useDeviceTimezone) R.drawable.fc_tick else R.drawable.fc_world,
                    onClick = { onSetUseDeviceTimezone(false) },
                    primary = !prefs.useDeviceTimezone,
                    modifier = Modifier.weight(1f),
                    testTag = "tz_tbilisi_option"
                )
                AeroBevelButton(
                    text = strings.timezoneDevice,
                    iconRes = if (prefs.useDeviceTimezone) R.drawable.fc_tick else R.drawable.fc_world,
                    onClick = { onSetUseDeviceTimezone(true) },
                    primary = prefs.useDeviceTimezone,
                    modifier = Modifier.weight(1f),
                    testTag = "tz_device_option"
                )
            }
        }

        // Card 2: Compact Permissions + Test Alert
        AeroPanel(
            title = strings.permissionsTitle,
            iconRes = R.drawable.fc_cog,
            contentPadding = 8.dp
        ) {
            CompactPermissionRow(
                label = strings.postNotificationsLabel,
                isOk = perms.postNotificationsGranted,
                statusText = if (perms.postNotificationsGranted) strings.permissionGranted else strings.permissionDenied,
                actionLabel = if (!perms.postNotificationsGranted) strings.grantAction else null,
                onAction = onRequestPostNotifications,
                testTag = "post_notifications_status"
            )

            Spacer(modifier = Modifier.height(4.dp))

            CompactPermissionRow(
                label = strings.exactAlarmsLabel,
                isOk = perms.exactAlarmsGranted,
                statusText = if (perms.exactAlarmsGranted) strings.permissionGranted else strings.permissionFallbackActive,
                actionLabel = if (!perms.exactAlarmsGranted) strings.settingsAction else null,
                onAction = onOpenExactAlarmSettings,
                testTag = "exact_alarm_status"
            )

            Spacer(modifier = Modifier.height(4.dp))

            val liveStatusText = when {
                !perms.liveUpdatesSupported -> strings.liveUpdatesChronometer
                perms.liveUpdatesGranted -> strings.permissionGranted
                else -> strings.permissionFallbackActive
            }
            val liveAction = if (perms.liveUpdatesSupported && !perms.liveUpdatesGranted) {
                strings.settingsAction
            } else {
                null
            }

            CompactPermissionRow(
                label = strings.liveUpdatesLabel,
                isOk = !perms.liveUpdatesSupported || perms.liveUpdatesGranted,
                statusText = liveStatusText,
                actionLabel = liveAction,
                onAction = onOpenLiveUpdatesSettings,
                testTag = "live_updates_status"
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AeroBevelButton(
                    text = strings.testNotificationButton,
                    iconRes = R.drawable.fc_alarm_bell,
                    onClick = onSendTestNotification,
                    primary = true,
                    modifier = Modifier.weight(1f),
                    testTag = "test_notification_button"
                )
                if (uiState.testNotificationMessage != null) {
                    Text(
                        text = uiState.testNotificationMessage,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (perms.postNotificationsGranted) palette.successText else palette.errorText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AeroToggleTile(
    label: String,
    @DrawableRes iconRes: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val palette = LocalAeroPalette.current
    val shape = RoundedCornerShape(4.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(
                if (selected) {
                    Brush.verticalGradient(
                        0.0f to palette.primaryBtnTop,
                        1.0f to palette.primaryBtnBottom
                    )
                } else {
                    Brush.verticalGradient(
                        0.0f to palette.buttonTop,
                        1.0f to palette.buttonBottom
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFF13437D) else palette.borderDark,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (iconRes != null) {
            FatCowIcon(resId = iconRes, contentDescription = label, size = 15.dp)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactPermissionRow(
    label: String,
    isOk: Boolean,
    statusText: String,
    actionLabel: String?,
    onAction: () -> Unit,
    testTag: String
) {
    val palette = LocalAeroPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.panelAltBg)
            .border(1.dp, palette.border, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            FatCowIcon(
                resId = if (isOk) R.drawable.fc_tick else R.drawable.fc_exclamation,
                contentDescription = statusText,
                size = 15.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AeroStatusBadge(
                text = statusText,
                backgroundColor = if (isOk) palette.successBg else palette.warningBg,
                textColor = if (isOk) palette.successText else palette.warningText,
                borderColor = palette.borderDark,
                modifier = Modifier.testTag(testTag)
            )
            if (actionLabel != null) {
                AeroStatusBadge(
                    text = actionLabel,
                    backgroundColor = palette.primaryBtnMid,
                    textColor = Color.White,
                    borderColor = Color(0xFF13437D),
                    modifier = Modifier
                        .clickable(onClick = onAction)
                        .testTag("${testTag}_action")
                )
            }
        }
    }
}
