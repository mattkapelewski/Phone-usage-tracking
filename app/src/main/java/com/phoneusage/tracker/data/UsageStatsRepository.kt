package com.phoneusage.tracker.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Reads per-app foreground time from [UsageStatsManager] and the list of user-facing
 * (launchable) apps from [PackageManager].
 */
class UsageStatsRepository(context: Context) {

    private val appContext = context.applicationContext

    private val usageStatsManager: UsageStatsManager
        get() = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /** Whether the user has granted the special "Usage access" permission. */
    fun hasUsageAccess(): Boolean {
        val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            appContext.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Foreground time in milliseconds per package, from local midnight until now. */
    fun getUsageTodayByPackage(): Map<String, Long> {
        val startOfDay = startOfTodayMillis()
        val now = System.currentTimeMillis()
        return computeForegroundDurations(startOfDay, now)
    }

    /** Total foreground time today across all packages not present in [excludedPackages]. */
    fun getTotalUsageMillisToday(excludedPackages: Set<String>): Long {
        return getUsageTodayByPackage()
            .filterKeys { it !in excludedPackages }
            .values
            .sum()
    }

    private fun computeForegroundDurations(startTime: Long, endTime: Long): Map<String, Long> {
        // Query from further back than startTime so a session that was already in the
        // foreground at startTime (e.g. an app left open across local midnight) is seen
        // and its start time can be clamped to startTime, instead of being dropped because
        // its MOVE_TO_FOREGROUND event falls outside the query window.
        val lookbackStart = startTime - TimeUnit.HOURS.toMillis(24)
        val events = usageStatsManager.queryEvents(lookbackStart, endTime)
        val totals = mutableMapOf<String, Long>()
        val foregroundStarts = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    foregroundStarts[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val start = foregroundStarts.remove(event.packageName)
                    if (start != null) {
                        val clampedStart = maxOf(start, startTime)
                        if (event.timeStamp > clampedStart) {
                            totals[event.packageName] =
                                (totals[event.packageName] ?: 0L) + (event.timeStamp - clampedStart)
                        }
                    }
                }
            }
        }

        // Anything still in the foreground when we queried counts up to "now".
        for ((pkg, start) in foregroundStarts) {
            val clampedStart = maxOf(start, startTime)
            if (endTime > clampedStart) {
                totals[pkg] = (totals[pkg] ?: 0L) + (endTime - clampedStart)
            }
        }

        return totals
    }

    private fun startOfTodayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /** All apps the user can launch from a home screen / app drawer, excluding this app. */
    fun getLaunchableApps(): List<AppInfo> {
        val pm = appContext.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)

        return resolveInfos
            .asSequence()
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != appContext.packageName }
            .mapNotNull { pkg ->
                runCatching {
                    val applicationInfo = pm.getApplicationInfo(pkg, 0)
                    AppInfo(
                        packageName = pkg,
                        label = pm.getApplicationLabel(applicationInfo).toString(),
                        icon = pm.getApplicationIcon(applicationInfo)
                    )
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
