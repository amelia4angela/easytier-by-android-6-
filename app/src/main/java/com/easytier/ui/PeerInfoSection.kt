package com.easytier.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easytier.ui.theme.*

data class PeerInfoItem(
    val isSelf: Boolean,
    val ip: String,
    val hostname: String,
    val version: String,
    val protocol: String,
    val latencyUs: Long,
    val lossRate: Double,
    val rxBytes: Long,
    val txBytes: Long,
    val rxSpeed: String,
    val txSpeed: String,
    val natType: String = "",
    val peerId: Int = 0,
    val isRelayed: Boolean = false
)

@Composable
fun PeerInfoSection(
    myNode: PeerInfoItem?,
    peers: List<PeerInfoItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    langZh: Boolean = true
) {
    val colors = currentColors()
    val allNodes = listOfNotNull(myNode) + peers
    val isConnected = myNode != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(4.dp)
    ) {
        Column {
            if (isConnected && allNodes.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = T("节点在线", "Online Nodes", langZh),
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${allNodes.size} ${T("在线", "online", langZh)}",
                        color = colors.textTertiary,
                        fontSize = 11.sp
                    )
                }

                allNodes.forEachIndexed { index, item ->
                    PeerRow(item = item, colors = colors, langZh = langZh)
                    if (index < allNodes.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(0.5.dp)
                                .background(colors.surfaceBorder.copy(alpha = 0.3f))
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("◇", fontSize = 28.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = T("启动 VPN 后自动显示节点信息",
                                    "Peers appear after starting VPN", langZh),
                            color = colors.textTertiary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeerRow(item: PeerInfoItem, colors: AppColors, langZh: Boolean) {
    var detailOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { detailOpen = !detailOpen }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dotColor = when {
                item.latencyUs > 0 && item.latencyUs < 100_000 -> colors.statusGood
                item.latencyUs in 100_000..499_999 -> colors.statusWarn
                item.latencyUs > 0 -> colors.statusBad
                else -> Color(0xFF6B7280)
            }
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = if (item.isSelf) T("我的节点", "My Node", langZh)
                else item.hostname.ifEmpty { "${T("节点", "Node", langZh)} #${item.peerId}" },
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (item.isSelf) {
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.accent.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(T("本机", "Self", langZh), color = colors.accentLight, fontSize = 10.sp)
                }
                Spacer(Modifier.width(4.dp))
            }

            Text(
                text = item.ip,
                color = colors.textTertiary,
                fontSize = 12.sp,
                maxLines = 1
            )

            Spacer(Modifier.width(8.dp))

            val latencyColor = when {
                item.latencyUs > 0 && item.latencyUs < 100_000 -> colors.statusGood
                item.latencyUs in 100_000..499_999 -> colors.statusWarn
                item.latencyUs > 0 -> colors.statusBad
                else -> colors.textTertiary
            }
            val latencyText = if (item.isSelf && item.latencyUs <= 0)
                T("本机", "Self", langZh)
            else formatLatencyShort(item.latencyUs)
            Text(
                text = latencyText,
                color = latencyColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.width(4.dp))
            Text("›", color = colors.textTertiary, fontSize = 16.sp)
        }

        AnimatedVisibility(
            visible = detailOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 0.dp)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceDim)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PeerDetailGrid(item, colors, langZh)
                }
            }
        }
    }
}

@Composable
private fun PeerDetailGrid(item: PeerInfoItem, colors: AppColors, langZh: Boolean) {
    val rows = listOf(
        T("协议", "Protocol", langZh) to item.protocol,
        T("NAT", "NAT", langZh) to item.natType.ifEmpty { "—" },
        T("版本", "Version", langZh) to item.version.ifEmpty { "—" },
        T("丢包率", "Loss", langZh) to String.format("%.1f%%", item.lossRate),
        T("上行", "TX", langZh) to item.txSpeed.ifEmpty { "0 B/s" },
        T("下行", "RX", langZh) to item.rxSpeed.ifEmpty { "0 B/s" },
        T("延迟", "Latency", langZh) to formatLatencyShort(item.latencyUs),
    )
    rows.chunked(2).forEach { pair ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            pair.forEach { (key, value) ->
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(key, color = colors.textTertiary, fontSize = 12.sp)
                        Text(value, color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

private fun formatLatencyShort(us: Long): String = when {
    us >= 1_000_000 -> String.format("%.1fs", us / 1_000_000.0)
    us >= 1_000 -> "${us / 1_000}ms"
    us > 0 -> "${us}μs"
    else -> "—"
}
