package com.phoneusage.tracker.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import java.util.Calendar

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
        val events = usageStatsManager.queryEvents(startTime, endTime)
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
                    if (start != null && event.timeStamp > start) {
                        totals[event.packageName] =
                            (totals[event.packageName] ?: 0L) + (event.timeStamp - start)
                    }
                }
            }
        }

        // Anything still in the foreground when we queried counts up to "now".
        for ((pkg, start) in foregroundStarts) {
            if (endTime > start) {
                totals[pkg] = (totals[pkg] ?: 0L) + (endTime - start)
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
