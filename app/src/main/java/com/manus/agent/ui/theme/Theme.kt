package com.manus.agent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColorScheme(
    primary = Purple500,
    secondary = Teal200,
    background = Black,
    surface = Purple700,
    onPrimary = White,
    onSecondary = Black,
    onBackground = White,
    onSurface = White
)

private val LightColorPalette = lightColorScheme(
    primary = Purple500,
    secondary = Teal200,
    background = White,
    surface = Purple700,
    onPrimary = White,
    onSecondary = Black,
    onBackground = Black,
    onSurface = White
)

@Composable
fun ManusAgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
