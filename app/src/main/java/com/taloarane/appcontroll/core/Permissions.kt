package com.taloarane.appcontroll.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import com.taloarane.appcontroll.service.AppControllAccessibilityService

object Permissions {

    fun isAccessibilityEnabled(context: Context): Boolean {
        if (AppControllAccessibilityService.instance != null) return true
        val expected = "${context.packageName}/${AppControllAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    fun openAccessibility(context: Context) =
        open(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun openUsageAccess(context: Context) =
        open(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

    fun openStorageAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .setData(Uri.parse("package:${context.packageName}"))
            if (i.resolveActivity(context.packageManager) != null) {
                open(context, i)
            } else {
                open(context, Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            openAppDetails(context, context.packageName)
        }
    }

    fun hasOverlay(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun openOverlay(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            open(
                context,
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
    }

    fun openAppDetails(context: Context, pkg: String) = open(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
    )

    fun uninstall(context: Context, pkg: String) = open(
        context,
        Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
    )

    private fun open(context: Context, intent: Intent) {
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
