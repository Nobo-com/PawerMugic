package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppThemeColors(
    val name: String,
    val background: Color,
    val surface: Color,
    val accent: Color,
    val accentDark: Color,
    val text: Color,
    val textSecondary: Color,
    val progressTrack: Color
)

val ThemeBlue = AppThemeColors(
    name = "Blue",
    background = Color(0xFF0F1115),
    surface = Color(0xFF1F2228),
    accent = Color(0xFF35CCFF),
    accentDark = Color(0xFF00A3FF),
    text = Color(0xFFE2E2E6),
    textSecondary = Color(0xFFA0A3AD),
    progressTrack = Color(0xFF1F2228)
)

val ThemePurple = AppThemeColors(
    name = "Purple",
    background = Color(0xFF130E1F),
    surface = Color(0xFF261D3A),
    accent = Color(0xFFB570FF),
    accentDark = Color(0xFF8930FF),
    text = Color(0xFFE2E2E6),
    textSecondary = Color(0xFFA0A3AD),
    progressTrack = Color(0xFF261D3A)
)

val ThemeGreen = AppThemeColors(
    name = "Green",
    background = Color(0xFF0E1A14),
    surface = Color(0xFF1D3328),
    accent = Color(0xFF10B981),
    accentDark = Color(0xFF059669),
    text = Color(0xFFE2E2E6),
    textSecondary = Color(0xFFA0A3AD),
    progressTrack = Color(0xFF1D3328)
)

val ThemeRed = AppThemeColors(
    name = "Red",
    background = Color(0xFF1A0E0E),
    surface = Color(0xFF331D1D),
    accent = Color(0xFFFF5252),
    accentDark = Color(0xFFD50000),
    text = Color(0xFFE2E2E6),
    textSecondary = Color(0xFFA0A3AD),
    progressTrack = Color(0xFF331D1D)
)

val LocalAppTheme = compositionLocalOf { ThemeBlue }

@Composable
fun MyApplicationTheme(
    appTheme: AppThemeColors = ThemeBlue,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = appTheme.accent,
        secondary = appTheme.accentDark,
        background = appTheme.background,
        surface = appTheme.surface,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = appTheme.text,
        onSurface = appTheme.text
    )

    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
