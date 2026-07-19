package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFDCE6D5),       // Sage green
    secondary = Color(0xFF30352F),     // Dark gray-green divider
    tertiary = Color(0xFFA3D899),      // Lighter green accent
    background = Color(0xFF111311),    // Minimalist dark charcoal-green background
    surface = Color(0xFF1A1D1B),       // Minimalist dark surface
    onBackground = Color(0xFFE2E3DF),
    onSurface = Color(0xFFE2E3DF)
)

private val LightColorScheme = lightColorScheme(
    primary = SandDark,
    secondary = SoftBorder,
    tertiary = SunGold,
    background = SandBackground,
    surface = CardBackground,
    onBackground = SandDark,
    onSurface = SandDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Always light mode as requested
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
