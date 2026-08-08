package com.taloarane.appcontroll.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Automates the Settings > App info screen: Force stop, Clear cache and Uninstall.
 * Works without root or Shizuku by driving the system UI on the user's behalf.
 */
class AppControllAccessibilityService : AccessibilityService() {

    companion object {
        private val FORCE_STOP = listOf("paksa berhenti", "force stop", "hentikan paksa", "berhenti paksa")
        private val STORAGE = listOf("penyimpanan", "storage", "penyimpanan & cache", "storage & cache", "storage and cache")
        private val CLEAR_CACHE = listOf("hapus cache", "kosongkan cache", "clear cache", "bersihkan cache")
        private val UNINSTALL = listOf("uninstal", "uninstall", "hapus instalan", "copot pemasangan")
        private val CONFIRM = listOf("ok", "oke", "paksa berhenti", "force stop", "hapus", "uninstall", "uninstal", "ya", "yes", "setuju")

        fun isEnabled(context: Context): Boolean {
            val expected = ComponentName(context, AppControllAccessibilityService::class.java).flattenToString()
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }
    }

    private var lastActionAt = 0L
    private var stage = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val task = CleanQueue.currentTask ?: run { stage = 0; return }
        val now = System.currentTimeMillis()
        if (now - lastActionAt < 350) return
        val root = rootInActiveWindow ?: return

        val handled = when (task.action) {
            CleanAction.FORCE_STOP -> handleForceStop(root)
            CleanAction.CLEAR_CACHE -> handleClearCache(root)
            CleanAction.UNINSTALL -> handleUninstall(root)
        }
        if (handled) lastActionAt = now
    }

    private fun handleForceStop(root: AccessibilityNodeInfo): Boolean {
        if (clickConfirmDialog(root)) {
            finishTask(true)
            return true
        }
        val node = findClickable(root, FORCE_STOP) ?: return false
        return node.performClick()
    }

    private fun handleUninstall(root: AccessibilityNodeInfo): Boolean {
        if (clickConfirmDialog(root)) {
            finishTask(true)
            return true
        }
        val node = findClickable(root, UNINSTALL) ?: return false
        return node.performClick()
    }

    private fun handleClearCache(root: AccessibilityNodeInfo): Boolean {
        findClickable(root, CLEAR_CACHE)?.let {
            val ok = it.performClick()
            if (ok) finishTask(true)
            return ok
        }
        if (stage == 0) {
            val storage = findClickable(root, STORAGE) ?: return false
            stage = 1
            return storage.performClick()
        }
        return false
    }

    private fun clickConfirmDialog(root: AccessibilityNodeInfo): Boolean {
        val buttons = listOfNotNull(
            root.findAccessibilityNodeInfosByViewId("android:id/button1")?.firstOrNull(),
            root.findAccessibilityNodeInfosByViewId("com.android.settings:id/button1")?.firstOrNull(),
        )
        val dialogButton = buttons.firstOrNull { it.isVisibleToUser && it.isEnabled }
        if (dialogButton != null && isDialogVisible(root)) {
            return dialogButton.performClick()
        }
        return false
    }

    private fun isDialogVisible(root: AccessibilityNodeInfo): Boolean {
        val alert = root.findAccessibilityNodeInfosByViewId("android:id/alertTitle")?.firstOrNull()
        val message = root.findAccessibilityNodeInfosByViewId("android:id/message")?.firstOrNull()
        return alert != null || message != null
    }

    private fun AccessibilityNodeInfo.performClick(): Boolean {
        var node: AccessibilityNodeInfo? = this
        while (node != null) {
            if (node.isClickable && node.isEnabled) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            node = node.parent
        }
        return false
    }

    private fun findClickable(root: AccessibilityNodeInfo, keywords: List<String>): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var fallback: AccessibilityNodeInfo? = null
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = buildString {
                append(node.text?.toString()?.lowercase().orEmpty())
                append(' ')
                append(node.contentDescription?.toString()?.lowercase().orEmpty())
            }.trim()
            val viewId = node.viewIdResourceName?.lowercase().orEmpty()
            if (keywords.any { text == it }) return node
            if (keywords.any { text.isNotEmpty() && text.contains(it) }) fallback = fallback ?: node
            if (viewId.isNotEmpty() && keywords.any { viewId.contains(it.replace(" ", "_")) }) {
                fallback = fallback ?: node
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return fallback
    }

    private fun finishTask(success: Boolean) {
        stage = 0
        CleanQueue.completeCurrent(success)
    }

    /** Swipes the task out of Recents and returns home. */
    fun swipeOutRecents() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
        Thread.sleep(600)
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    @Suppress("unused")
    private fun matches(text: CharSequence?, keywords: List<String>): Boolean =
        !TextUtils.isEmpty(text) && keywords.any { text!!.toString().lowercase().contains(it) }
}
