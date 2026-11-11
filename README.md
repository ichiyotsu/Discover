# TokiDiscover

TokiDiscover 是轻量、开源的网页浏览器。悬浮地址栏会在页面向下滚动时收起，向上滚动或点按后展开。可以翻译页面，也可以选择搜索引擎。支持无痕和标签页。

当前版本 **0.1.0**。Android 与 iOS / iPadOS 的应用 ID 是 `dev.ichiyotsu.tokidiscover`。Windows 程序名是 `TokiDiscover.exe`。

| 平台 | 版本 |
| --- | --- |
| Windows | `windows/TokiDiscover/TokiDiscover.csproj` 的 `Version` 是 0.1.0。`AssemblyVersion` 和 `FileVersion` 是 0.1.0.0。`windows/installer/TokiDiscover.iss` 的 `MyAppVersion` 是 0.1.0。安装包名为 `TokiDiscover-0.1.0-windows-setup.exe`、`TokiDiscover-0.1.0-windows-portable.zip`。 |
| Android | `android/app/build.gradle.kts` 的 `versionName` 是 0.1.0，`versionCode` 是 1。安装包名为 `TokiDiscover-0.1.0-android.apk`。 |
| iOS / iPadOS | `ios/project.yml` 的 `MARKETING_VERSION` 是 0.1.0，写入 `CFBundleShortVersionString`。`CURRENT_PROJECT_VERSION` 是 1，写入 `CFBundleVersion`。安装包名为 `TokiDiscover-0.1.0-ios-unsigned.ipa`。 |

已发布的 [v0.1.0](https://github.com/ichiyotsu/Tokidiscover/releases/tag/v0.1.0) 是正式版，包含 Windows 安装包、Windows 便携版、Android APK 和未签名 iOS IPA。还没有 `0.1.1` 或 `0.1.1-b1`。标签 `0.1.0-beta.1`、`0.1.0-beta.2`、`0.1.0-beta.3` 没有 `v` 前缀，是改成现在这三个原生客户端之前的包。

## 版本规则

`X.Y.Z`（如 0.1.0）是正式版。`X.Y.Z-bN`（如 0.1.1-b1）是测试版。

发布顺序是 0.1.0，然后 0.1.1-b1、0.1.1-b2，直到 0.1.1。同一组数字上，带 `-bN` 的测试版小于去掉后缀的正式版，所以 0.1.1-b1 < 0.1.1。测试版的 `X.Y.Z` 比当前正式版超前几个是正常的。

标签加 `v` 前缀，例如 `v0.1.0`、`v0.1.1-b1`。`scripts/version.sh` 按上面的顺序比较版本。`.github/workflows/release.yml` 用它判断：版本是 `X.Y.Z-bN` 时创建 prerelease，其余 `v*` 标签创建正式 release。

三个客户端都没有应用内更新检查。

## 安装已发布的版本

从 [Releases](https://github.com/ichiyotsu/Tokidiscover/releases) 下载 [v0.1.0](https://github.com/ichiyotsu/Tokidiscover/releases/tag/v0.1.0)。

Windows 安装程序可以选择目录。默认装到当前用户的 `%LOCALAPPDATA%\TokiDiscover`，不需要管理员权限。便携版不用安装，解压后运行 `TokiDiscover.exe`。Android 安装 `TokiDiscover-0.1.0-android.apk`。iOS 包未签名，见下面的 SideStore 一节。

## 从源码构建

Windows 需要 Windows 10 或 11，以及 .NET 10 SDK。

```powershell
dotnet publish windows/TokiDiscover/TokiDiscover.csproj -c Release -r win-x64 --self-contained true -p:WindowsPackageType=None -p:WindowsAppSDKSelfContained=true -o windows/TokiDiscover/bin/publish
```

安装包使用 [Inno Setup 6](https://jrsoftware.org/isinfo.php)：

```powershell
& "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe" windows/installer/TokiDiscover.iss
```

Android 需要 JDK 21，以及 Android SDK `platforms;android-37.2` 和 `build-tools;37.0.0`。

```bash
cd android && chmod +x gradlew && ./gradlew assembleRelease
```

签名 APK 在 `android/app/build/outputs/apk/release/`。

iOS 需要 macOS 26、Xcode 26 和 [XcodeGen](https://github.com/yonaskolb/XcodeGen)。`.github/workflows/release.yml` 在 `macos-26` 上用 `xcodebuild` 打未签名 IPA。部署目标是 iOS 26.0。

```bash
brew install xcodegen
cd ios
xcodegen generate
xcodebuild \
  -project TokiDiscover.xcodeproj \
  -scheme TokiDiscover \
  -configuration Release \
  -sdk iphoneos \
  -destination 'generic/platform=iOS' \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGN_IDENTITY="" \
  build
```

把生成的 `TokiDiscover.app` 放进 `Payload/`，打成 zip，扩展名改为 `.ipa`。

这个工作流在拉取请求、手动触发，以及推送 `v*` 标签时编译三个平台。只有标签推送会创建 [GitHub Release](https://github.com/ichiyotsu/Tokidiscover/releases)。

## Windows 说明

界面是 C#、WinUI 3（Windows App SDK）和 WebView2。Windows 11 使用 Mica。不支持 Mica 时使用 Acrylic。再不支持则使用实色。标题栏融入窗口。

Windows 11 已包含 WebView2。Windows 10 需要先安装 [Microsoft Edge WebView2 Runtime](https://developer.microsoft.com/microsoft-edge/webview2/)（Evergreen）。

发布的 Windows 包是未打包、自包含的。不需要另装 Windows App SDK 运行时。

搜索引擎是 Google、Bing、DuckDuckGo、百度。地址栏区分网址和搜索词。还有后退、前进、刷新、主页和标签页。

无痕标签使用单独的临时 WebView2 目录。最后一个无痕标签关闭后删除该目录。

WebView2 没有公开的整页翻译 API。翻译会打开翻译服务页面。

## Android 说明

界面是 Kotlin、Jetpack Compose 和系统 WebView。主题使用 `MaterialExpressiveTheme` 和 `MotionScheme.expressive()`。Android 12 及以上跟随壁纸动态取色。底部悬浮栏在滚动时收起。

`minSdk` 是 26（Android 8.0）。`compileSdk` / `targetSdk` 是 37，对应 `platforms;android-37.2`。安装包只有 `arm64-v8a`。Release 开启了 R8 和资源压缩。

签名密钥库在 `android/tokidiscover.jks`。口令在 `android/keystore.properties`。别名、库口令、密钥口令都是 `tokidiscover`。同一签名的后续版本可以覆盖安装。这个口令是公开的，只用于本仓库的开源构建。要对外发布自己的版本，请更换密钥库。

无痕在系统 WebView 支持多配置时使用名为 `toki-private` 的 Profile。无痕标签全部关闭后，清掉该配置的 Cookie 和 Web Storage。

翻译使用 ML Kit，在设备上翻译可见文本。语言模型不打进 APK，第一次使用时下载。失败时可以改用在线翻译页。

## 用 SideStore 安装 iOS 版

界面是 SwiftUI 和 WKWebView。地址栏使用 `glassEffect` 和 `GlassEffectContainer`。开启「降低透明度」时退回系统 Material。页面放在 `safeAreaInset` 里。

`TokiDiscover-0.1.0-ios-unsigned.ipa` 未签名。仓库里没有 Apple 开发者证书。包标识是 `dev.ichiyotsu.tokidiscover`。应用要求 iOS / iPadOS 26.0 或更高。

无痕使用 `WKWebsiteDataStore.nonPersistent()`，数据不写入磁盘。翻译使用系统 Translation 框架翻译可见文本，也可以改用在线翻译页。系统翻译第一次使用某种语言时，可能会要求下载语言包。

下面只复述 SideStore 官方文档，链接以文档站为准。这里说的是 GitHub 仓库里的 `README.md`。

官方入口：

- 文档首页：https://docs.sidestore.io/
- 准备：https://docs.sidestore.io/docs/installation/prerequisites
- 安装：https://docs.sidestore.io/docs/installation/install
- 常见问题：https://docs.sidestore.io/docs/faq
- SideStore 仓库：https://github.com/SideStore/SideStore

文档写明的前提（2026-11-11 查阅）：

- iPhone、iPad 或 iPod touch，有密码，系统为 iOS / iPadOS 15.0 或更高。TokiDiscover 自己要求 26.0 或更高。第一次安装 SideStore 需要一台电脑：Windows 8 或更高、macOS High Sierra 或更高、较新的 Linux，或未受托管限制的 Chromebook。
- 一个 Apple 账号（文档也称为 Apple ID）。Wi-Fi，不能只用蜂窝网络。
- 从 App Store 或 AltStore PAL 源安装 LocalDevVPN，并连接。文档说明它不连远程服务器。每次安装、更新或刷新都要开着。
- 按电脑系统安装 iloader。文档写明不支持 32 位 Windows，也不支持 Windows 10 的 ARM 版。Windows 需要苹果官网的 iTunes，或「Apple 设备」应用。Linux 需要 `usbmuxd`。iloader 的问题文档指向 idevice Discord，不是 SideStore 服务器。

安装 SideStore（文档「Install」页；本应用的系统属于文档里的 iOS / iPadOS 18.0 及更高一节）：

1. 用 USB 连接设备，在设备上信任这台电脑。
2. 打开 iloader，登录 Apple 账号。文档说明这个账号不必是设备上已登录的那个，并且区分大小写。
3. 选中设备，选择 Install SideStore (Stable)。
4. 在设备上打开设置，信任该开发者 App。iOS 18 及以上先信任并允许重启，再打开开发者模式，设备会重启。
5. 打开 LocalDevVPN 并连接。打开 SideStore，登录刚才用的 Apple 账号。
6. 在「My Apps」里点 SideStore 旁边的天数，完成刷新。文档把这个按钮写成「7 DAYS」，数字是到期前的天数。

签名有效期（文档 FAQ 与安装页）：

- 免费 Apple 账号：同时只能安装 3 个 App（含 SideStore 自己），一周最多 10 个不同的 App ID。过期后 App 打不开，需要再刷新。TokiDiscover 自己不能续期。
- 付费的每年 99 美元 Apple Developer 账号：文档写明可以去掉 3 个 App 的限制，并把有效期变成 365 天。这是苹果账号的限制。

安装这个 IPA：

- 把 [v0.1.0](https://github.com/ichiyotsu/Tokidiscover/releases/tag/v0.1.0) 的 `TokiDiscover-0.1.0-ios-unsigned.ipa` 传到设备，用 SideStore 以你的 Apple 账号重新签名后安装。仓库里不放签名证书。
- 应用不会自己安装 IPA，也不会向你要 Apple 账号。
- 本仓库没有发布 SideStore 源。

## 许可

应用代码使用 [Apache-2.0](LICENSE)。
