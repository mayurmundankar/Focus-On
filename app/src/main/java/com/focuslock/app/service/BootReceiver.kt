package com.focuslock.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focuslock.app.data.SessionStore

/** Restarting the phone (or updating the app) shouldn't end a session early. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (SessionStore.isActive(context)) {
                    FocusService.start(context)
                } else {
                    SessionStore.clearSession(context)
                    WebGuardService.turnOff()
                }
            }
        }
    }
}
