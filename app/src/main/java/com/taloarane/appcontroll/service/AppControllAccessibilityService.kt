package com.taloarane.appcontroll.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Drives "force stop", "clear cache" and "uninstall" by opening the system
 * App Info screen and tapping the buttons on behalf of the user. This is the
 * only way to do it without root or Shizuku.
 */
class AppControllAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* polling based */ }

    fun runTasks(tasks: List<CleanTask>) {
        if (CleanEngine.state.value.running || tasks.isEmpty()) return
        CleanEngine.cancelRequested = false
        scope.launch {
            Overlay.show(this@AppControllAccessibilityService)
            CleanEngine.state.value = CleanState(running = true, total = tasks.size)
            var ok = 0
            var bad = 0
            tasks.forEachIndexed { i, task ->
                if (CleanEngine.cancelRequested) return@forEachIndexed
                CleanEngine.state.value = CleanEngine.state.value.copy(
                    index = i + 1, current = task.label
                )
                Overlay.update(task.label, i + 1, tasks.size)
                val success = runCatching { process(task) }.getOrDefault(false)
                if (success) ok++ else bad++
                CleanEngine.state.value = CleanEngine.state.value.copy(succeeded = ok, failed = bad)
            }
            performGlobalAction(GLOBAL_ACTION_BACK)
            delay(200)
            performGlobalAction(GLOBAL_ACTION_HOME)
            Overlay.hide()
            CleanEngine.state.value = CleanEngine.state.value.copy(
                running = false,
                finishedMessage = "$ok OK · $bad ✕"
            )
            returnToApp()
        }
    }

    fun clearRecents(exceptSelf: Boolean = true) {
        scope.launch {
            Overlay.show(this@AppControllAccessibilityService)
            Overlay.update("Recents", 0, 0)
            performGlobalAction(GLOBAL_ACTION_RECENTS)
            delay(900)
            repeat(12) {
                if (CleanEngine.cancelRequested) return@repeat
                swipeAwayTopCard()
                delay(450)
            }
            performGlobalAction(GLOBAL_ACTION_HOME)
            Overlay.hide()
            if (exceptSelf) returnToApp()
        }
    }

    private suspend fun swipeAwayTopCard() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels / 2f
        val path = Path().apply {
            moveTo(x, metrics.heightPixels * 0.55f)
            lineTo(x, metrics.heightPixels * 0.05f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 220))
            .build()
        dispatchGesture(gesture, null, null)
        delay(120)
    }

    private suspend fun process(task: CleanTask): Boolean {
        var allOk = true
        for (action in task.actions) {
            val ok = when (action) {
                CleanAction.CLEAR_CACHE -> doClearCache(task.packageName)
                CleanAction.FORCE_STOP -> doForceStop(task.packageName)
                CleanAction.UNINSTALL -> doUninstall(task.packageName)
            }
            if (!ok) allOk = false
        }
        return allOk
    }

    private suspend fun openDetails(pkg: String) {
        runCatching {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
        delay(950)
    }

    private suspend fun doForceStop(pkg: String): Boolean {
        openDetails(pkg)
        val node = awaitNode(FORCE_STOP) ?: return false
        if (!clickNode(node)) return false
        delay(600)
        confirm()
        delay(500)
        return true
    }

    private suspend fun doClearCache(pkg: String): Boolean {
        openDetails(pkg)
        // Newer Android hides cache behind a "Storage" entry.
        val direct = findNode(CLEAR_CACHE)
        if (direct != null && clickNode(direct)) {
            delay(500); confirm(); return true
        }
        val storage = awaitNode(STORAGE, tries = 8) ?: return false
        if (!clickNode(storage)) return false
        delay(800)
        val cache = awaitNode(CLEAR_CACHE, tries = 10) ?: return false
        val clicked = clickNode(cache)
        delay(500)
        confirm()
        delay(300)
        performGlobalAction(GLOBAL_ACTION_BACK)
        delay(300)
        return clicked
    }

    private suspend fun doUninstall(pkg: String): Boolean {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        delay(1200)
        return true
    }

    private suspend fun confirm() {
        val node = findNode(CONFIRM) ?: return
        clickNode(node)
    }

    private suspend fun awaitNode(texts: List<String>, tries: Int = 12): AccessibilityNodeInfo? {
        repeat(tries) {
            if (CleanEngine.cancelRequested) return null
            findNode(texts)?.let { return it }
            delay(350)
        }
        return null
    }

    private fun findNode(texts: List<String>): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        for (t in texts) {
            val matches = runCatching { root.findAccessibilityNodeInfosByText(t) }.getOrNull()
            val hit = matches?.firstOrNull { node ->
                val label = (node.text ?: node.contentDescription ?: "").toString()
                label.equals(t, true) || label.contains(t, true)
            }
            if (hit != null) return hit
        }
        // Fallback: resource ids used by AOSP Settings.
        for (id in RES_IDS) {
            val byId = runCatching { root.findAccessibilityNodeInfosByViewId(id) }.getOrNull()
            byId?.firstOrNull()?.let { return it }
        }
        return null
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 6) {
            if (current.isClickable && current.isEnabled) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
            depth++
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun returnToApp() {
        runCatching {
            val i = packageManager.getLaunchIntentForPackage(packageName)
            i?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            if (i != null) startActivity(i)
        }
    }

    companion object {
        @Volatile
        var instance: AppControllAccessibilityService? = null

        private val FORCE_STOP = listOf(
            "Paksa berhenti", "Paksa henti", "Hentikan paksa", "Force stop", "Force Stop", "FORCE STOP"
        )
        private val CLEAR_CACHE = listOf(
            "Hapus cache", "Bersihkan cache", "Kosongkan cache", "Clear cache", "Clear Cache", "CLEAR CACHE"
        )
        private val STORAGE = listOf(
            "Penyimpanan dan cache", "Penyimpanan", "Storage and cache", "Storage & cache", "Storage"
        )
        private val CONFIRM = listOf("OK", "Oke", "Ok", "Paksa berhenti", "Force stop", "Hapus", "Delete", "Setuju")
        private val RES_IDS = listOf(
            "com.android.settings:id/force_stop_button",
            "com.android.settings:id/right_button",
            "android:id/button1"
        )

        fun enqueue(context: Context, tasks: List<CleanTask>): Boolean {
            val svc = instance ?: return false
            svc.runTasks(tasks)
            return true
        }
    }
}
