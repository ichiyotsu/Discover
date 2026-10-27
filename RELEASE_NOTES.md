# Luma 0.1.0

首个公开预览版：一个把地址栏做成悬浮玻璃胶囊的浏览器，桌面和 Android 双端。

Luma 的地址栏与工具栏作为一个整体停靠在窗口顶部或底部，设置内可实时切换。网页内容装在圆角页面卡片里，会随工具栏位置自动让位。桌面端基于 Electron 45 / Chromium + md3e Web Components；Android 端基于 Jetpack Compose + 系统 Chromium WebView。

## 下载

| 平台 | 文件 | 说明 |
| --- | --- | --- |
| Android 12+ | `Luma-0.1.0-android.apk` | 系统 WebView / Chromium，使用调试密钥签名 |
| Windows x64 | 源码 | 运行 `pnpm install && pnpm package` 生成便携包 |
| 全部源码 | Source code (zip) | GitHub 自动生成 |

> Windows 便携包约 218MB，本次未随 Release 上传，避免超出发布带宽；桌面端源码、构建脚本与文档已完整发布。

SHA-256：

```text
Luma-0.1.0-android.apk
D2B8C269C3824671991570DD920C4D6487B30AC94A329C288D1E0C5C265B6FFD
```

## 亮点

- **悬浮地址栏 + 工具栏**：玻璃胶囊、双层阴影、弹簧形变；顶部 / 底部停靠可切换。
- **桌面端**：多标签、地址补全、书签、历史、下载、缩放、全屏、自定义窗口控制。
- **Android 端**：原生 Compose 工具栏、标签页底部面板、历史面板、设置面板、键盘自动避让。
- **Material 3 Expressive**：桌面使用 md3e 的 HCT 动态色 / tokens / Web Components；Android 用 Compose 复刻同一套设计语言。
- **本地新标签页**：时间、问候、快捷网站、搜索，跟随主题和搜索引擎。
- **8 种主题色种子**、明暗模式、Bing / Google / 百度 / DuckDuckGo 搜索。

## 已知问题

- Windows 包未签名。若被 SmartScreen、Windows Smart App Control 或组织的应用控制策略拦截，需要手动允许，或从源码构建。
- Android APK 使用 debug keystore 签名，适合本地安装，不适合上架。
- 无 Widevine，Netflix 等 DRM 流媒体不可用。
- `window.open` 登录流程会转成新标签；少数依赖 opener 的 OAuth 流程可能失败。
- Android 端暂无书签管理、Incognito、跨进程标签恢复；网页摄像头 / 麦克风权限默认拒绝。
- WebView 行为取决于设备上的 Android System WebView 版本。

## 验证

- 桌面版通过 Playwright 流程测试：真实网页加载、标签、书签、地址补全、错误页、明暗主题、顶部 / 底部切换。
- Android 版在 Android 12（API 31）模拟器验证：启动、地址栏键盘、网页加载、标签页、顶部 / 底部切换、设置持久化。

## 从源码构建

```powershell
pnpm install
pnpm package
```

Android 构建见 `android/README.md`。

完整变更与设计说明：`README.md` / `DESIGN.md`。
