package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.AppLanguage
import com.example.domain.AssignmentType
import com.example.ui.RibbonTab
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAeroPalette

@DrawableRes
fun assignmentIconRes(type: AssignmentType): Int = when (type) {
    AssignmentType.BRIEFING -> R.drawable.fc_comment
    AssignmentType.BREAK -> R.drawable.fc_cup
    AssignmentType.CHECKPOINT_VI -> R.drawable.fc_shield
    AssignmentType.CHECKPOINT_III -> R.drawable.fc_shield_go
    AssignmentType.ATTENTION -> R.drawable.fc_exclamation
}

@Composable
fun FatCowIcon(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun AeroTitleBar(
    title: String,
    currentTimeFormatted: String,
    zoneLabel: String,
    language: AppLanguage,
    darkTheme: Boolean,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAeroPalette.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    0.0f to palette.titleTop,
                    0.48f to palette.titleMid,
                    1.0f to palette.titleBottom
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = if (palette.isDark) 0.18f else 0.70f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                FatCowIcon(
                    resId = R.drawable.fc_clock,
                    contentDescription = title,
                    size = 20.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = zoneLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.panelBg.copy(alpha = 0.92f))
                        .border(1.dp, palette.borderDark, RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("titlebar_live_clock"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentTimeFormatted,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = palette.textPrimary
                    )
                }

                AeroMiniToolbarButton(
                    label = if (language == AppLanguage.ENGLISH) "KA" else "EN",
                    iconRes = R.drawable.fc_world,
                    contentDescription = "Switch Language",
                    onClick = onToggleLanguage,
                    testTag = "language_toggle_button"
                )

                AeroMiniToolbarButton(
                    label = null,
                    iconRes = if (darkTheme) R.drawable.fc_lightbulb else R.drawable.fc_moon,
                    contentDescription = "Toggle Theme",
                    onClick = onToggleTheme,
                    testTag = "theme_toggle_button"
                )
            }
        }
    }
}

@Composable
fun AeroMiniToolbarButton(
    label: String?,
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalAeroPalette.current
    val shape = RoundedCornerShape(4.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 36.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0.0f to palette.buttonTop,
                    0.5f to palette.buttonMid,
                    1.0f to palette.buttonBottom
                )
            )
            .border(1.dp, palette.borderDark, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 7.dp, vertical = 5.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        FatCowIcon(resId = iconRes, contentDescription = contentDescription, size = 16.dp)
        if (label != null) {
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }
    }
}

@Composable
fun OfficeRibbonTabs(
    selectedTab: RibbonTab,
    strings: AppStrings,
    onSelectTab: (RibbonTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAeroPalette.current

    val items = listOf(
        Triple(RibbonTab.SHIFT, strings.tabShift, R.drawable.fc_clock_play),
        Triple(RibbonTab.TIMETABLE, strings.tabTimetable, R.drawable.fc_table),
        Triple(RibbonTab.REMINDERS, strings.tabReminders, R.drawable.fc_bell),
        Triple(RibbonTab.ABOUT, strings.tabAbout, R.drawable.fc_information)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.ribbonBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .padding(top = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { (tab, label, iconRes) ->
                val isSelected = tab == selectedTab
                val tabShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
                val interactionSource = remember { MutableInteractionSource() }

                val bgModifier = if (isSelected) {
                    Modifier.background(
                        Brush.verticalGradient(
                            0.0f to palette.panelBg,
                            1.0f to palette.panelAltBg
                        ),
                        shape = tabShape
                    )
                } else {
                    Modifier.background(
                        Brush.verticalGradient(
                            0.0f to palette.headerMid.copy(alpha = 0.55f),
                            1.0f to palette.headerBottom.copy(alpha = 0.45f)
                        ),
                        shape = tabShape
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(tabShape)
                        .then(bgModifier)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) palette.borderDark else palette.border.copy(alpha = 0.7f),
                            shape = tabShape
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = { onSelectTab(tab) }
                        )
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .testTag("ribbon_tab_${tab.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    FatCowIcon(resId = iconRes, contentDescription = label, size = 16.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = palette.borderDark)
    }
}

@Composable
fun AeroPanel(
    title: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 10.dp,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalAeroPalette.current
    val shape = RoundedCornerShape(5.dp)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = palette.panelBg,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0.0f to palette.headerTop,
                            0.5f to palette.headerMid,
                            1.0f to palette.headerBottom
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    FatCowIcon(resId = iconRes, contentDescription = title, size = 16.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (headerAction != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    headerAction()
                }
            }
            HorizontalDivider(thickness = 1.dp, color = palette.border)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content
            )
        }
    }
}

@Composable
fun AeroBevelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
    testTag: String = ""
) {
    val palette = LocalAeroPalette.current
    val shape = RoundedCornerShape(4.dp)
    val interactionSource = remember { MutableInteractionSource() }

    val topColor = when {
        !enabled -> palette.panelAltBg
        primary -> palette.primaryBtnTop
        else -> palette.buttonTop
    }
    val midColor = when {
        !enabled -> palette.panelAltBg
        primary -> palette.primaryBtnMid
        else -> palette.buttonMid
    }
    val bottomColor = when {
        !enabled -> palette.panelAltBg
        primary -> palette.primaryBtnBottom
        else -> palette.buttonBottom
    }
    val contentColor = when {
        !enabled -> palette.textSecondary.copy(alpha = 0.6f)
        primary -> Color.White
        else -> palette.textPrimary
    }

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0.0f to topColor,
                    0.48f to midColor,
                    1.0f to bottomColor
                )
            )
            .border(
                width = 1.dp,
                color = if (primary && enabled) Color(0xFF13437D) else palette.borderDark,
                shape = shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (iconRes != null) {
            FatCowIcon(resId = iconRes, contentDescription = text, size = 16.dp)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AeroStatusBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(3.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
fun AeroProgressBar(
    progress: Float?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalAeroPalette.current
    val shape = RoundedCornerShape(4.dp)
    val clamped = (progress ?: 0f).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0.0f to palette.panelAltBg,
                    1.0f to palette.ribbonBg
                )
            )
            .border(1.dp, palette.borderDark, shape)
            .padding(2.dp)
    ) {
        if (progress != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            0.0f to accentColor.copy(alpha = 0.72f),
                            0.45f to accentColor,
                            1.0f to accentColor.copy(alpha = 0.88f)
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.32f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor.copy(alpha = 0.55f))
            )
        }
    }
}
