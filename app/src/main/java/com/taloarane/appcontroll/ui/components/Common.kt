package com.taloarane.appcontroll.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taloarane.appcontroll.ui.theme.LocalAppShape

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.outline,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val shape = LocalAppShape.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(shape.cardStroke, accent.copy(alpha = if (shape.strongOutline) 0.9f else 0.5f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        content = content,
    )
}

@Composable
fun ProgressRing(
    percent: Int,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    brush: Brush = Brush.sweepGradient(
        listOf(Color(0xFF8B5CF6), Color(0xFF22D3EE), Color(0xFF8B5CF6)),
    ),
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val stroke = 14.dp.toPx()
            val diameter = minOf(size.width, size.height) - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            drawArc(
                color = trackColor,
                startAngle = 130f,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = brush,
                startAngle = 130f,
                sweepAngle = 280f * (percent.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "$percent%",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun HistoryChart(values: List<Float>, modifier: Modifier = Modifier, color: Color = Color(0xFF8B5CF6)) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val stepX = size.width / (values.size - 1)
        val path = Path()
        val fill = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - (value.coerceIn(0f, 100f) / 100f) * size.height
            if (index == 0) {
                path.moveTo(x, y)
                fill.moveTo(x, size.height)
                fill.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        drawPath(fill, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(path, color = color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun SegmentBar(segments: List<Pair<Color, Float>>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(6.dp)),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        segments.filter { it.second > 0f }.forEach { (color, weight) ->
            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color),
            )
        }
    }
}

@Composable
fun ThinBar(percent: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                .height(5.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
    }
}

@Composable
fun LabelValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun Dot(color: Color) {
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
    Spacer(Modifier.width(6.dp))
}
