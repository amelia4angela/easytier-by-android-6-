package com.easytier

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Helper for system notification bar messages.
 *
 * Two channels:
 *   [CHANNEL_VPN_STATUS] — persistent low-priority notification while VPN is running
 *   [CHANNEL_EVENTS] — normal-priority event notifications (connected / disconnected / error)
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_VPN_STATUS = "easytier_vpn_status"
        const val CHANNEL_EVENTS = "easytier_vpn_events"
        const val NOTIFY_ID_VPN = 1001
    }

    /** Create notification channels (safe to call multiple times — Android ignores duplicates) */
    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_VPN_STATUS,
                    "EasyTier VPN 状态",
                    NotificationManager.IMPORTANCE_LOW   // No sound, shows in shade
                ).apply {
                    description = "VPN 运行状态通知"
                    setShowBadge(false)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_EVENTS,
                    "EasyTier VPN 事件",
                    NotificationManager.IMPORTANCE_DEFAULT  // Sound + heads-up
                ).apply {
                    description = "VPN 连接/断开/错误事件"
                }
            )
        }
    }

    // ── Persistent VPN status notification ──

    /**
     * Build the ongoing notification shown while the VPN tunnel is active.
     * Uses [android.R.drawable.ic_lock_idle_lock] as the icon so it plays nice
     * with the system VPN key icon in the status bar.
     */
    fun buildVpnNotification(
        ip: String?,
        durationText: String,
        isRunning: Boolean,
        langZh: Boolean
    ): Notification {
        val title = if (langZh) {
            if (isRunning) "EasyTier VPN · 运行中" else "EasyTier VPN"
        } else {
            if (isRunning) "EasyTier VPN · Running" else "EasyTier VPN"
        }
        val content = if (isRunning) {
            val ipPart = if (ip != null) "IP: $ip" else (if (langZh) "获取中…" else "Connecting…")
            "$ipPart | $durationText"
        } else {
            if (langZh) "已断开" else "Disconnected"
        }

        val pi = PendingIntent.getActivity(
            context, 0,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_VPN_STATUS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pi)
            .setOngoing(isRunning)          // Not dismissible when VPN is active
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /** Post or update the persistent VPN notification. */
    fun showVpnNotification(ip: String?, durationText: String, isRunning: Boolean, langZh: Boolean) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFY_ID_VPN, buildVpnNotification(ip, durationText, isRunning, langZh))
    }

    /** Cancel the persistent VPN notification. */
    fun cancelVpnNotification() {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFY_ID_VPN)
        } catch (_: Exception) { }
    }

    // ── Event notifications (one-shot, auto-cancel) ──

    /** Show a one-shot event notification (auto-cancels on tap). */
    fun showEventNotification(title: String, text: String) {
        val pi = PendingIntent.getActivity(
            context, 0,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Unique ID each time so they don't overwrite each other
            nm.notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: Exception) { }
    }
}
