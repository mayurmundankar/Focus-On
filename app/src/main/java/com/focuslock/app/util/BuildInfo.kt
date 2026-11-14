package com.focuslock.app.util

import android.content.Context
import android.content.pm.ApplicationInfo

/** True for builds run from Android Studio / gradlew installDebug. */
fun isDebugBuild(context: Context): Boolean =
    (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
