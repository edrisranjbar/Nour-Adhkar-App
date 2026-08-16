package com.example.ui.theme

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
    surfaceVariant = Color(0xFF252A27),
    secondaryContainer = Color(0xFF263428),
    onSecondaryContainer = Color(0xFFDCE6D5),
    tertiaryContainer = Color(0xFF3A3022),
    onTertiaryContainer = Color(0xFFFFDDB3),
    onBackground = Color(0xFFE2E3DF),
    onSurface = Color(0xFFE2E3DF),
    onSurfaceVariant = Color(0xFFBDC9BF),
    outlineVariant = Color(0xFF3E4942)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF191C1A),
    secondary = Color(0xFFDCE6D5),
    tertiary = SunGold,
    background = SandBackground,
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F5F5),
    secondaryContainer = Color(0xFFE8F0E1),
    onSecondaryContainer = Color(0xFF24451F),
    tertiaryContainer = Color(0xFFFFF3E0),
    onTertiaryContainer = Color(0xFF6D3A00),
    onBackground = Color(0xFF191C1A),
    onSurface = Color(0xFF191C1A),
    onSurfaceVariant = Color(0xFF43493F),
    outlineVariant = Color(0xFFDCE6D5)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
