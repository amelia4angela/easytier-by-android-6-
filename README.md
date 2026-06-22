# 🛡️ EasyTier for Android — 唯一适配 Android 6+ 的 EasyTier 客户端

[![GitHub release](https://img.shields.io/github/v/release/amelia4angela/easytier-by-android-6-)](https://github.com/amelia4angela/easytier-by-android-6-/releases)
[![Platform](https://img.shields.io/badge/Android_6+-6a0dad?logo=android&label=minSdk)](https://developer.android.com/about/versions/marshmallow)
[![UI](https://img.shields.io/badge/UI-Material3-7C3AED?logo=materialdesign)](https://m3.material.io/)
[![License](https://img.shields.io/badge/license-Apache_2.0-blue.svg)](LICENSE)
[![Powered by EasyTier](https://img.shields.io/badge/Powered_by-EasyTier-00b4d8?logo=rust)](https://github.com/EasyTier/EasyTier)
[![Downloads](https://img.shields.io/github/downloads/amelia4angela/easytier-by-android-6-/total?color=success)](https://github.com/amelia4angela/easytier-by-android-6-/releases)

> **唯一专为 Android 6+ 打造的 EasyTier VPN 客户端** — 支持老手机、平板、机顶盒  \
> 原生 Material3 设计语言，浅色/深色主题，中英文双语界面  \
> 基于 EasyTier Rust JNI 核心，提供稳定安全的 P2P 组网能力

---

## 📱 为什么选择 EasyTier for Android？

| 💡 | 说明 |
|----|------|
| **🎯 适配 Android 6+** | minSdk=23，覆盖从 Marshmallow 到 Android 15 的全系设备，老手机也能用 |
| **🎨 精致 Material3 UI** | 紫色渐变主题 + 自定义色板，浅色/深色模式，输入框焦点动画，媲美原生应用 |
| **🦀 Rust 核心驱动** | 通过 JNI 桥接 EasyTier 官方 Rust 核心，性能强、内存占用低 |
| **📡 二层/三层 VPN** | 点对点直连，NAT 穿透，加密通信，去中心化组网 |
| **🔄 后台保活** | 前台服务 + 单例管理器 + 电池优化豁免，退出应用 VPN 不断 |
| **🌐 中英文双语** | 界面文字全部支持中英文切换，跟随系统或手动设置 |

### 专为老设备优化

市面上大多数 Android VPN 应用已放弃 Android 6/7/8 支持，但大量老旧手机、平板、机顶盒仍在使用这些版本。本项目从底层就针对 Android 6+ 做了专门优化：

| 适配项 | 说明 |
|--------|------|
| **minSdk=23** | 最低支持 Android 6.0 Marshmallow |
| **VpnService 兼容** | 不使用 Android 8+ 才有的 `setUnderlyingNetworks` 等 API |
| **x86 老架构支持** | 预编译 x86_64 jniLibs，老旧平板也能跑 |
| **前台服务适配** | 兼容 API 23–34 的前台服务类型变更 |
| **权限全覆盖** | API 23 运行时权限 → API 33+ 通知权限 |
| **低配流畅运行** | 轻量 Compose UI，1GB 内存设备也能流畅使用 |

> 💡 官方 EasyTier 仅提供桌面端（Mac/Windows/Linux）和 Docker 部署，**没有 Android 客户端**。本项目填补了空白。

---

## ✨ 全部特性

| 特性 | 状态 |
|------|------|
| 🚀 基于官方 EasyTier Rust 核心 | ✅ |
| 📱 **Android 6.0 (API 23) 至 15+** | ✅ |
| 🎨 **Material3 紫色渐变主题**（浅色/深色） | ✅ |
| 🌙 手动覆盖深色/浅色主题（持久化） | ✅ |
| 🌐 **中英文双语界面**（跟随系统 / 手动切换） | ✅ |
| 📋 配置管理（保存/编辑/导入/导出/多配置切换） | ✅ |
| 🔐 配置编辑面板（密钥可见切换、基本/高级选项折叠） | ✅ |
| 📡 **P2P 实时检测**（节点数 / RTT / NAT 类型） | ✅ |
| 📊 网速统计（上行/下行实时速率） | ✅ |
| 📝 日志查看器（环形缓冲区、自动滚动/暂停/分享） | ✅ |
| 🔔 系统通知栏实时状态（IP、运行时长） | ✅ |
| 🔄 **后台保活** — 退出应用后 VPN 持续运行 | ✅ |
| 🔋 **电池优化豁免** — 请求忽略电池限制，防止后台被杀 | ✅ |
| 🚨 **VPN 冲突检测** — 其他 VPN 抢占时自动识别并提示 | ✅ |
| 🔁 **一键重连** — VPN 被抢占后直接重新连接 | ✅ |
| 📲 检查更新（从 GitHub Releases 拉取） | ✅ |
| ℹ️ 关于页面（版本、编译者、运行时长） | ✅ |

---

## 🖼️ 界面预览

> Material3 设计语言 · 紫色渐变主题 · 输入框焦点动画 · 深色/浅色完美适配

| 浅色模式 | 深色模式 | 日志面板 |
|:--------:|:--------:|:--------:|
| 紫色渐变主色调<br/>清晰的信息层级 | 深色背景 + 紫色强调<br/>护眼舒适 | 自动滚动日志<br/>支持暂停/分享 |

| P2P 节点状态 | 配置编辑器 | 关于页面 |
|:----------:|:----------:|:--------:|
| 实时节点数 / RTT / NAT 类型 | 透明背景 + 焦点动画边框 | 版本号、运行时长<br/>GitHub 链接 |

---

## 🏗️ 项目架构

```text
app/
├── src/main/java/com/easytier/
│   ├── MainActivity.kt           # 主界面 + Compose UI + 业务逻辑
│   ├── ConfigEditActivity.kt     # 配置编辑器（Material3 主题同步）
│   ├── NotificationHelper.kt     # 前台服务通知
│   ├── AppLogger.kt              # 环形缓冲日志
│   ├── jni/
│   │   ├── EasyTierManager.kt    # 单例管理器（后台保活）
│   │   └── EasyTierJNI.kt        # Rust JNI 桥接
│   ├── vpn/
│   │   └── EasyTierVpnService.kt # VpnService（前台服务 + 自动重连）
│   └── ui/
│       ├── AppText.kt            # 共享翻译
│       ├── MainScreen.kt         # Compose 主界面
│       ├── ConfigForm.kt         # 配置表单（焦点动画、密钥切换）
│       ├── ConfigState.kt        # 配置状态持久化
│       ├── ConnectionButton.kt   # 启动/停止动画按钮
│       ├── PeerInfoSection.kt    # P2P 节点面板
│       └── theme/                # Material3 主题色板
```

### 技术栈

| 层 | 技术 |
|---|------|
| **UI** | Jetpack Compose + Material3 |
| **主题** | 自定义 Light/Dark 紫色渐变色板 |
| **VPN** | `VpnService.Builder` tun2io 虚拟网卡 |
| **核心** | EasyTier Rust JNI（`libeasytier_jni.so`） |
| **通知** | `NotificationCompat` + 前台服务 |
| **构建** | Gradle + Android SDK 34 + Kotlin |

---

## 🔧 本地构建

```bash
git clone https://github.com/amelia4angela/easytier-by-android-6-.git
cd easytier-by-android-6-
./gradlew assembleRelease
```

APK 路径：`app/build/outputs/apk/release/app-release.apk`

**前提条件：** Android SDK 34、NDK（或使用预编译 jniLibs）、Java 17+

---

## 📥 下载

➡️ **下载最新 APK：**[Releases](https://github.com/amelia4angela/easytier-by-android-6-/releases)

---

## 📋 更新日志

### v0.1.0
- 🎉 首个正式发布
- 🎨 **Material3 UI** — 紫色渐变主题，浅色/深色模式，输入框焦点动画
- 📱 **Android 6+ 兼容** — minSdk=23，覆盖全系老设备
- 🌐 **中英文双语** — 界面文字全部支持中英文切换
- 🦀 **Rust JNI 核心** — 基于 EasyTier 官方 Rust 核心
- 📡 **P2P 组网** — NAT 穿透、加密通信、去中心化
- 📋 **配置管理** — 保存/编辑/导入/导出/多配置切换
- 📊 **实时状态** — P2P 节点信息、网速统计
- 🔔 **通知栏** — IP + 运行时长实时刷新
- 🔄 **后台保活** — 退出应用 VPN 持续运行
- 🔋 **电池优化豁免** — 防止 Doze 模式杀后台
- 🚨 **VPN 冲突检测** — 自动识别并提示

---

## 📄 许可证

Apache 2.0 · Built with ❤️ for Android 6+ users

EasyTier 核心由 [EasyTier 官方项目](https://github.com/EasyTier/EasyTier) 提供。
