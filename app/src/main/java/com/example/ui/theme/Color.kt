package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.domain.AssignmentType

// Windows 7 Aero & Office 2007 Blue Light Palette
val AeroLightWindowBg = Color(0xFFEAF2FB)
val AeroLightRibbonBg = Color(0xFFDFECFC)
val AeroLightPanelBg = Color(0xFFFFFFFF)
val AeroLightPanelAltBg = Color(0xFFF4F8FE)
val AeroLightBorder = Color(0xFF8DB2E3)
val AeroLightBorderDark = Color(0xFF5D87B8)
val AeroLightInnerHighlight = Color(0xFFFFFFFF)

val AeroLightTitleTop = Color(0xFFE3EFFD)
val AeroLightTitleMid = Color(0xFFB5D4FA)
val AeroLightTitleBottom = Color(0xFF83B1E8)

val AeroLightHeaderTop = Color(0xFFF3F8FF)
val AeroLightHeaderMid = Color(0xFFD5E6FA)
val AeroLightHeaderBottom = Color(0xFFB7D2F5)

val AeroLightButtonTop = Color(0xFFFCFDFF)
val AeroLightButtonMid = Color(0xFFE5EFFC)
val AeroLightButtonBottom = Color(0xFFC8DDF8)

val AeroLightPrimaryBtnTop = Color(0xFF5FA2EA)
val AeroLightPrimaryBtnMid = Color(0xFF2F78CA)
val AeroLightPrimaryBtnBottom = Color(0xFF1E5BA4)

val AeroLightTextPrimary = Color(0xFF14273E)
val AeroLightTextSecondary = Color(0xFF425C7A)

// Windows 7 Aero / Office 2007 Obsidian Dark Variant
val AeroDarkWindowBg = Color(0xFF141B24)
val AeroDarkRibbonBg = Color(0xFF1A2533)
val AeroDarkPanelBg = Color(0xFF1E2937)
val AeroDarkPanelAltBg = Color(0xFF243142)
val AeroDarkBorder = Color(0xFF446387)
val AeroDarkBorderDark = Color(0xFF2D435E)
val AeroDarkInnerHighlight = Color(0xFF334966)

val AeroDarkTitleTop = Color(0xFF2A4568)
val AeroDarkTitleMid = Color(0xFF1B314D)
val AeroDarkTitleBottom = Color(0xFF112036)

val AeroDarkHeaderTop = Color(0xFF283C55)
val AeroDarkHeaderMid = Color(0xFF1E2F45)
val AeroDarkHeaderBottom = Color(0xFF162437)

val AeroDarkButtonTop = Color(0xFF31445B)
val AeroDarkButtonMid = Color(0xFF253549)
val AeroDarkButtonBottom = Color(0xFF1C293A)

val AeroDarkPrimaryBtnTop = Color(0xFF4284CD)
val AeroDarkPrimaryBtnMid = Color(0xFF265FA1)
val AeroDarkPrimaryBtnBottom = Color(0xFF194579)

val AeroDarkTextPrimary = Color(0xFFEBF2FA)
val AeroDarkTextSecondary = Color(0xFFA7BBD3)

// Assignment Semantic Colors (Light & Dark friendly)
data class AssignmentColorSpec(
    val accent: Color,
    val lightTint: Color,
    val darkTint: Color,
    val borderLight: Color,
    val borderDark: Color
)

fun assignmentColorSpec(type: AssignmentType): AssignmentColorSpec = when (type) {
    AssignmentType.CHECKPOINT_VI -> AssignmentColorSpec(
        accent = Color(0xFF1D5FB8),
        lightTint = Color(0xFFEAF2FF),
        darkTint = Color(0xFF1C304E),
        borderLight = Color(0xFF6B9BE0),
        borderDark = Color(0xFF4574B8)
    )
    AssignmentType.CHECKPOINT_III -> AssignmentColorSpec(
        accent = Color(0xFF147D73),
        lightTint = Color(0xFFE6F7F4),
        darkTint = Color(0xFF153835),
        borderLight = Color(0xFF56B2A8),
        borderDark = Color(0xFF2F847B)
    )
    AssignmentType.BREAK -> AssignmentColorSpec(
        accent = Color(0xFFB7791F),
        lightTint = Color(0xFFFFF6E4),
        darkTint = Color(0xFF3B2E18),
        borderLight = Color(0xFFDBA84E),
        borderDark = Color(0xFF9C722B)
    )
    AssignmentType.BRIEFING -> AssignmentColorSpec(
        accent = Color(0xFF4B4E8A),
        lightTint = Color(0xFFEEEEFA),
        darkTint = Color(0xFF252645),
        borderLight = Color(0xFF888CC6),
        borderDark = Color(0xFF5B5F9E)
    )
    AssignmentType.ATTENTION -> AssignmentColorSpec(
        accent = Color(0xFFB83232),
        lightTint = Color(0xFFFDECEC),
        darkTint = Color(0xFF441D20),
        borderLight = Color(0xFFE07979),
        borderDark = Color(0xFFAD4747)
    )
}
