package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ScheduleSlot
import com.example.domain.ShiftPhase
import com.example.domain.ShiftScheduleEngine
import com.example.domain.ShiftSnapshot
import com.example.ui.components.AeroPanel
import com.example.ui.components.AeroStatusBadge
import com.example.ui.components.FatCowIcon
import com.example.ui.components.assignmentIconRes
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAeroPalette
import com.example.ui.theme.assignmentColorSpec

@Composable
fun TimetableTab(
    snapshot: ShiftSnapshot,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val palette = LocalAeroPalette.current
    val dayOneSlots = ShiftScheduleEngine.slots.filter { it.startDayOffset == 0 }
    val dayTwoSlots = ShiftScheduleEngine.slots.filter { it.startDayOffset == 1 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AeroPanel(
            title = strings.timetableTitle,
            iconRes = R.drawable.fc_table
        ) {
            // Day 1 group header
            DayGroupHeader(
                title = strings.dayOneHeader,
                dateText = snapshot.shiftStartDate.toString()
            )
            Spacer(modifier = Modifier.height(6.dp))
            TableHeaderRow(strings = strings)
            dayOneSlots.forEach { slot ->
                TimetableRow(
                    slot = slot,
                    snapshot = snapshot,
                    strings = strings
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Day 2 (next calendar day after midnight) group header
            DayGroupHeader(
                title = strings.dayTwoHeader,
                dateText = snapshot.shiftStartDate.plusDays(1).toString()
            )
            Spacer(modifier = Modifier.height(6.dp))
            TableHeaderRow(strings = strings)
            dayTwoSlots.forEach { slot ->
                TimetableRow(
                    slot = slot,
                    snapshot = snapshot,
                    strings = strings
                )
            }
        }
    }
}

@Composable
private fun DayGroupHeader(
    title: String,
    dateText: String
) {
    val palette = LocalAeroPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.ribbonBg)
            .border(1.dp, palette.border, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMonoFamily,
            color = palette.textSecondary
        )
    }
}

@Composable
private fun TableHeaderRow(strings: AppStrings) {
    val palette = LocalAeroPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.panelAltBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = strings.timetableColumnTime,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = palette.textSecondary,
            modifier = Modifier.weight(0.36f)
        )
        Text(
            text = strings.timetableColumnAssignment,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = palette.textSecondary,
            modifier = Modifier.weight(0.44f)
        )
        Text(
            text = strings.timetableColumnStatus,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = palette.textSecondary,
            modifier = Modifier.weight(0.20f)
        )
    }
    HorizontalDivider(thickness = 1.dp, color = palette.border)
}

@Composable
private fun TimetableRow(
    slot: ScheduleSlot,
    snapshot: ShiftSnapshot,
    strings: AppStrings
) {
    val palette = LocalAeroPalette.current
    val spec = assignmentColorSpec(slot.assignmentType)
    val activeIdx = snapshot.activeSlotIndex
    val isActive = snapshot.activeSlotIndex == slot.index &&
        (snapshot.phase == ShiftPhase.ACTIVE_BRIEFING || snapshot.phase == ShiftPhase.ACTIVE_TIMED)
    val isNext = snapshot.nextSlot?.index == slot.index && !isActive &&
        (snapshot.phase == ShiftPhase.BEFORE_SHIFT ||
            snapshot.phase == ShiftPhase.ACTIVE_BRIEFING ||
            snapshot.phase == ShiftPhase.ACTIVE_TIMED)
    val isDone = when {
        snapshot.phase == ShiftPhase.COMPLETED -> true
        activeIdx != null && slot.index < activeIdx -> true
        else -> false
    }

    val rowTint = when {
        isActive -> if (palette.isDark) spec.darkTint else spec.lightTint
        isDone -> palette.panelAltBg.copy(alpha = 0.55f)
        else -> palette.panelBg
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowTint)
            .then(
                if (isActive) {
                    Modifier.border(1.5.dp, spec.accent)
                } else {
                    Modifier
                }
            )
            .testTag("timetable_row_${slot.index}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color accent stripe + Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(0.36f)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(spec.accent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.slotTimeRange(slot),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = palette.textPrimary
                )
            }

            // Icon + Assignment Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(0.44f)
                    .padding(horizontal = 4.dp)
            ) {
                FatCowIcon(
                    resId = assignmentIconRes(slot.assignmentType),
                    contentDescription = strings.assignmentName(slot.assignmentType),
                    size = 20.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.assignmentName(slot.assignmentType),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status Badge
            Box(
                modifier = Modifier.weight(0.20f),
                contentAlignment = Alignment.CenterEnd
            ) {
                when {
                    isActive -> AeroStatusBadge(
                        text = strings.statusNow,
                        backgroundColor = spec.accent,
                        textColor = Color.White,
                        borderColor = spec.accent
                    )
                    isNext -> AeroStatusBadge(
                        text = strings.statusNext,
                        backgroundColor = palette.warningBg,
                        textColor = palette.warningText,
                        borderColor = palette.borderDark
                    )
                    isDone -> AeroStatusBadge(
                        text = strings.statusDone,
                        backgroundColor = palette.panelAltBg,
                        textColor = palette.textSecondary,
                        borderColor = palette.border
                    )
                    else -> Text(
                        text = strings.statusUpcoming,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = palette.border.copy(alpha = 0.6f))
    }
}
