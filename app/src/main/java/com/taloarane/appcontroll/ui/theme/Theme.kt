package com.taloarane.appcontroll.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.taloarane.appcontroll.data.ThemeMode

val NeonPurple = Color(0xFF8B5CF6)
val NeonCyan = Color(0xFF22D3EE)
val NeonGreen = Color(0xFF22C55E)
val NeonYellow = Color(0xFFEAB308)
val NeonRed = Color(0xFFEF4444)
val NeonBlue = Color(0xFF3B82F6)

private val DarkScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color(0xFFF5F3FF),
    secondary = NeonCyan,
    background = Color(0xFF060A14),
    onBackground = Color(0xFFE6ECFF),
    surface = Color(0xFF0C1224),
    onSurface = Color(0xFFDCE4F7),
    surfaceVariant = Color(0xFF131B31),
    onSurfaceVariant = Color(0xFF9AA6C4),
    outline = Color(0xFF223055),
    error = NeonRed,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5B21B6),
    onPrimary = Color.White,
    secondary = Color(0xFF0E7490),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF10131C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10131C),
    surfaceVariant = Color(0xFFEDEFF6),
    onSurfaceVariant = Color(0xFF3B4256),
    outline = Color(0xFF1B2130),
    error = Color(0xFFB91C1C),
)

data class AppShape(val cardStroke: androidx.compose.ui.unit.Dp, val strongOutline: Boolean)

val LocalAppShape = staticCompositionLocalOf { AppShape(1.dp, false) }

@Composable
fun AppControllTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(
        LocalAppShape provides if (dark) AppShape(1.dp, false) else AppShape(1.5.dp, true),
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = Typography(),
            content = content,
        )
    }
}
