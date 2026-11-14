package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.AppLanguage

data class AeroPalette(
    val isDark: Boolean,
    val windowBg: Color,
    val ribbonBg: Color,
    val panelBg: Color,
    val panelAltBg: Color,
    val border: Color,
    val borderDark: Color,
    val innerHighlight: Color,
    val titleTop: Color,
    val titleMid: Color,
    val titleBottom: Color,
    val headerTop: Color,
    val headerMid: Color,
    val headerBottom: Color,
    val buttonTop: Color,
    val buttonMid: Color,
    val buttonBottom: Color,
    val primaryBtnTop: Color,
    val primaryBtnMid: Color,
    val primaryBtnBottom: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val successBg: Color,
    val successText: Color,
    val warningBg: Color,
    val warningText: Color,
    val errorBg: Color,
    val errorText: Color
)

private val LightAeroPalette = AeroPalette(
    isDark = false,
    windowBg = AeroLightWindowBg,
    ribbonBg = AeroLightRibbonBg,
    panelBg = AeroLightPanelBg,
    panelAltBg = AeroLightPanelAltBg,
    border = AeroLightBorder,
    borderDark = AeroLightBorderDark,
    innerHighlight = AeroLightInnerHighlight,
    titleTop = AeroLightTitleTop,
    titleMid = AeroLightTitleMid,
    titleBottom = AeroLightTitleBottom,
    headerTop = AeroLightHeaderTop,
    headerMid = AeroLightHeaderMid,
    headerBottom = AeroLightHeaderBottom,
    buttonTop = AeroLightButtonTop,
    buttonMid = AeroLightButtonMid,
    buttonBottom = AeroLightButtonBottom,
    primaryBtnTop = AeroLightPrimaryBtnTop,
    primaryBtnMid = AeroLightPrimaryBtnMid,
    primaryBtnBottom = AeroLightPrimaryBtnBottom,
    textPrimary = AeroLightTextPrimary,
    textSecondary = AeroLightTextSecondary,
    successBg = Color(0xFFE3F6E8),
    successText = Color(0xFF1A6B32),
    warningBg = Color(0xFFFFF5DC),
    warningText = Color(0xFF8C5A08),
    errorBg = Color(0xFFFDE8E8),
    errorText = Color(0xFF9F1C1C)
)

private val DarkAeroPalette = AeroPalette(
    isDark = true,
    windowBg = AeroDarkWindowBg,
    ribbonBg = AeroDarkRibbonBg,
    panelBg = AeroDarkPanelBg,
    panelAltBg = AeroDarkPanelAltBg,
    border = AeroDarkBorder,
    borderDark = AeroDarkBorderDark,
    innerHighlight = AeroDarkInnerHighlight,
    titleTop = AeroDarkTitleTop,
    titleMid = AeroDarkTitleMid,
    titleBottom = AeroDarkTitleBottom,
    headerTop = AeroDarkHeaderTop,
    headerMid = AeroDarkHeaderMid,
    headerBottom = AeroDarkHeaderBottom,
    buttonTop = AeroDarkButtonTop,
    buttonMid = AeroDarkButtonMid,
    buttonBottom = AeroDarkButtonBottom,
    primaryBtnTop = AeroDarkPrimaryBtnTop,
    primaryBtnMid = AeroDarkPrimaryBtnMid,
    primaryBtnBottom = AeroDarkPrimaryBtnBottom,
    textPrimary = AeroDarkTextPrimary,
    textSecondary = AeroDarkTextSecondary,
    successBg = Color(0xFF173623),
    successText = Color(0xFF80DDA0),
    warningBg = Color(0xFF3A2D15),
    warningText = Color(0xFFF6C76B),
    errorBg = Color(0xFF3E1C1E),
    errorText = Color(0xFFF39797)
)

val LocalAeroPalette = staticCompositionLocalOf { LightAeroPalette }

private val AeroLightMaterialScheme = lightColorScheme(
    primary = Color(0xFF1E5BA4),
    onPrimary = Color.White,
    secondary = Color(0xFF4B6D96),
    onSecondary = Color.White,
    background = AeroLightWindowBg,
    onBackground = AeroLightTextPrimary,
    surface = AeroLightPanelBg,
    onSurface = AeroLightTextPrimary,
    surfaceVariant = AeroLightRibbonBg,
    onSurfaceVariant = AeroLightTextSecondary,
    outline = AeroLightBorder
)

private val AeroDarkMaterialScheme = darkColorScheme(
    primary = Color(0xFF5FA2EA),
    onPrimary = Color(0xFF0A192B),
    secondary = Color(0xFF7DA3CE),
    onSecondary = Color(0xFF0D1B2B),
    background = AeroDarkWindowBg,
    onBackground = AeroDarkTextPrimary,
    surface = AeroDarkPanelBg,
    onSurface = AeroDarkTextPrimary,
    surfaceVariant = AeroDarkRibbonBg,
    onSurfaceVariant = AeroDarkTextSecondary,
    outline = AeroDarkBorder
)

private val AeroShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(3.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(5.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
)

@Composable
fun ShiftCompanionTheme(
    darkTheme: Boolean = false,
    language: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkAeroPalette else LightAeroPalette
    val colorScheme = if (darkTheme) AeroDarkMaterialScheme else AeroLightMaterialScheme

    CompositionLocalProvider(LocalAeroPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = aeroTypography(language),
            shapes = AeroShapes,
            content = content
        )
    }
}
