package com.taloarane.appcontroll.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taloarane.appcontroll.data.AppRepository
import com.taloarane.appcontroll.service.AppControllAccessibilityService
import com.taloarane.appcontroll.ui.LocalStrings
import com.taloarane.appcontroll.ui.components.SectionCard
import com.taloarane.appcontroll.ui.theme.NeonGreen

@Composable
fun PermissionGate(onContinue: () -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current

    val accessibilityOk = AppControllAccessibilityService.isEnabled(context)
    val usageOk = AppRepository.hasUsageAccess(context)
    val overlayOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    val storageOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(s.permTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(s.permBody, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))

        PermissionRow(s.permAccessibility, accessibilityOk) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        PermissionRow(s.permUsage, usageOk) {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        PermissionRow(s.permOverlay, overlayOk) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.packageName),
                    ),
                )
            }
        }
        PermissionRow(s.permStorage, storageOk) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + context.packageName),
                    ),
                )
            }
        }
        PermissionRow(s.permNotification, true) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        }

        Spacer(Modifier.height(18.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text(s.continueText) }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onGrant: () -> Unit) {
    val s = LocalStrings.current
    SectionCard(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (granted) s.granted else "-",
                    fontSize = 11.sp,
                    color = if (granted) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!granted) {
                Button(onClick = onGrant) { Text(s.grant, fontSize = 12.sp) }
            }
        }
    }
}
