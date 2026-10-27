# Luma for Android

Luma 的原生 Android 版本：**Jetpack Compose + Android WebView（Chromium）**。

悬浮地址栏和工具栏一起停靠在页面顶部或底部，设置内实时切换并持久化。网页由系统 Android System WebView 渲染，地址栏、标签页、历史、设置均为原生 Compose。

![Luma Android](https://raw.githubusercontent.com/ichiyotsu/Discover/main/docs/android-top.png)

## 功能

- 悬浮工具栏：前进、后退、刷新 / 停止、地址栏、标签页、菜单。
- 工具栏顶部 / 底部停靠，切换时页面自动让位。
- 地址栏：网址识别、搜索、历史记录补全、HTTPS 状态。
- 键盘避让：底部模式下输入时工具栏浮在键盘上方。
- 标签页：底部面板切换 / 新建 / 关闭，显示标题、URL、favicon。
- 历史记录面板：点击打开、清空。
- 设置：工具栏位置、明暗模式、搜索引擎（Bing / Google / 百度 / DuckDuckGo）、清除历史。
- 深色模式、系统栏配色、系统返回、外链 Intent、下载、WebView 新窗口。
- 新标签页：问候、时间、快捷方式、搜索入口。
- 错误页：主框架加载失败时显示 Compose 错误卡片。

## 安装

Release 0.1.0 的 APK：

```text
Luma-0.1.0-android.apk
```

要求 Android 12（API 31）或更高。安装包使用调试密钥签名，适合本地安装，不适合上架。

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

调试包：

```powershell
.\gradlew.bat assembleDebug
```

输出：

```text
app/build/outputs/apk/release/app-release.apk
app/build/outputs/apk/debug/app-debug.apk
```

## 结构

```text
app/src/main/java/com/luma/browser/
  MainActivity.kt           Activity、edge-to-edge、主题入口、Intent 处理
  browser/BrowserController.kt
                            WebView 生命周期、多标签、导航、下载、新窗口
  data/LumaSettings.kt      SharedPreferences 设置（位置 / 主题 / 搜索引擎）
  data/HistoryStore.kt      本地历史与补全
  ui/LumaBrowserScreen.kt   页面卡片、工具栏定位、IME 避让、错误页、补全
  ui/BrowserChrome.kt       悬浮胶囊：导航、地址栏、菜单
  ui/NewTabPage.kt          新标签页
  ui/Sheets.kt              设置 / 标签页 / 历史底部面板
  ui/theme/Theme.kt         Material 3 色彩、形状、字阶
  ui/Ambient.kt             环境渐变背景
```

## 设计说明

桌面版的界面直接运行 md3e Web Components。md3e 是 Web Components，无法在 Compose 里原样使用，因此 Android 端复用它的设计语言而不是代码：

- 胶囊形地址栏 + 悬浮工具栏，和桌面相同的顶部 / 底部停靠逻辑。
- 26dp 页面大圆角、32dp 工具栏圆角、柔和阴影与半透明表面。
- 从 `#5B5BD6` 种子色出发的品牌配色，深色模式使用独立 surface 层级。
- Compose `spring` 动效与桌面端的 0.34 / 1.42 弹簧曲线保持同一气质。

## 已知限制

- WebView 版本跟随系统；模拟器上的 Chromium 91 较旧，真实设备通常更新。
- 暂不包含书签管理、Incognito、跨进程标签恢复。
- 摄像头 / 麦克风等网页权限目前默认拒绝。
- 不支持扩展、账户同步、自动更新。
- 发布 APK 使用 debug keystore 签名；上架前需要替换为正式签名。
