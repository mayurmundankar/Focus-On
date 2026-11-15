package com.focuslock.app.service

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Browsers whose address bar Focus-On can read, and the view id of that bar.
 * Keep the package list identical to android:packageNames in res/xml/web_guard_config.xml:
 * Android only delivers events from those packages to the service.
 */
object Browsers {

    val URL_BAR_IDS: Map<String, List<String>> = mapOf(
        "com.android.chrome" to listOf("com.android.chrome:id/url_bar"),
        "com.chrome.beta" to listOf("com.chrome.beta:id/url_bar"),
        "com.chrome.dev" to listOf("com.chrome.dev:id/url_bar"),
        "com.chrome.canary" to listOf("com.chrome.canary:id/url_bar"),
        "com.brave.browser" to listOf("com.brave.browser:id/url_bar"),
        "com.brave.browser_beta" to listOf("com.brave.browser_beta:id/url_bar"),
        "com.microsoft.emmx" to listOf("com.microsoft.emmx:id/url_bar"),
        "com.vivaldi.browser" to listOf("com.vivaldi.browser:id/url_bar"),
        "com.kiwibrowser.browser" to listOf("com.kiwibrowser.browser:id/url_bar"),
        "org.mozilla.firefox" to listOf("org.mozilla.firefox:id/mozac_browser_toolbar_url_view"),
        "org.mozilla.firefox_beta" to listOf("org.mozilla.firefox_beta:id/mozac_browser_toolbar_url_view"),
        "org.mozilla.focus" to listOf("org.mozilla.focus:id/mozac_browser_toolbar_url_view"),
        "com.sec.android.app.sbrowser" to listOf("com.sec.android.app.sbrowser:id/location_bar_edit_text"),
        "com.opera.browser" to listOf("com.opera.browser:id/url_field"),
        "com.opera.mini.native" to listOf("com.opera.mini.native:id/url_field"),
        "com.duckduckgo.mobile.android" to listOf("com.duckduckgo.mobile.android:id/omnibarTextInput"),
        "com.mi.globalbrowser" to listOf("com.mi.globalbrowser:id/url"),
        "com.yandex.browser" to listOf("com.yandex.browser:id/bro_omnibar_address_title_text"),
    )

    /**
     * Buttons that take the current tab to the browser's start page (its new-tab page:
     * search box and shortcuts), in order of preference. Checked on Chrome, Brave and Edge.
     *  - home_button / bottom_home_button: Chromium "Open the homepage" (top or bottom bar)
     *  - edge_bottom_bar_plus_button: Edge's "Add New tab" (Edge has no Home button)
     */
    private val START_PAGE_BUTTON_IDS = listOf(
        "home_button",
        "bottom_home_button",
        "edge_bottom_bar_plus_button",
    )

    /**
     * Taps the browser's own Home / New tab button so the user lands on its normal start
     * page. Returns false if no such button is on screen (then the caller falls back).
     */
    fun openStartPage(root: AccessibilityNodeInfo, packageName: String): Boolean {
        for (suffix in START_PAGE_BUTTON_IDS) {
            val node = root.findAccessibilityNodeInfosByViewId("$packageName:id/$suffix")
                .firstOrNull { it.isVisibleToUser && it.isEnabled }
                ?: continue
            val target = clickableSelfOrParent(node) ?: continue
            if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        }
        return false
    }

    private fun clickableSelfOrParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 3) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    /** Words that address-bar view ids contain, for browsers that rename their ids. */
    private val URL_ID_HINTS = listOf("url", "address", "location", "omnibar")
    private const val MAX_NODES = 300

    /** The address bar text, or null if it can't be found. */
    fun readAddressBar(root: AccessibilityNodeInfo, packageName: String): String? {
        URL_BAR_IDS[packageName].orEmpty().forEach { id ->
            root.findAccessibilityNodeInfosByViewId(id)
                .firstNotNullOfOrNull { it.text?.toString()?.takeIf(String::isNotBlank) }
                ?.let { return it }
        }
        return findByIdHint(root)
    }

    /** Fallback: breadth-first search for a text node whose view id looks like an address bar. */
    private fun findByIdHint(root: AccessibilityNodeInfo): String? {
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var seen = 0
        while (queue.isNotEmpty() && seen < MAX_NODES) {
            val node = queue.removeFirst()
            seen++
            val id = node.viewIdResourceName?.lowercase()
            val text = node.text?.toString()
            // Skip empty containers like Chromium's "location_bar" and keep looking.
            if (id != null && !text.isNullOrBlank() && URL_ID_HINTS.any { it in id }) return text
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return null
    }
}
