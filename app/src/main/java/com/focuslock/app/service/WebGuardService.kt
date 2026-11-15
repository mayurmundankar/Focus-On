package com.focuslock.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Browser
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.focuslock.app.block.BlockActivity
import com.focuslock.app.data.SessionStore
import com.focuslock.app.data.Websites
import com.focuslock.app.service.EscapePlanner.Step
import java.lang.ref.WeakReference

/**
 * Covers the web versions of silenced apps (instagram.com for Instagram, …).
 *
 * Privacy and security scope, by design:
 *  - Android only sends this service events from the browsers listed in
 *    res/xml/web_guard_config.xml; it never sees other apps or the keyboard.
 *  - It does nothing unless a focus session is running.
 *  - It reads only the address bar, compares the host with the silenced apps'
 *    domains, and discards it. Nothing is logged, stored or sent (no internet permission).
 *  - Only the system can bind to it (BIND_ACCESSIBILITY_SERVICE).
 *
 * Never left on outside a focus session. UPI and banking apps refuse to work while
 * any non-system accessibility service is switched on (an anti-fraud rule), so this
 * service switches ITSELF off (disableSelf) when it starts with no session running,
 * when the session ends, when the app ends it early, when the user pauses it, and
 * the moment a payment or bank app opens (FocusService watches for those).
 * Only the user can switch it back on, in Accessibility settings.
 */
class WebGuardService : AccessibilityService() {

    private var lastCheckAt = 0L
    private var lastBlockAt = 0L

    /** How to get each browser off a silenced site, escalating if a method fails on this phone. */
    private val escapes = EscapePlanner()

    private val handler = Handler(Looper.getMainLooper())

    /** Fires at the end of the session; turns the service off unless a new session started. */
    private val autoOff = object : Runnable {
        override fun run() {
            if (SessionStore.isActive(this@WebGuardService)) scheduleAutoOff() else turnSelfOff()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
        if (!SessionStore.isActive(this)) {
            turnSelfOff()
            return
        }
        // Switched back on after a payment: the reminder is no longer needed.
        SessionStore.setWebPausedForPayment(this, false)
        FocusService.cancelRestoreNotice(this)
        scheduleAutoOff()
    }

    private fun scheduleAutoOff() {
        handler.removeCallbacks(autoOff)
        handler.postDelayed(autoOff, SessionStore.remainingMillis(this) + AUTO_OFF_SLACK_MS)
    }

    private fun turnSelfOff() {
        handler.removeCallbacks(autoOff)
        instance = null
        disableSelf()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        handler.removeCallbacks(autoOff)
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in Browsers.URL_BAR_IDS) return
        if (!SessionStore.isActive(this)) {
            turnSelfOff() // safety net: never stay on after a session
            return
        }

        // Pages fire many content-change events while loading or scrolling; check a few times a second.
        val now = SystemClock.uptimeMillis()
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            now - lastCheckAt < CONTENT_THROTTLE_MS
        ) return
        lastCheckAt = now

        val domains = Websites.domainsFor(SessionStore.blockedPackages(this))
        if (domains.isEmpty()) return

        val root = try {
            rootInActiveWindow
        } catch (e: RuntimeException) {
            null
        } ?: return
        if (root.packageName?.toString() != pkg) return

        // Can't read the address bar: nothing to decide (and no evidence either way).
        val addressBar = Browsers.readAddressBar(root, pkg) ?: return
        val domain = Websites.hostOf(addressBar)?.let { Websites.matchingDomain(it, domains) }
        if (domain == null) {
            // Start page, blank tab or any other site: whatever we did last time worked.
            escapes.onSafePage(pkg)
            return
        }

        if (now - lastBlockAt < RELAUNCH_COOLDOWN_MS) return
        lastBlockAt = now

        // Order matters: move the browser off the site first, then cover it.
        val step = escapes.escape(
            browser = pkg,
            tryHomeButton = { tapHomeButton(root, pkg) },
            tryBlankTab = { openBlankTab(pkg) },
        )
        showBlockScreen(domain, backToBrowser = step != Step.HOME_SCREEN)
    }

    /**
     * Step 1: tap the browser's own Home / New tab button so the silenced page is
     * replaced by its normal start page (search box, shortcuts). False if there's no
     * such button on this browser / phone, or anything about it goes wrong.
     */
    private fun tapHomeButton(root: AccessibilityNodeInfo, browserPackage: String): Boolean =
        try {
            Browsers.openStartPage(root, browserPackage)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Home button tap failed", e)
            false
        }

    /**
     * Step 2: switch the browser to about:blank. EXTRA_APPLICATION_ID makes browsers
     * reuse one "Focus-On" tab rather than opening a new one each time.
     * False if the browser refuses the request.
     */
    private fun openBlankTab(browserPackage: String): Boolean {
        val blank = Intent(Intent.ACTION_VIEW, Uri.parse(BLANK_PAGE))
            .setPackage(browserPackage)
            .putExtra(Browser.EXTRA_APPLICATION_ID, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        return try {
            startActivity(blank)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not switch the browser to a blank tab", e)
            false
        }
    }

    /**
     * [backToBrowser]: Back on the lock screen returns to the (now safe) browser.
     * False when nothing could move the browser off the site: then Back goes to the
     * home screen, so the user is never stuck in a lock-screen loop.
     */
    private fun showBlockScreen(domain: String, backToBrowser: Boolean) {
        val intent = Intent(this, BlockActivity::class.java)
            .putExtra(BlockActivity.EXTRA_LABEL, domain)
            .putExtra(BlockActivity.EXTRA_BACK_TO_BROWSER, backToBrowser)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not show block screen", e)
        }
    }

    override fun onInterrupt() = Unit

    companion object {
        @Volatile
        private var instance: WeakReference<WebGuardService>? = null

        /**
         * Switches website blocking off right now (session ended or paused). Apps can only
         * turn an accessibility service OFF; turning it on is always the user's choice.
         */
        fun isRunning(): Boolean = instance?.get() != null

        fun turnOff() {
            val service = instance?.get() ?: return
            service.handler.post { service.turnSelfOff() }
        }

        private const val TAG = "WebGuardService"
        private const val CONTENT_THROTTLE_MS = 300L
        private const val RELAUNCH_COOLDOWN_MS = 1_500L
        private const val AUTO_OFF_SLACK_MS = 1_000L

        /** Local, empty, works offline, and every tested browser accepts it from another app. */
        private const val BLANK_PAGE = "about:blank"
    }
}
