# Orbit Easytier For Android

[![GitHub release](https://img.shields.io/github/v/release/amelia4angela/easytier-by-android-6-)](https://github.com/amelia4angela/easytier-by-android-6-/releases)
[![Platform](https://img.shields.io/badge/Android_6+-6a0dad?logo=android&label=minSdk)](https://developer.android.com/about/versions/marshmallow)
[![UI](https://img.shields.io/badge/UI-Material3-7C3AED?logo=materialdesign)](https://m3.material.io/)
[![License](https://img.shields.io/badge/license-Apache_2.0-blue.svg)](LICENSE)
[![Downloads](https://img.shields.io/github/downloads/amelia4angela/easytier-by-android-6-/total?color=success)](https://github.com/amelia4angela/easytier-by-android-6-/releases)

---

A polished Android client for [EasyTier](https://github.com/EasyTier/EasyTier) — secure, P2P mesh VPN, rebuilt from the ground up with a modern UI.

While the official EasyTier already has an Android build (supports **Android 7.0+ / API 24+**), Orbit extends one version further to **Android 6.0 (API 23)** and ships a richer native experience with **Material3** design, multi-profile configuration, and real-time peer visualization.

| Feature | Official Android | Orbit |
|:--------|:----------------:|:-----:|
| Minimum Android | 7.0+ (API 24) | **6.0+ (API 23)** |
| UI Framework | System default | **Material3 (Jetpack Compose)** |
| Theme | N/A | **Purple gradient, light/dark** |
| Configuration UI | Basic | **Full editor with TOML support** |
| P2P status indicator | Text only | **Green tag + visual distinction** |
| Real-time dashboard | — | **Speed, latency, peers, logs** |

---

## Features

- 📱 **Android 6.0 ~ 15** — full backward compatibility, tested on devices from 2015 to 2025
- 🎨 **Material3 purple gradient** — light and dark themes, smooth animations
- 🛡️ **VPNService based** — uses Android's `VpnService` with tun2io virtual interface
- 🔒 **Encrypted P2P mesh** — NAT traversal, relay fallback, end-to-end encryption
- 🟢 **P2P / relay detection** — real-time green tag for P2P connections
- 📊 **Live status** — IP, latency, bandwidth, uptime at a glance
- 📝 **On-device log viewer** — scrollable, pausable, shareable
- 🔄 **Background persistence** — stays connected after app is closed (foreground service)
- 🔋 **Battery optimization exclusion** — exempt from Android Doze mode
- 🚨 **VPN conflict detection** — auto-detects other active VPNs
- 🌐 **Bilingual (Chinese / English)** — follows system locale
- 📋 **Multi-profile config management** — import, export, edit, switch

---

## Screenshots

| Light mode | Dark mode | Peers panel |
|:----------:|:---------:|:-----------:|
| Purple gradient<br/>Clean layout | Night-friendly<br/>Deep purple background | Green P2P tag<br/>Latency & type |

| Config editor | Log viewer | About |
|:-------------:|:----------:|:-----:|
| Transparent inputs<br/>TOML editing | Real-time scrolling<br/>Shareable | Version, uptime<br/>Build info |

---

## Architecture

```
orbit/
├── src/main/java/com/easytier/
│   ├── MainActivity.kt           # Main entry point
│   ├── ConfigEditActivity.kt     # TOML configuration editor
│   ├── NotificationHelper.kt     # Foreground service notification
│   ├── AppLogger.kt              # Ring-buffer log (memory safe)
│   ├── jni/
│   │   ├── EasyTierManager.kt    # Singleton lifecycle manager
│   │   └── EasyTierJNI.kt        # Rust JNI bridge
│   ├── vpn/
│   │   └── EasyTierVpnService.kt # VpnService with auto-reconnect
│   └── ui/
│       ├── AppText.kt            # Localized strings
│       ├── MainScreen.kt         # Compose main screen
│       ├── ConfigForm.kt         # Config form components
│       ├── ConfigState.kt        # Local config persistence
│       ├── ConnectionButton.kt   # Start/stop button with animation
│       ├── PeerInfoSection.kt    # P2P peer panel with status tags
│       └── theme/                # Purple gradient Material3 theme
```

### Tech Stack

| Layer | Technology |
|:------|:-----------|
| UI | Jetpack Compose + Material3 |
| Theme | Custom purple gradient |
| VPN | `VpnService.Builder` + tun2io |
| Core | EasyTier Rust via JNI (`libeasytier_jni.so`) |
| Notifications | `NotificationCompat` + foreground service |
| Build | Gradle + Android SDK 34 + Kotlin |

---

## Building from source

```bash
git clone https://github.com/amelia4angela/easytier-by-android-6-.git
cd easytier-by-android-6-
./gradlew :orbit:assembleRelease
```

APK output: `orbit/build/outputs/apk/release/orbit-release.apk`

**Prerequisites:** Android SDK 34, NDK (or use prebuilt jniLibs), Java 17+

---

## Download

### [⬇️ Download APK (latest release)](https://github.com/amelia4angela/easytier-by-android-6-/releases)

---

## Changelog

### v0.1.2

- 🟢 **P2P green tag** — peers are now visually distinguished by connection type
- 🚫 Removed `(via relay)` suffix from labels
- 🐛 Fixed latency unit conversion (μs → ms)
- 🎯 Improved P2P / relay detection accuracy
- ⏱️ Always poll every second

### v0.1.0

- 🎉 Initial release
- 🎨 Material3 purple gradient theme (light/dark)
- 📱 Android 6+ support
- 🌐 Bilingual UI (Chinese / English)
- 🦀 Rust JNI core engine
- 📡 P2P mesh networking + NAT traversal + encryption
- 📋 Multi-profile configuration management
- 📊 Real-time peer dashboard
- 🔔 Notification bar with live status
- 🔄 Background keep-alive (foreground service)
- 🔋 Battery optimization exclusion
- 🚨 VPN conflict detection

---

## License

Apache 2.0

---

## Acknowledgements

Built on top of [EasyTier](https://github.com/EasyTier/EasyTier) — a lightweight, cross-platform P2P mesh VPN written in Rust. The core networking engine (NAT traversal, encryption, virtual interface) is entirely theirs; this project wraps it in a native Android UI.
