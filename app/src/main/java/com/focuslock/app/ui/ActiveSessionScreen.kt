package com.focuslock.app.ui

import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslock.app.data.AppInfo
import com.focuslock.app.ui.components.Eyebrow
import com.focuslock.app.ui.components.GhostButton
import com.focuslock.app.ui.components.GoldRule
import com.focuslock.app.ui.components.LiveDot
import com.focuslock.app.ui.components.RotatingQuote
import com.focuslock.app.ui.components.Wordmark
import com.focuslock.app.ui.theme.Lux
import com.focuslock.app.ui.theme.LuxType
import com.focuslock.app.util.PermissionState
import com.focuslock.app.util.formatCountdown
import java.util.Date

@Composable
fun ActiveSessionScreen(
    startTime: Long,
    endTime: Long,
    now: Long,
    blocked: Set<String>,
    apps: List<AppInfo>?,
    perms: PermissionState,
    isDebug: Boolean,
    onFixPermissions: () -> Unit,
    onEndEarly: () -> Unit,
) {
    val context = LocalContext.current
    val remaining = (endTime - now).coerceAtLeast(0L)
    val total = (endTime - startTime).coerceAtLeast(1L)
    val progress by animateFloatAsState(
        targetValue = (remaining.toFloat() / total).coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "ring",
    )
    val endsAt = remember(endTime) { DateFormat.getTimeFormat(context).format(Date(endTime)) }
    val blockedApps = remember(apps, blocked) { apps?.filter { it.packageName in blocked } }
    val grayscale = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Wordmark()
            Spacer(Modifier.weight(1f))
            LiveDot()
            Spacer(Modifier.width(10.dp))
            Eyebrow("In focus")
        }

        Spacer(Modifier.height(40.dp))

        // Countdown ring
        Box(Modifier.size(288.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val inset = 8.dp.toPx()
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                drawCircle(
                    color = Lux.Hairline,
                    radius = arcSize.minDimension / 2,
                    style = Stroke(1.dp.toPx()),
                )
                drawArc(
                    color = Lux.Gold,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Eyebrow("Remaining", color = Lux.Muted)
                Spacer(Modifier.height(12.dp))
                Text(formatCountdown(remaining), style = LuxType.Numeral, color = Lux.Ivory)
                Spacer(Modifier.height(10.dp))
                Text("until $endsAt", style = LuxType.Small, color = Lux.Muted)
            }
        }

        Spacer(Modifier.height(44.dp))
        GoldRule()
        Spacer(Modifier.height(22.dp))
        RotatingQuote(
            quotes = Quotes.all,
            style = LuxType.Hero.copy(fontSize = 23.sp, lineHeight = 31.sp, textAlign = TextAlign.Center),
            alignment = Alignment.TopCenter,
            intervalMs = 9_000L,
        )

        if (!perms.requiredGranted) {
            Spacer(Modifier.height(16.dp))
            val shape = RoundedCornerShape(4.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Lux.Panel, shape)
                    .border(1.dp, Lux.Warning.copy(alpha = 0.6f), shape)
                    .padding(20.dp)
            ) {
                Eyebrow("Silence is paused", color = Lux.Warning)
                Spacer(Modifier.height(8.dp))
                Text(
                    "A required permission was turned off, so Focus-On can't step in front of your apps. Turn it back on to keep your word.",
                    style = LuxType.Small,
                    color = Lux.Muted,
                )
                Spacer(Modifier.height(14.dp))
                GhostButton("Restore", onFixPermissions)
            }
        }

        Spacer(Modifier.height(36.dp))
        Eyebrow("Silenced", color = Lux.Muted)
        Spacer(Modifier.height(16.dp))
        if (blockedApps == null) {
            blocked.forEach {
                Text(it, style = LuxType.Small, color = Lux.Muted)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                blockedApps.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { app ->
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.label,
                                colorFilter = grayscale,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .alpha(0.55f),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))
        Text(
            "No early exit. That's the point.",
            style = LuxType.Small,
            color = Lux.Faint,
        )

        // Only in builds run from Android Studio, so testing doesn't mean waiting.
        if (isDebug) {
            Spacer(Modifier.height(10.dp))
            Text(
                "End session (debug build)",
                style = LuxType.Small.copy(textDecoration = TextDecoration.Underline),
                color = Lux.Faint,
                modifier = Modifier
                    .clickable(onClick = onEndEarly)
                    .padding(8.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
