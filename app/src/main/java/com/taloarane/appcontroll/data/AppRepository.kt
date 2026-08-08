package com.taloarane.appcontroll.data

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager

data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val sizeBytes: Long,
    val cacheBytes: Long,
    val running: Boolean,
    val risk: Risk,
)

enum class Risk { NORMAL, WARN, DANGER }

object AppRepository {

    fun loadApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val running = runningPackages(context)
        val critical = criticalPackages(context)
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return installed.mapNotNull { info ->
            if (info.packageName == context.packageName) return@mapNotNull null
            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            val (size, cache) = storageOf(context, info)
            AppEntry(
                packageName = info.packageName,
                label = runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(info.packageName),
                isSystem = isSystem,
                sizeBytes = size,
                cacheBytes = cache,
                running = info.packageName in running,
                risk = when {
                    info.packageName in critical -> Risk.DANGER
                    isSystem -> Risk.WARN
                    else -> Risk.NORMAL
                },
            )
        }.sortedBy { it.label.lowercase() }
    }

    private fun storageOf(context: Context, info: ApplicationInfo): Pair<Long, Long> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return 0L to 0L
        if (!hasUsageAccess(context)) return 0L to 0L
        return runCatching {
            val ssm = context.getSystemService(StorageStatsManager::class.java)
            val stats = ssm.queryStatsForPackage(
                StorageManager.UUID_DEFAULT,
                info.packageName,
                Process.myUserHandle(),
            )
            (stats.appBytes + stats.dataBytes) to stats.cacheBytes
        }.getOrDefault(0L to 0L)
    }

    fun runningPackages(context: Context): Set<String> {
        if (!hasUsageAccess(context)) return emptySet()
        return runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60 * 60 * 1000, now)
                .filter { it.totalTimeInForeground > 0 }
                .map { it.packageName }
                .toSet()
        }.getOrDefault(emptySet())
    }

    fun criticalPackages(context: Context): Set<String> {
        val pm = context.packageManager
        val result = mutableSetOf(
            "com.android.systemui",
            "android",
            "com.google.android.gms",
            "com.android.settings",
            "com.android.phone",
            "com.android.providers.settings",
            "com.android.providers.telephony",
        )
        runCatching {
            val tm = context.getSystemService(TelecomManager::class.java)
            tm?.defaultDialerPackage?.let { result += it }
        }
        runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                ?.substringBefore('/')?.let { if (it.isNotBlank()) result += it }
        }
        runCatching {
            val imm = context.getSystemService(InputMethodManager::class.java)
            imm?.enabledInputMethodList?.forEach { result += it.packageName }
        }
        runCatching {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_HOME)
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName?.let { result += it }
        }
        runCatching {
            val sms = Settings.Secure.getString(context.contentResolver, "sms_default_application")
            if (!sms.isNullOrBlank()) result += sms
        }
        return result
    }

    fun hasUsageAccess(context: Context): Boolean = runCatching {
        val aom = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            aom.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            aom.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "-"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var index = 0
        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024
            index++
        }
        return String.format("%.1f %s", value, units[index])
    }
}
