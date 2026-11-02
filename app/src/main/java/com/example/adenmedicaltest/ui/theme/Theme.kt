package com.example.adenmedicaltest.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = OnIndigoContainer,
    secondary = TealSecondary,
    onSecondary = White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = OnTealContainer,
    background = Slate50,
    onBackground = Slate900,
    surface = White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate200,
    error = RoseError,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = IndigoLight,
    onPrimary = IndigoDark,
    primaryContainer = IndigoDark,
    onPrimaryContainer = IndigoContainer,
    secondary = TealSecondary,
    onSecondary = White,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate700,
    onSurface = Slate50,
    error = RoseError,
    onError = White
)

@Composable
fun AdenMedicalTestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
