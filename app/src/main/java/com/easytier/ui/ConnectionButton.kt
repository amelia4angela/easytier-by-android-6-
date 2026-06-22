package com.easytier.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easytier.ui.theme.*

enum class ConnectionStatus {
    READY, RUNNING, NO_TUN, STOPPED, STARTING, ERROR, REVOKED
}

data class ConnectionButtonState(
    val status: ConnectionStatus = ConnectionStatus.READY,
    val label: String = "启动 VPN",
    val subLabel: String = "就绪",
    val protectionLevel: Int = 0
)

// ═══════════════════════════════════════════════════
//  ControlFAB — minimalist floating action button
// ═══════════════════════════════════════════════════

@Composable
fun ControlFAB(
    isConnected: Boolean,
    isStarting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = currentColors()
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = tween(150),
        label = "fabScale"
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isConnected) Brush.radialGradient(
                    listOf(colors.statusBad, colors.statusBad.copy(alpha = 0.8f))
                )
                else Brush.radialGradient(
                    listOf(colors.accentLight, colors.accent.copy(alpha = 0.9f))
                )
            )
            .clickable(
                onClick = {
                    pressed = true
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isStarting) "⋯" else if (isConnected) "■" else "▶",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════
//  ConnectionBanner — minimalist status banner
// ═══════════════════════════════════════════════════

@Composable
fun ConnectionBanner(
    state: ConnectionButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    peerCount: Int = 0,
    uptime: String = "",
    rxSpeed: String = "",
    txSpeed: String = "",
    langZh: Boolean = true
) {
    val colors = currentColors()
    val isConnected = state.status == ConnectionStatus.RUNNING || state.status == ConnectionStatus.NO_TUN
    val isStarting = state.status == ConnectionStatus.STARTING
    val isRevoked = state.status == ConnectionStatus.REVOKED

    val bannerBg by animateColorAsState(
        targetValue = if (isConnected) colors.statusConnectedBg
                     else if (isRevoked || state.status == ConnectionStatus.ERROR) colors.statusBad.copy(alpha = 0.15f)
                     else colors.surfaceDim,
        animationSpec = tween(400),
        label = "bannerBg"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    isConnected -> Brush.horizontalGradient(
                        listOf(bannerBg, colors.statusGood.copy(alpha = 0.06f))
                    )
                    isRevoked || state.status == ConnectionStatus.ERROR -> Brush.horizontalGradient(
                        listOf(bannerBg, colors.statusBad.copy(alpha = 0.06f))
                    )
                    else -> Brush.horizontalGradient(listOf(colors.surface, colors.surfaceDim))
                }
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Status indicator — minimalist filled/empty circle
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isConnected -> colors.statusGood
                            isStarting -> colors.statusWarn
                            isRevoked || state.status == ConnectionStatus.ERROR -> colors.statusBad
                            else -> colors.textTertiary.copy(alpha = 0.4f)
                        }
                    ),
            )

            Spacer(Modifier.width(14.dp))

            // Info column
            Column(modifier = Modifier.weight(1f)) {
                if (isConnected) {
                    Text(
                        text = T("运行中", "Running", langZh),
                        color = colors.statusGood,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (uptime.isNotEmpty())
                            Text(uptime, color = colors.textTertiary, fontSize = 12.sp)
                        if (rxSpeed.isNotEmpty())
                            Text("↓ $rxSpeed", color = colors.textTertiary, fontSize = 11.sp)
                        if (txSpeed.isNotEmpty())
                            Text("↑ $txSpeed", color = colors.textTertiary, fontSize = 11.sp)
                        if (peerCount > 0)
                            Text("◉ $peerCount", color = colors.textTertiary, fontSize = 11.sp)
                    }
                } else if (isStarting) {
                    Text(
                        text = T("正在启动…", "Starting…", langZh),
                        color = colors.statusWarn,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(T("请稍候", "Please wait", langZh), color = colors.textTertiary, fontSize = 12.sp)
                } else if (state.status == ConnectionStatus.ERROR) {
                    Text(
                        text = T("连接错误", "Connection Error", langZh),
                        color = colors.statusBad,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(state.subLabel, color = colors.textTertiary, fontSize = 12.sp)
                } else if (isRevoked) {
                    Text(
                        text = T("VPN 被中断", "VPN Interrupted", langZh),
                        color = colors.statusBad,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = T("点击重新连接", "Tap to reconnect", langZh),
                        color = colors.textTertiary,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = T("已停止", "Stopped", langZh),
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = T("点击启动", "Tap to start", langZh),
                        color = colors.textTertiary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

