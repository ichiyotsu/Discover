import SwiftUI
import WebKit
import TokiDiscoverCore
/// Settings: engine, theme (system/light/dark via preferredColorScheme at app level — stored here), desktop UA, JavaScript,
/// per-item clear data (cookies via WKWebsiteDataStore / cache / history), About with the *real* bundle version.
struct SettingsView: View {
    @EnvironmentObject var model: AppModel
    @AppStorage("tokidiscover_theme") private var theme: String = "System"
    @State private var updateText: String?
    private var appVersion: String { Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.1.0" }
    private var buildNumber: String { Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1" }
    var body: some View {
        Form {
            Section("搜索引擎（默认 Bing）") { Picker("搜索引擎", selection: $model.engineName) { ForEach(SearchEngine.allCases, id: \.rawValue) { Text($0.rawValue).tag($0.rawValue) } }
                if model.engine == .custom { TextField("自定义模板（含 {query}）", text: $model.customTemplate) } }
            Section("主题") { Picker("主题", selection: $theme) { Text("跟随系统").tag("System"); Text("浅色").tag("Light"); Text("深色").tag("Dark") } }
            Section("浏览") { Toggle("桌面版 UA", isOn: $model.desktopUA); Toggle("JavaScript", isOn: $model.javaScriptEnabled)
                Text("新标签页固定为 TokiDiscover 主页，暂不支持自定义主页 URL").font(.caption).foregroundStyle(.secondary) }
            Section("清除浏览数据") {
                Button("清除 Cookie") { WKWebsiteDataStore.default().removeData(ofTypes: [WKWebsiteDataTypeCookies], modifiedSince: Date(timeIntervalSince1970: 0)) {} }
                Button("清除缓存") { WKWebsiteDataStore.default().removeData(ofTypes: [WKWebsiteDataTypeDiskCache, WKWebsiteDataTypeMemoryCache], modifiedSince: Date(timeIntervalSince1970: 0)) {} }
                Button("清除历史") { model.history = [] }
            }
            Section("关于") {
                Text("TokiDiscover \(appVersion)（\(buildNumber)）") // real bundle version, never hardcoded in UI text beyond the fallback
                Text("引擎：WKWebView（系统 WebKit）· 仓库 ichiyotsu/tokidiscover（待用户创建后检查更新）").font(.caption).foregroundStyle(.secondary)
                Button("检查更新") { updateText = "仓库未创建或无网络时无法检查 — 版本解析优先读安装包文件名（见 TokiDiscoverCore.UpdateChecker）" }
                if let updateText { Text(updateText).font(.caption) }
            }
        }
    }
}
