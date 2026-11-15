package com.focuslock.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.focuslock.app.MainActivity
import com.focuslock.app.R
import com.focuslock.app.block.BlockActivity
import com.focuslock.app.data.PaymentApps
import com.focuslock.app.data.ProtectedApps
import com.focuslock.app.data.SessionStore
import com.focuslock.app.util.Permissions
import java.util.Date

/**
 * The heart of the app. While a session is active it:
 *  1. shows an ongoing countdown notification (so Android keeps it alive),
 *  2. checks every 500 ms which app is in front, using UsageStatsManager,
 *  3. opens [BlockActivity] on top whenever a blocked app comes to the front,
 *  4. switches website blocking off the moment a payment or bank app opens
 *     (they refuse to pay while an accessibility service is on),
 *  5. stops itself when the timer runs out.
 */
class FocusService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usageStats: UsageStatsManager
    private lateinit var power: PowerManager

    private var lastQueryTime = 0L
    private var foregroundPackage: String? = null
    private var lastBlockShownAt = 0L

    /** Phone, in-call and emergency apps: never covered. Looked up once per service start. */
    private var protectedPackages: Set<String> = emptySet()

    /** UPI, wallet and bank apps. Looked up once per service start. */
    private var paymentPackages: Set<String> = emptySet()

    /** The "turn website blocking back on" notice was already shown for the last payment. */
    private var restoreNoticeShown = false

    private val poll = object : Runnable {
        override fun run() {
            if (!SessionStore.isActive(this@FocusService)) {
                onSessionFinished()
                return
            }
            // No point checking while the screen is off.
            if (power.isInteractive) checkForegroundApp()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        usageStats = getSystemService(UsageStatsManager::class.java)
        power = getSystemService(PowerManager::class.java)
        createNotificationChannels()
        protectedPackages = ProtectedApps.packages(this)
        paymentPackages = PaymentApps.packages(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android requires startForeground() soon after startForegroundService(), so do it first.
        goForeground()

        if (!SessionStore.isActive(this)) {
            stopEverything()
            return START_NOT_STICKY
        }
        if (lastQueryTime == 0L) {
            lastQueryTime = System.currentTimeMillis() - INITIAL_LOOKBACK_MS
        }
        handler.removeCallbacks(poll)
        handler.post(poll)
        // If Android kills us mid-session, ask it to restart the service.
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        super.onDestroy()
    }

    // ---------------------------------------------------------------- detection

    private fun checkForegroundApp() {
        val now = System.currentTimeMillis()
        // A small overlap means we never miss an event that landed right at the boundary.
        val events = usageStats.queryEvents(lastQueryTime - QUERY_OVERLAP_MS, now) ?: return
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == EVENT_ACTIVITY_RESUMED) {
                foregroundPackage = event.packageName
            }
        }
        lastQueryTime = now

        val current = foregroundPackage ?: return
        handlePaymentApps(current)
        // Calls always get through: never cover a ringing or active call (phone or app
        // call), and never cover the dialer / emergency apps, even in older sessions.
        if (ProtectedApps.isCallActive(this) || current in protectedPackages) return
        if (current in SessionStore.blockedPackages(this) &&
            now - lastBlockShownAt > RELAUNCH_COOLDOWN_MS
        ) {
            lastBlockShownAt = now
            showBlockScreen(current)
        }
    }

    /**
     * Payments must just work. When a payment or bank app comes to the front, website
     * blocking switches itself off before the user reaches the payment screen. Android
     * lets only the user switch an accessibility service back on, so once they leave the
     * payment app we post a notice that does it in one tap.
     *
     * A payment app the user chose to silence is covered by the lock screen instead and
     * changes nothing (otherwise opening it would be a way round website blocking).
     */
    private fun handlePaymentApps(current: String) {
        if (current in paymentPackages && current !in SessionStore.blockedPackages(this)) {
            if (WebGuardService.isRunning()) {
                WebGuardService.turnOff()
                SessionStore.setWebPausedForPayment(this, true)
                restoreNoticeShown = false
            }
            return
        }
        if (!restoreNoticeShown &&
            current != packageName &&
            SessionStore.webPausedForPayment(this) &&
            !WebGuardService.isRunning()
        ) {
            restoreNoticeShown = true
            notifyRestoreWebGuard()
        }
    }

    private fun showBlockScreen(blockedPackage: String) {
        val intent = Intent(this, BlockActivity::class.java)
            .putExtra(BlockActivity.EXTRA_PACKAGE, blockedPackage)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        try {
            // Allowed from the background because the user granted "Display over other apps".
            startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not show block screen", e)
        }
    }

    // ---------------------------------------------------------------- lifecycle

    private fun onSessionFinished() {
        SessionStore.clearSession(this)
        // Website blocking must not outlive the session: payment apps refuse to work while it's on.
        WebGuardService.turnOff()
        cancelRestoreNotice(this)
        notifySessionComplete()
        stopEverything()
    }

    private fun stopEverything() {
        handler.removeCallbacks(poll)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ---------------------------------------------------------------- notifications

    private fun goForeground() {
        val notification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID_ONGOING,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID_ONGOING, notification)
        }
    }

    private fun buildOngoingNotification(): Notification {
        val endTime = SessionStore.endTime(this)
        val count = SessionStore.blockedPackages(this).size
        val endsAt = DateFormat.getTimeFormat(this).format(Date(endTime))
        return NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setColor(0xFFC9A96E.toInt())
            .setContentTitle("In focus")
            .setContentText("$count ${if (count == 1) "app" else "apps"} silenced until $endsAt")
            // Live countdown in the notification without us having to update it.
            .setWhen(endTime)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppIntent())
            .build()
    }

    private fun notifySessionComplete() {
        if (!Permissions.hasNotificationPermission(this)) return
        val notification = NotificationCompat.Builder(this, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setColor(0xFFC9A96E.toInt())
            .setContentTitle("Focus complete.")
            .setContentText("You kept your word. Your apps are back.")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID_DONE, notification)
    }

    /** After a payment: one tap opens Accessibility settings to switch website blocking back on. */
    private fun notifyRestoreWebGuard() {
        if (!Permissions.hasNotificationPermission(this)) return
        val settings = PendingIntent.getActivity(
            this,
            1,
            Permissions.accessibilityIntent(this),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_WEB)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setColor(0xFFC9A96E.toInt())
            .setContentTitle("Website blocking paused for your payment")
            .setContentText("Tap to switch it back on. Your apps are still silenced.")
            .setAutoCancel(true)
            .setTimeoutAfter(SessionStore.remainingMillis(this).coerceAtLeast(1L))
            .setContentIntent(settings)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID_RESTORE_WEB, notification)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING, "In focus", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Countdown shown while you focus" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DONE, "Focus complete", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Tells you when your silenced apps are back" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_WEB, "Website blocking", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Offers to switch website blocking back on after a payment" }
        )
    }

    companion object {
        private const val TAG = "FocusService"

        private const val POLL_INTERVAL_MS = 500L
        private const val QUERY_OVERLAP_MS = 2_000L
        private const val INITIAL_LOOKBACK_MS = 10_000L
        private const val RELAUNCH_COOLDOWN_MS = 1_500L

        /** UsageEvents.Event.ACTIVITY_RESUMED (API 29+), same value as the older MOVE_TO_FOREGROUND. */
        private const val EVENT_ACTIVITY_RESUMED = 1

        private const val CHANNEL_ONGOING = "focus_session"
        private const val CHANNEL_DONE = "focus_done"
        private const val NOTIFICATION_ID_ONGOING = 1
        private const val NOTIFICATION_ID_DONE = 2
        private const val CHANNEL_WEB = "web_blocking"
        private const val NOTIFICATION_ID_RESTORE_WEB = 3

        /** Removes the "switch website blocking back on" notice (session over, or it's back on). */
        fun cancelRestoreNotice(context: Context) {
            context.getSystemService(NotificationManager::class.java)
                ?.cancel(NOTIFICATION_ID_RESTORE_WEB)
        }

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context, Intent(context, FocusService::class.java)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not start FocusService", e)
            }
        }

        fun stop(context: Context) {
            cancelRestoreNotice(context)
            context.stopService(Intent(context, FocusService::class.java))
        }
    }
}
