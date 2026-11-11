import SwiftUI
enum Dest: String, CaseIterable { case browser = "浏览", tabs = "标签", library = "书库", settings = "设置"
    var icon: String { switch self { case .browser: return "globe"; case .tabs: return "square.on.square"; case .library: return "bookmark"; case .settings: return "gearshape" } } }
/// Root: content + floating glass capsule toolbar (ultraThinMaterial = Haze counterpart) + floating omnibox on Browser.
struct RootView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.colorScheme) private var scheme
    @State private var dest: Dest = .browser
    @State private var omniboxText: String = ""
    private var dark: Bool { scheme == .dark }
    var body: some View {
        ZStack(alignment: .bottom) {
            Group { switch dest {
                case .browser: BrowserPage().environmentObject(model)
                case .tabs: TabsView(onPick: { dest = .browser }).environmentObject(model)
                case .library: LibraryView().environmentObject(model)
                case .settings: SettingsView().environmentObject(model) } }
                .padding(.bottom, 150)
                .background(TokiPalette.background(dark: dark))
            VStack(spacing: 8) {
                if dest == .browser && model.activeTab.url != "about:blank" { omnibox }
                // FloatingNavBar replica: 36pt capsule, glass, selected = solid primary pill + expanded label.
                HStack(spacing: 4) {
                    ForEach(Dest.allCases, id: \.self) { d in
                        Button { dest = d } label: {
                            HStack(spacing: 8) { Image(systemName: d.icon); if dest == d { Text(d.rawValue).font(.subheadline.bold()) } }
                                .padding(.horizontal, 16).frame(height: 48)
                                .background(dest == d ? TokiPalette.primary(dark: dark) : Color.clear, in: Capsule())
                                .foregroundStyle(dest == d ? TokiPalette.onPrimary(dark: dark) : Color.secondary)
                        }.buttonStyle(.plain)
                    }
                }.padding(8).background(.ultraThinMaterial, in: Capsule())
            }.padding(.bottom, 8).padding(.horizontal, 12)
        }
        .alert("已拦截", isPresented: Binding(get: { model.blockedMessage != nil }, set: { if !$0 { model.blockedMessage = nil } })) {
            Button("知道了") { model.blockedMessage = nil }
        } message: { Text(model.blockedMessage ?? "") }
    }
    private var omnibox: some View {
        VStack(spacing: 4) {
            if model.activeTab.progress > 0 && model.activeTab.progress < 1 { ProgressView(value: model.activeTab.progress).tint(TokiPalette.primary(dark: dark)) }
            HStack(spacing: 4) {
                TextField("搜索或输入网址", text: $omniboxText).textFieldStyle(.plain)
                    .onSubmit { model.submitOmnibox(omniboxText) }
                    .onAppear { omniboxText = model.activeTab.url == "about:blank" ? "" : model.activeTab.url }
                Button("刷新") { model.submitOmnibox(model.activeTab.url) }.font(.caption)
                Button("标签(\(model.tabs.count))") { dest = .tabs }.font(.caption)
            }.padding(.horizontal, 16).padding(.vertical, 10).background(.ultraThinMaterial, in: Capsule())
        }
    }
}
struct BrowserPage: View {
    @EnvironmentObject var model: AppModel
    var body: some View {
        if model.activeTab.url == "about:blank" { HomeView().environmentObject(model) }
        else { WebView(urlString: model.activeTab.url, desktopUA: model.desktopUA, javaScriptEnabled: model.javaScriptEnabled, isPrivate: model.activeTab.isPrivate,
                onCommit: { title, url in model.pageCommitted(title: title, url: url) },
                onProgress: { p in model.updateActive { $0.progress = p } })
            .id(model.activeTab.id) }
    }
}
