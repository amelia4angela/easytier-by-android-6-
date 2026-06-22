package com.easytier.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color as ComposeColor
import com.easytier.ui.theme.*

@Composable
fun ConfigForm(
    config: ConfigState,
    langZh: Boolean,
    onConfigChange: (ConfigState) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = currentColors()
    var advancedExpanded by remember { mutableStateOf(false) }
    var secretVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Basic Config ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                SectionLabel(T("基本配置", "Basic Config", langZh), colors)

                HmTextField(
                    value = config.networkName,
                    onValueChange = { onConfigChange(config.copy(networkName = it)) },
                    label = T("网络名称", "Network Name", langZh),
                    placeholder = T("例如 my-vpn", "e.g. my-vpn", langZh),
                    colors = colors
                )

                // Secret with eye toggle
                Text(
                    text = T("密钥", "Secret", langZh),
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 2.dp, bottom = 3.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HmTextField(
                        value = config.secret,
                        onValueChange = { onConfigChange(config.copy(secret = it)) },
                        label = "",
                        placeholder = T("输入网络密钥", "Enter network secret", langZh),
                        password = !secretVisible,
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.inputBg)
                            .border(0.5.dp, colors.inputBorder, RoundedCornerShape(12.dp))
                            .clickable { secretVisible = !secretVisible },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (secretVisible) "🙈" else "👁", fontSize = 16.sp)
                    }
                }

                // Server + Port
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HmTextField(
                        value = config.server,
                        onValueChange = { onConfigChange(config.copy(server = it)) },
                        label = T("服务器", "Server", langZh),
                        placeholder = T("例如 1.2.3.4", "e.g. 1.2.3.4", langZh),
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                    HmTextField(
                        value = config.port,
                        onValueChange = { onConfigChange(config.copy(port = it)) },
                        label = T("端口", "Port", langZh),
                        placeholder = "11000",
                        modifier = Modifier.width(100.dp),
                        keyboardType = KeyboardType.Number,
                        colors = colors
                    )
                }

                // Local IP
                HmTextField(
                    value = config.ipAddress,
                    onValueChange = { onConfigChange(config.copy(ipAddress = it)) },
                    label = T("本地 IP", "Local IP", langZh),
                    placeholder = T("例如 10.0.0.2/24", "e.g. 10.0.0.2/24", langZh),
                    colors = colors
                )

                // Device identity
                HmTextField(
                    value = config.hostname,
                    onValueChange = { onConfigChange(config.copy(hostname = it)) },
                    label = T("设备名称", "Device Name", langZh),
                    placeholder = T("例如 设备名（其他节点看到的名字）", "e.g. my-device (shown to other nodes)", langZh),
                    colors = colors
                )
                HmTextField(
                    value = config.instanceName,
                    onValueChange = { onConfigChange(config.copy(instanceName = it)) },
                    label = T("实例标识", "Instance ID", langZh),
                    placeholder = T("例如 设备名（内部标识，留空自动）", "e.g. my-device (internal ID, auto if empty)", langZh),
                    colors = colors
                )

                // Common toggles
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HmCheckbox(
                        checked = config.noTun,
                        onCheckedChange = { onConfigChange(config.copy(noTun = it)) },
                        label = T("无 TUN 模式", "No TUN Mode", langZh),
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                    HmCheckbox(
                        checked = config.dhcp,
                        onCheckedChange = { onConfigChange(config.copy(dhcp = it)) },
                        label = T("DHCP 自动 IP", "DHCP Auto IP", langZh),
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                }
            }

        // ── Advanced Options ──
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { advancedExpanded = !advancedExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (advancedExpanded) "▲" else "▼",
                            color = colors.textTertiary,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = T("高级选项", "Advanced Options", langZh),
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                AnimatedVisibility(
                    visible = advancedExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Network
                        SectionLabel(T("网络", "Network", langZh), colors)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HmCheckbox(checked = config.disableIpv6, onCheckedChange = { onConfigChange(config.copy(disableIpv6 = it)) }, label = T("禁用 IPv6", "Disable IPv6", langZh), modifier = Modifier.weight(1f), colors = colors)
                        }
                        HmTextField(
                            value = config.bindDevice,
                            onValueChange = { onConfigChange(config.copy(bindDevice = it)) },
                            label = T("绑定网卡", "Bind Device", langZh),
                            placeholder = T("例如 eth0, wlan0", "e.g. eth0, wlan0", langZh),
                            colors = colors
                        )

                        // P2P
                        SectionLabel(T("点对点 (P2P)", "Peer-to-Peer (P2P)", langZh), colors)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HmCheckbox(checked = config.disableP2p, onCheckedChange = { onConfigChange(config.copy(disableP2p = it)) }, label = T("禁用 P2P", "Disable P2P", langZh), modifier = Modifier.weight(1f), colors = colors)
                            HmCheckbox(checked = config.p2pOnly, onCheckedChange = { onConfigChange(config.copy(p2pOnly = it)) }, label = T("仅 P2P", "P2P Only", langZh), modifier = Modifier.weight(1f), colors = colors)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HmCheckbox(checked = config.lazyP2p, onCheckedChange = { onConfigChange(config.copy(lazyP2p = it)) }, label = T("延迟 P2P", "Lazy P2P", langZh), modifier = Modifier.weight(1f), colors = colors)
                            HmCheckbox(checked = config.needP2p, onCheckedChange = { onConfigChange(config.copy(needP2p = it)) }, label = T("需要 P2P", "Need P2P", langZh), modifier = Modifier.weight(1f), colors = colors)
                        }

                        // NAT / Hole Punch
                        SectionLabel(T("NAT / 打洞", "NAT / Hole Punch", langZh), colors)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HmCheckbox(checked = config.disableTcpHp, onCheckedChange = { onConfigChange(config.copy(disableTcpHp = it)) }, label = T("禁用 TCP 打洞", "Disable TCP HP", langZh), modifier = Modifier.weight(1f), colors = colors)
                            HmCheckbox(checked = config.disableUdpHp, onCheckedChange = { onConfigChange(config.copy(disableUdpHp = it)) }, label = T("禁用 UDP 打洞", "Disable UDP HP", langZh), modifier = Modifier.weight(1f), colors = colors)
                        }
                        HmCheckbox(checked = config.disableSymHp, onCheckedChange = { onConfigChange(config.copy(disableSymHp = it)) }, label = T("禁用 Sym 打洞", "Disable Sym HP", langZh), colors = colors)

                        // Proxy / KCP / QUIC
                        SectionLabel(T("代理 / KCP / QUIC", "Proxy / KCP / QUIC", langZh), colors)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HmCheckbox(checked = config.enableKcpProxy, onCheckedChange = { onConfigChange(config.copy(enableKcpProxy = it)) }, label = "KCP Proxy", modifier = Modifier.weight(1f), colors = colors)
                            HmCheckbox(checked = config.disableKcpInput, onCheckedChange = { onConfigChange(config.copy(disableKcpInput = it)) }, label = T("禁用 KCP 入站", "Disable KCP In", langZh), modifier = Modifier.weight(1f), colors = colors)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HmCheckbox(checked = config.enableQuicProxy, onCheckedChange = { onConfigChange(config.copy(enableQuicProxy = it)) }, label = "QUIC Proxy", modifier = Modifier.weight(1f), colors = colors)
                            HmCheckbox(checked = config.disableQuicInput, onCheckedChange = { onConfigChange(config.copy(disableQuicInput = it)) }, label = T("禁用 QUIC 入站", "Disable QUIC In", langZh), modifier = Modifier.weight(1f), colors = colors)
                        }
                        HmCheckbox(checked = config.proxyForwardBySystem, onCheckedChange = { onConfigChange(config.copy(proxyForwardBySystem = it)) }, label = T("系统代理转发", "System Proxy", langZh), colors = colors)
                        HmCheckbox(checked = config.relayAllRpc, onCheckedChange = { onConfigChange(config.copy(relayAllRpc = it)) }, label = T("中继所有节点 RPC", "Relay All RPC", langZh), colors = colors)
                        HmCheckbox(checked = config.exitNode, onCheckedChange = { onConfigChange(config.copy(exitNode = it)) }, label = T("出口节点模式", "Exit Node", langZh), colors = colors)

                        HmTextField(
                            value = config.proxyNetworks,
                            onValueChange = { onConfigChange(config.copy(proxyNetworks = it)) },
                            label = T("代理网络 CIDR", "Proxy CIDR", langZh),
                            placeholder = "10.0.0.0/8, 192.168.1.0/24",
                            colors = colors
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HmTextField(
                                value = config.socks5,
                                onValueChange = { onConfigChange(config.copy(socks5 = it)) },
                                label = "SOCKS5",
                                placeholder = "1080",
                                modifier = Modifier.weight(1f),
                                keyboardType = KeyboardType.Number,
                                colors = colors
                            )
                            HmTextField(
                                value = config.mtu,
                                onValueChange = { onConfigChange(config.copy(mtu = it)) },
                                label = "MTU",
                                placeholder = T("0=自动", "0=Auto", langZh),
                                modifier = Modifier.width(100.dp),
                                keyboardType = KeyboardType.Number,
                                colors = colors
                            )
                        }

                        HmTextField(
                            value = config.portForward,
                            onValueChange = { onConfigChange(config.copy(portForward = it)) },
                            label = T("端口转发", "Port Forward", langZh),
                            placeholder = "udp://0.0.0.0:12345/10.126.126.1:23456",
                            colors = colors
                        )

                        // Performance
                        SectionLabel(T("性能", "Performance", langZh), colors)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HmCheckbox(checked = config.latencyFirst, onCheckedChange = { onConfigChange(config.copy(latencyFirst = it)) }, label = T("延迟优先", "Latency First", langZh), modifier = Modifier.weight(1f), colors = colors)
                            HmCheckbox(checked = config.multiThread, onCheckedChange = { onConfigChange(config.copy(multiThread = it)) }, label = T("多线程", "Multi-Thread", langZh), modifier = Modifier.weight(1f), colors = colors)
                        }
                        HmCheckbox(checked = config.useSmoltcp, onCheckedChange = { onConfigChange(config.copy(useSmoltcp = it)) }, label = "smoltcp", colors = colors)

                        // Security
                        SectionLabel(T("安全", "Security", langZh), colors)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HmCheckbox(checked = config.disableEncrypt, onCheckedChange = { onConfigChange(config.copy(disableEncrypt = it)) }, label = T("禁用加密", "Disable Encrypt", langZh), modifier = Modifier.weight(1f), colors = colors)
                            HmCheckbox(checked = config.privateMode, onCheckedChange = { onConfigChange(config.copy(privateMode = it)) }, label = T("私密模式", "Private Mode", langZh), modifier = Modifier.weight(1f), colors = colors)
                        }
                        HmCheckbox(checked = config.acceptDns, onCheckedChange = { onConfigChange(config.copy(acceptDns = it)) }, label = T("接受 DNS", "Accept DNS", langZh), colors = colors)

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HmTextField(
                                value = config.encryptAlgo,
                                onValueChange = { onConfigChange(config.copy(encryptAlgo = it)) },
                                label = T("加密算法", "Encrypt Algo", langZh),
                                placeholder = T("留空=自动", "empty=auto", langZh),
                                modifier = Modifier.weight(1f),
                                colors = colors
                            )
                            HmTextField(
                                value = config.compression,
                                onValueChange = { onConfigChange(config.copy(compression = it)) },
                                label = T("压缩", "Compression", langZh),
                                placeholder = "none / zstd",
                                modifier = Modifier.weight(1f),
                                colors = colors
                            )
                        }

                        // Extra
                        SectionLabel(T("附加", "Extra", langZh), colors)
                        HmTextField(
                            value = config.exitNodes,
                            onValueChange = { onConfigChange(config.copy(exitNodes = it)) },
                            label = T("出口节点 VIP", "Exit Node VIPs", langZh),
                            placeholder = "10.0.0.2, 10.0.0.3",
                            colors = colors
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section label with an accent left bar for visual grouping.
 */
@Composable
private fun SectionLabel(text: String, colors: AppColors) {
    Text(
        text = text,
        color = colors.textTertiary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun HmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    colors: AppColors
) {
    var isFocused by remember { mutableStateOf(false) }

    // Animate border color on focus
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) colors.accent else colors.inputBorder,
        animationSpec = tween(200),
        label = "borderColor"
    )
    val bgColor = ComposeColor.Transparent

    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = colors.textTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 3.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .border(
                    width = if (isFocused) 1.dp else 0.5.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(10.dp)
                )
                .onFocusChanged { isFocused = it.isFocused }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = colors.textTertiary.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 13.sp
                ),
                cursorBrush = SolidColor(colors.accentLight),
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Modern checkbox with animated check mark, matching the accent-colored theme.
 */
@Composable
private fun HmCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    colors: AppColors
) {
    // Animate check state
    val boxColor by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.inputBg,
        animationSpec = tween(200),
        label = "checkBoxColor"
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(200),
        label = "checkAlpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 5.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(boxColor)
                .border(
                    width = 1.dp,
                    color = if (checked) colors.accent else colors.inputBorder,
                    shape = RoundedCornerShape(5.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "✓",
                color = ComposeColor.White
                    .copy(alpha = checkAlpha),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(y = (-1).dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 13.sp
        )
    }
}
