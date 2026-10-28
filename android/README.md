# Luma for Android

Luma 的原生 Android 版本：**Jetpack Compose + Android WebView（Chromium）**。当前测试版 **0.1.0 Beta 1**。

网页全屏显示，工具栏作为悬浮层覆盖在网页之上；底栏可随滚动自动收起成只显示网址的小胶囊。

![Luma Android](../docs/android-top.png)

## 0.1.0 Beta 1 更新

- **全屏网页 + 悬浮底栏**：网页铺满系统栏之间，底栏悬浮在页面上；底部注入滚动留白，网页最后一行可以滚到栏上方。
- **滚动自动收起**：向下滚动底栏收成只显示网址的胶囊，向上滚动或点击胶囊展开；设置内可关闭。
- **二级设置页**：设置改为全屏主列表 + 外观 / 工具栏 / 搜索引擎 / 标签页 / 数据 / 更新 / 关于 二级页面。
- **自定义搜索引擎**：支持搜索 URL 模板（`{query}` 或 `%s` 占位符）。
- **应用内更新**：从 GitHub Releases 检查更新，支持正式版 / Beta 通道；启动时自动检查，下载 APK 后调用系统安装器。
- **全屏标签页**：2 列网格 / 列表可切换，支持网页实时预览缩略图；后台 WebView 保持预加载。
- **工具栏自定义**：后退、前进、刷新、标签页、翻译可单独开关；地址栏和菜单始终保留。
- **翻译**：工具栏或菜单可打开 Google 翻译，自动翻译当前页；有选中文本时翻译选中内容。
- **历史导入导出**：支持 Luma JSON、Chrome / Edge 导出的历史 HTML、Chrome Takeout 的 `BrowserHistory.json`；通过系统文件选择器可直接选择 Google Drive / OneDrive 中的文件。
- **应用图标**：MingCute `ai-fill`，108dp adaptive icon + monochrome，符合 Android 13 主题图标规范。
- **历史管理**：完整列表、搜索、单条删除、按最近 1 小时 / 今天 / 7 天 / 全部清空，全部需要二次确认。

## 安装

```text
Luma-0.1.0-beta1-android.apk
```

要求 Android 12（API 31）或更高。APK 使用 debug keystore 签名，适合本地安装，不适合上架。

## 构建

环境：

- JDK 17+（本机使用 Android Studio JBR 25）
- Android SDK Platform 37、Build-Tools 36
- Gradle 9.6（已包含 wrapper）

```powershell
cd android
$env:JAVA_HOME = "D:\Application\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleRelease
```

输出：

```text
app/build/outputs/apk/release/app-release.apk
```

## 结构

```text
app/src/main/java/com/luma/browser/
  MainActivity.kt           Activity、edge-to-edge、主题入口、Intent 处理
  browser/BrowserController.kt
                            WebView 生命周期、多标签、滚动收起、预览、翻译、下载
  data/LumaSettings.kt      设置（位置 / 主题 / 搜索 / 更新 / 标签页 / 工具栏）
  data/HistoryStore.kt      历史记录、Chrome 导入、JSON 导出、范围清理
  data/UpdateManager.kt     GitHub Releases 检查与 APK 下载
  ui/LumaBrowserScreen.kt   全屏页面、悬浮栏、补全、错误页、更新对话框
  ui/BrowserChrome.kt       展开/收起工具栏、导航、菜单、翻译入口
  ui/SettingsScreen.kt      全屏二级设置页
  ui/TabSwitcherScreen.kt   全屏标签页 + 网页预览
  ui/HistoryScreen.kt       全屏历史列表 + 范围清理确认
  ui/NewTabPage.kt          新标签页
  ui/theme/Theme.kt         Material 3 色彩、形状、字阶
```

## 同步与插件

- **Google Drive / OneDrive 同步**：直接 OAuth 同步需要 Google Cloud OAuth Client 和 Microsoft Graph 应用注册，本版本没有配置这些凭据。当前可通过系统文件选择器把 `Luma-history-*.json` 存到 Drive/OneDrive 文件夹，实现手动跨设备同步。
- **插件**：Android System WebView 不支持 Chrome 扩展。真正的插件体系需要基于 Chromium fork 或自建 WebView，当前架构无法安全加载 `.crx`。后续可做受限的用户脚本（userscript）注入，但不是 Chrome 插件兼容。

## 已知限制

- WebView 版本跟随系统；模拟器上的 Chromium 91 较旧。
- 暂无书签管理、Incognito、跨进程标签恢复。
- 摄像头 / 麦克风等网页权限目前默认拒绝。
- 发布 APK 使用 debug keystore 签名；上架前需要正式签名。
- 应用内更新下载的是 GitHub Release 中的 APK；若签名不同，系统会拒绝覆盖安装。
