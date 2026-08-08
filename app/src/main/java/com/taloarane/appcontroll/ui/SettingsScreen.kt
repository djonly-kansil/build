package com.taloarane.appcontroll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taloarane.appcontroll.core.AppRepo
import com.taloarane.appcontroll.core.LocalL
import com.taloarane.appcontroll.core.Permissions
import com.taloarane.appcontroll.core.Prefs
import com.taloarane.appcontroll.core.ThemeMode

@Composable
fun SettingsScreen(prefs: Prefs) {
    val l = LocalL.current
    val extra = LocalExtra.current
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Section(l.language) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChipAc("Indonesia", prefs.language == "id", Modifier.weight(1f)) { prefs.updateLanguage("id") }
                FilterChipAc("English", prefs.language == "en", Modifier.weight(1f)) { prefs.updateLanguage("en") }
            }
        }
        Section(l.theme) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChipAc(l.themeDark, prefs.themeMode == ThemeMode.DARK, Modifier.weight(1f)) {
                    prefs.updateTheme(ThemeMode.DARK)
                }
                FilterChipAc(l.themeLight, prefs.themeMode == ThemeMode.LIGHT, Modifier.weight(1f)) {
                    prefs.updateTheme(ThemeMode.LIGHT)
                }
                FilterChipAc(l.themeSystem, prefs.themeMode == ThemeMode.SYSTEM, Modifier.weight(1f)) {
                    prefs.updateTheme(ThemeMode.SYSTEM)
                }
            }
        }
        Section(l.keepScreenOn) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (prefs.keepScreenOn) l.on else l.off,
                    color = extra.subtle,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = prefs.keepScreenOn, onCheckedChange = { prefs.updateKeepScreenOn(it) })
            }
        }
        Section(l.permissionsTitle) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PermRow(l.permAccessibility, Permissions.isAccessibilityEnabled(context)) {
                    Permissions.openAccessibility(context)
                }
                PermRow(l.permUsage, AppRepo.hasUsageAccess(context)) { Permissions.openUsageAccess(context) }
                PermRow(l.permStorage, AppRepo.hasStorageAccess(context)) { Permissions.openStorageAccess(context) }
                PermRow(l.permOverlay, Permissions.hasOverlay(context)) { Permissions.openOverlay(context) }
            }
        }
        Section(l.about) {
            Column {
                Text(
                    "App Controll · 1.0.0",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text("com.taloarane.appcontroll", fontSize = 11.sp, color = extra.subtle)
                Spacer(Modifier.height(8.dp))
                Text(l.aboutText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(70.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    AcCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun PermRow(label: String, granted: Boolean, onClick: () -> Unit) {
    val l = LocalL.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        ActionChip(
            if (granted) l.granted else l.grant,
            if (granted) NeonGreen else NeonRed,
            Modifier.width(96.dp),
            filled = granted,
            onClick = onClick
        )
    }
}



