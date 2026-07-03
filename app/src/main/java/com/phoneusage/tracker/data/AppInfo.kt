package com.phoneusage.tracker.data

import android.graphics.drawable.Drawable

/** A launchable app on the device, as shown in the app-selection list. */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

/** [AppInfo] combined with today's usage and whether it counts toward the total. */
data class AppUsageItem(
    val app: AppInfo,
    val usageMillisToday: Long,
    val isIncluded: Boolean
)
