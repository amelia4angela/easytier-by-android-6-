package com.easytier.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Theme-adaptive color set.
 * Simplifies the rest of the UI — each composable just calls [currentColors()].
 */

val LocalIsDarkTheme = staticCompositionLocalOf { false }

data class AppColors(
    // Background
    val bgStart: Color,
    val bgMid: Color,
    val bgEnd: Color,
    // Surface
    val surface: Color,
    val surfaceDim: Color,
    val surfaceBorder: Color,
    // Nav bar
    val navBar: Color,
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    // Accent
    val accentStart: Color,
    val accentEnd: Color,
    val accent: Color,
    val accentLight: Color,
    val accentContainer: Color,
    // Input
    val inputBg: Color,
    val inputBorder: Color,
    val inputFocusBorder: Color,
    // Status
    val statusGood: Color,
    val statusWarn: Color,
    val statusBad: Color,
    val statusGoodGlow: Color,
    val statusConnectedBg: Color,
    val statusConnectedBgLight: Color,
    // Decorative glows
    val glowColor: Color,
    val glowColorDark: Color,
    // Danger (delete, destructive actions)
    val dangerBg: Color,
    val dangerText: Color,
    // Warning / pause state
    val warning: Color,
    val warningBg: Color,
) {
    val accentGradient: Brush get() = Brush.linearGradient(listOf(accentStart, accentEnd))
}

@Composable
fun currentColors(): AppColors {
    val dark = LocalIsDarkTheme.current
    return if (dark) darkAppColors else lightAppColors
}

private val lightAppColors = AppColors(
    bgStart = LightBgStart,
    bgMid = LightBgMid,
    bgEnd = LightBgEnd,
    surface = Color.White,
    surfaceDim = LightSurfaceDim,
    surfaceBorder = LightCardBorder,
    navBar = Color.White.copy(alpha = 0.85f),
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    accentStart = LightAccentStart,
    accentEnd = LightAccentEnd,
    accent = LightAccent,
    accentLight = LightAccentLight,
    accentContainer = LightAccentContainer,
    inputBg = LightInputBg,
    inputBorder = LightInputBorder,
    inputFocusBorder = LightInputFocusBorder,
    statusGood = StatusGood,
    statusWarn = StatusWarn,
    statusBad = StatusBad,
    statusGoodGlow = StatusGoodGlow,
    statusConnectedBg = StatusConnectedBg,
    statusConnectedBgLight = StatusConnectedBgLight,
    glowColor = LightAccentStart.copy(alpha = 0.10f),
    glowColorDark = LightAccentEnd.copy(alpha = 0.06f),
    dangerBg = StatusBad.copy(alpha = 0.12f),
    dangerText = StatusBad,
    warning = StatusWarn,
    warningBg = StatusWarn.copy(alpha = 0.15f),
)

private val darkAppColors = AppColors(
    bgStart = DarkBgStart,
    bgMid = DarkBgMid,
    bgEnd = DarkBgEnd,
    surface = DarkGlass,
    surfaceDim = DarkGlassStrong,
    surfaceBorder = DarkGlassBorder,
    navBar = DarkNavBar,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    accentStart = DarkAccent,
    accentEnd = DarkAccentDark,
    accent = DarkAccent,
    accentLight = DarkAccentLight,
    accentContainer = DarkAccent.copy(alpha = 0.3f),
    inputBg = DarkInputBg,
    inputBorder = DarkInputBorder,
    inputFocusBorder = DarkInputFocusBorder,
    statusGood = StatusGood,
    statusWarn = StatusWarn,
    statusBad = StatusBad,
    statusGoodGlow = StatusGoodGlow,
    statusConnectedBg = StatusConnectedBg,
    statusConnectedBgLight = StatusConnectedBgLight,
    glowColor = DarkAccent.copy(alpha = 0.12f),
    glowColorDark = DarkAccentDark.copy(alpha = 0.08f),
    dangerBg = StatusBad.copy(alpha = 0.20f),
    dangerText = StatusBad,
    warning = StatusWarn,
    warningBg = StatusWarn.copy(alpha = 0.20f),
)
