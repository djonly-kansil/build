package com.taloarane.appcontroll

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taloarane.appcontroll.core.AppItem
import com.taloarane.appcontroll.core.AppRepo
import com.taloarane.appcontroll.core.LocalL
import com.taloarane.appcontroll.core.Permissions
import com.taloarane.appcontroll.core.Prefs
import com.taloarane.appcontroll.core.RamStats
import com.taloarane.appcontroll.core.StorageScan
import com.taloarane.appcontroll.service.AppControllAccessibilityService
import com.taloarane.appcontroll.service.CleanAction
import com.taloarane.appcontroll.service.CleanForegroundService
import com.taloarane.appcontroll.service.CleanTask
import com.taloarane.appcontroll.ui.AcCard
import com.taloarane.appcontroll.ui.AppControllTheme
import com.taloarane.appcontroll.ui.HomeScreen
import com.taloarane.appcontroll.ui.InfoRamScreen
import com.taloarane.appcontroll.ui.LocalExtra
import com.taloarane.appcontroll.ui.NeonCyan
import com.taloarane.appcontroll.ui.NeonPurple
import com.taloarane.appcontroll.ui.SettingsScreen
import com.taloarane.appcontroll.ui.StorageScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class Screen { HOME, RAM, ROM, SD, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 42)
            }
        }
        setContent { AppRoot() }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val prefs = remember { Prefs.get(context) }
    val activity = context as? ComponentActivity

    DisposableEffect(prefs.keepScreenOn) {
        val window = activity?.window
        if (prefs.keepScreenOn) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    AppControllTheme(prefs.themeMode) {
        CompositionLocalProvider(LocalL provides prefs.strings) {
            val l = LocalL.current
            val extra = LocalExtra.current
            var screen by remember { mutableStateOf(Screen.HOME) }
            var stats by remember { mutableStateOf(AppRepo.ramStats(context)) }
            val history = remember { mutableStateListOf<Float>() }
            val device = remember { AppRepo.deviceInfo(context) }
            var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
            var reload by remember { mutableStateOf(0) }

            LaunchedEffect(Unit) {
                while (true) {
                    stats = AppRepo.ramStats(context)
                    val pct = stats.used.toFloat() / stats.total.coerceAtLeast(1)
                    history.add(pct)
                    if (history.size > 60) history.removeAt(0)
                    delay(1000)
                }
            }

            LaunchedEffect(reload) {
                apps = withContext(Dispatchers.IO) { AppRepo.loadApps(context) }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                // Header
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (screen != Screen.HOME) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { screen = Screen.HOME }
                        )
                        Spacer(Modifier.size(10.dp))
                    }
                    Text(
                        l.appName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = l.settings,
                        tint = NeonPurple,
                        modifier = Modifier
                            .size(26.dp)
                            .clickable { screen = Screen.SETTINGS }
                    )
                }

                Box(Modifier.weight(1f)) {
                    when (screen) {
                        Screen.HOME -> HomeScreen(stats, history.toList(), device, apps, prefs) { reload++ }
                        Screen.SETTINGS -> SettingsScreen(prefs)
                        Screen.RAM -> InfoRamScreen(stats, apps) { app ->
                            if (Permissions.isAccessibilityEnabled(context)) {
                                CleanForegroundService.start(context)
                                AppControllAccessibilityService.enqueue(
                                    context,
                                    listOf(CleanTask(app.packageName, app.label, listOf(CleanAction.FORCE_STOP)))
                                )
                            } else {
                                Permissions.openAccessibility(context)
                            }
                        }
                        Screen.ROM -> StorageScreen(l.internalStorage, StorageScan.internal(), l.sdCardMissing)
                        Screen.SD -> StorageScreen(l.sdCard, StorageScan.sdCard(context), l.sdCardMissing)
                    }
                }

                // Bottom navigation
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(extra.cardBg)
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NavItem(l.infoRam, screen == Screen.RAM, Modifier.weight(1f)) {
                        screen = if (screen == Screen.RAM) Screen.HOME else Screen.RAM
                    }
                    NavItem(l.infoRom, screen == Screen.ROM, Modifier.weight(1f)) {
                        screen = if (screen == Screen.ROM) Screen.HOME else Screen.ROM
                    }
                    NavItem(l.infoSd, screen == Screen.SD, Modifier.weight(1f)) {
                        screen = if (screen == Screen.SD) Screen.HOME else Screen.SD
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItem(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val extra = LocalExtra.current
    AcCard(
        modifier = modifier.clickable { onClick() },
        accent = if (selected) NeonPurple else null,
        background = if (selected) NeonPurple.copy(alpha = 0.16f) else extra.innerBg
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) NeonPurple else extra.subtle
            )
        }
    }
}

@Suppress("unused")
private fun unusedRamStats(s: RamStats) = s
