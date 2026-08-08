package com.taloarane.appcontroll

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taloarane.appcontroll.data.AppEntry
import com.taloarane.appcontroll.data.Risk
import com.taloarane.appcontroll.service.AppControllAccessibilityService
import com.taloarane.appcontroll.service.CleanAction
import com.taloarane.appcontroll.service.CleanForegroundService
import com.taloarane.appcontroll.service.CleanQueue
import com.taloarane.appcontroll.ui.AppTab
import com.taloarane.appcontroll.ui.LocalStrings
import com.taloarane.appcontroll.ui.MainViewModel
import com.taloarane.appcontroll.ui.components.SectionCard
import com.taloarane.appcontroll.ui.screens.AppRow
import com.taloarane.appcontroll.ui.screens.CleanCard
import com.taloarane.appcontroll.ui.screens.FilterRow
import com.taloarane.appcontroll.ui.screens.PermissionGate
import com.taloarane.appcontroll.ui.screens.RamCard
import com.taloarane.appcontroll.ui.screens.RiskDialog
import com.taloarane.appcontroll.ui.screens.SettingsScreen
import com.taloarane.appcontroll.ui.screens.StorageScreen
import com.taloarane.appcontroll.ui.screens.TabRow3
import com.taloarane.appcontroll.ui.stringsFor
import com.taloarane.appcontroll.ui.theme.AppControllTheme
import com.taloarane.appcontroll.ui.theme.NeonGreen
import com.taloarane.appcontroll.ui.theme.NeonPurple

private enum class Screen { HOME, ROM, SD, SETTINGS, PERMISSIONS }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsState()

            LaunchedEffect(settings.keepScreenOn) {
                if (settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            AppControllTheme(settings.theme) {
                CompositionLocalProvider(LocalStrings provides stringsFor(settings.language)) {
                    AppRoot(vm)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(vm: MainViewModel) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val progress by vm.cleanProgress.collectAsState()
    val scan by vm.scan.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val romVolume by vm.romVolume.collectAsState()
    val sdVolume by vm.sdVolume.collectAsState()

    var screen by remember { mutableStateOf(if (settings.onboarded) Screen.HOME else Screen.PERMISSIONS) }
    var pendingConfirm by remember { mutableStateOf<Pair<List<AppEntry>, CleanAction>?>(null) }

    LaunchedEffect(Unit) { vm.refreshVolumes() }

    fun run(entries: List<AppEntry>, action: CleanAction) {
        if (entries.isEmpty()) return
        if (!AppControllAccessibilityService.isEnabled(context)) {
            screen = Screen.PERMISSIONS
            return
        }
        vm.queue(entries, action)
        CleanForegroundService.start(context)
    }

    fun request(entries: List<AppEntry>, action: CleanAction) {
        val worst = entries.maxByOrNull { it.risk.ordinal }?.risk ?: Risk.NORMAL
        if (worst == Risk.NORMAL || action == CleanAction.CLEAR_CACHE) {
            run(entries, action)
        } else {
            pendingConfirm = entries to action
        }
    }

    if (screen == Screen.PERMISSIONS) {
        PermissionGate(onContinue = {
            vm.setOnboarded(true)
            screen = Screen.HOME
        })
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = screen == Screen.HOME,
                    onClick = { screen = Screen.HOME },
                    icon = { Icon(Icons.Filled.Memory, contentDescription = s.navRam) },
                    label = { Text(s.navRam, fontSize = 10.sp) },
                )
                NavigationBarItem(
                    selected = screen == Screen.ROM,
                    onClick = {
                        screen = Screen.ROM
                        vm.refreshVolumes()
                    },
                    icon = { Icon(Icons.Filled.Storage, contentDescription = s.navRom) },
                    label = { Text(s.navRom, fontSize = 10.sp) },
                )
                NavigationBarItem(
                    selected = screen == Screen.SD,
                    onClick = {
                        screen = Screen.SD
                        vm.refreshVolumes()
                    },
                    icon = { Icon(Icons.Filled.SdCard, contentDescription = s.navSd) },
                    label = { Text(s.navSd, fontSize = 10.sp) },
                )
            }
        },
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        s.appName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    IconButton(onClick = { screen = Screen.SETTINGS }) {
                        Icon(Icons.Filled.Settings, contentDescription = s.settings, tint = NeonPurple)
                    }
                }

                when (screen) {
                    Screen.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onLanguage = vm::setLanguage,
                        onTheme = vm::setTheme,
                        onKeepScreenOn = vm::setKeepScreenOn,
                        onPermissions = { screen = Screen.PERMISSIONS },
                    )
                    Screen.ROM -> StorageScreen(
                        volume = romVolume,
                        language = settings.language,
                        scan = scan,
                        scanning = scanning,
                        onScan = { vm.scanStorage(romVolume) },
                        onDelete = { vm.deleteFiles(it, romVolume) },
                    )
                    Screen.SD -> StorageScreen(
                        volume = sdVolume,
                        language = settings.language,
                        scan = scan,
                        scanning = scanning,
                        onScan = { vm.scanStorage(sdVolume) },
                        onDelete = { vm.deleteFiles(it, sdVolume) },
                    )
                    else -> HomeList(
                        vm = vm,
                        onRequest = { entries, action -> request(entries, action) },
                    )
                }
            }

            if (progress.running) {
                SectionCard(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    accent = NeonGreen,
                ) {
                    Text(
                        "${progress.current}/${progress.total} · ${progress.label}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        s.cancel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable { CleanQueue.cancel() },
                    )
                }
            }
        }
    }

    pendingConfirm?.let { (entries, action) ->
        RiskDialog(
            risk = entries.maxByOrNull { it.risk.ordinal }?.risk ?: Risk.WARN,
            appLabel = entries.joinToString(", ") { it.label }.take(120),
            onDismiss = { pendingConfirm = null },
            onConfirm = {
                pendingConfirm = null
                run(entries, action)
            },
        )
    }
}

@Composable
private fun HomeList(vm: MainViewModel, onRequest: (List<AppEntry>, CleanAction) -> Unit) {
    val s = LocalStrings.current
    val settings by vm.settings.collectAsState()
    val ram by vm.ram.collectAsState()
    val history by vm.history.collectAsState()
    val filter by vm.filter.collectAsState()
    val tab by vm.tab.collectAsState()
    vm.apps.collectAsState()
    val visible = vm.visibleApps()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
    ) {
        item {
            RamCard(
                stats = ram,
                history = history,
                sdkInfo = "SDK ${Build.VERSION.SDK_INT} / 24",
                androidInfo = "Android ${Build.VERSION.RELEASE}",
            )
        }
        item {
            CleanCard { kind, action ->
                onRequest(vm.bulkTargets(kind, action), action)
            }
        }
        item { FilterRow(current = filter, onChange = vm::setFilter) }
        item { TabRow3(current = tab, onChange = vm::setTab) }

        if (visible.isEmpty()) {
            item { Text(s.emptyList, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(visible, key = { it.packageName }) { entry ->
            AppRow(
                entry = entry,
                whitelisted = entry.packageName in settings.whitelist,
                onAction = { app, action -> onRequest(listOf(app), action) },
                onToggleWhitelist = { vm.toggleWhitelist(it.packageName) },
            )
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}
