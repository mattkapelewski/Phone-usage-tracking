package com.phoneusage.tracker.util

/** e.g. "2h 15m", "45m", "0m" — used in the app UI where space allows. */
fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

/** e.g. "2h15m", "45m" — compact form with no spaces, for the tiny 1x1 widget. */
fun formatDurationCompact(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h${minutes}m" else "${minutes}m"
}
