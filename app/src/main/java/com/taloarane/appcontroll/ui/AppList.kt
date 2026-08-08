package com.taloarane.appcontroll.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taloarane.appcontroll.core.AppItem
import com.taloarane.appcontroll.core.AppRepo
import com.taloarane.appcontroll.core.LocalL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppRow(
    item: AppItem,
    whitelisted: Boolean,
    onStop: () -> Unit,
    onClear: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
    onWhitelist: () -> Unit,
) {
    val l = LocalL.current
    val extra = LocalExtra.current
    val context = LocalContext.current
    val icon = remember(item.packageName) { AppRepo.icon(context, item.packageName) }

    AcCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(extra.innerBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Image(icon, contentDescription = item.label, Modifier.size(34.dp))
                    } else {
                        Text(item.label.take(1).uppercase(), color = NeonPurple, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        item.packageName,
                        fontSize = 10.sp,
                        color = extra.subtle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(6.dp))
                StatusDot(item.active, if (item.active) l.running else l.stopped)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip(l.stop, NeonRed, Modifier.weight(1f), onClick = onStop)
                ActionChip(l.clear, NeonGreen, Modifier.weight(1f), onClick = onClear)
                ActionChip(l.info, NeonCyan, Modifier.weight(1f), onClick = onInfo)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip(l.uninstall, NeonYellow, Modifier.weight(1f), onClick = onUninstall)
                ActionChip(
                    l.whitelist,
                    if (whitelisted) NeonPurple else extra.subtle,
                    Modifier.weight(1f),
                    filled = whitelisted,
                    onClick = onWhitelist
                )
            }
        }
    }
}

@Composable
fun StatusDot(active: Boolean, label: String) {
    val color = if (active) NeonGreen else LocalExtra.current.subtle
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActionChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    val extra = LocalExtra.current
    Box(
        modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) color.copy(alpha = 0.28f) else color.copy(alpha = if (extra.dark) 0.10f else 0.08f))
            .border(
                if (extra.dark) 1.dp else 1.5.dp,
                color.copy(alpha = if (extra.dark) 0.45f else 0.9f),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun AppInfoDialog(item: AppItem, onDismiss: () -> Unit) {
    val l = LocalL.current
    val extra = LocalExtra.current
    val fmt = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(l.close) } },
        title = { Text(item.label, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                InfoLine(l.packageLabel, item.packageName)
                InfoLine(l.uid, item.uid.toString())
                InfoLine(l.sdk, item.targetSdk.toString())
                InfoLine(l.version, item.versionName)
                InfoLine(l.apkSize, AppRepo.formatBytes(item.apkSize))
                InfoLine(l.installed, fmt.format(Date(item.firstInstall)))
                InfoLine(
                    l.lastUsed,
                    if (item.lastUsed <= 0) l.neverUsed else {
                        val days = ((System.currentTimeMillis() - item.lastUsed) / 86_400_000L).toInt()
                        "${fmt.format(Date(item.lastUsed))} · $days d"
                    }
                )
                InfoLine(l.system, if (item.isSystem) "✔" else "✕")
                Spacer(Modifier.height(4.dp))
                Text(
                    if (item.active) l.running else l.stopped,
                    color = if (item.active) NeonGreen else extra.subtle,
                    fontSize = 11.sp
                )
            }
        },
        shape = CardShape,
        containerColor = extra.cardBg,
        modifier = Modifier.border(extra.cardStrokeWidth, extra.cardStroke, CardShape)
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    val extra = LocalExtra.current
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(label, fontSize = 12.sp, color = extra.subtle, modifier = Modifier.width(110.dp))
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun WarnDialog(
    critical: Boolean,
    appLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val l = LocalL.current
    val extra = LocalExtra.current
    val color = if (critical) NeonRed else NeonYellow
    var seconds by remember { mutableStateOf(if (critical) 2 else 0) }
    LaunchedEffect(critical) {
        while (seconds > 0) {
            kotlinx.coroutines.delay(1000)
            seconds -= 1
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (critical) "⚠ ${l.danger}" else "⚠ ${l.warning}",
                color = color,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column {
                Text(appLabel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (critical) l.dangerMsg else l.warnSystemMsg,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = seconds == 0) {
                Text(if (seconds > 0) "${l.holdToConfirm} ($seconds)" else l.proceed, color = color)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(l.cancel) } },
        shape = CardShape,
        containerColor = extra.cardBg,
        modifier = Modifier.border(extra.cardStrokeWidth, color, CardShape)
    )
}
