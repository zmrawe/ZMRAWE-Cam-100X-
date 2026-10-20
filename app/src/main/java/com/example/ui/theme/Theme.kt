package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF001B24),
    primaryContainer = Color(0xFF003644),
    onPrimaryContainer = Color(0xFF70F3FF),
    secondary = CyberGold,
    onSecondary = Color(0xFF241C00),
    secondaryContainer = Color(0xFF3F3200),
    onSecondaryContainer = Color(0xFFFFE273),
    tertiary = CyberEmerald,
    onTertiary = Color(0xFF00220D),
    tertiaryContainer = Color(0xFF00391A),
    onTertiaryContainer = Color(0xFF6BFF9A),
    background = DarkObsidian,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = CyberRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Camera UI always thrives in dark HUD aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
