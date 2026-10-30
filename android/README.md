# Luna for Android

Luna 的原生 Android 版本：**Jetpack Compose + Android WebView（Chromium）**。当前测试版 **0.1.0-beta.2**。

网页全屏显示，工具栏作为悬浮层覆盖在网页之上；状态栏使用磨砂玻璃遮罩，底栏可随滚动或横向滑动收起。

![Luna Android](../docs/android-top.png)

## 0.1.0-beta.2 更新

- **品牌更名 Luna**：Android 应用名、界面文案、新标签页与关于页统一为 Luna；`applicationId` 仍保留 `com.luma.browser`，确保可以直接覆盖升级。
- **全新应用图标**：基于 Streamline `ai-edit-spark-solid`，108dp adaptive icon + 紫靛渐变背景 + monochrome，图形大小按 TokiNote 的安全区规范重制。来源见 [`docs/icon-sources.md`](../docs/icon-sources.md)。
- **简约主页与自定义背景**：默认主页只保留 Luna 字标和一个搜索框，继续使用当前渐变；外观页可从相册选择背景图片，或恢复默认。
- **Google Sans Flex + TokiUI**：全局 Material 排版使用 Google Sans Flex，圆角阶、层级和玻璃质感参照 TokiUI；中文缺字由系统字体回退。
- **磨砂状态栏**：状态栏覆盖半透明表面、柔光渐变与极细的分隔线，内容延伸到状态栏下方，形成磨砂玻璃层。
- **横向滑动收起底栏**：在底栏上向左或向右滑动，会收起为左下角 / 右下角的 Luna 展开按钮；点击按钮恢复完整工具栏。
- **全屏网页 + 悬浮底栏**：网页铺满系统栏之间，底栏悬浮在页面上；底部注入滚动留白，网页最后一行可以滚到栏上方。
- **滚动自动收起**：向下滚动底栏收成只显示网址的胶囊，向上滚动或点击胶囊展开；设置内可关闭。
- **全屏二级设置**：主列表 + 外观 / 工具栏 / 搜索引擎 / 标签页 / 数据 / 更新 / 关于。
- **自定义搜索引擎**：支持搜索 URL 模板（`{query}` 或 `%s` 占位符）。
- **应用内更新**：从 GitHub Releases 检查正式版 / Beta 通道，启动时自动检查，下载 APK 后调起系统安装器。
- **全屏标签页**：网格 / 列表布局、网页预览缩略图，后台 WebView 保持预加载。
- **工具栏自定义**：后退、前进、刷新、标签页、翻译可单独开关。
- **翻译**：菜单或工具栏打开 Google 翻译，整页翻译当前网页；有选中文本时优先翻译选中内容。
- **历史导入导出**：支持 Luna JSON、Chrome / Edge 历史 HTML、Chrome Takeout 的 `BrowserHistory.json`；文件选择器可直接访问 Google Drive / OneDrive。
- **历史管理**：完整列表、搜索、单条删除，以及按最近 1 小时 / 今天 / 7 天 / 全部清空，全部需要二次确认。

## 安装

```text
Luna-0.1.0-beta.2-android.apk
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
  ui/LumaBrowserScreen.kt   全屏页面、悬浮栏、横向滑动收起、补全、错误页、更新对话框
  ui/BrowserChrome.kt       展开/收起工具栏、导航、菜单、翻译入口
  ui/SettingsScreen.kt      全屏二级设置页
  ui/TabSwitcherScreen.kt   全屏标签页 + 网页预览
  ui/HistoryScreen.kt       全屏历史列表 + 范围清理确认
  ui/NewTabPage.kt          新标签页
  ui/theme/Theme.kt         Material 3 色彩、形状、字阶
```

> 目录名与 Kotlin 类名仍保留 `luma` 历史命名，仅用户可见品牌改为 Luna；这样不会丢失设置、历史与覆盖安装能力。

## 同步与插件

- **Google Drive / OneDrive 同步**：直接 OAuth 同步需要 Google Cloud OAuth Client 和 Microsoft Graph 应用注册，本版本没有配置这些凭据。当前可通过系统文件选择器把 `Luna-history-*.json` 存到 Drive/OneDrive 文件夹，实现手动跨设备同步。
- **插件**：Android System WebView 不支持 Chrome 扩展。真正的插件体系需要基于 Chromium fork 或自建 WebView，当前架构无法安全加载 `.crx`。后续可做受限的用户脚本（userscript）注入，但不是 Chrome 插件兼容。

## 已知限制

- WebView 版本跟随系统；模拟器上的 Chromium 91 较旧。
- 暂无书签管理、Incognito、跨进程标签恢复。
- 摄像头 / 麦克风等网页权限目前默认拒绝。
- 发布 APK 使用 debug keystore 签名；上架前需要正式签名。
- 应用内更新下载的是 GitHub Release 中的 APK；若签名不同，系统会拒绝覆盖安装。
