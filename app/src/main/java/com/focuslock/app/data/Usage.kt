package com.focuslock.app.data

import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val USAGE_WINDOW_DAYS = 7
private const val DAY_MS = 24 * 60 * 60 * 1000L

/**
 * Time each app spent in the foreground over the last [USAGE_WINDOW_DAYS] days,
 * keyed by package name. Needs Usage access; returns an empty map without it.
 */
suspend fun loadWeeklyUsage(context: Context): Map<String, Long> = withContext(Dispatchers.IO) {
    val usm = context.getSystemService(UsageStatsManager::class.java)
        ?: return@withContext emptyMap()
    val end = System.currentTimeMillis()
    val start = end - USAGE_WINDOW_DAYS * DAY_MS
    runCatching { usm.queryAndAggregateUsageStats(start, end) }
        .getOrNull()
        ?.mapValues { it.value.totalTimeInForeground }
        ?.filterValues { it > 0L }
        ?: emptyMap()
}
