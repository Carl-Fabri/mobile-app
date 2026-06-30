package com.example.utpstudywork

import android.app.Application
import com.example.utpstudywork.core.NotificationChannels

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
    }
}
