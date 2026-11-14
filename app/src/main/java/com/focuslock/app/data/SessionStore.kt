package com.focuslock.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Stores the current focus session on disk so it survives the app being killed
 * or the phone restarting. A session is just "block these packages until this time".
 */
object SessionStore {
    private const val PREFS = "focuslock"
    private const val KEY_END_TIME = "session_end_time"
    private const val KEY_BLOCKED = "session_blocked_packages"
    private const val KEY_SELECTION = "picker_selection"
    private const val KEY_LAST_DURATION = "last_duration_min"
    private const val KEY_START_TIME = "session_start_time"
    private const val KEY_ONBOARDED = "onboarded"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Starts a session and returns its end time (epoch millis). */
    fun startSession(context: Context, packages: Set<String>, minutes: Int): Long {
        val now = System.currentTimeMillis()
        val endTime = now + minutes * 60_000L
        val toBlock = packages - context.packageName // never block ourselves
        prefs(context).edit(commit = true) {
            putLong(KEY_START_TIME, now)
            putLong(KEY_END_TIME, endTime)
            putStringSet(KEY_BLOCKED, toBlock)
            putInt(KEY_LAST_DURATION, minutes)
        }
        return endTime
    }

    fun clearSession(context: Context) {
        prefs(context).edit(commit = true) {
            remove(KEY_START_TIME)
            remove(KEY_END_TIME)
            remove(KEY_BLOCKED)
        }
    }

    /** End time of the current session, or 0 if there is none. */
    fun endTime(context: Context): Long = prefs(context).getLong(KEY_END_TIME, 0L)

    /** Start time of the current session, or 0 if unknown (sessions from older versions). */
    fun startTime(context: Context): Long = prefs(context).getLong(KEY_START_TIME, 0L)

    fun isOnboarded(context: Context): Boolean = prefs(context).getBoolean(KEY_ONBOARDED, false)

    fun setOnboarded(context: Context) {
        prefs(context).edit { putBoolean(KEY_ONBOARDED, true) }
    }

    fun isActive(context: Context): Boolean = endTime(context) > System.currentTimeMillis()

    fun remainingMillis(context: Context): Long =
        (endTime(context) - System.currentTimeMillis()).coerceAtLeast(0L)

    fun blockedPackages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED, emptySet())?.toSet() ?: emptySet()

    /** The apps ticked in the picker last time, or null if the user never picked any. */
    fun savedSelection(context: Context): Set<String>? =
        prefs(context).getStringSet(KEY_SELECTION, null)?.toSet()

    fun saveSelection(context: Context, packages: Set<String>) {
        prefs(context).edit { putStringSet(KEY_SELECTION, packages.toSet()) }
    }

    fun lastDuration(context: Context): Int = prefs(context).getInt(KEY_LAST_DURATION, 60)
}
