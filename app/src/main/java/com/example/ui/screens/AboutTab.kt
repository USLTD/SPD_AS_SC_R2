package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.AeroPanel
import com.example.ui.components.FatCowIcon
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalAeroPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutTab(
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val palette = LocalAeroPalette.current

    val bundledIcons = listOf(
        R.drawable.fc_clock,
        R.drawable.fc_clock_play,
        R.drawable.fc_clock_red,
        R.drawable.fc_calendar,
        R.drawable.fc_table,
        R.drawable.fc_bell,
        R.drawable.fc_alarm_bell,
        R.drawable.fc_shield,
        R.drawable.fc_shield_go,
        R.drawable.fc_cup,
        R.drawable.fc_comment,
        R.drawable.fc_exclamation,
        R.drawable.fc_tick,
        R.drawable.fc_world,
        R.drawable.fc_cog,
        R.drawable.fc_information
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AeroPanel(
            title = strings.aboutTitle,
            iconRes = R.drawable.fc_information,
            contentPadding = 10.dp
        ) {
            Text(
                text = strings.aboutOverview,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.aboutScheduleNotes,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = palette.textSecondary
            )
        }

        AeroPanel(
            title = strings.aboutAttributionTitle,
            iconRes = R.drawable.fc_world,
            contentPadding = 10.dp
        ) {
            Text(
                text = strings.aboutAttributionBody,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                bundledIcons.forEach { resId ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.panelAltBg)
                            .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FatCowIcon(resId = resId, contentDescription = null, size = 18.dp)
                    }
                }
            }
        }
    }
}
