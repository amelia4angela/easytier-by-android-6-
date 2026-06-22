package com.easytier.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import com.easytier.AppLogger
import com.easytier.NotificationHelper
import com.easytier.jni.EasyTierJNI
import com.easytier.jni.EasyTierManager

/**
 * EasyTier VPN Service — exactly matching v66 working APK architecture.
 *
 * Key design decisions (from decompiled v66):
 * - Static volatile `self` for cross-component access (MainActivity checks self != null)
 * - onDestroy does NOT close vpnInterface — Android's VpnService.super.onDestroy() handles TUN teardown
 * - onRevoke closes fd + clears self (user manually disconnects in system settings)
 * - No isRunning flag, no cleanup() method — the system manages the lifecycle
 * - Returns START_STICKY on success (backwards compatible — original returns START_NOT_STICKY on error only)
 *
 * Notification (our addition):
 * - A persistent foreground notification is shown while the VPN tunnel is active
 * - Updated every 5s with current IP and running duration
 * - Cancelled onDestroy / onRevoke
 */
class EasyTierVpnService : VpnService() {

    companion object {
        private const val TAG = "VpnService"
        @Volatile var self: EasyTierVpnService? = null
        @Volatile var currentIp: String? = null
        /** Timestamp when the service was started (for duration calculation) */
        @Volatile var serviceStartedAt: Long = 0L
    }

    @Volatile private var vpnInterface: ParcelFileDescriptor? = null
    private var notificationHelper: NotificationHelper? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Update the persistent notification every 5 seconds with live IP + duration */
    private val notificationUpdateRunnable = object : Runnable {
        override fun run() {
            updateNotification()
            mainHandler.postDelayed(this, 5000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        self = this
        notificationHelper = NotificationHelper(this)
        notificationHelper?.createChannels()
        AppLogger.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ipv4Address = intent?.getStringExtra("ipv4_address") ?: run {
            AppLogger.e(TAG, "Missing ipv4_address")
            return START_NOT_STICKY
        }
        val proxyCidrs = intent?.getStringArrayListExtra("proxy_cidrs") ?: arrayListOf()
        val name = intent?.getStringExtra("instance_name") ?: "default"

        AppLogger.i(TAG, "onStartCommand: ipv4=$ipv4Address, instance=$name")

        try {
            val builder = Builder()
            builder.setMtu(1400)

            val parts = ipv4Address.split("/")
            builder.addAddress(parts[0], if (parts.size > 1) parts[1].toInt() else 24)

            for (cidr in proxyCidrs) {
                val c = parseCidr(cidr) ?: continue
                builder.addRoute(c.first, c.second)
            }
            builder.addRoute("10.0.0.0", 8)
            builder.addRoute("172.16.0.0", 12)
            builder.addRoute("192.168.0.0", 16)
            builder.setSession(name)

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                AppLogger.e(TAG, "VPN establish returned null")
                return START_NOT_STICKY
            }

            val fd = vpnInterface!!.fd
            AppLogger.i(TAG, "setTunFd name=$name fd=$fd")
            val result = EasyTierJNI.setTunFd(name, fd)
            if (result == 0) {
                AppLogger.i(TAG, "TUN fd set: fd=$fd")
            } else {
                val err = EasyTierJNI.getLastError()
                AppLogger.e(TAG, "setTunFd failed: result=$result error=$err")
            }

            // ── Foreground notification ──
            currentIp = parts[0]
            serviceStartedAt = System.currentTimeMillis()
            startForeground(
                NotificationHelper.NOTIFY_ID_VPN,
                notificationHelper!!.buildVpnNotification(
                    currentIp, "0s", isRunning = true, langZh = true
                )
            )
            // Start periodic notification update
            mainHandler.post(notificationUpdateRunnable)

        } catch (t: Throwable) {
            AppLogger.e(TAG, "onStartCommand exception", t)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        AppLogger.d(TAG, "onDestroy")
        // Stop notification updates
        mainHandler.removeCallbacks(notificationUpdateRunnable)
        // Cancel the persistent notification
        notificationHelper?.cancelVpnNotification()
        self = null
        // Android VpnService.super.onDestroy() tears down the TUN internally.
        // Do NOT manually close vpnInterface here — that's what onRevoke is for.
        super.onDestroy()
    }

    override fun onRevoke() {
        AppLogger.d(TAG, "onRevoke: closing fd")
        mainHandler.removeCallbacks(notificationUpdateRunnable)
        notificationHelper?.cancelVpnNotification()
        try { vpnInterface?.close() } catch (_: Exception) { }
        vpnInterface = null
        self = null

        // Notify EasyTierManager about external revoke (another VPN app took over).
        // If the user intentionally stopped, _isRunning is already false,
        // so notifyExternalRevoke() won't trigger the interrupted callback.
        try {
            EasyTierManager.getInstance().notifyExternalRevoke()
        } catch (_: Exception) { }

        super.onRevoke()
    }

    /** Update the persistent notification with current IP and duration. */
    private fun updateNotification() {
        try {
            val elapsed = if (serviceStartedAt > 0) {
                val ms = System.currentTimeMillis() - serviceStartedAt
                val sec = ms / 1000
                if (sec < 60) "${sec}s"
                else "${sec / 60}m ${sec % 60}s"
            } else "…"

            notificationHelper?.showVpnNotification(
                ip = currentIp,
                durationText = elapsed,
                isRunning = true,
                langZh = true
            )
        } catch (_: Exception) { }
    }

    /** Called by MainActivity to update the IP shown in the notification. */
    fun updateIp(newIp: String?) {
        currentIp = newIp
    }

    private fun parseCidr(cidr: String): Pair<String, Int>? {
        try {
            val p = cidr.split("/")
            return if (p.size == 2) Pair(p[0], p[1].toInt()) else null
        } catch (_: Exception) { return null }
    }
}
