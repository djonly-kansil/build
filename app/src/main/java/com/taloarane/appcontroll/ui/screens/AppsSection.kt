package com.taloarane.appcontroll.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.taloarane.appcontroll.data.AppEntry
import com.taloarane.appcontroll.data.AppRepository
import com.taloarane.appcontroll.data.Risk
import com.taloarane.appcontroll.service.CleanAction
import com.taloarane.appcontroll.ui.AppFilter
import com.taloarane.appcontroll.ui.AppTab
import com.taloarane.appcontroll.ui.LocalStrings
import com.taloarane.appcontroll.ui.components.SectionCard
import com.taloarane.appcontroll.ui.theme.NeonCyan
import com.taloarane.appcontroll.ui.theme.NeonGreen
import com.taloarane.appcontroll.ui.theme.NeonRed
import com.taloarane.appcontroll.ui.theme.NeonYellow

@Composable
fun CleanCard(onClean: (AppTab, CleanAction) -> Unit) {
    val s = LocalStrings.current
    SectionCard(accent = NeonGreen) {
        Text(s.cleanTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionPill(s.cleanUser, NeonRed, Modifier.weight(1f)) { onClean(AppTab.USER, CleanAction.FORCE_STOP) }
            ActionPill(s.cleanSystem, NeonYellow, Modifier.weight(1f)) { onClean(AppTab.SYSTEM, CleanAction.FORCE_STOP) }
            ActionPill(s.cleanCache, NeonGreen, Modifier.weight(1f)) { onClean(AppTab.USER, CleanAction.CLEAR_CACHE) }
        }
    }
}

@Composable
fun ActionPill(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
fun FilterRow(current: AppFilter, onChange: (AppFilter) -> Unit) {
    val s = LocalStrings.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            AppFilter.ACTIVE to s.filterActive,
            AppFilter.ALL to s.filterAll,
            AppFilter.INACTIVE to s.filterInactive,
        ).forEach { (value, label) ->
            val selected = value == current
            AssistChip(
                onClick = { onChange(value) },
                label = { Text(label, fontSize = 12.sp) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected) NeonCyan.copy(alpha = 0.18f) else Color.Transparent,
                    labelColor = if (selected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
fun TabRow3(current: AppTab, onChange: (AppTab) -> Unit) {
    val s = LocalStrings.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            AppTab.USER to s.tabUser,
            AppTab.SYSTEM to s.tabSystem,
            AppTab.WHITELIST to s.tabWhitelist,
        ).forEach { (value, label) ->
            val selected = value == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .clickable { onChange(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun AppRow(
    entry: AppEntry,
    whitelisted: Boolean,
    onAction: (AppEntry, CleanAction) -> Unit,
    onToggleWhitelist: (AppEntry) -> Unit,
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val icon = remember(entry.packageName) {
        runCatching { context.packageManager.getApplicationIcon(entry.packageName) }.getOrNull()
    }
    SectionCard(
        accent = when (entry.risk) {
            Risk.DANGER -> NeonRed
            Risk.WARN -> NeonYellow
            Risk.NORMAL -> MaterialTheme.colorScheme.outline
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = icon,
                contentDescription = entry.label,
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    entry.packageName,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${AppRepository.formatBytes(entry.sizeBytes)} · cache ${AppRepository.formatBytes(entry.cacheBytes)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onToggleWhitelist(entry) }) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = s.tabWhitelist,
                    tint = if (whitelisted) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(if (whitelisted) 1f else 0.35f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallAction(s.stop, Icons.Filled.Stop, NeonRed, Modifier.weight(1f)) { onAction(entry, CleanAction.FORCE_STOP) }
            SmallAction(s.clean, Icons.Filled.CleaningServices, NeonGreen, Modifier.weight(1f)) { onAction(entry, CleanAction.CLEAR_CACHE) }
            SmallAction(s.info, Icons.Filled.Info, NeonCyan, Modifier.weight(1f)) { openAppInfo(context, entry.packageName) }
            SmallAction(s.uninstall, Icons.Filled.Delete, NeonYellow, Modifier.weight(1f)) { onAction(entry, CleanAction.UNINSTALL) }
        }
    }
}

@Composable
private fun SmallAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
        Text(label, fontSize = 9.sp, color = color)
    }
}

fun openAppInfo(context: Context, pkg: String) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", pkg, null)),
        )
    }
}
