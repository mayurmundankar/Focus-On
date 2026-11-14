package com.focuslock.app.block

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslock.app.data.SessionStore
import com.focuslock.app.data.appLabel
import com.focuslock.app.ui.Quotes
import com.focuslock.app.ui.components.Eyebrow
import com.focuslock.app.ui.components.GoldRule
import com.focuslock.app.ui.theme.FocusOnTheme
import com.focuslock.app.ui.theme.Lux
import com.focuslock.app.ui.theme.LuxType
import com.focuslock.app.util.formatCountdown
import kotlinx.coroutines.delay

/**
 * Shown on top of a silenced app: a black page, one bold statement, and the time left.
 * Back (or any way out) goes to the home screen.
 */
class BlockActivity : ComponentActivity() {

    private var appName by mutableStateOf("This app")
    private var quote by mutableStateOf(Quotes.random())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        appName = labelFrom(intent)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        setContent {
            FocusOnTheme {
                BlockScreen(
                    appName = appName,
                    quote = quote,
                    remainingMillis = { SessionStore.remainingMillis(this@BlockActivity) },
                    onSessionOver = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        appName = labelFrom(intent)
        quote = Quotes.random()
    }

    private fun labelFrom(intent: Intent?): String =
        intent?.getStringExtra(EXTRA_PACKAGE)?.let { appLabel(this, it) } ?: "This app"

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(home)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "blocked_package"
    }
}

@Composable
private fun BlockScreen(
    appName: String,
    quote: String,
    remainingMillis: () -> Long,
    onSessionOver: () -> Unit,
) {
    var remaining by remember { mutableLongStateOf(remainingMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            remaining = remainingMillis()
            if (remaining <= 0L) {
                onSessionOver()
                break
            }
            delay(1_000)
        }
    }

    // The statement rises in slowly each time the page appears.
    val reveal = remember(quote) { Animatable(0f) }
    LaunchedEffect(quote) {
        reveal.animateTo(1f, tween(durationMillis = 1_200, easing = FastOutSlowInEasing))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Lux.Black)
            .systemBarsPadding()
            .padding(horizontal = 32.dp)
    ) {
        Eyebrow(
            text = "$appName · silenced",
            color = Lux.GoldDeep,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 28.dp),
        )

        Column(
            Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    alpha = reveal.value
                    translationY = (1f - reveal.value) * 24.dp.toPx()
                }
        ) {
            GoldRule()
            Spacer(Modifier.height(28.dp))
            Text(text = quote, style = LuxType.Quote, color = Lux.Ivory)
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(formatCountdown(remaining), style = LuxType.Timer, color = Lux.Gold)
            Spacer(Modifier.height(6.dp))
            Eyebrow(
                text = "Remaining",
                color = Lux.Faint,
                style = LuxType.Eyebrow.copy(fontSize = 9.sp),
            )
        }
    }
}
