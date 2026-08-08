package com.taloarane.appcontroll.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.taloarane.appcontroll.AppControllApp
import com.taloarane.appcontroll.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Keeps a progress notification alive while the accessibility queue runs. */
class CleanForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            CleanEngine.cancelRequested = true
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, build("...", 0, 0))
        scope.launch {
            CleanEngine.state.collectLatest { s ->
                if (!s.running && s.total > 0) {
                    stopSelf()
                } else {
                    notify(build(s.current, s.index, s.total))
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun notify(n: Notification) {
        runCatching {
            (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                .notify(NOTIF_ID, n)
        }
    }

    private fun build(label: String, index: Int, total: Int): Notification {
        val cancel = android.app.PendingIntent.getService(
            this, 1,
            Intent(this, CleanForegroundService::class.java).setAction(ACTION_CANCEL),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 23) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
        )
        return NotificationCompat.Builder(this, AppControllApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_clean)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(if (total > 0) "$index/$total · $label" else label)
            .setProgress(total.coerceAtLeast(1), index, total == 0)
            .setOngoing(true)
            .addAction(0, "Batal", cancel)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIF_ID = 4211
        const val ACTION_CANCEL = "com.taloarane.appcontroll.CANCEL"

        fun start(context: Context) {
            val i = Intent(context, CleanForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }
    }
}
