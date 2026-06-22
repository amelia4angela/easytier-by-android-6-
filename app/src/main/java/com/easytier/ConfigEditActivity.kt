package com.easytier

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.easytier.ui.ConfigForm
import com.easytier.ui.ConfigState
import com.easytier.ui.theme.EasyTierTheme
import org.json.JSONObject
import java.io.File

/**
 * 配置编辑 Activity（Compose + Material 3）
 *
 * 对标官方 <RemoteManagement> 组件：配置列表 + 编辑器 + 导入导出
 *
 * 返回结果：
 *   RESULT_OK → intent 包含 EXTRA_CONFIG_NAME（用户选择的配置名）
 *   RESULT_CANCELED → 用户取消
 */
class ConfigEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CURRENT_CONFIG = "current_config_name"
        const val EXTRA_CONFIG_NAME = "selected_config_name"
        private const val REQUEST_IMPORT_TOML = 2001
    }

    private var currentConfigName = "default"
    private var langZh = true
    private var isDarkTheme = false

    // ── Lifecycle ──

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        langZh = getSharedPreferences("easytier_lang", MODE_PRIVATE)
            .getBoolean("lang_zh", true)
        isDarkTheme = getSharedPreferences("easytier_theme", MODE_PRIVATE)
            .getBoolean("dark_theme", false)
        currentConfigName = intent?.getStringExtra(EXTRA_CURRENT_CONFIG) ?: "default"

        // Load initial state from disk
        val initialJson = loadConfigFromDisk(currentConfigName)
        val initialConfig = jsonToConfigState(initialJson)

        setContent {
            EasyTierTheme(darkTheme = isDarkTheme) {
                ConfigEditScreen(
                    initialConfig = initialConfig,
                    currentConfigName = currentConfigName,
                    langZh = langZh,
                    onApply = { name -> onApplyConfig(name) },
                    onBack = { finish() },
                    onImport = { importToml() },
                    onNewConfig = { name ->
                        currentConfigName = name
                        saveConfigToDisk(name, jsonToConfigState(loadConfigFromDisk("default")))
                    },
                    onDeleteConfig = { name ->
                        deleteConfigFromDisk(name)
                        // After delete, load "default" if was current
                        if (currentConfigName == name) {
                            currentConfigName = "default"
                        }
                    },
                    onLoadConfig = { name ->
                        val json = loadConfigFromDisk(name)
                        jsonToConfigState(json)
                    },
                    onSaveConfig = { name, state ->
                        currentConfigName = name
                        saveConfigToDisk(name, state)
                    },
                    onExportToml = { state -> exportToml(state) }
                )
            }
        }
    }

    // ── ConfigEditScreen (Compose) ──

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ConfigEditScreen(
        initialConfig: ConfigState,
        currentConfigName: String,
        langZh: Boolean,
        onApply: (String) -> Unit,
        onBack: () -> Unit,
        onImport: () -> Unit,
        onNewConfig: (String) -> Unit,
        onDeleteConfig: (String) -> Unit,
        onLoadConfig: (String) -> ConfigState,
        onSaveConfig: (String, ConfigState) -> Unit,
        onExportToml: (ConfigState) -> Unit
    ) {
        val tr: (String, String) -> String = { zh, en -> if (langZh) zh else en }

        // ── State ──
        var configState by remember { mutableStateOf(initialConfig) }
        var configName by remember { mutableStateOf(currentConfigName) }
        var showConfigPicker by remember { mutableStateOf(false) }
        var showNewConfigDialog by remember { mutableStateOf(false) }
        var showDeleteConfirm by remember { mutableStateOf(false) }
        var showExportSheet by remember { mutableStateOf(false) }
        var exportTomlContent by remember { mutableStateOf("") }
        val configNames = remember { mutableStateListOf<String>() }
        // Refresh config list
        LaunchedEffect(configName) {
            configNames.clear()
            configNames.addAll(listConfigFiles())
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(tr("编辑网络", "Edit Network"), fontWeight = FontWeight.Normal) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = tr("返回", "Back")
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onImport() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.FileOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(tr("导入", "Import"), maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                exportTomlContent = buildTomlFromConfig(configState)
                                showExportSheet = true
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(tr("导出", "Export"), maxLines = 1)
                        }

                        Button(
                            onClick = {
                                onSaveConfig(configName, configState)
                                onApply(configName)
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(tr("应用", "Apply"), maxLines = 1)
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 8.dp)
            ) {
                ConfigForm(
                    config = configState,
                    langZh = langZh,
                    onConfigChange = { configState = it }
                )
            }
        }

        // ── Dialogs ──

        // Config Picker
        if (showConfigPicker) {
            AlertDialog(
                onDismissRequest = { showConfigPicker = false },
                title = { Text(tr("选择配置", "Select Config")) },
                text = {
                    if (configNames.isEmpty()) {
                        Text(tr("暂无已保存的配置", "No saved configs"))
                    } else {
                        Column {
                            configNames.forEach { name ->
                                val isCurrent = name == configName
                                TextButton(
                                    onClick = {
                                        // Auto-save before switching
                                        onSaveConfig(configName, configState)
                                        configState = onLoadConfig(name)
                                        configName = name
                                        showConfigPicker = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isCurrent) "$name ✓" else name,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showConfigPicker = false }) {
                        Text(tr("取消", "Cancel"))
                    }
                }
            )
        }

        // New Config Dialog
        if (showNewConfigDialog) {
            var newName by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showNewConfigDialog = false },
                title = { Text(tr("新建配置", "New Config")) },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(tr("配置名称", "Config name")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = newName.trim()
                        if (name.isEmpty()) {
                            Toast.makeText(
                                this@ConfigEditActivity,
                                tr("名称不能为空", "Name cannot be empty"),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@TextButton
                        }
                        // Save current first, then save as new
                        onSaveConfig(configName, configState)
                        onNewConfig(name)
                        configName = name
                        showNewConfigDialog = false
                    }) {
                        Text(tr("保存", "Save"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewConfigDialog = false }) {
                        Text(tr("取消", "Cancel"))
                    }
                }
            )
        }

        // Delete Confirm Dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(tr("删除配置", "Delete Config")) },
                text = {
                    Text(tr("确定删除「$configName」？", "Delete '$configName'?"))
                },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteConfig(configName)
                        configState = onLoadConfig("default")
                        configName = "default"
                        showDeleteConfirm = false
                        Toast.makeText(
                            this@ConfigEditActivity,
                            tr("已删除", "Deleted"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text(
                            tr("删除", "Delete"),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(tr("取消", "Cancel"))
                    }
                }
            )
        }

        // Export TOML Bottom Sheet
        if (showExportSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExportSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = tr("导出配置 (TOML)", "Export Config (TOML)"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = exportTomlContent,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, exportTomlContent)
                                putExtra(Intent.EXTRA_SUBJECT, "EasyTier Config: $configName")
                            }
                            startActivity(Intent.createChooser(intent,
                                tr("分享配置", "Share config")))
                            showExportSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(tr("分享", "Share"))
                    }
                }
            }
        }
    }

    // ── Apply ──

    private fun onApplyConfig(name: String) {
        val result = Intent()
        result.putExtra(EXTRA_CONFIG_NAME, name)
        setResult(RESULT_OK, result)
        finish()
    }

    // ── Config Persistence ──

    private fun getConfigsDir(): File {
        return File(filesDir, "configs").also { it.mkdirs() }
    }

    private fun getExternalConfigsDir(): File {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).parentFile,
                "EasyTier/configs"
            ).also { it.mkdirs() }
        } else getConfigsDir()
    }

    private fun sanitizeName(name: String): String {
        return name.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(64)
    }

    private fun listConfigFiles(): List<String> {
        return getConfigsDir().listFiles()
            ?.filter { it.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?.sorted() ?: emptyList()
    }

    private fun loadConfigFromDisk(name: String): JSONObject {
        var file = File(getConfigsDir(), "${sanitizeName(name)}.json")
        if (!file.exists()) {
            val extFile = File(getExternalConfigsDir(), "${sanitizeName(name)}.json")
            if (extFile.exists()) {
                try {
                    extFile.copyTo(file, overwrite = true)
                    AppLogger.i("ConfigEdit", "Config restored from external: $name")
                } catch (_: Throwable) {}
            }
        }
        return if (file.exists()) {
            try {
                JSONObject(file.readText())
            } catch (t: Throwable) {
                AppLogger.e("ConfigEdit", "Config parse error: $name", t)
                JSONObject()
            }
        } else {
            JSONObject()
        }
    }

    private fun jsonToConfigState(j: JSONObject): ConfigState {
        return ConfigState(
            networkName = j.optString("networkName"),
            secret = j.optString("secret"),
            server = j.optString("server"),
            port = j.optString("port"),
            ipAddress = j.optString("ip"),
            ipPrefix = j.optString("prefix").ifEmpty { "24" },
            instanceName = j.optString("instanceName"),
            hostname = j.optString("hostname"),
            dhcp = j.optBoolean("dhcp"),
            disableIpv6 = j.optBoolean("disableIpv6"),
            noTun = j.optBoolean("noTun"),
            bindDevice = j.optString("bindDevice"),
            disableP2p = j.optBoolean("disableP2p"),
            p2pOnly = j.optBoolean("p2pOnly"),
            lazyP2p = j.optBoolean("lazyP2p"),
            needP2p = j.optBoolean("needP2p"),
            disableTcpHp = j.optBoolean("disableTcpHp"),
            disableUdpHp = j.optBoolean("disableUdpHp"),
            disableSymHp = j.optBoolean("disableSymHp"),
            enableKcpProxy = j.optBoolean("enableKcpProxy"),
            disableKcpInput = j.optBoolean("disableKcpInput"),
            enableQuicProxy = j.optBoolean("enableQuicProxy"),
            disableQuicInput = j.optBoolean("disableQuicInput"),
            proxyForwardBySystem = j.optBoolean("proxyForwardBySystem"),
            relayAllRpc = j.optBoolean("relayAllRpc"),
            exitNode = j.optBoolean("exitNode"),
            latencyFirst = j.optBoolean("latencyFirst"),
            multiThread = j.optBoolean("multiThread"),
            useSmoltcp = j.optBoolean("useSmoltcp"),
            mtu = j.optString("mtu"),
            disableEncrypt = j.optBoolean("disableEncrypt"),
            privateMode = j.optBoolean("privateMode"),
            acceptDns = j.optBoolean("acceptDns"),
            socks5 = j.optString("socks5"),
            portForward = j.optString("portForward"),
            encryptAlgo = j.optString("encryptAlgo"),
            compression = j.optString("compression"),
            proxyNetworks = j.optString("proxyNetworks"),
            exitNodes = j.optString("exitNodes")
        )
    }

    private fun configStateToJson(s: ConfigState): JSONObject = JSONObject().also { j ->
        j.put("networkName", s.networkName)
        j.put("secret", s.secret)
        j.put("server", s.server)
        j.put("port", s.port)
        j.put("ip", s.ipAddress)
        j.put("prefix", s.ipPrefix)
        j.put("instanceName", s.instanceName)
        j.put("hostname", s.hostname)
        j.put("dhcp", s.dhcp)
        j.put("disableIpv6", s.disableIpv6)
        j.put("noTun", s.noTun)
        j.put("bindDevice", s.bindDevice)
        j.put("disableP2p", s.disableP2p)
        j.put("p2pOnly", s.p2pOnly)
        j.put("lazyP2p", s.lazyP2p)
        j.put("needP2p", s.needP2p)
        j.put("disableTcpHp", s.disableTcpHp)
        j.put("disableUdpHp", s.disableUdpHp)
        j.put("disableSymHp", s.disableSymHp)
        j.put("enableKcpProxy", s.enableKcpProxy)
        j.put("disableKcpInput", s.disableKcpInput)
        j.put("enableQuicProxy", s.enableQuicProxy)
        j.put("disableQuicInput", s.disableQuicInput)
        j.put("proxyForwardBySystem", s.proxyForwardBySystem)
        j.put("relayAllRpc", s.relayAllRpc)
        j.put("exitNode", s.exitNode)
        j.put("latencyFirst", s.latencyFirst)
        j.put("multiThread", s.multiThread)
        j.put("useSmoltcp", s.useSmoltcp)
        j.put("mtu", s.mtu)
        j.put("disableEncrypt", s.disableEncrypt)
        j.put("privateMode", s.privateMode)
        j.put("acceptDns", s.acceptDns)
        j.put("socks5", s.socks5)
        j.put("portForward", s.portForward)
        j.put("encryptAlgo", s.encryptAlgo)
        j.put("compression", s.compression)
        j.put("proxyNetworks", s.proxyNetworks)
        j.put("exitNodes", s.exitNodes)
    }

    private fun saveConfigToDisk(name: String, state: ConfigState) {
        val json = configStateToJson(state).toString()
        val sanitized = sanitizeName(name)

        File(getConfigsDir(), "$sanitized.json").writeText(json)
        try {
            File(getExternalConfigsDir(), "$sanitized.json").writeText(json)
        } catch (t: Throwable) {
            AppLogger.w("ConfigEdit", "External config save failed: ${t.message}")
        }

        AppLogger.i("ConfigEdit", "Config saved: $name")
    }

    private fun deleteConfigFromDisk(name: String) {
        File(getConfigsDir(), "${sanitizeName(name)}.json").delete()
        try {
            File(getExternalConfigsDir(), "${sanitizeName(name)}.json").delete()
        } catch (_: Throwable) {}
    }

    // ── Build TOML from ConfigState ──

    private fun buildTomlFromConfig(s: ConfigState): String {
        val sb = StringBuilder()

        val networkName = s.networkName.ifEmpty { "game-vpn" }
        val networkSecret = s.secret.ifEmpty { "zhangchao242" }
        val server = s.server.ifEmpty { "118.195.243.101" }
        val port = s.port.ifEmpty { "11010" }
        val ipAddr = s.ipAddress.ifEmpty { "10.144.0.4" }
        val ipPrefix = s.ipPrefix.ifEmpty { "24" }
        val peerUri = "tcp://$server:$port"

        sb.append("instance_name = \"${s.instanceName.ifEmpty { "default" }}\"\n")
        sb.append("hostname = \"${s.hostname}\"\n")
        sb.append("ipv4 = \"$ipAddr/$ipPrefix\"\n")
        sb.append("dhcp = ${s.dhcp}\n\n")

        sb.append("[network_identity]\n")
        sb.append("network_name = \"$networkName\"\n")
        sb.append("network_secret = \"$networkSecret\"\n\n")

        sb.append("[[peer]]\n")
        sb.append("uri = \"$peerUri\"\n\n")

        if (s.disableIpv6) sb.append("disable_ipv6 = true\n")
        if (s.noTun) sb.append("no_tun = true\n")
        if (s.bindDevice.isNotEmpty()) sb.append("bind_device = \"${s.bindDevice}\"\n")

        if (s.disableP2p) sb.append("disable_p2p = true\n")
        if (s.p2pOnly) sb.append("p2p_only = true\n")
        if (s.lazyP2p) sb.append("lazy_p2p = true\n")
        if (s.needP2p) sb.append("need_p2p = true\n")

        if (s.disableTcpHp) sb.append("disable_tcp_hole_punching = true\n")
        if (s.disableUdpHp) sb.append("disable_udp_hole_punching = true\n")
        if (s.disableSymHp) sb.append("disable_sym_hole_punching = true\n")

        if (s.enableKcpProxy) sb.append("enable_kcp_proxy = true\n")
        if (s.disableKcpInput) sb.append("disable_kcp_input = true\n")
        if (s.enableQuicProxy) sb.append("enable_quic_proxy = true\n")
        if (s.disableQuicInput) sb.append("disable_quic_input = true\n")
        if (s.proxyForwardBySystem) sb.append("proxy_forward_by_system = true\n")
        if (s.relayAllRpc) sb.append("relay_all_peer_rpc = true\n")
        if (s.exitNode) sb.append("enable_exit_node = true\n")

        if (s.latencyFirst) sb.append("latency_first = true\n")
        if (s.multiThread) sb.append("multi_thread = true\n")
        if (s.useSmoltcp) sb.append("use_smoltcp = true\n")
        if (s.mtu.isNotEmpty()) sb.append("mtu = ${s.mtu}\n")

        if (s.disableEncrypt) sb.append("disable_encryption = true\n")
        if (s.privateMode) sb.append("private_mode = true\n")
        if (s.acceptDns) sb.append("accept_dns = true\n")

        if (s.socks5.isNotEmpty()) sb.append("socks5 = ${s.socks5}\n")

        if (s.portForward.isNotEmpty()) {
            val parts = s.portForward.split("://")
            if (parts.size == 2) {
                val slashIdx = parts[1].lastIndexOf('/')
                if (slashIdx > 0) {
                    sb.append("[[port_forward]]\n")
                    sb.append("port_forward_protocol = \"${parts[0]}\"\n")
                    sb.append("port_forward_local = \"${parts[1].substring(0, slashIdx)}\"\n")
                    sb.append("port_forward_remote = \"${parts[1].substring(slashIdx + 1)}\"\n")
                }
            }
        }

        if (s.encryptAlgo.isNotEmpty()) sb.append("encryption_algorithm = \"${s.encryptAlgo}\"\n")
        if (s.compression.isNotEmpty() && s.compression != "none") sb.append("compression = \"${s.compression}\"\n")

        if (s.proxyNetworks.isNotEmpty()) {
            s.proxyNetworks.split(",").forEach { cidr ->
                val c = cidr.trim()
                if (c.isNotEmpty()) sb.append("[[proxy_networks]]\nproxy_network_cidr = \"$c\"\n")
            }
        }

        if (s.exitNodes.isNotEmpty()) {
            s.exitNodes.split(",").forEach { vip ->
                val v = vip.trim()
                if (v.isNotEmpty()) sb.append("[[exit_nodes]]\nnode = \"$v\"\n")
            }
        }

        return sb.toString()
    }

    // ── Import / Export ──

    private fun exportToml(state: ConfigState) {
        val toml = buildTomlFromConfig(state)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, toml)
            putExtra(Intent.EXTRA_SUBJECT, "EasyTier Config: $currentConfigName")
        }
        startActivity(Intent.createChooser(intent, tr("分享配置", "Share config")))
    }

    private fun importToml() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_TITLE, tr("选择配置文件 (TOML)", "Select config file (TOML)"))
        }
        startActivityForResult(intent, REQUEST_IMPORT_TOML)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMPORT_TOML && resultCode == RESULT_OK && data?.data != null) {
            handleTomlImport(data.data!!)
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleTomlImport(uri: android.net.Uri) {
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
            val parsedState = parseAndApplyToml(text)
            // Update Compose state via saved JSON
            saveConfigToDisk(parsedState.first, parsedState.second)
            val json = configStateToJson(parsedState.second)
            // We need to reload — so trigger a new activity launch pattern
            // Actually, let's save and tell user
            val autoName = parsedState.first
            AppLogger.i("ConfigEdit", "Imported config: $autoName")
            Toast.makeText(this, tr("已导入并保存: $autoName", "Imported & saved: $autoName"), Toast.LENGTH_SHORT).show()
            // The UI will be stale, but user can switch config to see it
            // For a better UX, finish and let MainActivity reload
            val result = Intent()
            result.putExtra(EXTRA_CONFIG_NAME, autoName)
            setResult(RESULT_OK, result)
            finish()
        } catch (t: Throwable) {
            Toast.makeText(this, tr("导入错误: ${t.message}", "Import error: ${t.message}"), Toast.LENGTH_LONG).show()
            AppLogger.e("ConfigEdit", "TOML import error", t)
        }
    }

    private fun parseAndApplyToml(text: String): Pair<String, ConfigState> {
        val j = JSONObject()
        val defaults = ConfigState()
        var currentSection = ""

        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            if (trimmed.startsWith("[")) {
                currentSection = trimmed.removeSurrounding("[", "]")
                continue
            }

            val eqIdx = trimmed.indexOf('=')
            if (eqIdx < 0) continue
            val key = trimmed.substring(0, eqIdx).trim()
            val rawValue = trimmed.substring(eqIdx + 1).trim()
            val value = rawValue.removeSurrounding("\"").trim()

            when (key) {
                "instance_name" -> j.put("instanceName", value)
                "hostname" -> j.put("hostname", value)
                "ipv4" -> {
                    val slashIdx = value.indexOf('/')
                    if (slashIdx > 0) {
                        j.put("ip", value.substring(0, slashIdx))
                        j.put("prefix", value.substring(slashIdx + 1))
                    } else { j.put("ip", value) }
                }
                "dhcp" -> j.put("dhcp", parseBool(rawValue))
                "network_name" -> j.put("networkName", value)
                "network_secret" -> j.put("secret", value)
                "uri" -> {
                    val parts = value.split("://")
                    if (parts.size == 2) {
                        val hostPort = parts[1].split(":")
                        if (hostPort.size == 2) {
                            j.put("server", hostPort[0])
                            j.put("port", hostPort[1])
                        } else { j.put("server", parts[1]) }
                    }
                }
                "disable_ipv6" -> j.put("disableIpv6", parseBool(rawValue))
                "no_tun" -> j.put("noTun", parseBool(rawValue))
                "bind_device" -> j.put("bindDevice", value)
                "disable_p2p" -> j.put("disableP2p", parseBool(rawValue))
                "p2p_only" -> j.put("p2pOnly", parseBool(rawValue))
                "lazy_p2p" -> j.put("lazyP2p", parseBool(rawValue))
                "need_p2p" -> j.put("needP2p", parseBool(rawValue))
                "disable_tcp_hole_punching" -> j.put("disableTcpHp", parseBool(rawValue))
                "disable_udp_hole_punching" -> j.put("disableUdpHp", parseBool(rawValue))
                "disable_sym_hole_punching" -> j.put("disableSymHp", parseBool(rawValue))
                "enable_kcp_proxy" -> j.put("enableKcpProxy", parseBool(rawValue))
                "disable_kcp_input" -> j.put("disableKcpInput", parseBool(rawValue))
                "enable_quic_proxy" -> j.put("enableQuicProxy", parseBool(rawValue))
                "disable_quic_input" -> j.put("disableQuicInput", parseBool(rawValue))
                "proxy_forward_by_system" -> j.put("proxyForwardBySystem", parseBool(rawValue))
                "relay_all_peer_rpc" -> j.put("relayAllRpc", parseBool(rawValue))
                "enable_exit_node" -> j.put("exitNode", parseBool(rawValue))
                "latency_first" -> j.put("latencyFirst", parseBool(rawValue))
                "multi_thread" -> j.put("multiThread", parseBool(rawValue))
                "use_smoltcp" -> j.put("useSmoltcp", parseBool(rawValue))
                "mtu" -> j.put("mtu", value)
                "disable_encryption" -> j.put("disableEncrypt", parseBool(rawValue))
                "private_mode" -> j.put("privateMode", parseBool(rawValue))
                "accept_dns" -> j.put("acceptDns", parseBool(rawValue))
                "socks5" -> j.put("socks5", value)
                "encryption_algorithm" -> j.put("encryptAlgo", value)
                "compression" -> j.put("compression", value)
                "proxy_network_cidr" -> {
                    val existing = j.optString("proxyNetworks")
                    if (existing.isEmpty()) j.put("proxyNetworks", value)
                    else j.put("proxyNetworks", "$existing,$value")
                }
                "node" -> {
                    val existing = j.optString("exitNodes")
                    if (existing.isEmpty()) j.put("exitNodes", value)
                    else j.put("exitNodes", "$existing,$value")
                }
                "port_forward_protocol" -> j.put("_pfProto", value)
                "port_forward_local" -> j.put("_pfLocal", value)
                "port_forward_remote" -> {
                    val proto = j.optString("_pfProto", "udp")
                    val local = j.optString("_pfLocal", "")
                    j.put("portForward", "$proto://$local/$value")
                    j.remove("_pfLocal")
                    j.remove("_pfProto")
                }
            }
        }

        val config = jsonToConfigState(j)
        val name = config.networkName.ifEmpty { config.instanceName.ifEmpty { "imported" } }
        return Pair(name, config)
    }

    private fun parseBool(s: String): Boolean =
        s.trim().lowercase() in listOf("true", "yes", "1")

    private fun tr(zh: String, en: String): String = if (langZh) zh else en
}
