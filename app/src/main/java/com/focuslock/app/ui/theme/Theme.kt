package com.focuslock.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.focuslock.app.R

/** Focus-On palette: near-black, ivory text, champagne gold accents. Always dark. */
object Lux {
    val Black = Color(0xFF000000)
    val Ink = Color(0xFF09090A)
    val Panel = Color(0xFF121214)
    val PanelHigh = Color(0xFF1A1A1D)
    val Hairline = Color(0xFF26262A)
    val Gold = Color(0xFFC9A96E)
    val GoldLight = Color(0xFFEBD7A8)
    val GoldDeep = Color(0xFF8A7044)
    val Ivory = Color(0xFFF3EEE4)
    val Muted = Color(0xFF8F8A80)
    val Faint = Color(0xFF55534E)
    val Warning = Color(0xFFD98C6B)
}

/**
 * Fonts bundled in res/font so the app looks the same on every phone (some phones,
 * like Motorola, swap the system serif for their own typeface). Both are variable
 * fonts, so each weight sets the matching "wght" axis value. Licences: assets/licenses.
 */
@OptIn(ExperimentalTextApi::class)
object LuxFonts {
    private fun playfair(weight: FontWeight) = Font(
        R.font.playfair_display, weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

    private fun inter(weight: FontWeight) = Font(
        R.font.inter, weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

    /** Playfair Display: statements, titles, numbers. */
    val Display = FontFamily(
        playfair(FontWeight.Normal),
        playfair(FontWeight.SemiBold),
        playfair(FontWeight.Bold),
        playfair(FontWeight.ExtraBold),
    )

    /** Inter: labels and body text. */
    val Text = FontFamily(
        inter(FontWeight.Normal),
        inter(FontWeight.Medium),
        inter(FontWeight.SemiBold),
    )
}

/** Serif for statements and numbers, letter-spaced sans for labels. */
object LuxType {
    private val Serif = LuxFonts.Display
    private val Sans = LuxFonts.Text

    val Wordmark = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 0.2.sp,
        fontFeatureSettings = "lnum",
    )
    val Hero = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
        fontFeatureSettings = "lnum",
    )
    val Quote = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 42.sp,
        letterSpacing = (-0.4).sp,
        fontFeatureSettings = "lnum",
    )
    val Title = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp,
        fontFeatureSettings = "lnum",
    )
    val Numeral = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 52.sp, letterSpacing = (-1).sp,
        fontFeatureSettings = "lnum, tnum",
    )
    val Eyebrow = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 3.sp,
    )
    val Body = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 23.sp)
    val Label = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp)
    val Small = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp)
    val Timer = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, letterSpacing = 2.sp,
        fontFeatureSettings = "tnum",
    )
}

private val Scheme = darkColorScheme(
    primary = Lux.Gold,
    onPrimary = Lux.Black,
    secondary = Lux.Gold,
    onSecondary = Lux.Black,
    background = Lux.Ink,
    onBackground = Lux.Ivory,
    surface = Lux.Ink,
    onSurface = Lux.Ivory,
    surfaceVariant = Lux.PanelHigh,
    onSurfaceVariant = Lux.Muted,
    surfaceContainerHigh = Lux.Panel,
    surfaceContainerHighest = Lux.PanelHigh,
    outline = Lux.Hairline,
    outlineVariant = Lux.Hairline,
    error = Lux.Warning,
)

@Composable
fun FocusOnTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Typography(), content = content)
}
