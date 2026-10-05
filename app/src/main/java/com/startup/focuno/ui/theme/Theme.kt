package com.startup.focuno.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Every slot is mapped explicitly. Material's dark baseline is grey, and any slot left at its default
// would show up as a grey sheet, dialog or menu on top of the purple UI.
private val FocunoColorScheme = darkColorScheme(
    primary = NeonViolet,
    onPrimary = TextPrimary,
    primaryContainer = TrackInactive,
    onPrimaryContainer = NeonMagenta,
    inversePrimary = NeonViolet,
    secondary = NeonMagenta,
    onSecondary = DeepVoidPurple,
    secondaryContainer = TrackInactive,
    onSecondaryContainer = NeonMagenta,
    tertiary = ElectricCyan,
    onTertiary = DeepVoidPurple,
    tertiaryContainer = TrackInactive,
    onTertiaryContainer = ElectricCyan,
    background = DeepVoidPurple,
    onBackground = TextPrimary,
    surface = DeepVoidPurple,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceTint = NeonViolet,
    inverseSurface = TextPrimary,
    inverseOnSurface = DeepVoidPurple,
    error = CrimsonAlert,
    onError = DeepVoidPurple,
    errorContainer = TrackInactive,
    onErrorContainer = CrimsonAlert,
    outline = TextTertiary,
    outlineVariant = TrackInactive,
    scrim = Color.Black,
    surfaceBright = TrackInactive,
    surfaceDim = DeepVoidPurple,
    surfaceContainerLowest = DeepVoidPurple,
    surfaceContainerLow = SurfaceElevated,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = TrackInactive,
    surfaceContainerHighest = TrackInactive,
)

private val LocalFocunoColors = staticCompositionLocalOf { FocunoDarkColors }

/** Dark-only by design: there is no light scheme and no dynamic colour. */
@Composable
fun FocunoTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFocunoColors provides FocunoDarkColors) {
        MaterialTheme(
            colorScheme = FocunoColorScheme,
            typography = FocunoTypography,
            shapes = FocunoShapes,
            content = content,
        )
    }
}

object FocunoTheme {
    val colors: FocunoColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFocunoColors.current
}
