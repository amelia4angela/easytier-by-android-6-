package com.easytier

import com.easytier.ui.ConfigState
import org.json.JSONObject

/**
 * Shared utility for ConfigState ↔ JSON ↔ TOML conversion.
 *
 * Single source of truth — both [MainActivity] and [ConfigEditActivity]
 * delegate to these functions so adding a field to [ConfigState] only
 * needs updates here (plus the Compose form itself).
 */
object TomlUtils {

    /** Sentinel value meaning "user hasn't filled this field yet" */
    const val TOML_FIELD_EMPTY = ""

    // ═══════════════════════════════════════════════
    //  ConfigState ↔ JSON
    // ═══════════════════════════════════════════════

    /** Serialize [ConfigState] to JSONObject (for file persistence). */
    fun configStateToJson(s: ConfigState): JSONObject = JSONObject().also { j ->
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

    /** Deserialize JSONObject → [ConfigState] (from file load). */
    fun jsonToConfigState(j: JSONObject): ConfigState = ConfigState(
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

    // ═══════════════════════════════════════════════
    //  ConfigState → TOML (export + daemon start)
    // ═══════════════════════════════════════════════

    /** Build TOML string from [ConfigState] for the EasyTier daemon. */
    fun buildToml(s: ConfigState): String {
        val sb = StringBuilder()

        val networkName = s.networkName.ifEmpty { TOML_FIELD_EMPTY }
        val networkSecret = s.secret.ifEmpty { TOML_FIELD_EMPTY }
        val server = s.server.ifEmpty { TOML_FIELD_EMPTY }
        val port = s.port.ifEmpty { TOML_FIELD_EMPTY }
        val ipAddr = s.ipAddress.ifEmpty { TOML_FIELD_EMPTY }
        val ipPrefix = s.ipPrefix.ifEmpty { "24" }
        val peerUri = if (server != TOML_FIELD_EMPTY && port != TOML_FIELD_EMPTY)
            "tcp://$server:$port"
        else
            "tcp://$TOML_FIELD_EMPTY"
        val instanceName = s.instanceName.ifEmpty { "default" }
        val hostname = s.hostname.ifEmpty { TOML_FIELD_EMPTY }

        sb.append("instance_name = \"$instanceName\"\n")
        if (hostname.isNotEmpty()) sb.append("hostname = \"$hostname\"\n")
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

        if (s.socks5.isNotEmpty()) sb.append("socks5 = \"${s.socks5}\"\n")

        if (s.portForward.isNotEmpty()) {
            val parts = s.portForward.split("://")
            if (parts.size == 2) {
                val slashIdx = parts[1].lastIndexOf('/')
                if (slashIdx > 0) {
                    val localPart = parts[1].substring(0, slashIdx)
                    val remotePart = parts[1].substring(slashIdx + 1)
                    sb.append("[[port_forward]]\n")
                    sb.append("port_forward_protocol = \"${parts[0]}\"\n")
                    sb.append("port_forward_local = \"$localPart\"\n")
                    sb.append("port_forward_remote = \"$remotePart\"\n")
                }
            }
        }

        if (s.encryptAlgo.isNotEmpty())
            sb.append("encryption_algorithm = \"${s.encryptAlgo}\"\n")
        if (s.compression.isNotEmpty() && s.compression != "none")
            sb.append("compression = \"${s.compression}\"\n")

        if (s.proxyNetworks.isNotEmpty()) {
            s.proxyNetworks.split(",").forEach { cidr ->
                val c = cidr.trim()
                if (c.isNotEmpty())
                    sb.append("[[proxy_networks]]\nproxy_network_cidr = \"$c\"\n")
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

    // ═══════════════════════════════════════════════
    //  TOML → JSON (import)
    // ═══════════════════════════════════════════════

    /** Parse raw TOML text into a JSONObject with the same keys as [configStateToJson]. */
    fun parseTomlToJson(text: String): JSONObject {
        val j = JSONObject()
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
        return j
    }

    /** Derive a human-friendly config name from parsed TOML content. */
    fun deriveConfigName(j: JSONObject): String {
        val fromNetwork = j.optString("networkName").ifEmpty { null }
        val fromInstance = j.optString("instanceName").ifEmpty { null }
        return fromNetwork ?: fromInstance ?: "imported"
    }

    /** Parse a TOML boolean value string. */
    fun parseBool(s: String): Boolean =
        s.trim().lowercase() in listOf("true", "yes", "1")
}
