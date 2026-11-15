package com.focuslock.app.service

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Android only sends events from the XML list; the code only reads bars from the Kotlin list. */
class BrowsersConfigTest {

    @Test
    fun accessibilityConfig_listsExactlyTheKnownBrowsers() {
        val xml = File("src/main/res/xml/web_guard_config.xml").readText()
        val listed = Regex("""android:packageNames="([^"]+)"""")
            .find(xml)!!.groupValues[1]
            .split(',').map(String::trim).toSet()
        assertEquals(Browsers.URL_BAR_IDS.keys, listed)
    }
}
