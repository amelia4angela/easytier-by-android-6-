package com.easytier.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import com.easytier.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private enum class Tab { STATUS, CONFIG, MORE }

// ═══════════════════════════════════════════════════
//  MainScreen — 3-tab layout with bottom navigation
// ═══════════════════════════════════════════════════

@Composable
fun MainScreen(
    connectionState: ConnectionButtonState,
    onToggleConnection: () -> Unit,
    config: ConfigState,
    onConfigChange: (ConfigState) -> Unit,
    currentConfigName: String,
    langZh: Boolean,
    onConfigNameClick: () -> Unit,
    onSaveConfig: () -> Unit,
    onAutoSaveConfig: () -> Unit = {},
    onManageConfigs: () -> Unit,
    onImportToml: () -> Unit,
    onExportToml: () -> Unit,
    onDeleteConfig: () -> Unit,
    myNodeItem: PeerInfoItem?,
    peerItems: List<PeerInfoItem>,
    onConfigServer: () -> Unit,
    onToggleLanguage: () -> Unit,
    onLaunchConfigEditor: () -> Unit,
    onViewLogs: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    connectionStartTime: Long = 0L,
    modifier: Modifier = Modifier
) {
    val colors = currentColors()
    var currentTab by remember { mutableStateOf(Tab.STATUS) }
    var showAboutPage by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(colors.bgStart, colors.bgMid, colors.bgEnd))
                )
        )

        // Decorative glows
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(280.dp)
                .offset(x = 60.dp, y = (-80).dp)
                .clip(RoundedCornerShape(140.dp))
                .background(colors.glowColor)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(220.dp)
                .offset(x = (-80).dp, y = 160.dp)
                .clip(RoundedCornerShape(110.dp))
                .background(colors.glowColorDark)
        )

        // Content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (showAboutPage) 0.dp else 72.dp)
        ) {
            if (showAboutPage) {
                AboutPage(
                    langZh = langZh,
                    isConnected = connectionState.status == ConnectionStatus.RUNNING
                            || connectionState.status == ConnectionStatus.NO_TUN,
                    connectionStartTime = connectionStartTime,
                    onBack = { showAboutPage = false }
                )
            } else {
                when (currentTab) {
                    Tab.STATUS -> StatusTab(
                    connectionState = connectionState,
                    onToggleConnection = onToggleConnection,
                    myNodeItem = myNodeItem,
                    peerItems = peerItems,
                    onViewLogs = onViewLogs,
                    langZh = langZh
                )
                Tab.CONFIG -> ConfigTab(
                    config = config,
                    onConfigChange = onConfigChange,
                    currentConfigName = currentConfigName,
                    langZh = langZh,
                    onConfigNameClick = onConfigNameClick,
                    onSaveConfig = onSaveConfig,
                    onAutoSaveConfig = onAutoSaveConfig,
                    onManageConfigs = onManageConfigs,
                    onImportToml = onImportToml,
                    onExportToml = onExportToml,
                    onDeleteConfig = onDeleteConfig,
                    onLaunchConfigEditor = onLaunchConfigEditor,
                    onApplyConfig = {
                        onToggleConnection()
                        currentTab = Tab.STATUS
                    }
                )
                Tab.MORE -> MoreTab(
                    langZh = langZh,
                    onToggleLanguage = onToggleLanguage,
                    onConfigServer = onConfigServer,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onNavigateToAbout = { showAboutPage = true },
                    isConnected = connectionState.status == ConnectionStatus.RUNNING
                            || connectionState.status == ConnectionStatus.NO_TUN,
                    connectionStartTime = connectionStartTime
                )
            }
            }
        }

        // Bottom Navigation
        if (!showAboutPage) {
            BottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                langZh = langZh,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ═══════════════════════════════════════════════════
//  Bottom Navigation
// ═══════════════════════════════════════════════════

@Composable
private fun BottomNavigationBar(
    currentTab: Tab,
    onTabSelected: (Tab) -> Unit,
    langZh: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = currentColors()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.navBar)
            .border(width = 0.5.dp, color = colors.surfaceBorder, shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
            .padding(top = 6.dp, start = 0.dp, end = 0.dp, bottom = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(
                Triple("●", T("状态", "Status", langZh), Tab.STATUS),
                Triple("≡", T("配置", "Config", langZh), Tab.CONFIG),
                Triple("○", T("更多", "More", langZh), Tab.MORE)
            ).forEach { (icon, label, tab) ->
                val isActive = currentTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(icon, fontSize = 20.sp)
                        Text(
                            text = label,
                            color = if (isActive) colors.textPrimary else colors.textTertiary,
                            fontSize = 11.sp,
                            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                        )
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isActive) colors.accent else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════
//  Status Tab
// ═══════════════════════════════════════════════════

@Composable
private fun StatusTab(
    connectionState: ConnectionButtonState,
    onToggleConnection: () -> Unit,
    myNodeItem: PeerInfoItem?,
    peerItems: List<PeerInfoItem>,
    onViewLogs: () -> Unit,
    langZh: Boolean
) {
    val colors = currentColors()
    val scrollState = rememberScrollState()
    val isConnected = connectionState.status == ConnectionStatus.RUNNING ||
                      connectionState.status == ConnectionStatus.NO_TUN
    val isStarting = connectionState.status == ConnectionStatus.STARTING

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ConnectionBanner(
                state = connectionState,
                onClick = onToggleConnection,
                peerCount = (if (myNodeItem != null) 1 else 0) + peerItems.size,
                langZh = langZh
            )

            PeerInfoSection(
                myNode = myNodeItem,
                peers = peerItems,
                expanded = true,
                onToggle = {},
                langZh = langZh
            )

            // Quick actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceDim)
                        .border(0.5.dp, colors.surfaceBorder, RoundedCornerShape(10.dp))
                        .clickable { onViewLogs() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "☰ ${T("查看日志", "View Log", langZh)}",
                        color = colors.textSecondary, fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceDim)
                        .border(0.5.dp, colors.surfaceBorder, RoundedCornerShape(10.dp))
                        .clickable { onToggleConnection() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "↻ ${T("重启服务", "Restart", langZh)}",
                        color = colors.textSecondary, fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(80.dp))
        }

        // Floating control button
        ControlFAB(
            isConnected = isConnected,
            isStarting = isStarting,
            onClick = onToggleConnection,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        )
    }
}

// ═══════════════════════════════════════════════════
//  Config Tab
// ═══════════════════════════════════════════════════

@Composable
private fun ConfigTab(
    config: ConfigState,
    onConfigChange: (ConfigState) -> Unit,
    currentConfigName: String,
    langZh: Boolean,
    onConfigNameClick: () -> Unit,
    onSaveConfig: () -> Unit,
    onAutoSaveConfig: () -> Unit,
    onManageConfigs: () -> Unit,
    onImportToml: () -> Unit,
    onExportToml: () -> Unit,
    onDeleteConfig: () -> Unit,
    onLaunchConfigEditor: () -> Unit,
    onApplyConfig: () -> Unit
) {
    val colors = currentColors()
    val scrollState = rememberScrollState()
    var hasUnsaved by remember { mutableStateOf(false) }
    var lastConfig by remember { mutableStateOf(config) }

    // ⏱ Debounced auto-save: when config text fields change, wait 3s idle then auto-save
    LaunchedEffect(config) {
        if (config != lastConfig) {
            hasUnsaved = true
            lastConfig = config
            delay(3000L)
            onAutoSaveConfig()
            hasUnsaved = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        // ── Config Management Bar ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Config name selector + unsaved indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.inputBg)
                            .clickable { onConfigNameClick() }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("▪", fontSize = 14.sp, color = colors.textTertiary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                currentConfigName,
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.weight(1f))
                            if (hasUnsaved) {
                                Text(
                                    "● ${T("未保存", "Unsaved", langZh)}",
                                    color = colors.accentLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("▼", color = colors.textTertiary, fontSize = 10.sp)
                        }
                    }
                }

                // Row 2: Action buttons (reflow on small screens)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ActionBtn(
                        text = T("💾 保存", "💾 Save", langZh),
                        onClick = onSaveConfig,
                        colors = colors,
                        accent = true,
                        modifier = Modifier.weight(1f)
                    )
                    ActionBtn(
                        text = T("📥 导入", "📥 Import", langZh),
                        onClick = onImportToml,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                    ActionBtn(
                        text = T("📤 导出", "📤 Export", langZh),
                        onClick = onExportToml,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Secondary actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ActionBtn(
                        text = T("📋 管理", "📋 Manage", langZh),
                        onClick = onManageConfigs,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                    ActionBtn(
                        text = T("✏️ 编辑器", "✏️ Editor", langZh),
                        onClick = onLaunchConfigEditor,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                    ActionBtn(
                        text = T("🗑 删除", "🗑 Delete", langZh),
                        onClick = onDeleteConfig,
                        colors = colors,
                        danger = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Config Form ──
        ConfigForm(
            config = config,
            langZh = langZh,
            onConfigChange = onConfigChange
        )

        // ── Apply Button ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.accentGradient)
                .clickable {
                    // Save before applying
                    onAutoSaveConfig()
                    onApplyConfig()
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▸ ${T("应用并启动", "Apply & Start", langZh)}",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ActionBtn(
    text: String,
    onClick: () -> Unit,
    colors: AppColors,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    danger: Boolean = false
) {
    val textColor = when {
        accent -> Color.White
        danger -> colors.dangerText
        else -> colors.textSecondary
    }
    val bgMod = when {
        accent -> Modifier.background(colors.accentGradient)
        danger -> Modifier.background(colors.dangerBg)
        else -> Modifier.background(colors.inputBg)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(bgMod)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

// ═══════════════════════════════════════════════════
//  More Tab — Features & About
// ═══════════════════════════════════════════════════

@Composable
private fun MoreTab(
    langZh: Boolean,
    onToggleLanguage: () -> Unit,
    onConfigServer: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToAbout: () -> Unit,
    isConnected: Boolean = false,
    connectionStartTime: Long = 0L
) {
    val ctx = LocalContext.current
    val version: String = remember {
        try {
            val pkg = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            pkg.versionName ?: "0.1.3"
        } catch (_: Exception) { "0.1.3" }
    }
    val colors = currentColors()
    val scrollState = rememberScrollState()

    // Live uptime ticker
    var uptimeText by remember { mutableStateOf("") }
    LaunchedEffect(isConnected, connectionStartTime) {
        if (!isConnected || connectionStartTime <= 0L) {
            uptimeText = ""
            return@LaunchedEffect
        }
        while (true) {
            val elapsed = System.currentTimeMillis() - connectionStartTime
            val secs = (elapsed / 1000) % 60
            val mins = (elapsed / 60000) % 60
            val hours = elapsed / 3600000
            uptimeText = if (hours > 0)
                String.format("%dh %dm %ds", hours, mins, secs)
            else
                String.format("%dm %ds", mins, secs)
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Config Server Entry
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(0.5.dp, colors.surfaceBorder, RoundedCornerShape(12.dp))
                .clickable { onConfigServer() }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("☁", fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        T("配置服务器", "Config Server", langZh),
                        color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )
                    Text(
                        T("从远端 URL 一键拉取 TOML 配置", "Fetch TOML config from URL", langZh),
                        color = colors.textTertiary, fontSize = 12.sp
                    )
                }
                Text("→", color = colors.textTertiary, fontSize = 14.sp)
            }
        }

        // Language Toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(0.5.dp, colors.surfaceBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⋮", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        T("语言", "Language", langZh),
                        color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (langZh) colors.accentContainer else colors.inputBg)
                            .border(
                                0.5.dp,
                                if (langZh) colors.accent.copy(alpha = 0.3f) else colors.inputBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { if (!langZh) onToggleLanguage() }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("中文", color = if (langZh) colors.accentLight else colors.textTertiary, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!langZh) colors.accentContainer else colors.inputBg)
                            .border(
                                0.5.dp,
                                if (!langZh) colors.accent.copy(alpha = 0.3f) else colors.inputBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { if (langZh) onToggleLanguage() }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("English", color = if (!langZh) colors.accentLight else colors.textTertiary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Theme Toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(0.5.dp, colors.surfaceBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isDarkTheme) "●" else "○", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        T("主题", "Theme", langZh),
                        color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isDarkTheme) colors.accentContainer else colors.inputBg)
                            .border(
                                0.5.dp,
                                if (!isDarkTheme) colors.accent.copy(alpha = 0.3f) else colors.inputBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { if (isDarkTheme) onToggleTheme() }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            T("浅色", "Light", langZh),
                            color = if (!isDarkTheme) colors.accentLight else colors.textTertiary,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDarkTheme) colors.accentContainer else colors.inputBg)
                            .border(
                                0.5.dp,
                                if (isDarkTheme) colors.accent.copy(alpha = 0.3f) else colors.inputBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { if (!isDarkTheme) onToggleTheme() }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            T("深色", "Dark", langZh),
                            color = if (isDarkTheme) colors.accentLight else colors.textTertiary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // About Card (clickable → navigates to AboutPage)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(0.5.dp, colors.surfaceBorder, RoundedCornerShape(12.dp))
                .clickable { onNavigateToAbout() }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("◇", fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        T("关于 Orbit", "About Orbit", langZh),
                        color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )
                    Text(
                        T("查看版本、描述和运行信息", "Version, description & runtime info", langZh),
                        color = colors.textTertiary, fontSize = 12.sp
                    )
                }
                Text("→", color = colors.textTertiary, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ═══════════════════════════════════════════════════
//  AboutPage — full-screen about screen
// ═══════════════════════════════════════════════════

@Composable
fun AboutPage(
    langZh: Boolean,
    isConnected: Boolean = false,
    connectionStartTime: Long = 0L,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val version: String = remember {
        try {
            val pkg = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            pkg.versionName ?: "0.1.3"
        } catch (_: Exception) { "0.1.3" }
    }
    val colors = currentColors()
    val scrollState = rememberScrollState()

    // Live uptime ticker
    var uptimeText by remember { mutableStateOf("") }
    LaunchedEffect(isConnected, connectionStartTime) {
        if (!isConnected || connectionStartTime <= 0L) {
            uptimeText = ""
            return@LaunchedEffect
        }
        while (true) {
            val elapsed = System.currentTimeMillis() - connectionStartTime
            val secs = (elapsed / 1000) % 60
            val mins = (elapsed / 60000) % 60
            val hours = elapsed / 3600000
            uptimeText = if (hours > 0)
                String.format("%dh %dm %ds", hours, mins, secs)
            else
                String.format("%dm %ds", mins, secs)
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(colors.bgStart, colors.bgMid, colors.bgEnd))
            )
    ) {
        // Back button + title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(0.5.dp, colors.surfaceBorder, RoundedCornerShape(12.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("← " + T("返回", "Back", langZh), color = colors.accentLight, fontSize = 13.sp)
            }
            Text(
                T("关于 Orbit", "About Orbit", langZh),
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // About card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(0.5.dp, colors.surfaceBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "EasyTier",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = T("版本：2.6.4-8428a89d", "Version: 2.6.4-8428a89d", langZh),
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = T(
                            "一个简单、安全、去中心化的内网穿透 VPN 组网方案，使用 Rust 语言和 Tokio 框架实现。",
                            "A simple, secure, decentralized intranet penetration VPN mesh solution, built with Rust and Tokio.",
                            langZh
                        ),
                        color = colors.textTertiary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = T("此版本由涵编译。", "This build by Han.", langZh),
                        color = colors.textTertiary,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    // Official EasyTier link
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accentContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("◎ ", color = colors.accentLight, fontSize = 12.sp)
                            Text(
                                "github.com/EasyTier/EasyTier",
                                color = colors.accentLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // User's fork link
                    val context = LocalContext.current
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceDim)
                            .clickable {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://github.com/amelia4angela/orbit-easytier-android")
                                )
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("◇ ", color = colors.accentLight, fontSize = 12.sp)
                            Text(
                                "amelia4angela · 编译版",
                                color = colors.accentLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }

                    // Version info
                    Spacer(Modifier.height(10.dp))

                    var updateStatus by remember { mutableStateOf<String?>(null) }
                    var checking by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceDim)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = T("应用版本", "App Version", langZh),
                                    color = colors.textTertiary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "v$version",
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Check update button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (checking) colors.textTertiary.copy(alpha = 0.15f)
                                        else colors.accentContainer.copy(alpha = 0.5f)
                                    )
                                    .clickable(enabled = !checking) {
                                        checking = true
                                        updateStatus = null
                                        scope.launch {
                                            try {
                                                val result = withContext(Dispatchers.IO) {
                                                    val conn = URL(
                                                        "https://api.github.com/repos/amelia4angela/orbit-easytier-android/releases/latest"
                                                    ).openConnection() as HttpURLConnection
                                                    conn.setRequestProperty("Accept", "application/vnd.github+json")
                                                    conn.setRequestProperty("User-Agent", "EasyTier-Android")
                                                    conn.connectTimeout = 10000
                                                    conn.readTimeout = 10000
                                                    val code = conn.responseCode
                                                    if (code == 200) {
                                                        val body = conn.inputStream.bufferedReader().readText()
                                                        val tagStart = body.indexOf("\"tag_name\":\"") + "\"tag_name\":\"".length
                                                        val tagEnd = body.indexOf("\"", tagStart)
                                                        body.substring(tagStart, tagEnd)
                                                    } else {
                                                        null
                                                    }
                                                }
                                                updateStatus = if (result != null) {
                                                    val latest = result.removePrefix("v")
                                                    val current = version
                                                    if (latest > current) {
                                                        "new:$result"
                                                    } else {
                                                        "latest"
                                                    }
                                                } else {
                                                    "error"
                                                }
                                            } catch (_: Exception) {
                                                updateStatus = "error"
                                            }
                                            checking = false
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (checking) {
                                        Text("⋯", color = colors.textTertiary, fontSize = 12.sp)
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = T("检查更新", "Check Update", langZh),
                                        color = colors.accentLight,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Update status
                    if (updateStatus != null) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        updateStatus == "latest" -> colors.statusGood.copy(alpha = 0.1f)
                                        updateStatus == "error" -> colors.statusBad.copy(alpha = 0.1f)
                                        updateStatus!!.startsWith("new:") -> colors.statusWarn.copy(alpha = 0.1f)
                                        else -> colors.surfaceDim
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = when {
                                    updateStatus == "latest" ->
                                        T("✔ 已是最新版本 v$version", "✔ Latest version v$version", langZh)
                                    updateStatus!!.startsWith("new:") -> {
                                        val ver = updateStatus!!.removePrefix("new:")
                                        T("发现新版本 $ver，点击上方链接下载", "New version $ver, tap link above to download", langZh)
                                    }
                                    else -> T("检查失败，请检查网络连接", "Check failed, check network", langZh)
                                },
                                color = when {
                                    updateStatus == "latest" -> colors.statusGood
                                    updateStatus!!.startsWith("new:") -> colors.statusWarn
                                    else -> colors.statusBad
                                },
                                fontSize = 12.sp
                            )
                        }
                        // Click on "new:" to open release page
                        if (updateStatus != null && updateStatus!!.startsWith("new:")) {
                            val ver = updateStatus!!.removePrefix("new:")
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.accentContainer.copy(alpha = 0.3f))
                                    .clickable {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(
                                                "https://github.com/amelia4angela/orbit-easytier-android/releases/tag/$ver"
                                            )
                                        )
                                        context.startActivity(intent)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = T("前往下载 →", "Go to Download →", langZh),
                                    color = colors.accentLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    if (isConnected && uptimeText.isNotEmpty()) {
                        AboutRow(
                            T("运行时长", "Uptime", langZh),
                            uptimeText, colors
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(colors.surfaceBorder.copy(alpha = 0.5f))
                        )

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════
//  Small helpers
// ═══════════════════════════════════════════════════

@Composable
private fun AboutRow(label: String, value: String?, colors: AppColors) {
    if (value != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = colors.textSecondary, fontSize = 14.sp)
            Text(value, color = colors.textPrimary, fontSize = 14.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.surfaceBorder.copy(alpha = 0.5f))
        )
    }
}
