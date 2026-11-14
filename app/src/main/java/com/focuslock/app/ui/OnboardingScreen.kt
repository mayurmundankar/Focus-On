package com.focuslock.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.focuslock.app.ui.theme.LuxFonts
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslock.app.ui.components.Eyebrow
import com.focuslock.app.ui.components.GhostButton
import com.focuslock.app.ui.components.GoldButton
import com.focuslock.app.ui.components.GoldCheck
import com.focuslock.app.ui.components.GoldRule
import com.focuslock.app.ui.components.Hairline
import com.focuslock.app.ui.components.Wordmark
import com.focuslock.app.ui.theme.Lux
import com.focuslock.app.ui.theme.LuxType
import com.focuslock.app.util.PermissionState
import com.focuslock.app.util.Permissions
import kotlinx.coroutines.launch

private data class Manifesto(
    val numeral: String,
    val eyebrow: String,
    val headline: String,
    val body: String,
)

private val PAGES = listOf(
    Manifesto(
        numeral = "I",
        eyebrow = "The truth",
        headline = "Most apps on your phone are built to take your time.",
        body = "Endless feeds. Autoplay. Notifications timed to pull you back. Whole teams are paid to win your attention, and most days they do.",
    ),
    Manifesto(
        numeral = "II",
        eyebrow = "The choice",
        headline = "The majority stays distracted. You don't have to.",
        body = "Pick the apps that drain you. Silence them for as long as you decide. No snooze. No early exit.",
    ),
    Manifesto(
        numeral = "III",
        eyebrow = "The reward",
        headline = "Focused hours compound.",
        body = "One deep hour a day is 365 hours a year. Protect your attention and it builds the person you keep promising yourself.",
    ),
)

/**
 * First-run manifesto followed by the permissions page. When the user has been
 * through it before but a required permission was turned off, it opens straight
 * on the permissions page.
 */
@Composable
fun OnboardingScreen(
    perms: PermissionState,
    startOnPermissions: Boolean,
    onRequestNotifications: () -> Unit,
    onFinish: () -> Unit,
) {
    val pageCount = PAGES.size + 1
    val pager = rememberPagerState(initialPage = if (startOnPermissions) PAGES.size else 0) { pageCount }
    val scope = rememberCoroutineScope()
    val onLast = pager.currentPage == pageCount - 1

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Wordmark()
            Spacer(Modifier.weight(1f))
            if (!onLast) {
                Eyebrow(
                    "Skip",
                    color = Lux.Muted,
                    modifier = Modifier
                        .clickable { scope.launch { pager.animateScrollToPage(pageCount - 1) } }
                        .padding(8.dp),
                )
            }
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            if (page < PAGES.size) {
                ManifestoPage(PAGES[page])
            } else {
                PermissionsPage(perms, onRequestNotifications)
            }
        }

        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 20.dp)) {
            PageDashes(count = pageCount, current = pager.currentPage)
            Spacer(Modifier.height(24.dp))
            if (onLast) {
                GoldButton(
                    text = if (perms.requiredGranted) "Enter Focus-On" else "Allow the two required keys",
                    onClick = onFinish,
                    enabled = perms.requiredGranted,
                )
            } else {
                GoldButton(
                    text = "Continue",
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                )
            }
        }
    }
}

@Composable
private fun ManifestoPage(page: Manifesto) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Oversized roman numeral as a quiet watermark.
        Text(
            text = page.numeral,
            style = TextStyle(
                fontFamily = LuxFonts.Display,
                fontWeight = FontWeight.Bold,
                fontSize = 240.sp,
                lineHeight = 240.sp,
                textAlign = TextAlign.End,
            ),
            color = Lux.Gold.copy(alpha = 0.07f),
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 16.dp),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 24.dp)
        ) {
            Eyebrow(page.eyebrow)
            Spacer(Modifier.height(20.dp))
            GoldRule()
            Spacer(Modifier.height(24.dp))
            Text(
                text = page.headline,
                style = LuxType.Hero.copy(fontSize = 38.sp, lineHeight = 46.sp),
                color = Lux.Ivory,
            )
            Spacer(Modifier.height(20.dp))
            Text(page.body, style = LuxType.Body, color = Lux.Muted)
        }
    }
}

@Composable
private fun PermissionsPage(perms: PermissionState, onRequestNotifications: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Eyebrow("Before we begin")
        Spacer(Modifier.height(20.dp))
        GoldRule()
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Grant the keys.",
            style = LuxType.Hero.copy(fontSize = 38.sp, lineHeight = 46.sp),
            color = Lux.Ivory,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Focus-On needs to see which app is open and to step in front of the ones you silence. It has no internet access. Nothing leaves your phone.",
            style = LuxType.Body,
            color = Lux.Muted,
        )
        Spacer(Modifier.height(28.dp))
        Hairline()
        PermissionRow(
            title = "Usage access",
            description = "See which app is open, and which apps take most of your time.",
            required = true,
            granted = perms.usageAccess,
            onGrant = { Permissions.open(context, Permissions.usageAccessIntent()) },
        )
        PermissionRow(
            title = "Display over other apps",
            description = "Step in front of a silenced app the moment it opens.",
            required = true,
            granted = perms.overlay,
            onGrant = { Permissions.open(context, Permissions.overlayIntent(context)) },
        )
        PermissionRow(
            title = "Notifications",
            description = "Show the countdown while you focus.",
            required = false,
            granted = perms.notifications,
            onGrant = onRequestNotifications,
        )
        PermissionRow(
            title = "Unrestricted battery",
            description = "Stop Android from closing Focus-On mid-session.",
            required = false,
            granted = perms.batteryUnrestricted,
            onGrant = { Permissions.open(context, Permissions.batteryOptimizationIntent(context)) },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    required: Boolean,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = LuxType.Label, color = Lux.Ivory)
                    Spacer(Modifier.width(10.dp))
                    Eyebrow(
                        if (required) "Required" else "Recommended",
                        color = if (required) Lux.GoldDeep else Lux.Faint,
                        style = LuxType.Eyebrow.copy(fontSize = 9.sp, letterSpacing = 2.sp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(description, style = LuxType.Small, color = Lux.Muted)
            }
            Spacer(Modifier.width(16.dp))
            if (granted) GoldCheck(checked = true) else GhostButton("Allow", onGrant)
        }
        Hairline()
    }
}

@Composable
private fun PageDashes(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 28.dp else 8.dp, label = "dash")
            Box(
                Modifier
                    .width(width)
                    .height(2.dp)
                    .background(if (i == current) Lux.Gold else Lux.Hairline)
            )
        }
    }
}
