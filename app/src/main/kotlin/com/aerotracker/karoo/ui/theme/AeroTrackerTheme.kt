package com.aerotracker.karoo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AeroDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E676),
    onPrimary = Color(0xFF003300),
    primaryContainer = Color(0xFF003300),
    onPrimaryContainer = Color(0xFF00E676),
    secondary = Color(0xFF69F0AE),
    background = Color(0xFF0D0D0D),
    surface = Color(0xFF1A1A1A),
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFF44336)
)

@Composable
fun AeroTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AeroDarkColorScheme,
        content = content
    )
}
