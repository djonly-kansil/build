package com.taloarane.appcontroll.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.taloarane.appcontroll.core.AppRepo
import com.taloarane.appcontroll.core.DeviceInfo
import com.taloarane.appcontroll.core.LocalL
import com.taloarane.appcontroll.core.RamStats
import java.util.Locale

@Composable
fun RamCard(stats: RamStats, history: List<Float>, device: DeviceInfo) {
    val l = LocalL.current
    val extra = LocalExtra.current
    val percent = if (stats.total > 0) (stats.used.toFloat() / stats.total) else 0f

    AcCard(modifier = Modifier.fillMaxWidth(), accent = NeonPurple) {
        Column(Modifier.padding(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(listOf(NeonPurple.copy(0.35f), NeonCyan.copy(0.25f)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▣", fontSize = 22.sp, color = NeonPurple)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(l.ram, fontSize = 26.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Row {
                        Text("Android SDK ${device.sdk} ", fontSize = 12.sp, color = extra.subtle)
                        Text("/ ${device.minSdk}", fontSize = 12.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                }
                AcCard(accent = NeonGreen, background = extra.innerBg) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            "SDK ${device.sdk} / ${device.minSdk}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("Android ${device.release}", fontSize = 10.sp, color = extra.subtle)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Ring + numbers + chart
            AcCard(background = extra.innerBg) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(96.dp)) {
                            val stroke = 11.dp.toPx()
                            drawArc(
                                color = if (extra.dark) Color.White.copy(0.07f) else Color(0x22000000),
                                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                                style = Stroke(stroke, cap = StrokeCap.Round),
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size(size.width - stroke, size.height - stroke)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(listOf(NeonCyan, NeonPurple, NeonCyan)),
                                startAngle = -90f, sweepAngle = 360f * percent, useCenter = false,
                                style = Stroke(stroke, cap = StrokeCap.Round),
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size(size.width - stroke, size.height - stroke)
                            )
                        }
                        Text(
                            "${(percent * 100).toInt()}%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                AppRepo.formatBytes(stats.used),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                " / ${AppRepo.formatBytes(stats.total)}",
                                fontSize = 14.sp,
                                color = extra.subtle,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Text(l.used, fontSize = 13.sp, color = NeonCyan)
                        Spacer(Modifier.height(6.dp))
                        Text(l.totalRam, fontSize = 11.sp, color = extra.subtle)
                        Text(
                            AppRepo.formatBytes(stats.total),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Sparkline(history, Modifier.weight(1.1f).height(78.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // Segmented usage bar
            val total = stats.total.coerceAtLeast(1)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Seg(NeonRed, stats.userApps.toFloat() / total)
                Seg(NeonGreen, stats.cached.toFloat() / total)
                Seg(NeonYellow, stats.systemMem.toFloat() / total)
                Seg(NeonBlue, stats.available.toFloat() / total)
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat(Modifier.weight(1f), l.userApps, stats.userApps, total, NeonRed, "▦")
                MiniStat(Modifier.weight(1f), l.cache, stats.cached, total, NeonGreen, "◔")
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat(Modifier.weight(1f), l.system, stats.systemMem, total, NeonYellow, "⚙")
                MiniStat(Modifier.weight(1f), l.free, stats.available, total, NeonBlue, "✦")
            }

            Spacer(Modifier.height(10.dp))

            AcCard(background = extra.innerBg) {
                Column(Modifier.padding(12.dp)) {
                    Row {
                        InfoBit(Modifier.weight(1f), l.architecture, device.arch, NeonPurple)
                        InfoBit(Modifier.weight(1f), l.ramType, device.ramType, NeonCyan)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        InfoBit(Modifier.weight(1f), l.pageSize, device.pageSize, NeonBlue)
                        InfoBit(Modifier.weight(1f), l.security, device.security, NeonGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Seg(color: Color, fraction: Float) {
    Box(
        Modifier
            .weight(fraction.coerceAtLeast(0.02f))
            .height(14.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
    )
}

@Composable
private fun MiniStat(
    modifier: Modifier,
    title: String,
    value: Long,
    total: Long,
    color: Color,
    glyph: String,
) {
    val extra = LocalExtra.current
    val pct = (value.toFloat() / total.coerceAtLeast(1)) * 100
    AcCard(modifier = modifier, accent = color, background = if (extra.dark) color.copy(alpha = 0.07f) else extra.cardBg) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Text(glyph, color = color, fontSize = 14.sp) }
                Spacer(Modifier.width(8.dp))
                Text(title, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                AppRepo.formatBytes(value),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                String.format(Locale.getDefault(), "%.0f%%", pct),
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            ProgressBarLine(pct / 100f, color, height = 4.dp)
        }
    }
}

@Composable
private fun InfoBit(modifier: Modifier, label: String, value: String, color: Color) {
    val extra = LocalExtra.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.18f))
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 10.sp, color = extra.subtle)
            Text(
                value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun Sparkline(history: List<Float>, modifier: Modifier = Modifier) {
    val extra = LocalExtra.current
    Canvas(modifier) {
        if (history.size < 2) return@Canvas
        val step = size.width / (history.size - 1)
        val path = Path()
        val fill = Path()
        history.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height * (1f - v.coerceIn(0f, 1f))
            if (i == 0) {
                path.moveTo(x, y); fill.moveTo(x, size.height); fill.lineTo(x, y)
            } else {
                path.lineTo(x, y); fill.lineTo(x, y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(NeonPurple.copy(0.35f), Color.Transparent)))
        drawPath(path, color = NeonPurple, style = Stroke(2.5f))
        drawLine(
            color = if (extra.dark) Color.White.copy(0.12f) else Color(0x22000000),
            start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = 1.5f
        )
    }
}
