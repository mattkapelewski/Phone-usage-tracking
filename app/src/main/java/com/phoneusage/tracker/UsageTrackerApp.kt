package com.phoneusage.tracker

import android.app.Application
import com.phoneusage.tracker.widget.WidgetUpdateWorker

class UsageTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WidgetUpdateWorker.schedulePeriodic(this)
    }
}
