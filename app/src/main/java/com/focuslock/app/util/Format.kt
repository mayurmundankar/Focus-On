package com.focuslock.app.util

import java.util.Locale

/** 5_025_000 ms -> "1:23:45", 95_000 ms -> "01:35". */
fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) + 999L) / 1000L
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

/** Screen-time style: 7_500_000 ms -> "2h 05m", 2_700_000 -> "45m", under a minute -> "<1m". */
fun formatUsage(millis: Long): String {
    val totalMin = millis.coerceAtLeast(0L) / 60_000L
    return when {
        totalMin < 1 -> "<1m"
        totalMin < 60 -> "${totalMin}m"
        else -> String.format(Locale.US, "%dh %02dm", totalMin / 60, totalMin % 60)
    }
}

/** 45 -> "45 min", 60 -> "1 h", 90 -> "1 h 30 min". */
fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
