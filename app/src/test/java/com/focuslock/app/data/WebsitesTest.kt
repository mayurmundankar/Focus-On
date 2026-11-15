package com.focuslock.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebsitesTest {

    private val insta = setOf("instagram.com")

    @Test
    fun hostOf_readsWhatBrowsersShow() {
        assertEquals("instagram.com", Websites.hostOf("instagram.com"))
        assertEquals("instagram.com", Websites.hostOf("instagram.com/reels/abc"))
        assertEquals("www.instagram.com", Websites.hostOf("https://www.instagram.com/?hl=en"))
        assertEquals("m.youtube.com", Websites.hostOf("  HTTPS://M.YouTube.com/watch?v=1  "))
        assertEquals("instagram.com", Websites.hostOf("http://user:pw@instagram.com:443/x"))
        assertEquals("instagram.com", Websites.hostOf("instagram.com./"))
    }

    @Test
    fun hostOf_ignoresSearchTextAndJunk() {
        assertNull(Websites.hostOf("instagram login"))
        assertNull(Websites.hostOf("instagram"))
        assertNull(Websites.hostOf(""))
        assertNull(Websites.hostOf("Search or type URL"))
        assertNull(Websites.hostOf("about:blank"))
        assertNull(Websites.hostOf(".com"))
    }

    @Test
    fun matching_coversSubdomains() {
        assertEquals("instagram.com", Websites.matchingDomain("instagram.com", insta))
        assertEquals("instagram.com", Websites.matchingDomain("www.instagram.com", insta))
        assertEquals("instagram.com", Websites.matchingDomain("m.instagram.com", insta))
    }

    @Test
    fun matching_rejectsLookalikes() {
        assertNull(Websites.matchingDomain("notinstagram.com", insta))
        assertNull(Websites.matchingDomain("instagram.com.evil.net", insta))
        assertNull(Websites.matchingDomain("instagram.co", insta))
        assertNull(Websites.matchingDomain("google.com", insta))
    }

    @Test
    fun domainsFor_mapsSilencedApps() {
        val d = Websites.domainsFor(setOf("com.instagram.android", "com.google.android.youtube", "unknown.app"))
        assertEquals(setOf("instagram.com", "youtube.com", "youtu.be"), d)
        assertTrue(Websites.domainsFor(emptySet()).isEmpty())
    }

    @Test
    fun endToEnd_searchResultsAreNotBlocked() {
        // Googling "instagram.com" shows google.com in the address bar, which stays open.
        val host = Websites.hostOf("google.com/search?q=instagram.com")!!
        assertNull(Websites.matchingDomain(host, insta))
    }
}
