package com.taloarane.appcontroll.core

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.util.Locale

data class AppItem(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val uid: Int,
    val targetSdk: Int,
    val versionName: String,
    val apkSize: Long,
    val firstInstall: Long,
    val lastUsed: Long,
    val active: Boolean,
)

data class RamStats(
    val total: Long,
    val available: Long,
    val used: Long,
    val userApps: Long,
    val cached: Long,
    val systemMem: Long,
    val free: Long,
    val threshold: Long,
    val lowMemory: Boolean,
)

data class DeviceInfo(
    val arch: String,
    val ramType: String,
    val pageSize: String,
    val security: String,
    val sdk: Int,
    val minSdk: Int,
    val release: String,
)

object AppRepo {

    val CRITICAL = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.gms",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.providers.settings",
        "com.android.providers.telephony",
        "com.android.providers.contacts",
        "com.android.providers.media",
        "com.android.bluetooth",
        "com.android.nfc",
        "android",
    )

    fun isCritical(context: Context, pkg: String): Boolean {
        if (pkg in CRITICAL) return true
        if (pkg == defaultLauncher(context)) return true
        if (pkg == currentIme(context)) return true
        if (pkg.startsWith("com.android.inputmethod")) return true
        if (pkg.endsWith(".launcher") || pkg.contains("launcher")) return true
        return false
    }

    fun defaultLauncher(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
    }

    fun currentIme(context: Context): String? = runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
    }.getOrNull()

    fun loadApps(context: Context): List<AppItem> {
        val pm = context.packageManager
        val usage = usageMap(context)
        val now = System.currentTimeMillis()
        val flags = PackageManager.GET_META_DATA
        val list = runCatching { pm.getInstalledApplications(flags) }.getOrDefault(emptyList())
        return list.mapNotNull { ai ->
            runCatching {
                val pkgInfo = pm.getPackageInfo(ai.packageName, 0)
                val apk = runCatching { File(ai.sourceDir).length() }.getOrDefault(0L)
                val last = usage[ai.packageName] ?: 0L
                AppItem(
                    packageName = ai.packageName,
                    label = pm.getApplicationLabel(ai).toString(),
                    isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                        (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                    uid = ai.uid,
                    targetSdk = ai.targetSdkVersion,
                    versionName = pkgInfo.versionName ?: "-",
                    apkSize = apk,
                    firstInstall = pkgInfo.firstInstallTime,
                    lastUsed = last,
                    active = last > 0 && now - last < 30 * 60_000L,
                )
            }.getOrNull()
        }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun usageMap(context: Context): Map<String, Long> {
        if (!hasUsageAccess(context)) return emptyMap()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 30L * 24 * 60 * 60 * 1000
        val stats = runCatching {
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end)
        }.getOrNull() ?: return emptyMap()
        val map = HashMap<String, Long>()
        stats.forEach { s ->
            val prev = map[s.packageName] ?: 0L
            if (s.lastTimeUsed > prev) map[s.packageName] = s.lastTimeUsed
        }
        return map
    }

    fun hasUsageAccess(context: Context): Boolean {
        val aom = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            aom.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            aom.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasStorageAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED

    fun ramStats(context: Context): RamStats {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val meminfo = readMemInfo()
        val total = if (mi.totalMem > 0) mi.totalMem else (meminfo["MemTotal"] ?: 0L)
        val avail = mi.availMem
        val cached = ((meminfo["Cached"] ?: 0L) + (meminfo["Buffers"] ?: 0L) + (meminfo["SReclaimable"] ?: 0L))
        val memFree = meminfo["MemFree"] ?: avail
        val used = (total - avail).coerceAtLeast(0)
        val systemMem = ((meminfo["Slab"] ?: 0L) + (meminfo["KernelStack"] ?: 0L) + (meminfo["PageTables"] ?: 0L))
        val userApps = (used - systemMem).coerceAtLeast(0)
        return RamStats(
            total = total,
            available = avail,
            used = used,
            userApps = userApps,
            cached = cached.coerceAtMost(total),
            systemMem = systemMem,
            free = memFree,
            threshold = mi.threshold,
            lowMemory = mi.lowMemory,
        )
    }

    private fun readMemInfo(): Map<String, Long> = runCatching {
        File("/proc/meminfo").readLines().mapNotNull { line ->
            val parts = line.split(":")
            if (parts.size < 2) return@mapNotNull null
            val kb = parts[1].trim().removeSuffix(" kB").trim().toLongOrNull() ?: return@mapNotNull null
            parts[0].trim() to kb * 1024
        }.toMap()
    }.getOrDefault(emptyMap())

    fun deviceInfo(context: Context): DeviceInfo {
        val arch = if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) "64-bit" else "32-bit"
        val pageSize = runCatching {
            val s = android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE)
            "${s / 1024} KB"
        }.getOrDefault("4 KB")
        val ramType = when {
            Build.VERSION.SDK_INT >= 33 -> "LPDDR5"
            Build.VERSION.SDK_INT >= 29 -> "LPDDR4X"
            else -> "LPDDR3"
        }
        val security = if (Build.VERSION.SDK_INT >= 34) "Memory Tagging (MTE)" else "ASLR"
        return DeviceInfo(arch, ramType, pageSize, security, Build.VERSION.SDK_INT, 24, Build.VERSION.RELEASE)
    }

    fun icon(context: Context, pkg: String): ImageBitmap? = runCatching {
        drawableToBitmap(context.packageManager.getApplicationIcon(pkg)).asImageBitmap()
    }.getOrNull()

    private fun drawableToBitmap(d: Drawable): Bitmap {
        if (d is BitmapDrawable && d.bitmap != null) return d.bitmap
        val w = d.intrinsicWidth.takeIf { it > 0 } ?: 96
        val h = d.intrinsicHeight.takeIf { it > 0 } ?: 96
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        d.setBounds(0, 0, c.width, c.height)
        d.draw(c)
        return bmp
    }

    fun formatBytes(bytes: Long, decimals: Int = 1): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var v = bytes.toDouble()
        var i = 0
        while (v >= 1024 && i < units.lastIndex) {
            v /= 1024; i++
        }
        return String.format(Locale.getDefault(), "%.${if (i <= 1) 0 else decimals}f %s", v, units[i])
    }
}
