package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = CyanAccentDark,
    secondary = CyanAccentLight,
    background = AppBackground,
    surface = CardBackground,
    surfaceVariant = CardBackgroundElevated,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor
)

private val LightColorScheme = lightColorScheme(
    primary = CyanAccent,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = CyanAccentLight,
    secondary = CyanAccentDark,
    background = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF49454F),
    outline = androidx.compose.ui.graphics.Color(0xFF79747E)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
