package com.signalfind.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = SignalPrimary,
    onPrimary = SignalOnPrimary,
    primaryContainer = SignalPrimaryDark,
    onPrimaryContainer = SignalOnPrimary,
    secondary = SignalAccent,
    background = SignalBackground,
    surface = SignalSurface,
    error = SignalError,
)

private val DarkScheme = darkColorScheme(
    primary = SignalPrimaryNight,
    onPrimary = SignalBackgroundNight,
    primaryContainer = SignalPrimaryDark,
    onPrimaryContainer = SignalOnPrimary,
    secondary = SignalAccent,
    background = SignalBackgroundNight,
    surface = SignalSurfaceNight,
    error = SignalError,
)

@Composable
fun SignalFindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content
    )
}
