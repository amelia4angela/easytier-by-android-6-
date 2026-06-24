package com.easytier.ui.theme

import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════
//  EasyTier Color Palette — Light & Dark Modes
// ════════════════════════════════════════════════════

// ── Light Theme (青春靓丽 — youthful & vibrant) ──
val LightBgStart = Color(0xFFFDF2F8)         // Warm pink-ish
val LightBgMid = Color(0xFFF0F4FF)            // Light blue
val LightBgEnd = Color(0xFFFAF5FF)            // Soft violet

val LightSurface = Color(0xFFFFFFFF)           // White
val LightSurfaceDim = Color(0xFFF8F8FF)        // Near-white for secondary cards
val LightCardBorder = Color(0x1A6366F1)        // Subtle indigo border

val LightTextPrimary = Color(0xFF1E1B2E)       // Near-black
val LightTextSecondary = Color(0xFF6B6584)     // Muted purple-gray
val LightTextTertiary = Color(0xFFA59DB8)      // Light muted

// ── Light Accent (gradient: indigo → pink) ──
val LightAccentStart = Color(0xFF6366F1)       // Indigo-500
val LightAccentEnd = Color(0xFFEC4899)         // Pink-500
val LightAccent = Color(0xFF7C69F0)            // Mid-point for solid uses
val LightAccentLight = Color(0xFFA78BFA)       // Violet-400
val LightAccentContainer = Color(0xFFEEF2FF)   // Very light indigo bg

val LightInputBg = Color(0xFFF5F3FF)           // Slight violet tint
val LightInputBorder = Color(0xFFD4D0E0)
val LightInputFocusBorder = Color(0xFF7C69F0)

// ── Dark Theme (深色 — deep blue-purple) ──
val DarkBgStart = Color(0xFF0F0C29)            // Deep blue-black
val DarkBgMid = Color(0xFF302B63)              // Purple
val DarkBgEnd = Color(0xFF24243E)              // Grey-purple

val DarkGlass = Color(0x14FFFFFF)              // 8% white — card fill
val DarkGlassStrong = Color(0x0DFFFFFF)        // 5% white — thin card
val DarkGlassBorder = Color(0x26FFFFFF)        // 15% white — border
val DarkNavBar = Color(0x1EFFFFFF)              // 12% white — bottom nav bg

val DarkTextPrimary = Color(0xE6FFFFFF)        // 90% white
val DarkTextSecondary = Color(0x8CFFFFFF)      // 55% white
val DarkTextTertiary = Color(0x59FFFFFF)       // 35% white

val DarkAccent = Color(0xFF8B5CF6)             // Violet-500
val DarkAccentDark = Color(0xFF6D28D9)         // Violet-700
val DarkAccentLight = Color(0xFFA78BFA)        // Violet-400

val DarkInputBg = Color(0x0FFFFFFF)            // 6% white
val DarkInputBorder = Color(0x1AFFFFFF)        // 10% white
val DarkInputFocusBorder = Color(0x808B5CF6)   // 50% accent

// ── Status (shared between themes) ──
val StatusGood = Color(0xFF4ADE80)             // Green-400
val StatusWarn = Color(0xFFFBBF24)             // Amber-400
val StatusBad = Color(0xFFF87171)              // Red-400
val StatusGoodGlow = Color(0x664ADE80)         // Glow for green
val StatusConnectedBg = Color(0x264ADE80)      // Connected banner bg
val StatusConnectedBgLight = Color(0xE8F5E9)   // Light green bg for light mode

// ── Material 3 tokens — Dark ──
val md_theme_dark_primary = DarkAccentLight
val md_theme_dark_onPrimary = Color(0xFF0A1BB4)
val md_theme_dark_primaryContainer = DarkAccent.copy(alpha = 0.3f)
val md_theme_dark_onPrimaryContainer = DarkAccentLight
val md_theme_dark_secondary = Color(0xFFC5C4DD)
val md_theme_dark_onSecondary = Color(0xFF2E2F42)
val md_theme_dark_secondaryContainer = Color(0xFF454559)
val md_theme_dark_onSecondaryContainer = Color(0xFFE1E0F9)
val md_theme_dark_tertiary = Color(0xFFD5BEC8)
val md_theme_dark_onTertiary = Color(0xFF3B2832)
val md_theme_dark_background = DarkBgStart
val md_theme_dark_onBackground = DarkTextPrimary
val md_theme_dark_surface = DarkGlass
val md_theme_dark_onSurface = DarkTextPrimary
val md_theme_dark_surfaceVariant = DarkGlass
val md_theme_dark_onSurfaceVariant = DarkTextSecondary
val md_theme_dark_outline = Color(0xFF908F9A)
val md_theme_dark_outlineVariant = Color(0xFF46464F)
val md_theme_dark_error = Color(0xFFFFB4AB)

// ── Material 3 tokens — Light ──
val md_theme_light_primary = LightAccent
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = LightAccentContainer
val md_theme_light_onPrimaryContainer = Color(0xFF211361)
val md_theme_light_secondary = Color(0xFF5E5C72)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFE3E0F9)
val md_theme_light_onSecondaryContainer = Color(0xFF1B1A2C)
val md_theme_light_tertiary = Color(0xFF78536B)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_background = LightBgEnd
val md_theme_light_onBackground = LightTextPrimary
val md_theme_light_surface = LightSurface
val md_theme_light_onSurface = LightTextPrimary
val md_theme_light_surfaceVariant = LightSurfaceDim
val md_theme_light_onSurfaceVariant = LightTextSecondary
val md_theme_light_outline = Color(0xFFC4C0D0)
val md_theme_light_outlineVariant = Color(0xFFE4E0F0)
val md_theme_light_error = Color(0xFFBA1A1A)
