package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.AssignmentType
import com.example.domain.ShiftPhase
import com.example.domain.ShiftSnapshot
import com.example.ui.ShiftUiState
import com.example.ui.components.AeroBevelButton
import com.example.ui.components.AeroPanel
import com.example.ui.components.AeroProgressBar
import com.example.ui.components.AeroStatusBadge
import com.example.ui.components.FatCowIcon
import com.example.ui.components.assignmentIconRes
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAeroPalette
import com.example.ui.theme.assignmentColorSpec
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftDashboardTab(
    uiState: ShiftUiState,
    strings: AppStrings,
    onSelectDate: (LocalDate) -> Unit,
    onActivateShift: () -> Unit,
    onDeactivateShift: () -> Unit,
    onMarkBriefingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val snapshot = uiState.snapshot

    if (showDatePicker) {
        val initialMillis = uiState.preferences.shiftStartDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onSelectDate(picked)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        val isWide = maxWidth >= 680.dp

        if (isWide) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactShiftActionBar(
                    uiState = uiState,
                    strings = strings,
                    onOpenDatePicker = { showDatePicker = true },
                    onActivateShift = onActivateShift,
                    onDeactivateShift = onDeactivateShift
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    NextCountdownCard(
                        snapshot = snapshot,
                        strings = strings,
                        modifier = Modifier.weight(1f)
                    )
                    CurrentAssignmentCard(
                        snapshot = snapshot,
                        strings = strings,
                        onMarkBriefingComplete = onMarkBriefingComplete,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactShiftActionBar(
                    uiState = uiState,
                    strings = strings,
                    onOpenDatePicker = { showDatePicker = true },
                    onActivateShift = onActivateShift,
                    onDeactivateShift = onDeactivateShift
                )
                NextCountdownCard(snapshot = snapshot, strings = strings)
                CurrentAssignmentCard(
                    snapshot = snapshot,
                    strings = strings,
                    onMarkBriefingComplete = onMarkBriefingComplete
                )
            }
        }
    }
}

@Composable
private fun CompactShiftActionBar(
    uiState: ShiftUiState,
    strings: AppStrings,
    onOpenDatePicker: () -> Unit,
    onActivateShift: () -> Unit,
    onDeactivateShift: () -> Unit
) {
    val palette = LocalAeroPalette.current
    val snapshot = uiState.snapshot
    val dateFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US) }
    val startDateText = uiState.preferences.shiftStartDate.format(dateFmt)

    val (badgeText, badgeBg, badgeFg) = when (snapshot.phase) {
        ShiftPhase.INACTIVE -> Triple(strings.inactiveBadge, palette.panelAltBg, palette.textSecondary)
        ShiftPhase.BEFORE_SHIFT -> Triple(strings.beforeShiftBadge, palette.warningBg, palette.warningText)
        ShiftPhase.ACTIVE_BRIEFING,
        ShiftPhase.ACTIVE_TIMED -> Triple(strings.activeBadge, palette.successBg, palette.successText)
        ShiftPhase.COMPLETED -> Triple(strings.completedBadge, palette.panelAltBg, palette.textPrimary)
    }

    Surface(
        shape = RoundedCornerShape(5.dp),
        color = palette.panelBg,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AeroBevelButton(
                text = startDateText,
                iconRes = R.drawable.fc_calendar,
                onClick = onOpenDatePicker,
                modifier = Modifier.weight(1f),
                testTag = "select_date_button"
            )

            AeroStatusBadge(
                text = badgeText,
                backgroundColor = badgeBg,
                textColor = badgeFg,
                borderColor = palette.borderDark,
                modifier = Modifier.testTag("shift_status_badge")
            )

            if (uiState.preferences.isShiftActive) {
                AeroBevelButton(
                    text = strings.deactivateShift,
                    iconRes = R.drawable.fc_control_stop_blue,
                    onClick = onDeactivateShift,
                    primary = false,
                    modifier = Modifier.weight(0.95f),
                    testTag = "deactivate_shift_button"
                )
            } else {
                AeroBevelButton(
                    text = strings.activateShift,
                    iconRes = R.drawable.fc_control_play_blue,
                    onClick = onActivateShift,
                    primary = true,
                    modifier = Modifier.weight(0.95f),
                    testTag = "activate_shift_button"
                )
            }
        }
    }
}

@Composable
private fun NextCountdownCard(
    snapshot: ShiftSnapshot,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val palette = LocalAeroPalette.current
    val targetSlot = snapshot.nextSlot
    val accentSpec = targetSlot?.let { assignmentColorSpec(it.assignmentType) }
    val timePill = when (snapshot.phase) {
        ShiftPhase.BEFORE_SHIFT,
        ShiftPhase.ACTIVE_BRIEFING,
        ShiftPhase.ACTIVE_TIMED -> {
            val t = snapshot.nextTransitionTimeLabel ?: "10:00"
            val isNextDay = targetSlot?.startDayOffset == 1 ||
                (targetSlot == null && snapshot.phase == ShiftPhase.ACTIVE_TIMED)
            "${strings.transitionAtPrefix} $t" + if (isNextDay) " ${strings.nextCalendarDayTag}" else ""
        }
        else -> null
    }

    AeroPanel(
        title = strings.nextAssignmentTitle,
        iconRes = R.drawable.fc_clock_red,
        contentPadding = 10.dp,
        modifier = modifier,
        headerAction = timePill?.let { pill ->
            {
                AeroStatusBadge(
                    text = pill,
                    backgroundColor = palette.panelBg,
                    textColor = palette.textPrimary,
                    borderColor = palette.borderDark
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(5.dp))
                .background(if (palette.isDark) Color(0xFF0E1520) else Color(0xFFF3F7FD))
                .border(1.dp, palette.borderDark, RoundedCornerShape(5.dp))
                .padding(vertical = 8.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = snapshot.countdownFormatted,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                color = if (snapshot.phase == ShiftPhase.INACTIVE) {
                    palette.textSecondary
                } else {
                    accentSpec?.accent ?: palette.textPrimary
                },
                modifier = Modifier.testTag("countdown_display")
            )
        }

        if (snapshot.phase == ShiftPhase.BEFORE_SHIFT ||
            snapshot.phase == ShiftPhase.ACTIVE_BRIEFING ||
            snapshot.phase == ShiftPhase.ACTIVE_TIMED
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            val isTerminal = targetSlot == null && snapshot.phase == ShiftPhase.ACTIVE_TIMED
            val displayIcon = targetSlot?.let { assignmentIconRes(it.assignmentType) }
                ?: R.drawable.fc_tick
            val displayTitle = if (isTerminal) {
                strings.shiftEndsAtLabel
            } else {
                strings.assignmentName(targetSlot!!.assignmentType)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.panelAltBg)
                    .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FatCowIcon(resId = displayIcon, contentDescription = displayTitle, size = 18.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("next_assignment_name")
                )
            }
        }
    }
}

@Composable
private fun CurrentAssignmentCard(
    snapshot: ShiftSnapshot,
    strings: AppStrings,
    onMarkBriefingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAeroPalette.current
    val currentSlot = snapshot.currentSlot

    AeroPanel(
        title = strings.currentAssignmentTitle,
        iconRes = currentSlot?.let { assignmentIconRes(it.assignmentType) } ?: R.drawable.fc_clock,
        contentPadding = 10.dp,
        modifier = modifier
    ) {
        when (snapshot.phase) {
            ShiftPhase.INACTIVE -> {
                Text(
                    text = strings.noActiveShiftTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = strings.noActiveShiftBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
            ShiftPhase.BEFORE_SHIFT -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FatCowIcon(
                        resId = R.drawable.fc_comment,
                        contentDescription = strings.beforeShiftTitle,
                        size = 24.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.beforeShiftTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }
            }
            ShiftPhase.COMPLETED -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FatCowIcon(
                        resId = R.drawable.fc_tick,
                        contentDescription = strings.shiftCompletedTitle,
                        size = 24.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.shiftCompletedTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        modifier = Modifier.testTag("current_assignment_name")
                    )
                }
            }
            ShiftPhase.ACTIVE_BRIEFING,
            ShiftPhase.ACTIVE_TIMED -> {
                val slot = currentSlot!!
                val spec = assignmentColorSpec(slot.assignmentType)
                val tint = if (palette.isDark) spec.darkTint else spec.lightTint
                val border = if (palette.isDark) spec.borderDark else spec.borderLight

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = tint,
                    border = BorderStroke(1.dp, border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FatCowIcon(
                                resId = assignmentIconRes(slot.assignmentType),
                                contentDescription = strings.assignmentName(slot.assignmentType),
                                size = 26.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.assignmentName(slot.assignmentType),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("current_assignment_name")
                                )
                                Text(
                                    text = strings.slotTimeRange(slot) +
                                        if (slot.startDayOffset > 0) " • ${strings.nextCalendarDayTag}" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        AeroProgressBar(
                            progress = snapshot.progressFraction,
                            accentColor = spec.accent
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val elapsedText = snapshot.segmentElapsedFormatted?.let {
                                "${strings.elapsedLabel}: $it"
                            } ?: ""
                            val totalText = snapshot.segmentTotalFormatted?.let {
                                "${strings.totalLabel}: $it"
                            } ?: if (snapshot.canMarkBriefingComplete) strings.briefingUnknownEndHint else ""

                            Text(
                                text = elapsedText,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = palette.textSecondary
                            )
                            Text(
                                text = totalText,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = palette.textSecondary
                            )
                        }
                    }
                }

                if (snapshot.canMarkBriefingComplete) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AeroBevelButton(
                        text = strings.markBriefingComplete,
                        iconRes = R.drawable.fc_tick,
                        onClick = onMarkBriefingComplete,
                        primary = true,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "mark_briefing_complete_button"
                    )
                }
            }
        }
    }
}
