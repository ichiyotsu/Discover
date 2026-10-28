# Luma 0.1.0 Beta 1

Luma 的第二个公开版本，也是 Android 端的首个功能完整测试版。桌面端保持不变，本版本重点重做 Android 浏览体验。

## 下载

| 平台 | 文件 | 说明 |
| --- | --- | --- |
| Android 12+ | `Luma-0.1.0-beta1-android.apk` | 系统 WebView / Chromium，使用调试密钥签名 |
| Windows x64 | 源码 | 运行 `pnpm install && pnpm package` 生成便携包 |
| 全部源码 | Source code (zip) | GitHub 自动生成 |

> Windows 便携包约 218MB，本次仍未随 Release 上传；桌面端源码、构建脚本与文档已完整发布。

SHA-256：

```text
Luma-0.1.0-beta1-android.apk
C14E916DCB470E385EC2197F6B127468CDBD30D9AA9D4AB64A7721434D11A54D
```

## Android 新功能

- **全屏网页 + 悬浮底栏**：网页铺满系统栏之间，底栏悬浮在网页之上；页面底部注入滚动留白，最后一行内容不会被底栏挡住。
- **滚动自动收起**：向下滑动时底栏收成只显示网址的小胶囊，向上滑动或点击胶囊展开；设置内可关闭。
- **全屏二级设置**：主列表 + 外观 / 工具栏 / 搜索引擎 / 标签页 / 数据 / 更新 / 关于，不再挤在一个底部面板里。
- **自定义搜索引擎**：搜索 URL 模板支持 `{query}` 或 `%s` 占位符。
- **应用内更新**：从 GitHub Releases 检查正式版 / Beta 通道，启动时自动检查；下载 APK 后调起系统安装器。
- **全屏标签页**：网格 / 列表布局可切换，支持网页预览缩略图；后台标签页保持 WebView 预加载。
- **工具栏自定义**：后退、前进、刷新、标签页、翻译可单独开关；地址栏与菜单始终保留。
- **翻译**：菜单或工具栏打开 Google 翻译，整页翻译当前网页；有选中文本时优先翻译选中内容。
- **历史导入导出**：支持 Luma JSON、Chrome / Edge 导出的历史 HTML、Chrome Takeout 的 `BrowserHistory.json`；通过系统文件选择器可直接选择 Google Drive / OneDrive 中的文件。
- **历史管理**：完整列表、搜索、单条删除，以及按最近 1 小时 / 今天 / 7 天 / 全部清空，全部需要二次确认。
- **新应用图标**：MingCute `ai-fill`，108dp adaptive icon + monochrome，适配 Android 13 主题图标。

## 同步与插件说明

- **Google Drive / OneDrive 自动同步**尚未接入。直接同步需要 Google Cloud OAuth Client 和 Microsoft Graph 应用注册；当前可用系统文件选择器把历史 JSON 存到 Drive/OneDrive，实现手动跨设备同步。
- **Chrome 插件**不可用。Android System WebView 不提供扩展 API，真正的插件体系需要 Chromium fork 或自建内核；当前版本不加载 `.crx`。

## 已知问题

- Android APK 使用 debug keystore 签名，适合本地安装，不适合上架；后续正式版若换签名，将无法直接覆盖安装。
- Android 端暂无书签管理、Incognito、跨进程标签恢复；网页摄像头 / 麦克风权限默认拒绝。
- WebView 行为取决于设备上的 Android System WebView 版本。
- 应用内更新依赖 GitHub Releases 中的 APK 资产；网络受限时检查会静默失败。
- Windows 包未签名，可能被 SmartScreen 或应用控制策略拦截。
- 无 Widevine，Netflix 等 DRM 流媒体不可用。

## 验证

- Android 0.1.0-beta1 在 Android 12（API 31）模拟器验证：安装、启动、全屏网页、底栏悬浮、滚动收起/展开、二级设置、自定义搜索页、全屏标签页预览、历史列表、新图标。
- 桌面端构建与 Playwright 流程保持 0.1.0 的验证结果，本版本未改动桌面代码。

## 从源码构建

```powershell
pnpm install
pnpm package
```

Android 构建见 `android/README.md`。完整设计说明：`DESIGN.md`。
