package com.taloarane.appcontroll.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AcCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    background: Color? = null,
    content: @Composable () -> Unit,
) {
    val extra = LocalExtra.current
    Card(
        modifier = modifier,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = background ?: extra.cardBg),
        border = BorderStroke(
            extra.cardStrokeWidth,
            accent?.copy(alpha = if (extra.dark) 0.55f else 1f) ?: extra.cardStroke
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (extra.dark) 0.dp else 2.dp),
    ) { content() }
}

@Composable
fun ProgressBarLine(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
) {
    val track = if (LocalExtra.current.dark) Color.White.copy(alpha = 0.08f)
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}
