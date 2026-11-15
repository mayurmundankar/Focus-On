package com.focuslock.app.data

/**
 * Websites that are the same service as an installed app, so silencing the
 * Instagram app also silences instagram.com in a browser. Pure Kotlin, unit-tested.
 */
object Websites {

    /** App package → the domains of the same service (subdomains included automatically). */
    private val APP_DOMAINS: Map<String, List<String>> = mapOf(
        "com.instagram.android" to listOf("instagram.com"),
        "com.instagram.barcelona" to listOf("threads.net", "threads.com"),
        "com.google.android.youtube" to listOf("youtube.com", "youtu.be"),
        "com.google.android.apps.youtube.music" to listOf("music.youtube.com"),
        "com.facebook.katana" to listOf("facebook.com", "fb.com", "fb.watch"),
        "com.facebook.lite" to listOf("facebook.com", "fb.com", "fb.watch"),
        "com.facebook.orca" to listOf("messenger.com"),
        "com.zhiliaoapp.musically" to listOf("tiktok.com"),
        "com.ss.android.ugc.trill" to listOf("tiktok.com"),
        "com.twitter.android" to listOf("x.com", "twitter.com"),
        "com.reddit.frontpage" to listOf("reddit.com", "redd.it"),
        "com.snapchat.android" to listOf("snapchat.com"),
        "com.linkedin.android" to listOf("linkedin.com"),
        "com.pinterest" to listOf("pinterest.com", "pin.it"),
        "com.netflix.mediaclient" to listOf("netflix.com"),
        "in.startv.hotstar" to listOf("hotstar.com"),
        "com.amazon.avod.thirdpartyclient" to listOf("primevideo.com"),
        "tv.twitch.android.app" to listOf("twitch.tv"),
        "com.discord" to listOf("discord.com"),
        "com.whatsapp" to listOf("web.whatsapp.com"),
        "org.telegram.messenger" to listOf("web.telegram.org"),
        "com.spotify.music" to listOf("open.spotify.com"),
        "com.quora.android" to listOf("quora.com"),
        "com.tumblr" to listOf("tumblr.com"),
        "com.vkontakte.android" to listOf("vk.com"),
    )

    /** All domains to block for these silenced packages. */
    fun domainsFor(packages: Set<String>): Set<String> =
        packages.flatMapTo(mutableSetOf()) { APP_DOMAINS[it].orEmpty() }

    /**
     * Host from whatever a browser's address bar shows:
     * "https://www.instagram.com/reels/x" or "instagram.com/reels" → "www.instagram.com" / "instagram.com".
     * Returns null for search text ("instagram login") or anything that isn't an address.
     */
    fun hostOf(addressBarText: String): String? {
        var s = addressBarText.trim().lowercase()
        if (s.isEmpty() || s.any { it.isWhitespace() }) return null
        s = s.substringAfter("://")
        s = s.substringBefore('/').substringBefore('?').substringBefore('#')
        s = s.substringAfterLast('@') // user:pass@host
        s = s.substringBefore(':') // port
        s = s.trimEnd('.')
        if (!s.contains('.') || s.startsWith('.')) return null
        if (!s.all { it.isLetterOrDigit() || it == '.' || it == '-' }) return null
        return s
    }

    /**
     * The blocked domain this host belongs to, or null. Exact label matching:
     * "m.instagram.com" → "instagram.com", but "notinstagram.com" and
     * "instagram.com.evil.net" → null.
     */
    fun matchingDomain(host: String, domains: Set<String>): String? =
        domains.firstOrNull { host == it || host.endsWith(".$it") }
}
