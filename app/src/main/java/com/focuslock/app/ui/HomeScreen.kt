package com.focuslock.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslock.app.data.AppInfo
import com.focuslock.app.data.USAGE_WINDOW_DAYS
import com.focuslock.app.ui.components.ChoiceChip
import com.focuslock.app.ui.components.Eyebrow
import com.focuslock.app.ui.components.GoldButton
import com.focuslock.app.ui.components.GoldCheck
import com.focuslock.app.ui.components.GoldRule
import com.focuslock.app.ui.components.Hairline
import com.focuslock.app.ui.components.RotatingQuote
import com.focuslock.app.ui.components.SectionHeader
import com.focuslock.app.ui.components.UsageBar
import com.focuslock.app.ui.components.Wordmark
import com.focuslock.app.ui.theme.Lux
import com.focuslock.app.ui.theme.LuxType
import com.focuslock.app.util.formatMinutes
import com.focuslock.app.util.formatUsage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Minutes. */
private val PRESETS = listOf(25, 45, 60, 90, 120, 180)

/** How many apps show before "Show all". */
private const val TOP_COUNT = 8

/** How many apps the "last 7 days" insight adds up. */
private const val INSIGHT_COUNT = 5

@Composable
fun HomeScreen(
    apps: List<AppInfo>?,
    usage: Map<String, Long>,
    selected: Set<String>,
    onToggleApp: (String) -> Unit,
    durationMin: Int,
    onDurationChange: (Int) -> Unit,
    now: Long,
    isDebug: Boolean,
    onStart: (Set<String>) -> Unit,
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    // Most-used first, then alphabetical.
    val ranked = remember(apps, usage) {
        apps?.sortedWith(
            compareByDescending<AppInfo> { usage[it.packageName] ?: 0L }
                .thenBy { it.label.lowercase() }
        )
    }
    val topUsage = remember(ranked, usage) {
        ranked?.firstOrNull()?.let { usage[it.packageName] } ?: 0L
    }
    // Apps that were ticked when the list loaded stay visible even outside the top list.
    val pinned = remember(ranked) { selected }
    val visible = remember(ranked, query, showAll, pinned) {
        val q = query.trim()
        when {
            ranked == null -> null
            q.isNotEmpty() -> ranked.filter { it.label.contains(q, ignoreCase = true) }
            showAll -> ranked
            else -> {
                val top = ranked.take(TOP_COUNT)
                top + ranked.filter { it.packageName in pinned && it !in top }
            }
        }
    }
    val chosen = remember(apps, selected) {
        apps?.map { it.packageName }?.filter { it in selected }?.toSet() ?: emptySet()
    }

    val insightApps = remember(ranked, usage) {
        ranked.orEmpty().filter { (usage[it.packageName] ?: 0L) > 0L }.take(INSIGHT_COUNT)
    }
    val insightTotal = insightApps.sumOf { usage[it.packageName] ?: 0L }
    val reclaimPerDay = chosen.sumOf { usage[it] ?: 0L } / USAGE_WINDOW_DAYS
    val endsAt = DateFormat.getTimeFormat(context).format(Date(now + durationMin * 60_000L))
    val today = remember(now / 60_000L) {
        SimpleDateFormat("EEE · d MMM", Locale.getDefault()).format(Date(now))
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Wordmark()
                    Spacer(Modifier.weight(1f))
                    Eyebrow(today, color = Lux.Muted)
                }
            }

            // Hero statement
            item {
                Spacer(Modifier.height(28.dp))
                GoldRule()
                Spacer(Modifier.height(24.dp))
                RotatingQuote(quotes = Quotes.all, style = LuxType.Hero)
            }

            // What the last week cost
            if (insightTotal >= 60_000L) {
                item {
                    Spacer(Modifier.height(16.dp))
                    InsightCard(totalMs = insightTotal, appCount = insightApps.size)
                }
            }

            item {
                Spacer(Modifier.height(48.dp))
                SectionHeader(
                    number = "01",
                    eyebrow = "Silence",
                    title = "Choose what drains you.",
                    subtitle = if (usage.isNotEmpty()) {
                        "Ranked by the time each app took from you this week."
                    } else {
                        "Tick every app that pulls you away from your work."
                    },
                )
                Spacer(Modifier.height(20.dp))
                SearchField(query = query, onQueryChange = { query = it })
                Spacer(Modifier.height(4.dp))
            }

            if (visible == null) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Lux.Gold, strokeWidth = 1.5.dp)
                    }
                }
            } else {
                items(visible, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        weekMs = usage[app.packageName] ?: 0L,
                        topMs = topUsage,
                        showUsage = usage.isNotEmpty(),
                        checked = app.packageName in selected,
                        onToggle = { onToggleApp(app.packageName) },
                    )
                }
                if (visible.isEmpty()) {
                    item {
                        Text(
                            "No app matches \"$query\".",
                            style = LuxType.Small,
                            color = Lux.Muted,
                            modifier = Modifier.padding(vertical = 20.dp),
                        )
                    }
                }
            }

            if (query.isBlank() && ranked != null && ranked.size > TOP_COUNT) {
                item {
                    Eyebrow(
                        if (showAll) "Show fewer" else "Show all ${ranked.size} apps",
                        modifier = Modifier
                            .clickable { showAll = !showAll }
                            .padding(vertical = 18.dp),
                    )
                }
            }

            item {
                Spacer(Modifier.height(40.dp))
                SectionHeader(number = "02", eyebrow = "Duration", title = "For how long?")
                Spacer(Modifier.height(20.dp))
                DurationGrid(
                    durationMin = durationMin,
                    isDebug = isDebug,
                    onSelect = onDurationChange,
                    onCustom = { showCustom = true },
                )
            }
        }

        // Fixed footer with the start action
        Column(
            Modifier
                .fillMaxWidth()
                .background(Lux.Ink)
                .navigationBarsPadding()
        ) {
            Hairline()
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 16.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        text = when {
                            chosen.isEmpty() -> "Nothing selected"
                            reclaimPerDay >= 60_000L ->
                                "${chosen.size} silenced · ≈${formatUsage(reclaimPerDay)} back a day"
                            else -> "${chosen.size} silenced"
                        },
                        style = LuxType.Small,
                        color = if (chosen.isEmpty()) Lux.Faint else Lux.GoldLight,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("Ends $endsAt", style = LuxType.Small, color = Lux.Muted)
                }
                Spacer(Modifier.height(12.dp))
                GoldButton(
                    text = if (chosen.isEmpty()) "Select an app to silence" else "Begin focus",
                    onClick = { showConfirm = true },
                    enabled = chosen.isNotEmpty(),
                )
            }
        }
    }

    if (showCustom) {
        CustomDurationDialog(
            initialMinutes = durationMin,
            onDismiss = { showCustom = false },
            onConfirm = {
                onDurationChange(it)
                showCustom = false
            },
        )
    }

    if (showConfirm) {
        val word = if (chosen.size == 1) "app" else "apps"
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = Lux.Panel,
            shape = RoundedCornerShape(6.dp),
            title = {
                Text(
                    "Silence ${chosen.size} $word for ${formatMinutes(durationMin)}?",
                    style = LuxType.Title,
                    color = Lux.Ivory,
                )
            },
            text = {
                Text(
                    "Until $endsAt they stay locked. There is no early exit. That's the point.",
                    style = LuxType.Body,
                    color = Lux.Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onStart(chosen)
                }) {
                    Text("COMMIT", style = LuxType.Eyebrow.copy(fontSize = 13.sp), color = Lux.Gold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("NOT YET", style = LuxType.Eyebrow.copy(fontSize = 13.sp), color = Lux.Muted)
                }
            },
        )
    }
}

@Composable
private fun InsightCard(totalMs: Long, appCount: Int) {
    val shape = RoundedCornerShape(4.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Lux.Panel, shape)
            .border(1.dp, Lux.Hairline, shape)
            .padding(24.dp)
    ) {
        Eyebrow("The last $USAGE_WINDOW_DAYS days", color = Lux.Muted)
        Spacer(Modifier.height(14.dp))
        Text(formatUsage(totalMs), style = LuxType.Numeral, color = Lux.GoldLight)
        Spacer(Modifier.height(8.dp))
        val apps = if (appCount == 1) "your most-used app" else "your $appCount most-used apps"
        Text(
            "given to $apps. That's ${formatUsage(totalMs / USAGE_WINDOW_DAYS)} every single day.",
            style = LuxType.Body,
            color = Lux.Muted,
        )
        Spacer(Modifier.height(14.dp))
        Text("Take it back.", style = LuxType.Label, color = Lux.Ivory)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = LuxType.Body.copy(color = Lux.Ivory),
        cursorBrush = SolidColor(Lux.Gold),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Column {
                Box(Modifier.padding(vertical = 10.dp)) {
                    if (query.isEmpty()) {
                        Text("Search all apps", style = LuxType.Body, color = Lux.Faint)
                    }
                    inner()
                }
                Hairline(color = if (query.isEmpty()) Lux.Hairline else Lux.GoldDeep)
            }
        },
    )
}

@Composable
private fun AppRow(
    app: AppInfo,
    weekMs: Long,
    topMs: Long,
    showUsage: Boolean,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                bitmap = app.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = LuxType.Label,
                    color = if (checked) Lux.Ivory else Lux.Ivory.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showUsage) {
                    Spacer(Modifier.height(7.dp))
                    if (weekMs >= 60_000L) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UsageBar(
                                fraction = if (topMs > 0) weekMs.toFloat() / topMs else 0f,
                                modifier = Modifier.width(56.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${formatUsage(weekMs / USAGE_WINDOW_DAYS)} a day",
                                style = LuxType.Small,
                                color = Lux.Muted,
                            )
                        }
                    } else {
                        Text("Rarely used", style = LuxType.Small, color = Lux.Faint)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            GoldCheck(checked)
        }
        Hairline()
    }
}

@Composable
private fun DurationGrid(
    durationMin: Int,
    isDebug: Boolean,
    onSelect: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    val isTest = isDebug && durationMin == 1
    val isCustom = durationMin !in PRESETS && !isTest
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PRESETS.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { minutes ->
                    ChoiceChip(
                        text = formatMinutes(minutes),
                        selected = durationMin == minutes,
                        onClick = { onSelect(minutes) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        // Custom spans exactly two grid columns so its edge lines up with the tiles above.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 10.dp
            val column = (maxWidth - gap * 2) / 3
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                ChoiceChip(
                    text = if (isCustom) "Custom · ${formatMinutes(durationMin)}" else "Custom",
                    selected = isCustom,
                    onClick = onCustom,
                    modifier = if (isDebug) Modifier.width(column * 2 + gap) else Modifier.fillMaxWidth(),
                )
                if (isDebug) {
                    ChoiceChip(
                        text = "1 min · test",
                        selected = isTest,
                        onClick = { onSelect(1) },
                        modifier = Modifier.width(column),
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomDurationDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var hours by remember { mutableIntStateOf((initialMinutes / 60).coerceIn(0, 12)) }
    var minutes by remember { mutableIntStateOf((initialMinutes % 60) / 5 * 5) }
    val total = hours * 60 + minutes
    val valid = total >= 5

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Lux.Panel,
        shape = RoundedCornerShape(6.dp),
        title = { Text("Your own length.", style = LuxType.Title, color = Lux.Ivory) },
        text = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stepper(
                    label = "Hours",
                    value = hours,
                    onMinus = { if (hours > 0) hours-- },
                    onPlus = { if (hours < 12) hours++ },
                )
                Stepper(
                    label = "Minutes",
                    value = minutes,
                    onMinus = { minutes = if (minutes == 0) 55 else minutes - 5 },
                    onPlus = { minutes = if (minutes == 55) 0 else minutes + 5 },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(total) }, enabled = valid) {
                Text(
                    if (valid) "SET ${formatMinutes(total).uppercase()}" else "AT LEAST 5 MIN",
                    style = LuxType.Eyebrow.copy(fontSize = 13.sp),
                    color = if (valid) Lux.Gold else Lux.Faint,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = LuxType.Eyebrow.copy(fontSize = 13.sp), color = Lux.Muted)
            }
        },
    )
}

@Composable
private fun Stepper(label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(label, color = Lux.Muted)
        Spacer(Modifier.height(12.dp))
        StepButton("+", onPlus)
        Text(
            text = "%02d".format(value),
            style = LuxType.Numeral.copy(fontSize = 44.sp),
            color = Lux.Ivory,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        StepButton("−", onMinus)
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, Lux.Hairline, RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = LuxType.Label.copy(fontSize = 20.sp, fontWeight = FontWeight.Normal), color = Lux.Gold)
    }
}
