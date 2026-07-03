package com.phoneusage.tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.phoneusage.tracker.R
import com.phoneusage.tracker.data.AppPrefsRepository
import com.phoneusage.tracker.data.UsageStatsRepository
import com.phoneusage.tracker.ui.MainActivity
import com.phoneusage.tracker.util.formatDurationCompact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 1x1 home-screen widget showing today's total phone usage for the apps the user selected. */
class UsageWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, UsageWidgetProvider::class.java)
            )
            refreshWidgets(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun refreshWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        scope.launch {
            try {
                val text = buildTotalText(context)
                for (id in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, id, text)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.phoneusage.tracker.action.REFRESH_WIDGET"

        /** Call after the app changes something that affects the total (e.g. app selection). */
        fun requestUpdate(context: Context) {
            val intent = Intent(context, UsageWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            context.sendBroadcast(intent)
        }

        suspend fun buildTotalText(context: Context): String {
            val usageRepo = UsageStatsRepository(context)
            if (!usageRepo.hasUsageAccess()) {
                return context.getString(R.string.widget_permission_needed)
            }
            val excluded = AppPrefsRepository(context).getExcludedPackagesOnce()
            val totalMillis = usageRepo.getTotalUsageMillisToday(excluded)
            return formatDurationCompact(totalMillis)
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            text: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_usage)
            views.setTextViewText(R.id.widget_total_time, text)

            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
