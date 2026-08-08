package com.taloarane.appcontroll.ui

import android.app.ActivityManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taloarane.appcontroll.core.AppItem
import com.taloarane.appcontroll.core.AppRepo
import com.taloarane.appcontroll.core.DeviceInfo
import com.taloarane.appcontroll.core.LocalL
import com.taloarane.appcontroll.core.Permissions
import com.taloarane.appcontroll.core.Prefs
import com.taloarane.appcontroll.core.RamStats
import com.taloarane.appcontroll.service.AppControllAccessibilityService
import com.taloarane.appcontroll.service.CleanAction
import com.taloarane.appcontroll.service.CleanForegroundService
import com.taloarane.appcontroll.service.CleanTask

enum class Filter { ACTIVE, ALL, INACTIVE }
enum class AppTab { USER, SYSTEM, WHITELIST }

@Composable
fun HomeScreen(
    stats: RamStats,
    history: List<Float>,
    device: DeviceInfo,
    apps: List<AppItem>,
    prefs: Prefs,
    onRefresh: () -> Unit,
) {
    val l = LocalL.current
    val extra = LocalExtra.current
    val context = LocalContext.current
    val self = context.packageName

    var filter by remember { mutableStateOf(Filter.ACTIVE) }
    var tab by remember { mutableStateOf(AppTab.USER) }
    var infoItem by remember { mutableStateOf<AppItem?>(null) }
    var pending by remember { mutableStateOf<PendingConfirm?>(null) }

    fun launchTasks(tasks: List<CleanTask>) {
        if (tasks.isEmpty()) {
            Toast.makeText(context, l.nothingToDo, Toast.LENGTH_SHORT).show()
            return
        }
        if (!Permissions.isAccessibilityEnabled(context)) {
            Toast.makeText(context, l.accessibilityNeeded, Toast.LENGTH_LONG).show()
            Permissions.openAccessibility(context)
            return
        }
        CleanForegroundService.start(context)
        AppControllAccessibilityService.enqueue(context, tasks)
    }

    fun request(item: AppItem, actions: List<CleanAction>) {
        if (item.isSystem || AppRepo.isCritical(context, item.packageName)) {
            pending = PendingConfirm(
                label = item.label,
                critical = AppRepo.isCritical(context, item.packageName),
                tasks = listOf(CleanTask(item.packageName, item.label, actions))
            )
        } else {
            launchTasks(listOf(CleanTask(item.packageName, item.label, actions)))
        }
    }


    val whitelist = prefs.whitelist
    val visible = apps.filter { app ->
        val inWhitelist = app.packageName in whitelist || app.packageName == self
        val tabOk = when (tab) {
            AppTab.USER -> !app.isSystem && !inWhitelist
            AppTab.SYSTEM -> app.isSystem && !inWhitelist
            AppTab.WHITELIST -> inWhitelist
        }
        val filterOk = when (filter) {
            Filter.ACTIVE -> app.active
            Filter.ALL -> true
            Filter.INACTIVE -> !app.active
        }
        tabOk && filterOk
    }

    LazyColumn(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { RamCard(stats, history, device) }

        item {
            AcCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        l.clean,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BigButton(l.cleanUser, NeonRed, Modifier.weight(1f)) {
                            val targets = apps.filter {
                                !it.isSystem && it.active &&
                                    it.packageName !in whitelist && it.packageName != self
                            }
                            trimBackground(context, targets.map { it.packageName })
                            launchTasks(
                                targets.map {
                                    CleanTask(it.packageName, it.label, listOf(CleanAction.FORCE_STOP))
                                }
                            )
                            AppControllAccessibilityService.instance?.clearRecents()
                        }
                        BigButton(l.cleanSystem, NeonYellow, Modifier.weight(1f)) {
                            val targets = apps.filter {
                                it.isSystem && it.active &&
                                    !AppRepo.isCritical(context, it.packageName) &&
                                    it.packageName !in whitelist && it.packageName != self
                            }
                            if (targets.isEmpty()) {
                                Toast.makeText(context, l.nothingToDo, Toast.LENGTH_SHORT).show()
                            } else {
                                pending = PendingConfirm(
                                    label = "${targets.size} × ${l.tabSystem}",
                                    critical = false,
                                    tasks = targets.map {
                                        CleanTask(it.packageName, it.label, listOf(CleanAction.FORCE_STOP))
                                    }
                                )
                            }
                        }

                        BigButton(l.cleanCache, NeonGreen, Modifier.weight(1f)) {
                            val freedBefore = AppRepo.ramStats(context).available
                            trimBackground(
                                context,
                                apps.filter { it.packageName !in whitelist && it.packageName != self }
                                    .map { it.packageName }
                            )
                            onRefresh()
                            val freed = (AppRepo.ramStats(context).available - freedBefore).coerceAtLeast(0)
                            Toast.makeText(
                                context,
                                "${l.done}: ${AppRepo.formatBytes(freed)}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChipAc(l.filterActive, filter == Filter.ACTIVE, Modifier.weight(1f)) { filter = Filter.ACTIVE }
                FilterChipAc(l.filterAll, filter == Filter.ALL, Modifier.weight(1f)) { filter = Filter.ALL }
                FilterChipAc(l.filterInactive, filter == Filter.INACTIVE, Modifier.weight(1f)) { filter = Filter.INACTIVE }
            }
        }

        item {
            AcCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TabPill(l.tabUser, tab == AppTab.USER, Modifier.weight(1f)) { tab = AppTab.USER }
                    TabPill(l.tabSystem, tab == AppTab.SYSTEM, Modifier.weight(1f)) { tab = AppTab.SYSTEM }
                    TabPill(l.tabWhitelist, tab == AppTab.WHITELIST, Modifier.weight(1f)) { tab = AppTab.WHITELIST }
                }
            }
        }

        if (visible.isEmpty()) {
            item {
                Text(
                    l.empty,
                    color = extra.subtle,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }

        items(visible, key = { it.packageName }) { app ->
            AppRow(
                item = app,
                whitelisted = app.packageName in whitelist,
                onStop = { request(app, listOf(CleanAction.FORCE_STOP)) },
                onClear = { request(app, listOf(CleanAction.CLEAR_CACHE)) },
                onInfo = { infoItem = app },
                onUninstall = {
                    request(
                        app,
                        listOf(CleanAction.CLEAR_CACHE, CleanAction.FORCE_STOP, CleanAction.UNINSTALL)
                    )
                },
                onWhitelist = { prefs.toggleWhitelist(app.packageName) },
            )
        }

        item { Spacer(Modifier.height(70.dp)) }
    }

    infoItem?.let { AppInfoDialog(it) { infoItem = null } }

    pending?.let { confirm ->
        WarnDialog(
            critical = confirm.critical,
            appLabel = confirm.label,
            onDismiss = { pending = null },
            onConfirm = {
                val tasks = confirm.tasks
                pending = null
                launchTasks(tasks)
            }
        )
    }
}

data class PendingConfirm(
    val label: String,
    val critical: Boolean,
    val tasks: List<CleanTask>,
)


fun trimBackground(context: Context, packages: List<String>) {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    packages.forEach { pkg -> runCatching { am.killBackgroundProcesses(pkg) } }
}

@Composable
fun BigButton(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val extra = LocalExtra.current
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = if (extra.dark) 0.14f else 0.10f))
            .border(
                if (extra.dark) 1.dp else 1.5.dp,
                color.copy(alpha = if (extra.dark) 0.5f else 1f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun FilterChipAc(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val extra = LocalExtra.current
    val color = if (selected) NeonCyan else extra.subtle
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) NeonCyan.copy(alpha = 0.18f) else extra.cardBg)
            .border(
                if (selected) 1.5.dp else extra.cardStrokeWidth,
                if (selected) NeonCyan else extra.cardStroke,
                RoundedCornerShape(50)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun TabPill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val extra = LocalExtra.current
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) NeonPurple.copy(alpha = 0.22f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) NeonPurple else extra.subtle,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
