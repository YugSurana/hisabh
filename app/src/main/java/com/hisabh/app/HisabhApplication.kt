package com.hisabh.app

import android.app.Application
import com.hisabh.app.data.local.HisabhDatabase
import com.hisabh.app.domain.helper.NotificationHelper

class HisabhApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Notification Channels for Android 8+ (Oreo)
        NotificationHelper.createNotificationChannel(this)
        // Eagerly initialize Room Database instance
        HisabhDatabase.getInstance(this)
    }
}

