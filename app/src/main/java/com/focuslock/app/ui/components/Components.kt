package com.focuslock.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.focuslock.app.ui.theme.LuxFonts
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslock.app.ui.theme.Lux
import com.focuslock.app.ui.theme.LuxType
import kotlinx.coroutines.delay

private val Corner = RoundedCornerShape(4.dp)

/** Small, wide-spaced, upper-case label. */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Lux.Gold,
    style: TextStyle = LuxType.Eyebrow,
) {
    Text(text.uppercase(), modifier = modifier, style = style, color = color, maxLines = 1)
}

/** "Focus-On" with a gold hyphen. */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            append("Focus")
            withStyle(SpanStyle(color = Lux.Gold)) { append("-") }
            append("On")
        },
        modifier = modifier,
        style = LuxType.Wordmark,
        color = Lux.Ivory,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = Lux.Hairline) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

/** Short gold line used above statements. */
@Composable
fun GoldRule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(28.dp)
            .height(1.dp)
            .background(Lux.Gold)
    )
}

/** Primary action: champagne gold with a slow light sweep across it. */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val sheen = rememberInfiniteTransition(label = "sheen")
    val x by sheen.animateFloat(
        initialValue = -700f,
        targetValue = 1800f,
        animationSpec = infiniteRepeatable(tween(3_800, easing = LinearEasing), RepeatMode.Restart),
        label = "sheenX",
    )
    val brush = if (enabled) {
        Brush.linearGradient(
            colors = listOf(Lux.Gold, Lux.GoldLight, Lux.Gold),
            start = Offset(x, 0f),
            end = Offset(x + 420f, 160f),
        )
    } else {
        SolidColor(Lux.PanelHigh)
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(Corner)
            .background(brush)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = LuxType.Eyebrow.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = if (enabled) Lux.Black else Lux.Faint,
        )
    }
}

/** Secondary action: gold outline. */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(38.dp)
            .clip(Corner)
            .border(1.dp, Lux.GoldDeep, Corner)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = LuxType.Eyebrow.copy(letterSpacing = 2.sp),
            color = Lux.Gold,
        )
    }
}

/** Selectable tile used for durations. */
@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(52.dp)
            .clip(Corner)
            .background(if (selected) Lux.Gold.copy(alpha = 0.10f) else Color.Transparent)
            .border(1.dp, if (selected) Lux.Gold else Lux.Hairline, Corner)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = LuxType.Label.copy(fontSize = 14.sp),
            color = if (selected) Lux.GoldLight else Lux.Ivory,
            maxLines = 1,
        )
    }
}

/** Round gold tick, or a faint empty ring. */
@Composable
fun GoldCheck(checked: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val r = size.minDimension / 2
        if (checked) {
            drawCircle(Lux.Gold, radius = r)
            val tick = Path().apply {
                moveTo(size.width * 0.29f, size.height * 0.52f)
                lineTo(size.width * 0.44f, size.height * 0.66f)
                lineTo(size.width * 0.72f, size.height * 0.37f)
            }
            drawPath(
                tick,
                Lux.Black,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        } else {
            drawCircle(Lux.Faint, radius = r - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
        }
    }
}

/** Thin gold bar showing a share of the most-used app's time. */
@Composable
fun UsageBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(2.dp)
            .background(Lux.Hairline)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                .background(Lux.Gold)
        )
    }
}

/** "01 ── SILENCE" followed by a serif title. */
@Composable
fun SectionHeader(number: String, eyebrow: String, title: String, subtitle: String? = null) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = number,
                style = LuxType.Eyebrow.copy(
                    fontFamily = LuxFonts.Display, fontSize = 13.sp, letterSpacing = 1.sp,
                ),
                color = Lux.Gold,
            )
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .width(24.dp)
                    .height(1.dp)
                    .background(Lux.GoldDeep)
            )
            Spacer(Modifier.width(12.dp))
            Eyebrow(eyebrow, color = Lux.Muted)
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = LuxType.Title, color = Lux.Ivory)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = LuxType.Small, color = Lux.Muted)
        }
    }
}

/**
 * Cross-fades through the quotes. Every quote is also laid out invisibly underneath,
 * so the box is exactly as tall as the tallest one on this screen and font size:
 * no wasted gap, no clipping, and the layout never jumps between quotes.
 */
@Composable
fun RotatingQuote(
    quotes: List<String>,
    style: TextStyle,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopStart,
    intervalMs: Long = 7_000L,
) {
    val order = remember(quotes) { quotes.shuffled() }
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(order) {
        while (true) {
            delay(intervalMs)
            index = (index + 1) % order.size
        }
    }
    Box(modifier.fillMaxWidth()) {
        order.forEach { quote ->
            Text(
                quote,
                style = style,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0f),
            )
        }
        Crossfade(
            targetState = index,
            animationSpec = tween(1_000),
            modifier = Modifier.matchParentSize(),
            label = "quote",
        ) { i ->
            Box(Modifier.fillMaxSize(), contentAlignment = alignment) {
                Text(order[i], style = style, color = Lux.Ivory)
            }
        }
    }
}

/** Gold dot that slowly breathes, used as a "live" indicator. */
@Composable
fun LiveDot(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "live")
    val a by pulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_400), RepeatMode.Reverse),
        label = "liveAlpha",
    )
    Box(
        modifier
            .size(6.dp)
            .alpha(a)
            .clip(RoundedCornerShape(50))
            .background(Lux.Gold)
    )
}
