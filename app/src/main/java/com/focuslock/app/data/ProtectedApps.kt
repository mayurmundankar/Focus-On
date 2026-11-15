package com.focuslock.app.data

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.telecom.TelecomManager

/**
 * Things Focus-On must never get in the way of, whatever the user selects.
 * No extra permissions needed for any of these checks.
 */
object ProtectedApps {

    /** Phone, in-call and emergency apps that exist on many phones. */
    private val KNOWN = setOf(
        "com.android.dialer",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.android.incallui",
        "com.android.server.telecom",
        "com.android.phone",
        "com.android.emergency",
        "com.google.android.apps.safetyhub",
        "com.android.cellbroadcastreceiver",
        "com.google.android.cellbroadcastreceiver",
    )

    /**
     * Packages that can never be silenced: the phone's dialer (an incoming call opens
     * it, and a lock screen on top would stop the user answering), in-call screens and
     * emergency apps.
     */
    fun packages(context: Context): Set<String> {
        val telecom = context.getSystemService(TelecomManager::class.java)
        return KNOWN + setOfNotNull(
            runCatching { telecom?.defaultDialerPackage }.getOrNull(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { telecom?.systemDialerPackage }.getOrNull()
            } else {
                null
            },
        )
    }

    /**
     * True while any call is ringing or in progress: phone calls and app calls
     * (WhatsApp, Meet, …). Nothing is blocked then, so calls always get through.
     */
    fun isCallActive(context: Context): Boolean {
        val mode = context.getSystemService(AudioManager::class.java)?.mode ?: return false
        return mode == AudioManager.MODE_RINGTONE ||
            mode == AudioManager.MODE_IN_CALL ||
            mode == AudioManager.MODE_IN_COMMUNICATION
    }
}
