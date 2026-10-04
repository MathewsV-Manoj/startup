package com.startup.focuno.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

val DeepVoidPurple = Color(0xFF0D0417)
val SurfaceElevated = Color(0xFF1A0F2E)
val SurfaceGlass = Color(0x1AFFFFFF)
val SurfaceGlassBorder = Color(0x33FFFFFF)
val NeonViolet = Color(0xFF9D4EDD)
val NeonMagenta = Color(0xFFC77DFF)
val ElectricCyan = Color(0xFF64FFDA)
val AmberWarn = Color(0xFFFFB84D)
val CrimsonAlert = Color(0xFFFF5C7A)
val TextPrimary = Color(0xFFF4F0FA)
val TextSecondary = Color(0xFFB3A6C9)
val TextTertiary = Color(0xFF7A6E8C)
val TrackInactive = Color(0xFF2A1F3D)

/**
 * Focuno roles that Material's ColorScheme has no slot for.
 * Read them through [FocunoTheme.colors] so screens never hard-code a hex value.
 */
@Immutable
data class FocunoColors(
    val productive: Color,
    val distracting: Color,
    val warning: Color,
    val glassFill: Color,
    val glassBorder: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val trackInactive: Color,
    val gaugeStart: Color,
    val gaugeEnd: Color,
)

val FocunoDarkColors = FocunoColors(
    productive = ElectricCyan,
    distracting = CrimsonAlert,
    warning = AmberWarn,
    glassFill = SurfaceGlass,
    glassBorder = SurfaceGlassBorder,
    surfaceElevated = SurfaceElevated,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textTertiary = TextTertiary,
    trackInactive = TrackInactive,
    gaugeStart = NeonViolet,
    gaugeEnd = ElectricCyan,
)
