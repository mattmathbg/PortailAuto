package com.crous.gateopener.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Teal400,
    onPrimary = Slate900,
    primaryContainer = Teal600,
    secondary = Emerald500,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = Slate100,
    onSurface = Slate100,
    outline = DarkCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Teal600,
    onPrimary = Slate100,
    primaryContainer = Teal400,
    secondary = Emerald600,
    background = Slate100,
    surface = Slate200,
    onBackground = Slate900,
    onSurface = Slate900,
    outline = Slate400
)

@Composable
fun PortailGateOpenerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
