package com.focuno.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val FocusIndigo = Color(0xFF4C5BD4)
private val FocusIndigoDark = Color(0xFFB9C0FF)
private val CalmTeal = Color(0xFF2A9D8F)
private val AlertAmber = Color(0xFFE9A23B)

private val LightColors = lightColorScheme(
    primary = FocusIndigo,
    secondary = CalmTeal,
    tertiary = AlertAmber,
)

private val DarkColors = darkColorScheme(
    primary = FocusIndigoDark,
    secondary = CalmTeal,
    tertiary = AlertAmber,
)

@Composable
fun FocunoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FocunoTypography,
        content = content,
    )
}
