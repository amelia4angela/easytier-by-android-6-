package com.easytier.ui

/**
 * UI state for all config form fields.
 * Mirrors the fields in MainActivity's existing XML layout.
 * Used by Compose to read/write values without touching Android Views.
 */
data class ConfigState(
    // Primary fields
    val networkName: String = "",
    val secret: String = "",
    val server: String = "",
    val port: String = "",
    val ipAddress: String = "",
    val ipPrefix: String = "",

    // Instance identity
    val instanceName: String = "",
    val hostname: String = "",

    // Network
    val dhcp: Boolean = false,
    val disableIpv6: Boolean = false,
    val noTun: Boolean = false,

    // P2P
    val disableP2p: Boolean = false,
    val p2pOnly: Boolean = false,
    val lazyP2p: Boolean = false,
    val needP2p: Boolean = false,

    // NAT / Hole Punch
    val disableTcpHp: Boolean = false,
    val disableUdpHp: Boolean = false,
    val disableSymHp: Boolean = false,

    // Proxy / KCP / QUIC
    val enableKcpProxy: Boolean = false,
    val disableKcpInput: Boolean = false,
    val enableQuicProxy: Boolean = false,
    val disableQuicInput: Boolean = false,
    val proxyForwardBySystem: Boolean = false,
    val relayAllRpc: Boolean = false,
    val exitNode: Boolean = false,

    // Performance
    val latencyFirst: Boolean = false,
    val multiThread: Boolean = false,
    val useSmoltcp: Boolean = false,

    // Security
    val disableEncrypt: Boolean = false,
    val privateMode: Boolean = false,
    val acceptDns: Boolean = false,

    // Extra text fields
    val bindDevice: String = "",
    val mtu: String = "",
    val socks5: String = "",
    val portForward: String = "",
    val encryptAlgo: String = "",
    val compression: String = "",
    val proxyNetworks: String = "",
    val exitNodes: String = ""
)
