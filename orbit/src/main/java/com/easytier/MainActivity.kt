package com.easytier

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.OpenableColumns
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import com.easytier.ui.theme.currentColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.easytier.AppLogger
import com.easytier.jni.EasyTierJNI
import com.easytier.jni.EasyTierManager
import com.easytier.jni.ipv4IntToString
import com.easytier.ui.*
import com.easytier.ui.theme.EasyTierTheme
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Main activity using Jetpack Compose UI.
 * All business logic (start/stop VPN, config management, peer info)
 * is preserved unchanged. Only the UI rendering layer is migrated.
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "easytier_prefs"
        private const val PREFS_STATE = "easytier_state"
        private const val PREFS_LANG = "easytier_lang"
        private const val PREFS_THEME = "easytier_theme"
        private const val REQUEST_IMPORT_TOML = 1001
        private const val REQUEST_EDIT_CONFIG = 1003
        private const val REQUEST_STORAGE = 2000
        private const val REQUEST_NOTIFICATION = 2001

        /** Sentinel value meaning "user hasn't filled this field yet" */
    }

    // ── Core State ──
    private var isRunning = false
    // manager is now a singleton — manager field removed (后台保活)
    private var daemonLostSince: Long = 0
    private var startedAt by mutableStateOf(0L)
    private var peerStatsTime: Long = 0
    private val mainHandler = Handler(Looper.getMainLooper())
    private var notificationHelper: NotificationHelper? = null
    private val prevPeerStats = HashMap<Int, Pair<Long, Long>>()

    // ── Compose-Observed State ──
    private var langZh by mutableStateOf(true)
    private var isDarkTheme by mutableStateOf(false)
    private var configState by mutableStateOf(ConfigState())
    private var connectionState by mutableStateOf(ConnectionButtonState())
    private var myNodeItem by mutableStateOf<PeerInfoItem?>(null)
    private var peerItems by mutableStateOf(listOf<PeerInfoItem>())
    private var pollingActive = false
    // Log dialog state
    private var showLogDialogState by mutableStateOf(false)
    private var peerInfoExpanded by mutableStateOf(false)
    private var currentConfigName by mutableStateOf("default")
    private var configChanged by mutableStateOf(false)   // tracks unsaved changes
    private var appVersion by mutableStateOf("0.1.3")    // read from PackageManager

    // ── Permission Launchers (modern ActivityResult, works all API levels) ──
    private lateinit var vpnPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private lateinit var storageManageLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private lateinit var batteryOptimizationLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    // ═══════════════════════════════════════════════
    //  Lifecycle
    // ═══════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        langZh = getSharedPreferences(PREFS_LANG, MODE_PRIVATE).getBoolean("lang_zh", true)
        isDarkTheme = getSharedPreferences(PREFS_THEME, MODE_PRIVATE).getBoolean("dark_theme", false)

        // ➕ 恢复上次使用的配置名，启动时自动加载对应的配置
        val statePrefs = getSharedPreferences(PREFS_STATE, MODE_PRIVATE)
        val savedName = statePrefs.getString("last_config_name", "default") ?: "default"
        currentConfigName = savedName

        // ➕ 从 PackageManager 读取真实版本号
        try {
            val pkgInfo = packageManager.getPackageInfo(packageName, 0)
            appVersion = pkgInfo.versionName ?: "0.1.3"
        } catch (_: Exception) { }

        // Initialize singleton manager — survives activity destroy (后台保活)
        EasyTierManager.init(this)

        migrateOldConfigs()
        restoreState()

        // ── Permission Launchers ──
        vpnPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                AppLogger.i("MainActivity", "VPN permission granted (launcher)")
                val mgr = EasyTierManager.getInstance()
                val status = mgr.getStatus()
                val ip = status.currentIpv4
                val cidrs = status.currentProxyCidrs
                if (ip != null) {
                    mgr.restartVpnService(ip, cidrs)
                    startPolling()
                    updateConnectionState(
                        ConnectionStatus.RUNNING,
                        subLabel = tr("运行中", "Running")
                    )
                    notificationHelper?.showEventNotification(
                        tr("Orbit 已连接", "Orbit Connected"),
                        tr("IPv4: $ip | 正在运行", "IPv4: $ip | Running")
                    )
                }
            } else {
                AppLogger.w("MainActivity", "VPN permission denied (launcher)")
                updateConnectionState(ConnectionStatus.ERROR,
                    subLabel = tr("VPN 权限被拒绝", "VPN permission denied"))
                isRunning = false
                getSharedPreferences(PREFS_STATE, MODE_PRIVATE).edit()
                    .putBoolean("is_running", false).apply()
                EasyTierManager.getInstance().stop()
                notificationHelper?.showEventNotification(
                    tr("Orbit 启动失败", "Orbit Failed"),
                    tr("VPN 权限被拒绝", "VPN permission denied")
                )
            }
        }

        // Launcher for MANAGE_EXTERNAL_STORAGE settings page (API 30+)
        storageManageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // After returning from system settings → re-check permission
            if (Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()) {
                AppLogger.i("MainActivity", "MANAGE_EXTERNAL_STORAGE granted")
                // Re-sync configs now that we have access
                syncConfigsToExternal()
                Toast.makeText(this,
                    tr("已获得文件访问权限", "File access permission granted"),
                    Toast.LENGTH_SHORT).show()
            }
        }

        // Launcher for battery optimization exemption (后台保活)
        batteryOptimizationLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // User returned from system settings — check if now exempted
            if (isIgnoringBatteryOptimizations()) {
                AppLogger.i("MainActivity", "Battery optimization exemption granted")
                getSharedPreferences(PREFS_STATE, MODE_PRIVATE).edit()
                    .putBoolean("battery_opt_exempted", true).apply()
                Toast.makeText(this,
                    tr("已获得后台运行权限", "Background running permission granted"),
                    Toast.LENGTH_SHORT).show()
            } else {
                AppLogger.w("MainActivity", "Battery optimization exemption NOT granted")
            }
        }

        // ➕ 权限引导：首次启动解释各权限用途，用户同意后才请求
        showPermissionIntroDialogIfNeeded()

        notificationHelper = NotificationHelper(this)
        notificationHelper?.createChannels()

        setContent {
            EasyTierTheme(darkTheme = isDarkTheme) {
                MainScreen(
                    connectionState = connectionState,
                    onToggleConnection = {
                        try {
                            if (connectionState.status == ConnectionStatus.REVOKED) {
                                startEasyTier()
                            } else if (isRunning) {
                                stopEasyTier()
                            } else {
                                startEasyTier()
                            }
                        } catch (t: Throwable) {
                            Toast.makeText(this@MainActivity,
                                if (langZh) "操作失败: ${t.message}" else "Failed: ${t.message}",
                                Toast.LENGTH_LONG).show()
                        }
                    },
                    config = configState,
                    onConfigChange = { configState = it; configChanged = true },
                    currentConfigName = currentConfigName,
                    langZh = langZh,
                    onConfigNameClick = { showConfigManagerDialog() },
                    onSaveConfig = { promptSaveAs() },
                    onAutoSaveConfig = { autoSaveCurrentConfig() },
                    onManageConfigs = { showConfigManagerDialog() },
                    onImportToml = { launchImportToml() },
                    onExportToml = { exportConfigAsToml() },
                    onDeleteConfig = { showDeleteConfigDialog() },
                    myNodeItem = myNodeItem,
                    peerItems = peerItems,
                    onConfigServer = { promptConfigServer() },
                    onToggleLanguage = { toggleLanguage() },
                    onLaunchConfigEditor = { launchConfigEditor() },
                    onViewLogs = { showLogDialogState = true },
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { toggleTheme() },
                    connectionStartTime = startedAt
                )

                // Log dialog — Compose version with auto-scroll + pause
                if (showLogDialogState) {
                    val colors = currentColors()
                    var logContent by remember { mutableStateOf(AppLogger.getLatestLines(120)) }
                    val scrollState = rememberScrollState()
                    val coroutineScope = rememberCoroutineScope()
                    var paused by remember { mutableStateOf(false) }

                    // Auto-scroll to bottom on open, then auto-refresh every 2s
                    LaunchedEffect(showLogDialogState) {
                        // Small delay to let layout settle, then scroll to bottom
                        delay(100)
                        scrollState.scrollTo(scrollState.maxValue)
                        // Auto-refresh loop
                        while (isActive) {
                            delay(2000)
                            val newLog = AppLogger.getLatestLines(120)
                            if (newLog != logContent) {
                                logContent = newLog
                                if (!paused) {
                                    // Scroll to bottom to follow latest
                                    scrollState.scrollTo(scrollState.maxValue)
                                }
                            }
                        }
                    }

                    Dialog(
                        onDismissRequest = { showLogDialogState = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.surface)
                                .border(0.5.dp, colors.surfaceBorder, RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Title row: icon + title + pause indicator
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "≡ ${tr("日志", "Log")}",
                                        color = colors.textPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (paused) {
                                        Text(
                                            "⏸ ${tr("已暂停", "Paused")}",
                                            color = colors.warning,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))

                                // Log content
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(400.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSystemInDarkTheme()) Color(0xFF1A1A2E) else Color(0xFFF5F3FF))
                                        .border(0.5.dp, colors.inputBorder, RoundedCornerShape(12.dp))
                                        .verticalScroll(scrollState)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        logContent,
                                        color = colors.textSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                // Action buttons: Pause/Resume | Clear | Close
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Pause / Resume
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (paused) colors.accent else colors.warningBg)
                                            .clickable {
                                                paused = !paused
                                                if (!paused) {
                                                    // Resumed: refresh + scroll to bottom immediately
                                                    logContent = AppLogger.getLatestLines(120)
                                                    coroutineScope.launch {
                                                        scrollState.scrollTo(scrollState.maxValue)
                                                    }
                                                }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (paused) "▶ ${tr("继续", "Resume")}"
                                            else "⏸ ${tr("暂停", "Pause")}",
                                            color = if (paused) Color.White else colors.warning,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    // Clear
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.dangerBg)
                                            .clickable {
                                                AppLogger.clear()
                                                logContent = ""
                                                coroutineScope.launch {
                                                    scrollState.scrollTo(0)
                                                }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "✕ ${tr("清除", "Clear")}",
                                            color = colors.dangerText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    // Close
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.accentGradient)
                                            .clickable { showLogDialogState = false }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            tr("关闭", "Close"),
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Restore running state — check singleton first (后台保活) */
    private fun restoreState() {
        val statePrefs = getSharedPreferences(PREFS_STATE, MODE_PRIVATE)

        // If singleton manager is still running (survived Activity destroy), reconnect UI
        if (EasyTierManager.isRunning) {
            AppLogger.i("MainActivity", "Manager singleton is running — reconnecting UI")
            isRunning = true
            startedAt = System.currentTimeMillis()
            val status = EasyTierManager.getInstance().getStatus()
            if (status.currentIpv4 != null) {
                updateConnectionState(ConnectionStatus.RUNNING,
                    subLabel = tr("运行中", "Running"))
                startPolling()
                notificationHelper?.showEventNotification(
                    tr("Orbit 已连接", "Orbit Connected"),
                    tr("IPv4: ${status.currentIpv4} | 正在运行", "IPv4: ${status.currentIpv4} | Running")
                )
            } else {
                updateConnectionState(ConnectionStatus.STARTING,
                    subLabel = tr("正在启动…", "Starting…"))
            }
            statePrefs.edit().putBoolean("is_running", true).apply()
            return
        }

        val wasRunning = statePrefs.getBoolean("is_running", false)
        if (wasRunning) {
            AppLogger.i("MainActivity", "State says was running but manager gone → clean up")
            statePrefs.edit().putBoolean("is_running", false).apply()
        }
        // Restore configs from external if internal got wiped
        restoreFromExternalIfMissing()
        loadConfig(currentConfigName)
        syncConfigsToExternal()
    }

    private fun requestNotificationPermission() {
        // Android 13+ (API 33+): POST_NOTIFICATIONS needed for foreground service notification
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION
                )
            }
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            // Android 11+ (API 30+): need MANAGE_EXTERNAL_STORAGE for shared storage access
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                try {
                    storageManageLauncher.launch(intent)
                } catch (_: Exception) {
                    Toast.makeText(this,
                        tr("请在设置中授予「所有文件访问权限」以读写外置配置文件",
                            "Please grant 'All files access' in Settings for external configs"),
                        Toast.LENGTH_LONG).show()
                }
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            // Android 6–10 (API 23–29): runtime READ/WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ) {
                    Toast.makeText(this,
                        tr("需要存储权限以读写配置文件", "Storage permission needed for config files"),
                        Toast.LENGTH_LONG).show()
                }
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_STORAGE
                )
            }
        }
        // API < 23: no runtime permissions needed
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, tr("存储权限已获取", "Storage permission granted"), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, tr("未获取存储权限，部分功能受限", "No storage permission, some features limited"),
                    Toast.LENGTH_LONG).show()
            }
            return
        }
        if (requestCode == REQUEST_NOTIFICATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AppLogger.i("MainActivity", "POST_NOTIFICATIONS granted")
            } else {
                AppLogger.w("MainActivity", "POST_NOTIFICATIONS denied — foreground service notification hidden")
            }
        }
    }

    // ═══════════════════════════════════════════════
    //  Battery Optimization Exemption (后台保活)
    // ═══════════════════════════════════════════════

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    /**
     * Request exemption from battery optimization so the VPN service
     * is not killed by Doze / standby when the app is in the background.
     *
     * On Android 6–9 (API 23–28), this opens a system dialog asking the user
     * to allow background running. On API 29+, the dialog only shows for
     * specific app categories — we still attempt it silently.
     *
     * If already exempted, this is a no-op.
     */
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT < 23) return

        // Check if already exempted
        if (isIgnoringBatteryOptimizations()) return

        // Check if we already asked this launch
        val prefs = getSharedPreferences(PREFS_STATE, MODE_PRIVATE)
        if (prefs.getBoolean("battery_opt_exempted", false)) return

        AppLogger.i("MainActivity", "Requesting battery optimization exemption…")
        try {
            val intent = Intent(
                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
            batteryOptimizationLauncher.launch(intent)
        } catch (e: Exception) {
            AppLogger.w("MainActivity", "Cannot request battery opt exemption: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════
    //  Permission Intro Dialog (首次启动引导)
    // ═══════════════════════════════════════════════

    /**
     * Show a one-time dialog explaining what each permission is used for,
     * then — if the user agrees — request them. If skipped, show a warning.
     */
    private fun showPermissionIntroDialogIfNeeded() {
        val prefs = getSharedPreferences(PREFS_STATE, MODE_PRIVATE)
        if (prefs.getBoolean("permissions_intro_shown", false)) {
            // Already shown, silently request (only needed ones)
            requestAllPermissions()
            return
        }

        val zh = langZh
        val title = if (zh) "权限说明" else "Permissions Required"
        val message = if (zh) {
            "Orbit 需要以下权限才能正常工作：\n\n" +
            "🔒 VPN 权限\n" +
            "   创建虚拟网卡 (TUN) — 所有 VPN 应用都需要\n\n" +
            "💾 存储权限\n" +
            "   读写配置文件（保存、导入、导出配置）\n   如拒绝，配置将保存在应用内部存储区\n\n" +
            "🔔 通知权限\n" +
            "   显示 VPN 连接状态和事件通知\n   如拒绝，将没有通知提示\n\n" +
            "🔋 电池优化豁免\n" +
            "   允许 VPN 在后台持续运行不被系统杀死\n   如拒绝，退出应用后 VPN 可能中断\n\n" +
            "是否授予这些权限？"
        } else {
            "Orbit needs the following permissions:\n\n" +
            "🔒 VPN Permission\n" +
            "   Create virtual TUN adapter — required by all VPN apps\n\n" +
            "💾 Storage Permission\n" +
            "   Read/write config files (save, import, export)\n   If denied, configs are stored in internal app storage\n\n" +
            "🔔 Notification Permission\n" +
            "   Show VPN connection status and event notifications\n   If denied, no notification alerts\n\n" +
            "🔋 Battery Optimization Exemption\n" +
            "   Keep VPN running in the background\n   If denied, VPN may stop when app is closed\n\n" +
            "Grant these permissions?"
        }

        AlertDialog.Builder(this).apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton(if (zh) "同意并继续" else "Agree & Continue") { _, _ ->
                prefs.edit().putBoolean("permissions_intro_shown", true).apply()
                requestAllPermissions()
            }
            setNegativeButton(if (zh) "跳过" else "Skip") { _, _ ->
                prefs.edit().putBoolean("permissions_intro_shown", true).apply()
                val warning = if (zh)
                    "部分功能可能无法正常使用 — 可在「更多」页面随时开启权限"
                else
                    "Some features may not work — you can enable permissions later in Settings"
                Toast.makeText(this@MainActivity, warning, Toast.LENGTH_LONG).show()
            }
            setCancelable(false)
        }.show()
    }

    /** Request all runtime permissions in sequence (silent, no intro) */
    private fun requestAllPermissions() {
        requestStoragePermission()
        requestNotificationPermission()
        requestBatteryOptimizationExemption()
    }

    // ═══════════════════════════════════════════════
    //  Connection State Helpers
    // ═══════════════════════════════════════════════

    private fun updateConnectionState(
        status: ConnectionStatus,
        label: String? = null,
        subLabel: String? = null,
        protectionLevel: Int? = null
    ) {
        connectionState = connectionState.copy(
            status = status,
            label = label ?: tr(
                if (isRunning) "停止 VPN" else "启动 VPN",
                if (isRunning) "Stop VPN" else "Start VPN"
            ),
            subLabel = subLabel ?: when (status) {
                ConnectionStatus.READY -> tr("已停止", "Stopped")
                ConnectionStatus.RUNNING -> tr("运行中", "Running")
                ConnectionStatus.NO_TUN -> tr("运行中", "Running")
                ConnectionStatus.STOPPED -> tr("已停止", "Stopped")
                ConnectionStatus.STARTING -> tr("正在启动…", "Starting…")
                ConnectionStatus.ERROR -> tr("错误", "Error")
                ConnectionStatus.REVOKED -> tr("VPN 被中断", "VPN Interrupted")
            },
            protectionLevel = protectionLevel ?: connectionState.protectionLevel
        )
    }

    // ═══════════════════════════════════════════════
    //  Config Management (file-based)
    // ═══════════════════════════════════════════════

    private fun getConfigsDir(): File = File(filesDir, "configs").also { it.mkdirs() }

    /** External shared storage (/EasyTier/configs/) — survives uninstall */
    private fun getExternalConfigsDir(): File {
        return if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            // API 30+ without MANAGE_EXTERNAL_STORAGE → use internal fallback
            getConfigsDir()
        } else if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).parentFile,
                "EasyTier/configs"
            ).also { it.mkdirs() }
        } else getConfigsDir()
    }

    /** Copy all configs from internal to external storage */
    private fun syncConfigsToExternal() {
        try {
            val extDir = getExternalConfigsDir()
            getConfigsDir().listFiles()?.forEach { f ->
                if (f.extension == "json") {
                    val dest = File(extDir, f.name)
                    if (!dest.exists()) f.copyTo(dest, overwrite = true)
                }
            }
        } catch (t: Throwable) {
            AppLogger.w("MainActivity", "syncConfigsToExternal failed: ${t.message}")
        }
    }

    /** Restore configs from external storage if internal is empty */
    private fun restoreFromExternalIfMissing() {
        try {
            val intDir = getConfigsDir()
            if (intDir.listFiles()?.any { it.extension == "json" } == true) return
            val extDir = getExternalConfigsDir()
            val copied = extDir.listFiles()?.filter { it.extension == "json" }?.map { f ->
                f.copyTo(File(intDir, f.name), overwrite = true)
                f.name
            } ?: emptyList()
            if (copied.isNotEmpty()) {
                AppLogger.i("MainActivity", "Restored configs from external: ${copied.joinToString()}")
            }
        } catch (t: Throwable) {
            AppLogger.w("MainActivity", "restoreFromExternalIfMissing failed: ${t.message}")
        }
    }

    private fun listConfigFiles(): List<String> =
        getConfigsDir().listFiles()
            ?.filter { it.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?.sorted() ?: emptyList()

    private fun sanitizeName(name: String): String =
        name.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(64)

    /** Migrate old SharedPreferences configs to file storage */
    private fun migrateOldConfigs() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val configsDir = getConfigsDir()
        if (configsDir.listFiles()?.any { it.extension == "json" } == true) return

        var migrated = false
        for ((key, value) in prefs.all) {
            if (value is String && value.trimStart().startsWith("{")) {
                File(configsDir, "${sanitizeName(key)}.json").writeText(value)
                migrated = true
            }
        }
        if (migrated) {
            prefs.edit().clear().apply()
            AppLogger.i("MainActivity", "Configs migrated from SharedPreferences to files")
        }
    }

    private fun saveConfig(name: String) {
        val file = File(getConfigsDir(), "${sanitizeName(name)}.json")
        val json = TomlUtils.configStateToJson(configState).toString()
        file.writeText(json)
        // Also save to external storage (survives uninstall)
        try {
            val extFile = File(getExternalConfigsDir(), "${sanitizeName(name)}.json")
            extFile.writeText(json)
        } catch (t: Throwable) {
            AppLogger.w("MainActivity", "External config save failed: ${t.message}")
        }
        currentConfigName = name
        configChanged = false
        // Persist last config name so it auto-loads on next startup
        getSharedPreferences(PREFS_STATE, MODE_PRIVATE).edit()
            .putString("last_config_name", name).apply()
        Toast.makeText(this, tr("已保存: $name", "Saved: $name"), Toast.LENGTH_SHORT).show()
        AppLogger.i("MainActivity", "Config saved: $name (file)")
    }

    private fun loadConfig(name: String) {
        var file = File(getConfigsDir(), "${sanitizeName(name)}.json")
        // If internal is missing, try external and copy back
        if (!file.exists()) {
            val extFile = File(getExternalConfigsDir(), "${sanitizeName(name)}.json")
            if (extFile.exists()) {
                try {
                    extFile.copyTo(file, overwrite = true)
                    AppLogger.i("MainActivity", "Config restored from external: $name")
                } catch (_: Throwable) {}
            }
        }
        if (file.exists()) {
            try {
                configState = TomlUtils.jsonToConfigState(JSONObject(file.readText()))
                currentConfigName = name
                // Persist last config name so it survives app restart
                getSharedPreferences(PREFS_STATE, MODE_PRIVATE).edit()
                    .putString("last_config_name", name).apply()
                AppLogger.i("MainActivity", "Config loaded: $name")
            } catch (t: Throwable) {
                Toast.makeText(this,
                    tr("加载失败: ${t.message}", "Load error: ${t.message}"),
                    Toast.LENGTH_LONG).show()
                return
            }
        } else {
            resetToDefaults()
            currentConfigName = "default"
        }
    }

    private fun deleteConfig(name: String) {
        File(getConfigsDir(), "${sanitizeName(name)}.json").delete()
        // Also delete external copy
        try {
            File(getExternalConfigsDir(), "${sanitizeName(name)}.json").delete()
        } catch (_: Throwable) {}
        if (currentConfigName == name) {
            currentConfigName = "default"
            loadConfig("default")
        }
        Toast.makeText(this, tr("已删除: $name", "Deleted: $name"), Toast.LENGTH_SHORT).show()
    }

    private fun resetToDefaults() {
        configState = ConfigState(ipPrefix = "24")
    }

    // ── Dialogs ──

    private fun showConfigManagerDialog() {
        val names = listConfigFiles()
        val items = names.map { n ->
            if (n == currentConfigName) "$n ✓" else n
        }.toTypedArray()

        if (items.isEmpty()) {
            Toast.makeText(this, tr("暂无配置，请输入参数后保存", "No configs yet"), Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(tr("配置管理器", "Config Manager"))
            .setMessage(tr("当前: $currentConfigName", "Current: $currentConfigName"))
            .setItems(items) { _, which -> loadConfig(names[which]) }
            .setPositiveButton(tr("新建…", "New…")) { _, _ -> promptSaveAs() }
            .setNeutralButton(tr("删除", "Delete")) { _, _ -> showDeleteConfigDialog() }
            .setNegativeButton(tr("取消", "Cancel"), null)
            .show()
    }

    private fun showDeleteConfigDialog() {
        val names = listConfigFiles()
        if (names.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle(tr("选择要删除的配置", "Select config to delete"))
            .setItems(names.toTypedArray()) { _, which ->
                val name = names[which]
                if (name == currentConfigName && currentConfigName == "default") {
                    Toast.makeText(this, tr("默认配置不可删除", "Cannot delete default"),
                        Toast.LENGTH_SHORT).show()
                    return@setItems
                }
                AlertDialog.Builder(this)
                    .setTitle(tr("确认删除", "Confirm delete"))
                    .setMessage(tr("确定删除「$name」？", "Delete '$name'?"))
                    .setPositiveButton(tr("删除", "Delete")) { _, _ -> deleteConfig(name) }
                    .setNegativeButton(tr("取消", "Cancel"), null)
                    .show()
            }.setPositiveButton(tr("取消", "Cancel"), null).show()
    }

    private fun promptSaveAs() {
        val input = EditText(this)
        input.setSingleLine(true)
        input.hint = tr("输入新配置名称", "Enter config name")

        val existingNames = listConfigFiles()
        AlertDialog.Builder(this)
            .setTitle(tr("保存为新配置", "Save New Config"))
            .setView(input)
            .setPositiveButton(tr("保存", "Save")) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, tr("名称不能为空", "Name cannot be empty"),
                        Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (name in existingNames) {
                    AlertDialog.Builder(this)
                        .setTitle(tr("覆盖确认", "Overwrite?"))
                        .setMessage(tr("配置「$name」已存在，覆盖吗？", "Config '$name' exists. Overwrite?"))
                        .setPositiveButton(tr("覆盖", "Overwrite")) { _, _ -> saveConfig(name) }
                        .setNegativeButton(tr("取消", "Cancel"), null)
                        .show()
                } else {
                    saveConfig(name)
                }
            }
            .setNegativeButton(tr("取消", "Cancel"), null)
            .show()
    }

    /** Auto-save the current config with its existing name (no prompt) */
    private fun autoSaveCurrentConfig() {
        if (currentConfigName.isNotEmpty()) {
            saveConfig(currentConfigName)
        } else {
            promptSaveAs()
        }
    }

    private fun exportConfigAsToml() {
        val toml = TomlUtils.buildToml(configState)
        Intent(Intent.ACTION_SEND).also { intent ->
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, toml)
            intent.putExtra(Intent.EXTRA_SUBJECT, "Orbit Config: $currentConfigName")
            startActivity(Intent.createChooser(intent, tr("分享配置", "Share config")))
        }
    }

    private fun launchConfigEditor() {
        val intent = Intent(this, ConfigEditActivity::class.java).apply {
            putExtra(ConfigEditActivity.EXTRA_CURRENT_CONFIG, currentConfigName)
        }
        startActivityForResult(intent, REQUEST_EDIT_CONFIG)
    }

    private fun launchImportToml() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_TITLE,
            tr("选择配置文件 (TOML)", "Select config file (TOML)"))
        startActivityForResult(intent, REQUEST_IMPORT_TOML)
    }

    // ═══════════════════════════════════════════════
    //  Start / Stop — EasyTierManager
    // ═══════════════════════════════════════════════

    private fun startEasyTier() {
        try {
            AppLogger.i("MainActivity", "--- startEasyTier ---")

            // Validate required fields — don't leak private defaults
            val c = configState
            if (c.networkName.isBlank() || c.secret.isBlank() || c.server.isBlank() || c.port.isBlank()) {
                runOnUiThread {
                    Toast.makeText(this,
                        tr("请先填写完整的网络配置（网络名/密钥/服务器/端口）", "Fill in network name/secret/server/port first"),
                        Toast.LENGTH_LONG).show()
                }
                return
            }

            // Request battery optimization exemption for background keep-alive（后台保活）
            requestBatteryOptimizationExemption()

            // Stop if already running (singleton handles idempotency internally)
            if (EasyTierManager.isRunning) {
                EasyTierManager.getInstance().stop()
            }

            val config = TomlUtils.buildToml(configState)
            val noTun = configState.noTun

            val mgr = EasyTierManager.getInstance()
            // Set up callbacks — these fire on the monitor thread, post to UI thread
            mgr.onNetworkChanged = { ip, cidrs ->
                runOnUiThread {
                    if (!noTun) {
                        val vpnIntent = VpnService.prepare(this@MainActivity)
                        if (vpnIntent != null) {
                            AppLogger.i("MainActivity", "Requesting VPN permission…")
                            vpnPermissionLauncher.launch(vpnIntent)
                        } else {
                            mgr.restartVpnService(ip, cidrs)
                            startPolling()
                            updateConnectionState(
                                ConnectionStatus.RUNNING,
                                subLabel = tr("运行中", "Running")
                            )
                            notificationHelper?.showEventNotification(
                                tr("Orbit 已连接", "Orbit Connected"),
                                tr("IPv4: $ip | 正在运行", "IPv4: $ip | Running")
                            )
                        }
                    } else {
                        updateConnectionState(ConnectionStatus.NO_TUN,
                            subLabel = tr("运行中（无 TUN）", "Running (no TUN)"))
                    }
                }
            }
            mgr.onStatusUpdate = { status ->
                runOnUiThread {
                    updateConnectionState(connectionState.status, subLabel = status)
                }
            }
            mgr.onStopped = {
                runOnUiThread {
                    getSharedPreferences(PREFS_STATE, MODE_PRIVATE).edit()
                        .putBoolean("is_running", false).apply()
                    this@MainActivity.isRunning = false
                    myNodeItem = null
                    peerItems = emptyList()
                    stopPolling()
                    updateConnectionState(ConnectionStatus.STOPPED)
                }
            }
            mgr.onVpnRevoked = {
                runOnUiThread {
                    AppLogger.w("MainActivity", "VPN externally revoked by another app")
                    this@MainActivity.isRunning = true
                    stopPolling()
                    updateConnectionState(
                        ConnectionStatus.REVOKED,
                        subLabel = tr("VPN 被其他应用中断", "Interrupted by another VPN app")
                    )
                }
            }

            startedAt = System.currentTimeMillis()
            isRunning = true
            getSharedPreferences(PREFS_STATE, MODE_PRIVATE).edit()
                .putBoolean("is_running", true).apply()

            updateConnectionState(ConnectionStatus.STARTING,
                label = tr("停止 VPN", "Stop VPN"),
                subLabel = if (noTun) tr("运行中（无 TUN）", "Running (no TUN)")
                    else tr("正在启动…", "Starting…"))

            val startInstanceName = configState.instanceName.ifEmpty { "default" }
            mgr.start(startInstanceName, config, noTun)
        } catch (t: Throwable) {
            updateConnectionState(ConnectionStatus.ERROR,
                subLabel = tr("错误: ${t.message}", "Error: ${t.message}"))
            AppLogger.e("MainActivity", "Exception in startEasyTier", t)
            try {
                Toast.makeText(this,
                    tr("启动失败: ${t.message}", "Start failed: ${t.message}"),
                    Toast.LENGTH_LONG).show()
            } catch (_: Throwable) { }
        }
    }

    private fun stopEasyTier() {
        AppLogger.i("MainActivity", "--- stopEasyTier ---")
        isRunning = false
        mainHandler.removeCallbacksAndMessages(null)
        EasyTierManager.getInstance().stop()
        getSharedPreferences(PREFS_STATE, MODE_PRIVATE).edit()
            .putBoolean("is_running", false).apply()
        myNodeItem = null
        peerItems = emptyList()
        updateConnectionState(ConnectionStatus.STOPPED)
        notificationHelper?.showEventNotification(
            tr("Orbit 已断开", "Orbit Disconnected"),
            tr("VPN 服务已停止", "VPN service stopped")
        )
    }

    private var pollingJob: kotlinx.coroutines.Job? = null

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = lifecycleScope.launch(Dispatchers.Main) {
            while (isActive && this@MainActivity.isRunning) {
                refreshPeerInfo()
                delay(500L)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    // ═══════════════════════════════════════════════
    //  Peer Info → Compose State
    // ═══════════════════════════════════════════════

    private suspend fun refreshPeerInfo() {
        try {
            val infosJson = withContext(Dispatchers.IO) { EasyTierJNI.collectNetworkInfos(30) }
            if (infosJson.isNullOrEmpty()) { myNodeItem = null; peerItems = emptyList(); return }

            val map = JSONObject(infosJson as String).optJSONObject("map") ?: run {
                myNodeItem = null; peerItems = emptyList(); return
            }
            if (map.length() == 0) { myNodeItem = null; peerItems = emptyList(); return }

            val now = System.currentTimeMillis()
            val dt = if (peerStatsTime > 0) (now - peerStatsTime).toDouble() / 1000.0 else 0.0
            val natNames = arrayOf("Unknown", "OpenInternet", "NoPAT", "FullCone", "Restricted",
                "PortRestricted", "Symmetric", "SymUdpFirewall", "SymEasyInc", "SymEasyDec")

            var newMyNode: PeerInfoItem? = null
            val newPeers = mutableListOf<PeerInfoItem>()
            val names = map.names()

            // Build peer_id → instance_name mapping from outer map keys
            // route.hostname is the system hostname (="localhost" on Android),
            // so we use the config's instance_name instead.
            val peerIdToName = mutableMapOf<Int, String>()
            if (names != null) {
                for (ni in 0 until names.length()) {
                    val k = names.getString(ni)
                    val n = map.getJSONObject(k)
                    val ni2 = n.optJSONObject("my_node_info")
                    if (ni2 != null) {
                        val pid = ni2.optInt("peer_id", 0)
                        if (pid > 0) peerIdToName[pid] = k
                    }
                }
            }

            if (names != null) {
                for (ni in 0 until names.length()) {
                    val key = names.getString(ni)
                    val node = map.getJSONObject(key)
                    val nodeInfo = node.optJSONObject("my_node_info")

                    // ── My Node Info ──
                    if (nodeInfo != null) {
                        newMyNode = buildMyNodePeerInfo(nodeInfo, node, natNames, dt)
                        myNodeItem = newMyNode
                    }

                    // ── Peers ──
                    val routes = node.optJSONArray("routes")
                    val peers = node.optJSONArray("peers")
                    if (routes == null && peers == null) continue

                    val peerMap = HashMap<Int, JSONObject>()
                    if (peers != null) {
                        for (pi in 0 until peers.length()) {
                            val p = peers.getJSONObject(pi)
                            peerMap[p.optInt("peer_id")] = p
                        }
                    }

                    val myIps = mutableSetOf<String>()
                    if (nodeInfo != null) {
                        val vip = nodeInfo.optJSONObject("virtual_ipv4")
                        val addrObj = vip?.optJSONObject("address")
                        val addrLong = addrObj?.optLong("addr", 0) ?: 0L
                        if (addrLong > 0) myIps.add(ipv4IntToString(addrLong))
                    }

                    if (routes != null) {
                        for (ri in 0 until routes.length()) {
                            val route = routes.getJSONObject(ri)
                            val peerId = route.optInt("peer_id", 0)
                            val ipv4Obj = route.optJSONObject("ipv4_addr")

                            var peerIp = ""
                            if (ipv4Obj != null) {
                                val addrObj = ipv4Obj.optJSONObject("address")
                                val addrLong = addrObj?.optLong("addr", 0) ?: 0L
                                if (addrLong > 0) {
                                    val ip = ipv4IntToString(addrLong)
                                    if (myIps.contains(ip)) continue
                                    peerIp = "$ip/${ipv4Obj.optInt("network_length", 24)}"
                                }
                            }

                            val hostname = peerIdToName[peerId] ?: route.optString("hostname", "?")
                            val version = route.optString("version", "?")
                            val pathLatencyUs = route.optLong("path_latency", 0) * 1000L

                            val peerObj = peerMap[peerId]
                            var protocol = "?"
                            var latencyUs: Long = pathLatencyUs
                            var lossRate = 0.0
                            var rx = 0L; var tx = 0L

                            if (peerObj != null) {
                                val conns = peerObj.optJSONArray("conns")
                                if (conns != null && conns.length() > 0) {
                                    for (ci in 0 until conns.length()) {
                                        val conn = conns.getJSONObject(ci)
                                        if (ci == 0) {
                                            val tunnel = conn.optJSONObject("tunnel")
                                            protocol = tunnel?.optString("tunnel_type", "?") ?: "?"
                                            lossRate = conn.optDouble("loss_rate", 0.0)
                                        }
                                        val stats = conn.optJSONObject("stats")
                                        if (stats != null) {
                                            val slat = stats.optLong("latency_us", 0)
                                            if (slat > 0 && (ci == 0 || slat < latencyUs)) latencyUs = slat
                                            rx += stats.optLong("rx_bytes", 0)
                                            tx += stats.optLong("tx_bytes", 0)
                                        }
                                    }
                                }
                            }

                            var downSpeed = ""; var upSpeed = ""
                            if (dt > 0.5) {
                                val prev = prevPeerStats[peerId]
                                if (prev != null) {
                                    val rxDelta = rx - prev.first
                                    val txDelta = tx - prev.second
                                    if (rxDelta >= 0) downSpeed = formatSpeed((rxDelta / dt).toLong())
                                    if (txDelta >= 0) upSpeed = formatSpeed((txDelta / dt).toLong())
                                }
                                prevPeerStats[peerId] = Pair(rx, tx)
                            } else {
                                prevPeerStats[peerId] = Pair(rx, tx)
                            }

                            val cost = route.optInt("cost", 0)

                            // ── Detect P2P ──
                            // Primary: directly_connected_conns (authoritative, set by PeerManager)
                            val directlyConnected = peerObj?.optJSONArray("directly_connected_conns")
                            var peerHasDirectP2P = directlyConnected != null && directlyConnected.length() > 0
                            // Fallback: if peer has a UDP tunnel, it's definitely P2P (UDP-only for P2P)
                            if (!peerHasDirectP2P && peerObj != null && cost > 0) {
                                val pConns = peerObj.optJSONArray("conns")
                                if (pConns != null) {
                                    for (ci in 0 until pConns.length()) {
                                        val tunnel = pConns.getJSONObject(ci).optJSONObject("tunnel")
                                        if (tunnel?.optString("tunnel_type", "") == "udp") {
                                            peerHasDirectP2P = true
                                            break
                                        }
                                    }
                                }
                            }

                            // When P2P, find actual tunnel protocol (UDP/TCP) from the conn
                            var p2pProtocol = ""
                            if (peerHasDirectP2P && peerObj != null) {
                                val pConns = peerObj.optJSONArray("conns")
                                if (pConns != null) {
                                    for (ci in 0 until pConns.length()) {
                                        val tunnel = pConns.getJSONObject(ci).optJSONObject("tunnel")
                                        val tt = tunnel?.optString("tunnel_type", "")
                                        when (tt) {
                                            "udp" -> { p2pProtocol = "UDP"; break }
                                            "tcp" -> if (p2pProtocol.isEmpty()) p2pProtocol = "TCP"
                                        }
                                    }
                                }
                            }
                            // Override first-conn protocol with P2P protocol when available
                            if (peerHasDirectP2P && p2pProtocol.isNotEmpty()) {
                                protocol = p2pProtocol
                            }

                            val isRelayed = !peerHasDirectP2P && cost > 0

                            val routeCostLabel = when {
                                cost == 0 -> tr("本机", "Local")
                                peerHasDirectP2P -> ""
                                else -> "relay"
                            }

                            var natType = ""
                            val stunInfo = route.optJSONObject("stun_info")
                            if (stunInfo != null) {
                                val natIdx = stunInfo.optInt("udp_nat_type", 0)
                                if (natIdx in 0..9) natType = natNames[natIdx]
                            }

                            newPeers.add(PeerInfoItem(
                                isSelf = false,
                                ip = peerIp,
                                hostname = hostname,
                                version = version,
                                protocol = if (routeCostLabel.isEmpty()) protocol else routeCostLabel,
                                latencyUs = latencyUs,
                                lossRate = lossRate * 100.0,
                                rxBytes = rx,
                                txBytes = tx,
                                rxSpeed = downSpeed,
                                txSpeed = upSpeed,
                                natType = natType,
                                peerId = peerId,
                                isRelayed = isRelayed
                            ))
                        }
                    }
                }
            }

            // Clean up stale peer stats for disconnected peers
            val currentPeerIds = newPeers.map { it.peerId }.toSet()
            prevPeerStats.keys.removeAll { it !in currentPeerIds }

            peerStatsTime = now
            peerItems = newPeers

        } catch (t: Throwable) {
            AppLogger.e("MainActivity", "refreshPeerInfo error", t)
        }
    }

    /** Build PeerInfoItem for "my node" from JSON */
    private fun buildMyNodePeerInfo(
        nodeInfo: JSONObject, node: JSONObject, natNames: Array<String>, dt: Double
    ): PeerInfoItem {
        val vip = nodeInfo.optJSONObject("virtual_ipv4")
        var vipStr = "?"
        if (vip != null) {
            val addrObj = vip.optJSONObject("address")
            val addrLong = addrObj?.optLong("addr", 0) ?: 0L
            if (addrLong > 0) vipStr = "${ipv4IntToString(addrLong)}/${vip.optInt("network_length", 24)}"
        }

        val peerId = nodeInfo.optInt("peer_id", 0)
        val hostname = nodeInfo.optString("hostname", "?")
        val version = nodeInfo.optString("version", "?")
        val stunInfo = nodeInfo.optJSONObject("stun_info")
        val natIdx = stunInfo?.optInt("udp_nat_type", 0) ?: 0
        val natStr = if (natIdx in 0..9) natNames[natIdx] else ""

        var totalTx = 0L; var totalRx = 0L
        for ((_, v) in prevPeerStats) { totalTx += v.second; totalRx += v.first }
        val txRate = if (dt > 0.5) formatSpeed((totalTx / dt).toLong()) else ""
        val rxRate = if (dt > 0.5) formatSpeed((totalRx / dt).toLong()) else ""

        return PeerInfoItem(
            isSelf = true,
            ip = vipStr,
            hostname = hostname,
            version = version,
            protocol = "",
            latencyUs = 0,
            lossRate = 0.0,
            rxBytes = totalRx,
            txBytes = totalTx,
            rxSpeed = rxRate,
            txSpeed = txRate,
            natType = natStr,
            peerId = peerId,
            isRelayed = false
        )
    }

    // ═══════════════════════════════════════════════
    //  Activity Result
    // ═══════════════════════════════════════════════

    @Deprecated("Deprecated for API 34, keeping for minSdk 23 compat")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_EDIT_CONFIG) {
            if (resultCode == RESULT_OK && data != null) {
                val selected = data.getStringExtra(ConfigEditActivity.EXTRA_CONFIG_NAME)
                if (selected != null && selected != currentConfigName) {
                    loadConfig(selected)
                }
            }
            return
        }

        if (requestCode == REQUEST_IMPORT_TOML && resultCode == RESULT_OK && data?.data != null) {
            handleTomlImport(data.data!!)
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    // ═══════════════════════════════════════════════
    //  TOML Import
    // ═══════════════════════════════════════════════

    private fun handleTomlImport(uri: Uri) {
        try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            val fileName = cursor?.use {
                val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0 && it.moveToFirst()) it.getString(nameIdx) else "unknown.toml"
            } ?: "unknown.toml"

            val inputStream = contentResolver.openInputStream(uri) ?: run {
                Toast.makeText(this, tr("无法打开文件", "Cannot open file"), Toast.LENGTH_SHORT).show()
                return
            }
            val text = inputStream.bufferedReader().use { it.readText() }
            configState = TomlUtils.jsonToConfigState(TomlUtils.parseTomlToJson(text))
            val autoName = fileName.removeSuffix(".toml").removeSuffix(".TOML")
                .replace(Regex("[/\\\\:*?\"<>|]"), "_").take(64)
            saveConfig(autoName)
            Toast.makeText(this, tr("已导入并保存: $autoName", "Imported & saved: $autoName"),
                Toast.LENGTH_SHORT).show()
            AppLogger.i("MainActivity", "TOML import OK: $fileName")
        } catch (t: Throwable) {
            AppLogger.e("MainActivity", "TOML import error", t)
            Toast.makeText(this, tr("导入错误: ${t.message}", "Import error: ${t.message}"),
                Toast.LENGTH_LONG).show()
        }
    }

    private fun tr(zh: String, en: String): String = if (langZh) zh else en

    private fun toggleLanguage() {
        langZh = !langZh
        getSharedPreferences(PREFS_LANG, MODE_PRIVATE).edit().putBoolean("lang_zh", langZh).apply()
        Toast.makeText(this, if (langZh) "\u5df2\u5207\u6362\u4e3a\u4e2d\u6587" else "Switched to English",
            Toast.LENGTH_SHORT).show()
        AppLogger.i("MainActivity", "Language switched: ${if (langZh) "zh" else "en"}")
    }

    private fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        getSharedPreferences(PREFS_THEME, MODE_PRIVATE).edit().putBoolean("dark_theme", isDarkTheme).apply()
        AppLogger.i("MainActivity", "Theme switched: ${if (isDarkTheme) "dark" else "light"}")
    }

    // ═══════════════════════════════════════════════
    //  Config Server (from remote URL)
    // ═══════════════════════════════════════════════

    private fun promptConfigServer() {
        val input = EditText(this)
        input.setSingleLine(true)
        input.hint = if (langZh) "\u8f93\u5165\u914d\u7f6e\u670d\u52a1\u5668 URL" else "Enter config server URL"

        AlertDialog.Builder(this)
            .setTitle(tr("\u8fde\u63a5\u914d\u7f6e\u670d\u52a1\u5668", "Config Server"))
            .setView(input)
            .setPositiveButton(tr("\u83b7\u53d6", "Fetch")) { _: DialogInterface, _: Int ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) fetchConfigFromServer(url)
            }
            .setNegativeButton(tr("\u53d6\u6d88", "Cancel"), null)
            .show()
    }

    private fun fetchConfigFromServer(url: String) {
        Toast.makeText(this, tr("\u6b63\u5728\u83b7\u53d6\u2026", "Fetching\u2026"), Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                mainHandler.post {
                    configState = TomlUtils.jsonToConfigState(TomlUtils.parseTomlToJson(text))
                    saveConfig(currentConfigName)
                    Toast.makeText(this, tr("\u914d\u7f6e\u5df2\u52a0\u8f7d", "Config loaded"), Toast.LENGTH_SHORT).show()
                    AppLogger.i("MainActivity", "Config loaded from server: $url")
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    Toast.makeText(this, tr("\u83b7\u53d6\u5931\u8d25: ${t.message}", "Failed: ${t.message}"), Toast.LENGTH_LONG).show()
                    AppLogger.e("MainActivity", "Config server fetch error", t)
                }
            }
        }.start()
    }

    // ═══════════════════════════════════════════════
    //  Dialogs (kept as AlertDialog)
    // ═══════════════════════════════════════════════

    // ═══════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════

    private fun formatSpeed(bytesPerSec: Long): String = when {
        bytesPerSec >= 1_000_000 -> "%.1f MB".format(bytesPerSec / 1_000_000.0)
        bytesPerSec >= 1_000 -> "%.1f KB".format(bytesPerSec / 1_000.0)
        bytesPerSec >= 0 -> "${bytesPerSec} B"
        else -> "?"
    }
}
