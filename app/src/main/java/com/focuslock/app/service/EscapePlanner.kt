package com.focuslock.app.service

/**
 * Decides how to get a browser off a silenced website so the browser stays usable,
 * and escalates when a method turns out not to work on this phone.
 *
 * Ladder, per browser:
 *  1. HOME_BUTTON  tap the browser's own Home / New tab button → its start page
 *  2. BLANK_TAB    open about:blank in the browser
 *  3. HOME_SCREEN  nothing worked: Back from the lock screen goes to the phone's
 *                  home screen (never a loop that traps the user in the browser)
 *
 * A method "worked" only if the browser is next seen on a page that isn't silenced
 * ([onSafePage]). Seeing the silenced site again means the last method failed, so the
 * next call starts one step further down. Pure Kotlin so every path is unit-tested.
 */
class EscapePlanner {

    enum class Step { HOME_BUTTON, BLANK_TAB, HOME_SCREEN }

    private val lastUsed = mutableMapOf<String, Step>()

    /** The browser is showing a page that isn't silenced: whatever we did last worked. */
    fun onSafePage(browser: String) {
        lastUsed.remove(browser)
    }

    /**
     * Gets [browser] off a silenced site. Each `try…` returns false when that method
     * isn't available right now (no button, intent refused), and the next one is tried.
     * Returns the step that was used.
     */
    fun escape(
        browser: String,
        tryHomeButton: () -> Boolean,
        tryBlankTab: () -> Boolean,
    ): Step {
        val start = when (lastUsed[browser]) {
            null -> Step.HOME_BUTTON
            Step.HOME_BUTTON -> Step.BLANK_TAB // Home didn't take us off the site
            Step.BLANK_TAB -> Step.HOME_SCREEN // neither did the blank tab
            Step.HOME_SCREEN -> Step.HOME_BUTTON // fresh attempt next time the browser opens
        }
        val used = when {
            start <= Step.HOME_BUTTON && tryHomeButton() -> Step.HOME_BUTTON
            start <= Step.BLANK_TAB && tryBlankTab() -> Step.BLANK_TAB
            else -> Step.HOME_SCREEN
        }
        lastUsed[browser] = used
        return used
    }
}
