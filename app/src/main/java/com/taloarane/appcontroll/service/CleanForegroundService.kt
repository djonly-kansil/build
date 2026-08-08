package com.taloarane.appcontroll.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.taloarane.appcontroll.MainActivity
import com.taloarane.appcontroll.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class CleanForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.taloarane.appcontroll.START_CLEAN"
        const val ACTION_CANCEL = "com.taloarane.appcontroll.CANCEL_CLEAN"
        private const val CHANNEL_ID = "clean_progress"
        private const val NOTIFICATION_ID = 4201

        fun start(context: Context) {
            val intent = Intent(context, CleanForegroundService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var overlay: View? = null
    private var overlayText: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            CleanQueue.cancel()
            stopSelf()
            return START_NOT_STICKY
        }
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Menyiapkan…", 0, 0))
        scope.launch { runQueue() }
        return START_NOT_STICKY
    }

    private suspend fun runQueue() {
        val tasks = CleanQueue.pending
        withContext(Dispatchers.Main) { showOverlay() }
        tasks.forEachIndexed { index, task ->
            if (CleanQueue.cancelled) return@forEachIndexed
            withContext(Dispatchers.Main) {
                updateOverlay("Sedang membersihkan… ${index + 1}/${tasks.size}\n${task.label}")
            }
            notify(task.label, index + 1, tasks.size)
            val waiter = CleanQueue.beginTask(task, index)
            launchSettings(task)
            val result = withTimeoutOrNull(12_000) { waiter.await() }
            if (result == null) CleanQueue.completeCurrent(false)
            delay(500)
        }
        CleanQueue.finish()
        withContext(Dispatchers.Main) {
            hideOverlay()
            startActivity(
                Intent(this@CleanForegroundService, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
        }
        stopSelf()
    }

    private fun launchSettings(task: CleanTask) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", task.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        runCatching { startActivity(intent) }.onFailure { CleanQueue.completeCurrent(false) }
    }

    private fun showOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return
        val wm = getSystemService(WindowManager::class.java) ?: return
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F2070B18"))
        }
        val text = TextView(this).apply {
            setTextColor(Color.parseColor("#E6ECFF"))
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            background = GradientDrawable().apply {
                cornerRadius = 32f
                setColor(Color.parseColor("#111830"))
                setStroke(3, Color.parseColor("#8B5CF6"))
            }
            text = "Sedang membersihkan…"
        }
        container.addView(text)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            android.graphics.PixelFormat.TRANSLUCENT,
        )
        runCatching { wm.addView(container, params) }.onSuccess {
            overlay = container
            overlayText = text
        }
    }

    private fun updateOverlay(message: String) {
        overlayText?.text = message
    }

    private fun hideOverlay() {
        val view = overlay ?: return
        runCatching { getSystemService(WindowManager::class.java)?.removeView(view) }
        overlay = null
        overlayText = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Pembersihan", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(channel)
    }

    private fun notify(label: String, current: Int, total: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(label, current, total))
    }

    private fun buildNotification(label: String, current: Int, total: Int): Notification {
        val cancelIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CleanForegroundService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("App Controll — membersihkan")
            .setContentText(if (total > 0) "$current/$total · $label" else label)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(null as android.graphics.drawable.Icon?, "Batal", cancelIntent).build(),
            )
            .build()
    }

    override fun onDestroy() {
        hideOverlay()
        CleanQueue.finish()
        scope.cancel()
        super.onDestroy()
    }
}
