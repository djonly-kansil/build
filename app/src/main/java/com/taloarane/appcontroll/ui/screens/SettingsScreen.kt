package com.taloarane.appcontroll.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taloarane.appcontroll.data.Settings
import com.taloarane.appcontroll.data.ThemeMode
import com.taloarane.appcontroll.ui.LocalStrings
import com.taloarane.appcontroll.ui.components.SectionCard
import com.taloarane.appcontroll.ui.theme.NeonCyan
import com.taloarane.appcontroll.ui.theme.NeonPurple

@Composable
fun SettingsScreen(
    settings: Settings,
    onLanguage: (String) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onPermissions: () -> Unit,
) {
    val s = LocalStrings.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(s.settings, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

        SectionCard(accent = NeonPurple) {
            Text(s.language, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionPill("Indonesia", if (settings.language == "id") NeonPurple else MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f)) { onLanguage("id") }
                ActionPill("English", if (settings.language == "en") NeonPurple else MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f)) { onLanguage("en") }
            }
        }

        SectionCard(accent = NeonCyan) {
            Text(s.theme, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionPill(s.themeDark, if (settings.theme == ThemeMode.DARK) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f)) { onTheme(ThemeMode.DARK) }
                ActionPill(s.themeLight, if (settings.theme == ThemeMode.LIGHT) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f)) { onTheme(ThemeMode.LIGHT) }
                ActionPill(s.themeSystem, if (settings.theme == ThemeMode.SYSTEM) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f)) { onTheme(ThemeMode.SYSTEM) }
            }
        }

        SectionCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(s.keepScreenOn, fontSize = 14.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Switch(checked = settings.keepScreenOn, onCheckedChange = onKeepScreenOn)
            }
        }

        SectionCard {
            Text(s.permTitle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            ActionPill(s.grant, NeonPurple) { onPermissions() }
        }

        SectionCard {
            Text(s.about, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                "App Controll 1.0 · com.taloarane.appcontroll\nManajer RAM & aplikasi tanpa root, otomatis lewat layanan aksesibilitas.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
