package com.taloarane.appcontroll.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.taloarane.appcontroll.core.ThemeMode

// Neon accents used by the RAM card
val NeonPurple = Color(0xFF8B5CF6)
val NeonCyan = Color(0xFF22D3EE)
val NeonRed = Color(0xFFEF4444)
val NeonGreen = Color(0xFF22C55E)
val NeonYellow = Color(0xFFEAB308)
val NeonBlue = Color(0xFF3B82F6)

data class Extra(
    val cardStroke: Color,
    val cardStrokeWidth: androidx.compose.ui.unit.Dp,
    val cardBg: Color,
    val innerBg: Color,
    val subtle: Color,
    val dark: Boolean,
)

val LocalExtra = compositionLocalOf {
    Extra(Color(0x33FFFFFF), 1.dp, Color(0xFF0D1424), Color(0xFF0A0F1C), Color(0xFF8FA0C8), true)
}

private val DarkColors = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color(0xFFF5F3FF),
    secondary = NeonCyan,
    background = Color(0xFF070A12),
    onBackground = Color(0xFFDCE3F7),
    surface = Color(0xFF0D1424),
    onSurface = Color(0xFFDCE3F7),
    surfaceVariant = Color(0xFF131C31),
    onSurfaceVariant = Color(0xFF9FB0D4),
    error = NeonRed,
    outline = Color(0xFF243354),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B21B6),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF0E7490),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF0B1020),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B1020),
    surfaceVariant = Color(0xFFEDF1F9),
    onSurfaceVariant = Color(0xFF31405E),
    error = Color(0xFFB91C1C),
    outline = Color(0xFF0B1020),
)

val CardShape = RoundedCornerShape(18.dp)

@Composable
fun AppControllTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val extra = if (dark) Extra(
        cardStroke = Color(0x1FFFFFFF),
        cardStrokeWidth = 1.dp,
        cardBg = Color(0xFF0D1424),
        innerBg = Color(0xFF0A0F1C),
        subtle = Color(0xFF8FA0C8),
        dark = true,
    ) else Extra(
        cardStroke = Color(0xFF11172B),
        cardStrokeWidth = 1.5.dp,
        cardBg = Color(0xFFFFFFFF),
        innerBg = Color(0xFFF7F9FE),
        subtle = Color(0xFF44506B),
        dark = false,
    )

    CompositionLocalProvider(LocalExtra provides extra) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = Typography(),
            content = content
        )
    }
}
