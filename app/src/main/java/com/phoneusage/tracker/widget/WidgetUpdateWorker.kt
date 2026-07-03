package com.phoneusage.tracker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Periodically refreshes the widget in the background. 15 minutes is the shortest interval
 * WorkManager allows for periodic work; the widget also refreshes immediately whenever the
 * app is opened or the selected-apps list changes.
 */
class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(applicationContext, UsageWidgetProvider::class.java)
        )
        if (appWidgetIds.isNotEmpty()) {
            val text = UsageWidgetProvider.buildTotalText(applicationContext)
            for (id in appWidgetIds) {
                UsageWidgetProvider.updateAppWidget(applicationContext, appWidgetManager, id, text)
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "widget_periodic_refresh"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
