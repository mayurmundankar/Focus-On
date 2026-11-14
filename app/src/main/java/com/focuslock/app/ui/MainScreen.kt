package com.focuslock.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.focuslock.app.data.AppInfo
import com.focuslock.app.data.SessionStore
import com.focuslock.app.data.loadLaunchableApps
import com.focuslock.app.data.loadWeeklyUsage
import com.focuslock.app.service.FocusService
import com.focuslock.app.ui.theme.Lux
import com.focuslock.app.util.PermissionState
import com.focuslock.app.util.Permissions
import com.focuslock.app.util.isDebugBuild
import kotlinx.coroutines.delay

/** Ticked the first time the app opens (if installed). */
private val DEFAULT_BLOCK_LIST = setOf(
    "com.instagram.android",
    "com.google.android.youtube",
)

private enum class Route { Onboarding, Home, Session }

/** Holds the app's state and switches between onboarding, home and the running session. */
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val isDebug = remember { isDebugBuild(context) }

    var perms by remember { mutableStateOf(PermissionState.read(context)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var sessionEnd by remember { mutableLongStateOf(SessionStore.endTime(context)) }
    var onboarded by remember { mutableStateOf(SessionStore.isOnboarded(context)) }
    var apps by remember { mutableStateOf<List<AppInfo>?>(null) }
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var selected by remember {
        mutableStateOf(SessionStore.savedSelection(context) ?: DEFAULT_BLOCK_LIST)
    }
    var durationMin by remember { mutableIntStateOf(SessionStore.lastDuration(context)) }
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { perms = PermissionState.read(context) }

    // Coming back from Settings: re-check permissions, and make sure the blocker is running.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        perms = PermissionState.read(context)
        sessionEnd = SessionStore.endTime(context)
        if (SessionStore.isActive(context)) FocusService.start(context)
    }

    // Tick once a second for countdowns.
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            sessionEnd = SessionStore.endTime(context)
            delay(1_000)
        }
    }

    LaunchedEffect(Unit) { apps = loadLaunchableApps(context) }

    // Usage needs Usage access, so (re)load once it's granted.
    LaunchedEffect(perms.usageAccess) {
        if (perms.usageAccess) usage = loadWeeklyUsage(context)
    }

    val requestNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedForNotifications) {
            askedForNotifications = true
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Permissions.open(context, Permissions.notificationSettingsIntent(context))
        }
    }

    val route = when {
        sessionEnd > now -> Route.Session
        !onboarded || !perms.requiredGranted -> Route.Onboarding
        else -> Route.Home
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Lux.Ink, contentColor = Lux.Ivory) {
        Crossfade(targetState = route, animationSpec = tween(600), label = "route") { r ->
            when (r) {
                Route.Onboarding -> OnboardingScreen(
                    perms = perms,
                    startOnPermissions = onboarded,
                    onRequestNotifications = requestNotifications,
                    onFinish = {
                        SessionStore.setOnboarded(context)
                        onboarded = true
                    },
                )

                Route.Home -> HomeScreen(
                    apps = apps,
                    usage = usage,
                    selected = selected,
                    onToggleApp = { pkg ->
                        selected = if (pkg in selected) selected - pkg else selected + pkg
                        SessionStore.saveSelection(context, selected)
                    },
                    durationMin = durationMin,
                    onDurationChange = { durationMin = it },
                    now = now,
                    isDebug = isDebug,
                    onStart = { packages ->
                        SessionStore.saveSelection(context, selected)
                        sessionEnd = SessionStore.startSession(context, packages, durationMin)
                        now = System.currentTimeMillis()
                        FocusService.start(context)
                    },
                )

                Route.Session -> {
                    val start = SessionStore.startTime(context).takeIf { it > 0L }
                        ?: (sessionEnd - SessionStore.lastDuration(context) * 60_000L)
                    ActiveSessionScreen(
                        startTime = start,
                        endTime = sessionEnd,
                        now = now,
                        blocked = SessionStore.blockedPackages(context),
                        apps = apps,
                        perms = perms,
                        isDebug = isDebug,
                        onFixPermissions = {
                            val intent = if (!perms.usageAccess) {
                                Permissions.usageAccessIntent()
                            } else {
                                Permissions.overlayIntent(context)
                            }
                            Permissions.open(context, intent)
                        },
                        onEndEarly = {
                            FocusService.stop(context)
                            SessionStore.clearSession(context)
                            sessionEnd = 0L
                        },
                    )
                }
            }
        }
    }
}
