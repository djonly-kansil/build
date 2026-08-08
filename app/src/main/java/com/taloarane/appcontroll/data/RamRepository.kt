package com.taloarane.appcontroll.data

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.system.Os
import android.system.OsConstants
import java.io.File

data class RamStats(
    val totalKb: Long = 0,
    val freeKb: Long = 0,
    val cacheKb: Long = 0,
    val systemKb: Long = 0,
    val userKb: Long = 0,
    val arch: String = "-",
    val ramType: String = "-",
    val pageSizeKb: Long = 4,
    val mte: Boolean = false,
) {
    val usedKb: Long get() = (totalKb - freeKb).coerceAtLeast(0)
    val usedPercent: Int get() = if (totalKb <= 0) 0 else ((usedKb * 100) / totalKb).toInt()
    fun percentOf(kb: Long): Int = if (totalKb <= 0) 0 else ((kb * 100) / totalKb).toInt()
}

object RamRepository {

    fun read(context: Context): RamStats {
        val meminfo = parseMeminfo()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }

        val total = meminfo["MemTotal"] ?: (mi.totalMem / 1024)
        val free = meminfo["MemAvailable"] ?: (mi.availMem / 1024)
        val cache = (meminfo["Cached"] ?: 0L) + (meminfo["SReclaimable"] ?: 0L) + (meminfo["Buffers"] ?: 0L)
        val system = (meminfo["Shmem"] ?: 0L) + (meminfo["SUnreclaim"] ?: 0L) +
            (meminfo["KernelStack"] ?: 0L) + (meminfo["PageTables"] ?: 0L) +
            (meminfo["VmallocUsed"] ?: 0L)
        val user = (total - free - cache - system).coerceAtLeast(0L)

        return RamStats(
            totalKb = total,
            freeKb = free,
            cacheKb = cache.coerceAtMost(total),
            systemKb = system.coerceAtMost(total),
            userKb = user,
            arch = if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) "64-bit" else "32-bit",
            ramType = detectRamType(),
            pageSizeKb = runCatching { Os.sysconf(OsConstants._SC_PAGESIZE) / 1024 }.getOrDefault(4L),
            mte = detectMte(),
        )
    }

    private fun parseMeminfo(): Map<String, Long> = runCatching {
        File("/proc/meminfo").readLines().mapNotNull { line ->
            val parts = line.split(":")
            if (parts.size < 2) return@mapNotNull null
            val value = parts[1].trim().removeSuffix(" kB").trim().toLongOrNull() ?: return@mapNotNull null
            parts[0].trim() to value
        }.toMap()
    }.getOrDefault(emptyMap())

    private fun detectRamType(): String = runCatching {
        val cpu = File("/proc/cpuinfo").readText().lowercase()
        when {
            cpu.contains("lpddr5") -> "LPDDR5"
            cpu.contains("lpddr4x") -> "LPDDR4X"
            cpu.contains("lpddr4") -> "LPDDR4"
            else -> if (Build.VERSION.SDK_INT >= 31) "LPDDR4X" else "LPDDR3"
        }
    }.getOrDefault("-")

    private fun detectMte(): Boolean = runCatching {
        File("/proc/cpuinfo").readText().lowercase().contains(" mte")
    }.getOrDefault(false)

    fun formatGb(kb: Long): String {
        val gb = kb / 1024.0 / 1024.0
        return String.format("%.1f", gb)
    }
}
