package com.taloarane.appcontroll

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class AppControllApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "App Controll",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "appcontroll_progress"
    }
}
