package com.focuslock.app.service

import com.focuslock.app.service.EscapePlanner.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EscapePlannerTest {

    private val chrome = "com.android.chrome"
    private val brave = "com.brave.browser"

    /** Records which methods were attempted. */
    private class Attempts(var home: Boolean = true, var blank: Boolean = true) {
        val tried = mutableListOf<String>()
        val homeFn = { tried += "home"; home }
        val blankFn = { tried += "blank"; blank }
    }

    @Test
    fun homeButtonIsTheFirstChoice() {
        val p = EscapePlanner()
        val a = Attempts()
        assertEquals(Step.HOME_BUTTON, p.escape(chrome, a.homeFn, a.blankFn))
        assertEquals(listOf("home"), a.tried)
    }

    @Test
    fun noHomeButton_fallsBackToBlankTabImmediately() {
        val p = EscapePlanner()
        val a = Attempts(home = false)
        assertEquals(Step.BLANK_TAB, p.escape(chrome, a.homeFn, a.blankFn))
        assertEquals(listOf("home", "blank"), a.tried)
    }

    @Test
    fun nothingAvailable_backGoesToHomeScreen() {
        val p = EscapePlanner()
        val a = Attempts(home = false, blank = false)
        assertEquals(Step.HOME_SCREEN, p.escape(chrome, a.homeFn, a.blankFn))
    }

    @Test
    fun homeTapThatDidNotNavigate_escalatesToBlankTab() {
        val p = EscapePlanner()
        val a = Attempts()
        p.escape(chrome, a.homeFn, a.blankFn) // tapped Home, but…
        // …the browser is seen on the silenced site again (no onSafePage in between)
        a.tried.clear()
        assertEquals(Step.BLANK_TAB, p.escape(chrome, a.homeFn, a.blankFn))
        assertEquals("must not retry the Home button that just failed", listOf("blank"), a.tried)
    }

    @Test
    fun blankTabThatDidNotWork_givesUpToHomeScreen_thenRetriesFresh() {
        val p = EscapePlanner()
        val a = Attempts()
        p.escape(chrome, a.homeFn, a.blankFn) // HOME_BUTTON (failed)
        p.escape(chrome, a.homeFn, a.blankFn) // BLANK_TAB (failed)
        a.tried.clear()
        assertEquals(Step.HOME_SCREEN, p.escape(chrome, a.homeFn, a.blankFn))
        assertFalse("no browser action once both failed", a.tried.isNotEmpty())
        // Next time the browser is opened, start again from the top.
        assertEquals(Step.HOME_BUTTON, p.escape(chrome, a.homeFn, a.blankFn))
    }

    @Test
    fun safePageResetsTheLadder() {
        val p = EscapePlanner()
        val a = Attempts()
        p.escape(chrome, a.homeFn, a.blankFn) // HOME_BUTTON
        p.onSafePage(chrome)                  // it worked: start page shown
        assertEquals(Step.HOME_BUTTON, p.escape(chrome, a.homeFn, a.blankFn))
    }

    @Test
    fun browsersAreTrackedSeparately() {
        val p = EscapePlanner()
        val a = Attempts()
        p.escape(chrome, a.homeFn, a.blankFn) // Chrome: HOME_BUTTON, still on site
        assertEquals(Step.HOME_BUTTON, p.escape(brave, a.homeFn, a.blankFn))
        assertEquals(Step.BLANK_TAB, p.escape(chrome, a.homeFn, a.blankFn))
    }
}
