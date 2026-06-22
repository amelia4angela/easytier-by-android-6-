# 🛡️ EasyTier for Android — 适配 Android 6+

[![GitHub release](https://img.shields.io/github/v/release/amelia4angela/easytier-by-android-6-)](https://github.com/amelia4angela/easytier-by-android-6-/releases)
[![Platform](https://img.shields.io/badge/platform-Android_6+-6a0dad?logo=android)](https://developer.android.com/about/versions/marshmallow)
[![License](https://img.shields.io/badge/license-Apache_2.0-blue.svg)](LICENSE)
[![Official EasyTier](https://img.shields.io/badge/Powered_by-EasyTier-00b4d8?logo=rust)](https://github.com/EasyTier/EasyTier)
[![Downloads](https://img.shields.io/github/downloads/amelia4angela/easytier-by-android-6-/total?color=success)](https://github.com/amelia4angela/easytier-by-android-6-/releases)
[![GitHub last commit](https://img.shields.io/github/last-commit/amelia4angela/easytier-by-android-6-)](https://github.com/amelia4angela/easytier-by-android-6-/commits/main)

> **EasyTier** — 一款**简单、安全、去中心化**的内网穿透 VPN 组网工具，使用 Rust 语言和 Tokio 异步框架开发。  
> **EasyTier for Android** 是 **唯一适配 Android 6+ 的老设备 EasyTier 客户端**。官方无 Android 版，本项目将 EasyTier 的 Rust 核心通过 **JNI 桥接**移植到 Android 平台，专为老旧手机/平板优化，提供完整的 Material3 图形界面，支持 Android 6.0 (API 23) 至 15+。
>
> 🏠 **[官方 EasyTier 项目](https://github.com/EasyTier/EasyTier)** — 支持 Mac、Windows、Linux、Docker（amd64 / arm64）  
> 📖 **[官方文档](https://easytier.cn/)** — 组网配置、网络拓扑、高级功能  
> 💬 **[官方 Discord](https://discord.gg/EasyTier)** — 社区讨论与技术交流

---

## 🎯 项目介绍

**EasyTier for Android** 是 [EasyTier](https://github.com/EasyTier/EasyTier) 的 Android 客户端实现。EasyTier 本身是一个使用 **Rust 语言** 和 **Tokio 异步框架** 开发的去中心化 VPN 组网工具，支持：

- **二层/三层 VPN** — 点对点直连，无需中心服务器
- **NAT 穿透** — STUN / TURN 自动协商，各种网络环境都能组网
- **加密通信** — WireGuard 协议加密隧道
- **去中心化** — 无单点故障，自动发现对等节点
- **跨平台** — 原生支持 Mac、Windows、Linux、Docker (amd64 / arm64)

本项目在 Android 端以 **JNI 桥接** 方式调用 EasyTier 的 Rust 核心库（`libeasytier_jni.so`），通过 `VpnService.Builder` 创建虚拟网卡，为手机用户提供原生体验的 VPN 客户端。

### 🎯 专为 Android 6+ 老设备适配

市面上大多数 Android VPN 应用已放弃 Android 6/7/8 支持，但大量老旧手机、平板、机顶盒仍在使用这些版本。本项目从底层就针对 Android 6+ 做了专门优化：

| 适配项 | 说明 |
|--------|------|
| **minSdk=23** | 最低支持 Android 6.0 Marshmallow |
| **VpnService 兼容** | 不使用 Android 8+ 才有的 `setUnderlyingNetworks` 等 API |
| **老架构兼容** | 针对 x86 老旧设备的 `jniLibs` 预编译库 |
| **前台服务适配** | 兼容 API 23-34 的前台服务类型变更 |
| **权限模型适配** | 处理 API 23 运行时权限到 API 33+ 通知权限的全覆盖 |
| **低配流畅运行** | 轻量 Material3 UI，对 1GB 内存设备友好 |

### 为什么需要 EasyTier for Android？

目前 EasyTier 官方仅提供桌面端（Mac/Windows/Linux）和 Docker 部署，**没有官方 Android 客户端**。本项目填补了这一空白，让你可以在手机上直接参与 EasyTier 组网——无论你是远程访问家中 NAS、出差连接公司内网，还是组建多设备游戏局域网。

> 如果你在找 EasyTier 的服务器端或桌面版，请移步 **[官方 EasyTier 仓库 →](https://github.com/EasyTier/EasyTier)**

---

## ✨ 特性

| 特性 | 状态 |
|------|------|
| 🚀 基于官方 EasyTier Rust 核心 | ✅ |
| 📱 Android 6.0 (API 23) 至 15+ 全版本兼容 | ✅ |
| 🎨 Material3 UI（浅色/深色模式） | ✅ |
| 🔄 手动覆盖深色/浅色主题（持久化） | ✅ |
| 🌐 中英文双语界面（跟随系统 / 手动切换） | ✅ |
| 📋 配置管理（保存/编辑/导入/导出/删除/多配置切换） | ✅ |
| 🔐 配置编辑面板（节点密钥可见切换、基本/高级选项折叠） | ✅ |
| 📡 P2P 连接状态实时检测（节点数 / RTT / NAT 类型） | ✅ |
| 📊 网速统计（上行/下行实时速率） | ✅ |
| 📝 日志查看器（环形缓冲区、自动滚动/暂停/清除/分享） | ✅ |
| 🔔 系统通知栏实时状态（IP、运行时长） | ✅ |
| 🔄 **后台保活** — 退出应用后 VPN 持续运行 | ✅ |
| 🔋 **电池优化豁免** — 请求系统忽略电池限制，防止后台被杀 | ✅ |
| 🚨 **VPN 冲突检测** — 其他 VPN 抢占时自动识别并提示 | ✅ |
| 🔁 **一键重连** — VPN 被抢占后直接重新连接 | ✅ |
| 📲 检查更新（从 GitHub Releases 拉取） | ✅ |
| ℹ️ 关于页面（版本、编译者、运行时长、GitHub 链接） | ✅ |

### 后台保活详情

> 在 Android 6+ 上，系统可能会因 **Doze 省电模式** 在后台杀死 VPN 服务。  
> 本应用通过以下多层机制确保 VPN 持续运行：
> 
> 1. **前台服务** — 前台通知让系统优先保活（通知栏显示 IP + 运行时长）
> 2. **单例管理器** — `EasyTierManager` 使用 `ApplicationContext`，不依赖 Activity 生命周期
> 3. **电池优化豁免** — 引导用户将应用加入「忽略电池优化」白名单
> 4. **START_STICKY 重启** — 服务被意外杀死后自动重启

---

## 📸 截图

| 浅色模式 | 深色模式 | 日志面板 |
|:-------:|:-------:|:--------:|
| (紫色渐变主题) | (深色背景 + 紫色强调) | (自动滚动日志) |

| P2P 状态 | 配置管理 | 关于页面 |
|:--------:|:--------:|:--------:|
| (实时节点信息) | (多配置切换) | (版本/运行时长) |

---

## 🏗️ 项目架构

```text
app/
├── src/main/java/com/easytier/
│   ├── MainActivity.kt           # 主界面 + Compose UI + 业务逻辑
│   ├── ConfigEditActivity.kt     # 配置编辑器（可滚动、透明背景、焦点动画）
│   ├── NotificationHelper.kt     # 前台服务通知管理（IP + 运行时长实时刷新）
│   ├── AppLogger.kt              # 环形缓冲区日志（2MB，自动截断）
│   ├── jni/
│   │   ├── EasyTierManager.kt    # 管理器单例 — 后台保活核心
│   │   └── EasyTierJNI.kt        # Rust JNI 桥接（启动/停止/状态查询）
│   ├── vpn/
│   │   └── EasyTierVpnService.kt # VpnService 实现（前台服务 + 自动重连）
│   └── ui/
│       ├── AppText.kt          # 共享翻译函数（T()）
│       ├── MainScreen.kt         # Compose 主界面（Tab 导航 + 底部 + 更多）
│       ├── ConfigForm.kt         # 配置表单组件（焦点动画边框、密钥可见切换）
│       ├── ConfigState.kt        # 配置管理状态（持久化存储）
│       ├── ConnectionButton.kt   # 启动/停止动画按钮（56dp 圆形）
│       ├── PeerInfoSection.kt    # P2P 节点信息面板（RTT / NAT 类型）
│       └── theme/
│           ├── AppColors.kt      # 浅色/深色色彩系统
│           ├── Color.kt          # 调色板定义（16 色调色板）
│           └── Theme.kt          # Material3 主题组合
```

### 技术栈

| 层 | 技术 |
|---|------|
| **UI** | Jetpack Compose + Material3 |
| **主题** | 自定义 Light/Dark 色板（`Color.kt`） |
| **VPN** | `VpnService.Builder` tun2io 虚拟网卡 |
| **核心** | EasyTier Rust JNI（`libeasytier_jni.so`） |
| **通知** | `NotificationCompat` + 前台服务 |
| **构建** | Gradle + Android SDK 34 |

---

## 🔧 本地构建

```bash
# 克隆仓库
git clone https://github.com/amelia4angela/easytier-by-android-6-.git
cd easytier-by-android-6-

# 构建 Release APK
./gradlew assembleRelease
```

APK 输出路径: `app/build/outputs/apk/release/app-release.apk`

### 前提条件

- Android Studio / SDK 34
- NDK（用于编译 Rust 原生库，或使用预编译 `jniLibs`）
- Java 17+（Gradle 8.x 需要）

### 从源码编译

本项目使用预编译的 `jniLibs`（`app/src/main/jniLibs/`），如果你需要从 Rust 源码编译：

```bash
# 需要安装 Rust + Android NDK 交叉编译工具链
cd rust/
cargo build --target aarch64-linux-android --release
cargo build --target armv7-linux-androideabi --release
cargo build --target i686-linux-android --release
```

---

## 📥 下载

从 **[Releases](https://github.com/amelia4angela/easytier-by-android-6-/releases)** 下载最新 APK。

---

## 📋 更新日志

### v0.1.10 (当前)
- **🧹 清理遗留**: 删除 Compose 迁移后残留的 9 个死 XML 资源文件（layout/menu/drawable）
- **🧼 代码整洁**: 批量清理无用 import、颜色值、命名规范，创建 proguard-rules.pro
- **🚀 构建完善**: 重构 IPv4 函数、补充构建配置注释、修复编译警告

### v0.1.9
- **🐛 修复**: socks5 TOML 输出缺失引号、`easytier_phone` 硬编码统一为 `default`
- **🧹 清理**: 删除死代码（checkForUpdates/showAboutDialog/compareVersions）
- **🧩 重构**: 抽取共享翻译函数 AppText.kt，消除 4 处重复定义
- **🗑️ 删除**: 废弃的 ConnectionButton 包装、onCheckUpdate 无用参数

### v0.1.8
- **🎨 UI 优化**: 编辑面板标题添加返回按钮、底部按钮右对齐、移除蓝色 ActionBar
- **🔧 主题**: 改用 NoActionBar 主题，彻底解决顶部蓝色框问题
- **🛠️ 代码修正**: 配置导入导出、instance_name 动态读取、DHCP 开关去重

### v0.1.7
- **🎨 UI 打磨**: 编辑面板透明背景，与主页面视觉统一
- **🔵 移除蓝色装饰**: SectionLabel 去掉蓝色竖线，更简洁干净
- **🔄 编辑面板可滚动**: `ConfigEditActivity` 和 `ConfigForm` 添加 `verticalScroll`，长配置不再溢出
- **✨ 输入框焦点动画**: `HmTextField` 聚焦时边框颜色过渡动画，提升交互质感
- **✅ Checkbox 勾号动画**: `HmCheckbox` 勾号淡入动画，手感更顺滑
- **🔐 密钥可见切换**: 配置编辑中节点密钥支持明文/密文切换显示

### v0.1.6
- **🛡️ VPN 冲突检测**: 其他 VPN 应用启动时自动识别并显示「VPN 被中断」红色提示
- **🔄 一键重连**: VPN 被抢占后点击启动按钮直接重新连接
- **🐛 版本号修复**: 关于页面版本号改为从 PackageManager 动态读取，不再硬编码
- **📝 UI 优化**: 连接状态指示灯和状态条更加直观清晰

### v0.1.3
- **✨ 后台保活**: 退出应用后 VPN 连接持续运行，不再随 Activity 销毁而断开
- **🔄 重连 UI**: 重新打开应用时自动识别 VPN 运行状态并恢复界面
- **🧩 单例重构**: EasyTierManager 改为单例模式，使用 ApplicationContext 替代 Activity 引用
- **🔋 电池优化豁免**: 首次启动和连接时自动请求忽略电池优化权限（防止 Doze 模式杀后台进程）
- **🛡️ RECEIVE_BOOT_COMPLETED**: 声明开机启动权限，为后续开机自启做准备

### v0.1.2
- **🔔 通知栏**: 新增前台服务通知（IP + 运行时长实时刷新）+ 连接事件通知
- **⏰ 运行时长**: 关于页面新增秒级刷新运行时长显示
- **🛡️ 权限适配**: API 33+ 请求 POST_NOTIFICATIONS
- **🐛 修复**: 前台服务类型兼容 API 34

### v0.1.1
- **🎨 UI 大改版**: 全面替换为 Material3 风格
- **🌗 深色/浅色主题**: 支持手动切换并持久化
- **🌐 中英文双语**: 界面文字全部支持切换
- **📋 配置管理**: 文件化配置存储，支持多配置切换
- **📊 实时 P2P 状态**: 节点连接信息、网速统计
- **📝 日志面板**: 自动滚动 + 暂停功能
- **ℹ️ 关于页面**: 版本号、编译者、检查更新
- **🐛 修复**: 连接状态三态（已停止/正在启动/运行中）

### v0.1.0
- 初始版本
- 基于官方 EasyTier JNI 桥接
- 基础配置管理
- 启动/停止 VPN 功能
- Android 6+ 兼容

---

## 📄 许可证

本项目基于 **Apache 2.0** 许可证开源。  
EasyTier 核心组件由 [EasyTier 官方项目](https://github.com/EasyTier/EasyTier) 提供，遵循其开源协议。

---

## 🔗 相关链接

- **[EasyTier 官方仓库](https://github.com/EasyTier/EasyTier)** — Rust 核心实现，支持 Mac/Windows/Linux/Docker
- **[EasyTier 官方文档](https://easytier.cn/)** — 组网配置指南
- **[EasyTier WebUI](https://github.com/EasyTier/EasyTier-WebUI)** — 官方 Web 管理界面
- **[Releases 下载](https://github.com/amelia4angela/easytier-by-android-6-/releases)** — APK 下载
