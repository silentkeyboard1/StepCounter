package com.example.stepcounter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = LightAccent,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightPrimaryText,
    onSurface = LightPrimaryText,
    onSurfaceVariant = LightSecondaryText
)

private val DarkColors = darkColorScheme(
    primary = DarkAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkPrimaryText,
    onSurface = DarkPrimaryText,
    onSurfaceVariant = DarkSecondaryText
)

@Composable
fun StepCounterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
