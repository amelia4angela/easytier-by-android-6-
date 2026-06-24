package com.easytier.jni

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.easytier.AppLogger
import com.easytier.vpn.EasyTierVpnService
import org.json.JSONObject

/** 解析 IPv4 int32 为字符串 */
internal fun ipv4IntToString(addr: Long): String {
    return "${(addr shr 24) and 0xFF}.${(addr shr 16) and 0xFF}.${(addr shr 8) and 0xFF}.${addr and 0xFF}"
}

/**
 * EasyTier 管理类 — 单例，使用 applicationContext，不受 Activity 生命周期影响。
 *
 * 这是 后台保活 的核心：
 * - 单例通过 companion object 持有，Activity 被销毁后管理器继续运行
 * - 使用 applicationContext 启动/停止 VpnService，不依赖 Activity
 * - 监控循环在主线程 Handler 上持续运行
 * - Activity 重新创建时通过 getInstance().isRunning 重建 UI 连接
 * - 只有用户主动点击"停止"或调 stop() 才会真正停止 VPN
 */
class EasyTierManager private constructor(
    private val appContext: Context
) {
    companion object {
        private const val TAG = "EasyTierManager"
        private const val MONITOR_INTERVAL = 3000L

        @Volatile
        private var instance: EasyTierManager? = null

        /** 初始化单例 — 建议在 Application.onCreate() 或 Activity.onCreate() 调用一次 */
        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        val appCtx = context.applicationContext
                        instance = EasyTierManager(appCtx)
                        AppLogger.i(TAG, "EasyTierManager singleton initialized")
                    }
                }
            }
        }

        /** 获取单例 — 未初始化时抛出异常 */
        fun getInstance(): EasyTierManager {
            return instance ?: throw IllegalStateException("EasyTierManager not initialized. Call init() first.")
        }

        /** 是否正在运行 — 可被 Activity 静态检查 */
        @JvmStatic
        val isRunning: Boolean
            get() = instance?.isRunningInternal ?: false
    }

    // ── 实例状态 ──
    private var currentInstanceName: String = ""
    private var currentNoTunMode: Boolean = false

    @Volatile
    private var _isRunning = false
    val isRunningInternal: Boolean get() = _isRunning

    private var currentIpv4: String? = null
        private set
    private var currentProxyCidrs: List<String> = emptyList()
        private set

    private val handler = Handler(Looper.getMainLooper())

    // ── Activity 回调（Activity 在 onResume/onPause 时设置/清除） ──
    /** 当监控到 IP/CIDR 变化时回调 — Activity 设置后接收网络就绪通知 */
    var onNetworkChanged: ((ipv4: String, cidrs: List<String>) -> Unit)? = null
    /** 状态更新回调 — 给 Activity 实时更新 UI */
    var onStatusUpdate: ((status: String) -> Unit)? = null
    /** 停止回调 — Activity 清理 UI */
    var onStopped: (() -> Unit)? = null
    /** VPN 被外部中断回调（其他 VPN 应用启动时触发） */
    var onVpnRevoked: (() -> Unit)? = null

    // ── 监控循环 ──
    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (_isRunning) {
                doMonitorNetworkStatus()
                handler.postDelayed(this, MONITOR_INTERVAL)
            }
        }
    }

    // ── 公开 API ──

    /**
     * 启动 EasyTier 实例和监控
     * @param instanceName 实例名称
     * @param networkConfig TOML 网络配置
     * @param noTunMode 是否无 TUN 模式（仅运行网络层，不启动 VPN）
     */
    fun start(instanceName: String, networkConfig: String, noTunMode: Boolean = false) {
        if (_isRunning) {
            AppLogger.w(TAG, "EasyTier is already running")
            return
        }

        currentInstanceName = instanceName
        currentNoTunMode = noTunMode

        try {
            val result = EasyTierJNI.runNetworkInstance(networkConfig)
            if (result == 0) {
                _isRunning = true
                AppLogger.i(TAG, "EasyTier instance started: $instanceName")
                onStatusUpdate?.invoke("Starting...")
                handler.post(monitorRunnable)
            } else {
                val error = EasyTierJNI.getLastError()
                AppLogger.e(TAG, "runNetworkInstance failed: result=$result error=$error")
                onStatusUpdate?.invoke("Start failed: $error")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception in start", e)
            onStatusUpdate?.invoke("Error: ${e.message}")
        }
    }

    /**
     * 停止 EasyTier 实例和监控
     * 这是停止 VPN 的唯一途径 — Activity 销毁不会调用此方法
     */
    fun stop() {
        if (!_isRunning) {
            AppLogger.w(TAG, "EasyTier is not running")
            return
        }

        _isRunning = false

        // 1. 停止监控
        handler.removeCallbacks(monitorRunnable)

        try {
            // 2. 关闭 TUN fd 先于 stopService（v66 官方顺序）
            try { EasyTierVpnService.self?.onRevoke() } catch (_: Exception) { }

            // 3. 停止 VpnService
            stopVpnService()

            // 4. 停止 Rust 实例
            EasyTierJNI.stopAllInstances()
            AppLogger.i(TAG, "EasyTier instance stopped: $currentInstanceName")

            // 5. 重置状态
            currentIpv4 = null
            currentProxyCidrs = emptyList()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception in stop", e)
        }

        onStopped?.invoke()
    }

    // ── VpnService 生命周期 ──

    /** 重启 VpnService（IP/CIDR 变化时） */
    fun restartVpnService(ipv4: String, proxyCidrs: List<String>) {
        try {
            if (currentNoTunMode) {
                AppLogger.i(TAG, "No-TUN mode, skipping VpnService restart")
                return
            }
            stopVpnService()
            startVpnService(ipv4, proxyCidrs)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception in restartVpnService", e)
        }
    }

    /** 启动 VpnService — 使用 applicationContext */
    @Synchronized
    fun startVpnService(ipv4: String, proxyCidrs: List<String>) {
        try {
            val intent = Intent(appContext, EasyTierVpnService::class.java)
            intent.putExtra("ipv4_address", ipv4)
            intent.putStringArrayListExtra("proxy_cidrs", ArrayList(proxyCidrs))
            intent.putExtra("instance_name", currentInstanceName)
            appContext.startService(intent)
            AppLogger.i(TAG, "VpnService started - IPv4: $ipv4, CIDRs: $proxyCidrs")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception in startVpnService", e)
        }
    }

    /** 停止 VpnService — 使用 applicationContext */
    @Synchronized
    private fun stopVpnService() {
        try {
            val intent = Intent(appContext, EasyTierVpnService::class.java)
            appContext.stopService(intent)
            AppLogger.i(TAG, "VpnService stopped")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception in stopVpnService", e)
        }
    }

    // ── 监控 ──

    private fun doMonitorNetworkStatus() {
        try {
            val infosJson = EasyTierJNI.collectNetworkInfos(10)
            if (infosJson.isNullOrEmpty()) {
                AppLogger.d(TAG, "No network info yet")
                return
            }

            val root = JSONObject(infosJson)
            val map = root.optJSONObject("map") ?: run {
                AppLogger.d(TAG, "No 'map' in network info")
                return
            }
            val node = map.optJSONObject(currentInstanceName) ?: run {
                AppLogger.d(TAG, "Instance $currentInstanceName not found in network info")
                return
            }

            AppLogger.d(TAG, "Network info: $node")

            if (!node.optBoolean("running", false)) {
                val errMsg = node.optString("error_msg", "")
                AppLogger.w(TAG, "Instance not running: $errMsg")
                return
            }

            val myNodeInfo = node.optJSONObject("my_node_info") ?: return
            val vip = myNodeInfo.optJSONObject("virtual_ipv4") ?: return

            val addrObj = vip.optJSONObject("address") ?: return
            val addrLong = addrObj.optLong("addr", 0)
            if (addrLong == 0L) return
            val ipv4Addr = ipv4IntToString(addrLong)
            val netLen = vip.optInt("network_length", 24)
            val newIpv4 = "$ipv4Addr/$netLen"

            val newProxyCidrs = mutableListOf<String>()
            val routes = node.optJSONArray("routes")
            if (routes != null) {
                for (i in 0 until routes.length()) {
                    val route = routes.getJSONObject(i)
                    val cidrArr = route.optJSONArray("proxy_cidrs")
                    if (cidrArr != null) {
                        for (j in 0 until cidrArr.length()) {
                            newProxyCidrs.add(cidrArr.getString(j))
                        }
                    }
                }
            }

            val ipv4Changed = newIpv4 != currentIpv4
            val cidrsChanged = newProxyCidrs != currentProxyCidrs

            if (ipv4Changed || cidrsChanged) {
                AppLogger.i(TAG, "Network status changed:")
                AppLogger.i(TAG, "  IPv4: $currentIpv4 -> $newIpv4")
                AppLogger.i(TAG, "  Proxy CIDRs: ${currentProxyCidrs.size} -> ${newProxyCidrs.size}")

                currentIpv4 = newIpv4
                currentProxyCidrs = newProxyCidrs.toList()

                onNetworkChanged?.invoke(newIpv4, newProxyCidrs)
            } else {
                AppLogger.d(TAG, "No network change - IPv4: $currentIpv4, CIDRs: ${currentProxyCidrs.size}")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception in monitorNetworkStatus", e)
        }
    }

    /** 获取当前状态 */
    fun getStatus(): EasyTierStatus {
        return EasyTierStatus(
            isRunning = _isRunning,
            instanceName = currentInstanceName,
            currentIpv4 = currentIpv4,
            currentProxyCidrs = currentProxyCidrs.toList()
        )
    }

    /**
     * Called by EasyTierVpnService.onRevoke() when another VPN app takes over.
     * Only fires the onVpnRevoked callback if _isRunning is still true
     * (the user did NOT intentionally stop via stop()).
     */
    fun notifyExternalRevoke() {
        if (_isRunning) {
            AppLogger.w(TAG, "External VPN revoke detected — another app took over")
            _isRunning = false
            handler.removeCallbacks(monitorRunnable)

            // Clean up the Rust instance and VPN service completely
            // so the next start() is a fresh, clean launch
            try { EasyTierVpnService.self?.onRevoke() } catch (_: Exception) { }
            stopVpnService()
            EasyTierJNI.stopAllInstances()

            currentIpv4 = null
            currentProxyCidrs = emptyList()

            onVpnRevoked?.invoke()
        } else {
            AppLogger.d(TAG, "notifyExternalRevoke skipped — user stopped intentionally")
        }
    }
}

/** 状态数据类 */
data class EasyTierStatus(
    val isRunning: Boolean,
    val instanceName: String,
    val currentIpv4: String?,
    val currentProxyCidrs: List<String>
)
