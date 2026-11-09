# TokiDiscover

TokiDiscover 0.1.0 是一个轻量、开源、跨平台的网页浏览器。三个平台各自使用原生界面，而不是套一层共用的网页壳。

| 平台 | 技术 | 界面 |
| --- | --- | --- |
| Windows 10 / 11 | C#、WinUI 3（Windows App SDK）、WebView2 | Fluent。Windows 11 使用 Mica，不支持时退回 Acrylic，再不行则使用实色 |
| Android 8.0+（面向 Android 17） | Kotlin、Jetpack Compose、系统 WebView | Material 3 Expressive：`MaterialExpressiveTheme`、动态取色、弹簧动效、悬浮工具栏 |
| iOS / iPadOS 26+ | SwiftUI、WKWebView | Liquid Glass 悬浮栏。内容放在安全区内，不长期压住网页 |

版本号：**0.1.0**。许可证：[Apache-2.0](LICENSE)。

## 功能

- 悬浮地址栏会在页面向下滚动时收成一颗小胶囊，向上滚动或点按后展开。网页排在栏的外侧，不会被栏永久挡住。
- 标签页、后退、前进、刷新、主页。
- 搜索引擎可选 Google、Bing、DuckDuckGo、百度。地址栏能区分网址和搜索词。
- 无痕模式：
  - Windows：独立的临时 WebView2 配置目录，最后一个无痕标签关闭后删除。
  - Android：在系统 WebView 支持多配置时使用名为 `toki-private` 的 Profile，并在无痕标签全部关闭后清掉该配置的 Cookie 与 Web Storage。
  - iOS：`WKWebsiteDataStore.nonPersistent()`，数据不写入磁盘。
- 翻译：
  - Android：ML Kit 设备端翻译可见文本；失败时可以改用在线翻译页。
  - Windows：WebView2 没有公开的整页翻译 API，因此打开翻译服务页面作为后备。
  - iOS：使用系统 Translation 框架翻译页面中的可见文本，也可以改用在线翻译页。

## 安装

构建产物由 GitHub Actions 在推送标签 `v*`（例如 `v0.1.0`）时上传到对应的 GitHub Release。也可以在 Actions 里手动运行 **Build**。

| 文件 | 用途 |
| --- | --- |
| `TokiDiscover-0.1.0-windows-setup.exe` | Windows 安装程序 |
| `TokiDiscover-0.1.0-windows-portable.zip` | Windows 便携版，解压后运行 `TokiDiscover.exe` |
| `TokiDiscover-0.1.0-android.apk` | Android arm64-v8a 已签名安装包 |
| `TokiDiscover-0.1.0-ios-unsigned.ipa` | 未签名 iOS 包，供侧载 |

### Windows

1. Windows 11 可以直接安装。Windows 10 需要先安装 [Microsoft Edge WebView2 Runtime](https://developer.microsoft.com/microsoft-edge/webview2/)（Evergreen）。
2. 运行 `TokiDiscover-0.1.0-windows-setup.exe`，或解压便携版后启动 `TokiDiscover.exe`。
3. 安装程序默认装到当前用户目录，不需要管理员权限。

Windows 包是未打包（unpackaged）的自包含 WinUI 3 程序，不依赖另外安装 Windows App SDK 运行时。

### Android

允许浏览器安装未知来源应用后，打开 APK 即可。`minSdk` 为 26（Android 8.0），`compileSdk` / `targetSdk` 为 Android 17（API 37.2 / API 37）。安装包只包含 `arm64-v8a`，并开启了 R8 与资源压缩。

签名密钥库提交在 `android/tokidiscover.jks`，口令写在 `android/keystore.properties`（别名、库口令、密钥口令都是 `tokidiscover`）。同一签名的后续版本可以直接覆盖安装。这个口令是公开的，只服务于本仓库的开源构建；如果你要对外发布自己的版本，请更换密钥库。

首次使用「翻译」时，ML Kit 会按需下载语言模型，模型不打进 APK。

### iOS / iPadOS

这个 IPA **没有 Apple 签名**，也不能从 App Store 或直接用系统安装器安装。仓库里没有 Apple 开发者证书。

用 AltStore 或 SideStore 侧载时，需要用你自己的 Apple ID 重新签名：

1. 在 iOS / iPadOS 26 或更高版本的设备上安装 [AltStore](https://altstore.io/) 或 [SideStore](https://sidestore.io/)。
2. 把 `TokiDiscover-0.1.0-ios-unsigned.ipa` 传到设备，在 AltStore / SideStore 里选择安装。
3. 按侧载工具的提示登录 Apple ID 并信任描述文件。免费 Apple ID 的签名大约 7 天后失效，需要重新签名。
4. 系统翻译第一次使用某种语言时，可能会要求下载语言包。

侧载应用的包标识是 `dev.ichiyotsu.tokidiscover`。

## 从源码构建

### Windows

需要 Windows 10 或 11、.NET 10 SDK。

```powershell
dotnet publish windows/TokiDiscover/TokiDiscover.csproj -c Release -r win-x64 --self-contained true -p:WindowsPackageType=None -p:WindowsAppSDKSelfContained=true -o windows/TokiDiscover/bin/publish
```

安装包使用 [Inno Setup 6](https://jrsoftware.org/isinfo.php)：

```powershell
& "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe" windows/installer/TokiDiscover.iss
```

### Android

需要 JDK 21 和 Android SDK Platform 37.2、Build-Tools 37.0.0。

```bash
export ANDROID_HOME="$HOME/android-sdk"
cd android
chmod +x gradlew
./gradlew assembleRelease
```

签名 APK 在 `android/app/build/outputs/apk/release/`。

### iOS

需要 macOS 26 和 Xcode 26，以及 [XcodeGen](https://github.com/yonaskolb/XcodeGen)。

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

把生成的 `TokiDiscover.app` 放进 `Payload/` 再打成 zip，并把扩展名改为 `.ipa`，就得到未签名安装包。

## 发布

`.github/workflows/release.yml` 会在拉取请求、手动触发，以及推送 `v*` 标签时编译三个平台。只有标签推送会创建 GitHub Release，并附上上面列出的四个文件。

## 设计说明

- Windows 遵循 Fluent 的克制、熟悉和系统个性化：标题栏融入窗口，控件使用系统画笔和圆角，Mica / Acrylic 随桌面变化。
- Android 使用 Material 3 Expressive。主题是 `MaterialExpressiveTheme` 加 `MotionScheme.expressive()`；Android 12 及以上跟随壁纸动态取色。底部 `HorizontalFloatingToolbar` 在滚动时收到悬浮按钮。
- iOS 26 的地址栏使用 `glassEffect` 与 `GlassEffectContainer`。开启「降低透明度」时退回系统 Material。页面放在 `safeAreaInset` 里，工具栏变矮时页面区域跟着变高。
