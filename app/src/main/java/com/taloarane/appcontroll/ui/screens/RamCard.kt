package com.taloarane.appcontroll.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taloarane.appcontroll.data.RamRepository
import com.taloarane.appcontroll.data.RamStats
import com.taloarane.appcontroll.ui.LocalStrings
import com.taloarane.appcontroll.ui.components.HistoryChart
import com.taloarane.appcontroll.ui.components.LabelValue
import com.taloarane.appcontroll.ui.components.ProgressRing
import com.taloarane.appcontroll.ui.components.SectionCard
import com.taloarane.appcontroll.ui.components.SegmentBar
import com.taloarane.appcontroll.ui.components.ThinBar
import com.taloarane.appcontroll.ui.theme.NeonBlue
import com.taloarane.appcontroll.ui.theme.NeonCyan
import com.taloarane.appcontroll.ui.theme.NeonGreen
import com.taloarane.appcontroll.ui.theme.NeonPurple
import com.taloarane.appcontroll.ui.theme.NeonRed
import com.taloarane.appcontroll.ui.theme.NeonYellow

@Composable
fun RamCard(stats: RamStats, history: List<Float>, sdkInfo: String, androidInfo: String) {
    val s = LocalStrings.current
    SectionCard(accent = NeonPurple) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.ram, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Android $sdkInfo",
                    fontSize = 13.sp,
                    color = NeonCyan,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(sdkInfo, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                Text(androidInfo, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(percent = stats.usedPercent, modifier = Modifier.width(140.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        RamRepository.formatGb(stats.usedKb) + " GB",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        " / ${RamRepository.formatGb(stats.totalKb)} GB",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(s.used, fontSize = 13.sp, color = NeonCyan)
                Spacer(Modifier.height(8.dp))
                Text(s.totalRam, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    RamRepository.formatGb(stats.totalKb) + " GB",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        HistoryChart(values = history, modifier = Modifier.fillMaxWidth().height(72.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("60 dtk", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("30 dtk", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("0 dtk", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(10.dp))
        SegmentBar(
            segments = listOf(
                NeonRed to stats.userKb.toFloat().coerceAtLeast(1f),
                NeonGreen to stats.cacheKb.toFloat().coerceAtLeast(1f),
                NeonYellow to stats.systemKb.toFloat().coerceAtLeast(1f),
                NeonBlue to stats.freeKb.toFloat().coerceAtLeast(1f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MemoryTile(s.userApps, stats.userKb, stats.percentOf(stats.userKb), NeonRed, Modifier.weight(1f))
            MemoryTile(s.cache, stats.cacheKb, stats.percentOf(stats.cacheKb), NeonGreen, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MemoryTile(s.system, stats.systemKb, stats.percentOf(stats.systemKb), NeonYellow, Modifier.weight(1f))
            MemoryTile(s.free, stats.freeKb, stats.percentOf(stats.freeKb), NeonBlue, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LabelValue(s.arch, stats.arch, Modifier.weight(1f))
            LabelValue(s.ramType, stats.ramType, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LabelValue(s.pageSize, "${stats.pageSizeKb} KB", Modifier.weight(1f))
            LabelValue(
                s.security,
                if (stats.mte) "Memory Tagging (MTE)" else "Standar",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MemoryTile(title: String, kb: Long, percent: Int, color: Color, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier, accent = color) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp)) {
                ThinBar(percent = 100, color = color)
            }
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                RamRepository.formatGb(kb),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(" GB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("$percent%", fontSize = 12.sp, color = color)
        Spacer(Modifier.height(6.dp))
        ThinBar(percent = percent, color = color)
    }
}
