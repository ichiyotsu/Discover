# TokiDiscover

Toki 系列的轻量三端浏览器：底部悬浮胶囊地址栏、多标签（含无痕）、书签/历史/下载书库，
设计规范与 Tokinote / tokimusic 完全一致（Matcha 配色、TokiShapes、悬浮毛玻璃胶囊）。

- 版本：**0.1.0-beta1**
- 平台与引擎：Android（Kotlin + Jetpack Compose，系统 WebView，包名 `com.tokidiscover.app`）/
  iOS（SwiftUI，WKWebView）/ Windows（原生 WinUI 3，WebView2）
- 默认搜索引擎：Bing（设置里可换 Google / DuckDuckGo / 百度 / 自定义模板）

## 功能清单

- Omnibox：输入智能判断（网址补 `https://`、搜索词按引擎拼 URL 并编码）、前进/后退/刷新/停止、
  primary 色加载进度条、站点标题显示；`javascript:` 等危险协议拦截
- 多标签：新建/关闭/切换、标签概览网格（卡片、当前高亮）、无痕标签（不写历史）
- 新标签页：胶囊搜索框 + 常用站点 Speed Dial（Bilibili / GitHub / Wikipedia 等）+ 最近历史
- 书库：PillGroup 切换 书签 / 历史 / 下载（书签默认文件夹、历史可清空；下载见各端限制说明）
- 页内查找、分享当前页、桌面版 UA 与 JavaScript 开关、按项清除 Cookie/缓存/历史
- 设置：搜索引擎、主题（跟随系统/浅/深）、关于（读真实版本、检查更新优先解析安装包文件名里的版本）

未实现/限制（诚实说明）：下载的实际文件管理、Windows 桌面 UA 与跨页面清数据接线、WebView2 无痕环境隔离、
广告拦截/扩展/同步均未做 — 完整清单见 `docs/ARCHITECTURE.md`。

## 目录

```
android/   Kotlin Compose 工程（com.tokidiscover.app）
ios/       SwiftUI 工程 + TokiDiscoverCore 纯逻辑包（UrlNormalizer + 测试）
windows/   WinUI 3 工程（TokiDiscover.Core 纯逻辑库 + TokiDiscover + TokiDiscover.Tests）
docs/      DESIGN.md 设计令牌 / ARCHITECTURE.md 引擎与限制
```

## 构建说明

- Android：Android Studio 打开 `android/` 或 `./gradlew :app:assembleDebug`（需 JDK 17 + Android SDK）。
  本仓库在 Linux VM 上**未编译** Android（当时无 JDK/Android SDK），以 CI 为准。
- iOS：Xcode 打开 `ios/TokiDiscover.xcodeproj`；纯逻辑包可 `cd ios/TokiDiscoverCore && swift test`（需 Swift 工具链）。
  Linux 无 Xcode/Swift，**未编译** iOS 本体与 Core 包；pbxproj 为手写最小壳，需 Xcode 打开验证。
- Windows：Visual Studio 打开 `windows/TokiDiscover.sln` 构建 WinUI 本体（需 Windows）。
  其中纯逻辑库与测试可在任何平台构建：`dotnet build windows/src/TokiDiscover.Core`、`dotnet test windows/tests/TokiDiscover.Tests`
  （注意：某些 Linux VM 上 vstest 的回环通信会被代理干扰，可改用 xunit console runner 直接跑测试 DLL）。

## 说明

Toki 系列个人项目。浏览器基于各平台系统 Web 引擎（WebView / WKWebView / WebView2），不包含自研内核。
GitHub 仓库 `ichiyotsu/tokidiscover` 待手动创建后推送；`.github/workflows` 文件日后经 GitHub App 推送可能 403，需网页端补。
