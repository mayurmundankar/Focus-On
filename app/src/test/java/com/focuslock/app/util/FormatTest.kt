package com.focuslock.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun countdown_underAnHour_showsMinutesAndSeconds() {
        assertEquals("01:35", formatCountdown(95_000))
        assertEquals("59:59", formatCountdown(3_599_000))
    }

    @Test
    fun countdown_overAnHour_showsHours() {
        assertEquals("1:23:45", formatCountdown(5_025_000))
        assertEquals("1:00:00", formatCountdown(3_600_000))
        assertEquals("3:00:00", formatCountdown(180 * 60_000L))
    }

    @Test
    fun countdown_roundsPartialSecondsUp() {
        // 1 ms left must still read 00:01, not 00:00, so the screen never shows zero early.
        assertEquals("00:01", formatCountdown(1))
        assertEquals("00:02", formatCountdown(1_001))
    }

    @Test
    fun countdown_zeroOrNegative_showsZero() {
        assertEquals("00:00", formatCountdown(0))
        assertEquals("00:00", formatCountdown(-5_000))
    }

    @Test
    fun usage_screenTimeStyle() {
        assertEquals("<1m", formatUsage(0))
        assertEquals("<1m", formatUsage(59_999))
        assertEquals("1m", formatUsage(60_000))
        assertEquals("45m", formatUsage(45 * 60_000L))
        assertEquals("1h 00m", formatUsage(60 * 60_000L))
        assertEquals("2h 05m", formatUsage(7_500_000))
        assertEquals("18h 20m", formatUsage((18 * 60 + 20) * 60_000L))
    }

    @Test
    fun minutes_matchesDurationChips() {
        assertEquals("1 min", formatMinutes(1))
        assertEquals("45 min", formatMinutes(45))
        assertEquals("1 h", formatMinutes(60))
        assertEquals("1 h 30 min", formatMinutes(90))
        assertEquals("2 h", formatMinutes(120))
        assertEquals("3 h", formatMinutes(180))
    }
}
