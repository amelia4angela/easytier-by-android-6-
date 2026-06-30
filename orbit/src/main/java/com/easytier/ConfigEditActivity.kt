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
        val initialConfig = TomlUtils.jsonToConfigState(initialJson)

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
                        saveConfigToDisk(name, TomlUtils.jsonToConfigState(loadConfigFromDisk("default")))
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
                        TomlUtils.jsonToConfigState(json)
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
                                exportTomlContent = TomlUtils.buildToml(configState)
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
        return if (android.os.Build.VERSION.SDK_INT >= 30 && !android.os.Environment.isExternalStorageManager()) {
            getConfigsDir()
        } else if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
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

    /**
     * Save config state to disk (internal + external).
     */
    private fun saveConfigToDisk(name: String, state: ConfigState) {
        val json = TomlUtils.configStateToJson(state).toString()
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

    // ── Import / Export ──

    private fun exportToml(state: ConfigState) {
        val toml = TomlUtils.buildToml(state)
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
            val json = TomlUtils.parseTomlToJson(text)
            val config = TomlUtils.jsonToConfigState(json)
            val autoName = TomlUtils.deriveConfigName(json)
            saveConfigToDisk(autoName, config)
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

    private fun tr(zh: String, en: String): String = if (langZh) zh else en
}
