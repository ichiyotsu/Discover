import SwiftUI
import Translation
import WebKit

struct ContentView: View {
    @StateObject private var model = BrowserModel()
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Namespace private var glassNamespace
    @State private var draft = ""
    @State private var showTabs = false
    @State private var sessionConfiguration: TranslationSession.Configuration?
    @FocusState private var addressFocused: Bool

    private var expanded: Bool {
        model.chromeExpanded || addressFocused || (model.active?.isHome ?? true)
    }

    var body: some View {
        ZStack {
            page
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            chrome
                .padding(.horizontal, expanded ? 12 : 64)
                .padding(.bottom, 8)
        }
        .background(Color(.systemBackground))
        .sheet(isPresented: $showTabs) { tabSheet }
        .onOpenURL { url in model.open(url: url, private: false) }
        .translationTask(sessionConfiguration) { session in
            var lines: [String] = []
            for chunk in model.pendingChunks {
                let response = try await session.translate(chunk)
                lines.append(response.targetText)
            }
            await model.applyTranslation(lines)
        }
        .onChange(of: model.translationConfiguration) { _, box in
            guard let box else { return }
            sessionConfiguration = TranslationSession.Configuration(source: nil, target: box.target)
        }
        .onChange(of: addressFocused) { _, focused in
            model.addressFocused = focused
            if focused { model.chromeExpanded = true }
        }
        .alert(model.status ?? "", isPresented: Binding(
            get: { model.status != nil },
            set: { if !$0 { model.status = nil } }
        )) {
            Button("OK", role: .cancel) { model.status = nil }
        }
    }

    @ViewBuilder
    private var page: some View {
        if let tab = model.active {
            ZStack {
                if !tab.isHome {
                    WebContainer(webView: tab.webView)
                }
                if tab.isHome {
                    HomeView(engineID: model.engineID, onEngine: { model.engineID = $0 }, onSubmit: model.submit)
                }
            }
            .overlay(alignment: .top) {
                if tab.loading {
                    ProgressView(value: max(tab.progress, 0.05))
                        .progressViewStyle(.linear)
                }
            }
        }
    }

    private var chrome: some View {
        GlassEffectContainer(spacing: 16) {
            Group {
                if expanded {
                    expandedBar.glassEffectID("chrome", in: glassNamespace)
                } else {
                    pill.glassEffectID("chrome", in: glassNamespace)
                }
            }
        }
        .animation(reduceMotion ? nil : .spring(response: 0.38, dampingFraction: 0.86), value: expanded)
    }

    private var expandedBar: some View {
        HStack(spacing: 8) {
            glassButton("chevron.left", Copy.back, enabled: model.active?.canGoBack == true || model.active?.isHome == false) {
                model.goBack()
            }
            glassButton("chevron.right", Copy.forward, enabled: model.active?.canGoForward == true) {
                model.goForward()
            }
            addressField
            glassButton("arrow.clockwise", Copy.reload, enabled: model.active?.isHome == false) {
                model.reload()
            }
            Menu {
                Button(Copy.home) { model.home() }
                Button(Copy.translate) { Task { await model.prepareTranslation() } }
                Button(Copy.translateOnline) { model.translateOnline() }
                Button(Copy.newTab) { model.newTab(private: false) }
                Button(Copy.privateTab) { model.newTab(private: true) }
                Button(Copy.tabs) { showTabs = true }
                Menu(Copy.searchEngine) {
                    ForEach(SearchEngine.all) { engine in
                        Button(engine.label) { model.engineID = engine.id }
                    }
                }
            } label: {
                Image(systemName: "ellipsis")
                    .frame(width: 36, height: 36)
            }
            .buttonStyle(.glass)
            .accessibilityLabel(Copy.tabs)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .modifier(ChromeGlass(reduceTransparency: reduceTransparency))
    }

    private var pill: some View {
        Button {
            model.chromeExpanded = true
        } label: {
            HStack(spacing: 8) {
                Image(systemName: model.active?.isPrivate == true ? "lock.fill" : "globe")
                Text(model.active?.title ?? "TokiDiscover")
                    .lineLimit(1)
            }
            .padding(.horizontal, 16)
            .frame(height: 44)
        }
        .buttonStyle(.plain)
        .modifier(ChromeGlass(reduceTransparency: reduceTransparency))
        .accessibilityLabel(Copy.expand)
    }

    private var addressField: some View {
        TextField(Copy.searchHint, text: $draft)
            .textFieldStyle(.plain)
            .focused($addressFocused)
            .submitLabel(.go)
            .onSubmit { model.submit(draft) }
            .padding(.horizontal, 12)
            .frame(height: 36)
            .background(Color.primary.opacity(0.06), in: Capsule())
            .onChange(of: model.active?.url) { _, url in
                if !addressFocused { draft = url?.absoluteString ?? "" }
            }
            .onChange(of: model.active?.id) { _, _ in
                draft = model.active?.isHome == true ? "" : (model.active?.url?.absoluteString ?? "")
            }
    }

    private func glassButton(_ system: String, _ label: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: system)
                .frame(width: 36, height: 36)
        }
        .buttonStyle(.glass)
        .disabled(!enabled)
        .accessibilityLabel(label)
    }

    private var tabSheet: some View {
        NavigationStack {
            List {
                Section(Copy.searchEngine) {
                    ForEach(SearchEngine.all) { engine in
                        Button(engine.label) { model.engineID = engine.id }
                    }
                }
                Section(Copy.tabs) {
                    ForEach(model.tabs) { tab in
                        HStack {
                            Image(systemName: tab.isPrivate ? "lock.fill" : "globe")
                            VStack(alignment: .leading) {
                                Text(tab.title).lineLimit(1)
                                Text(tab.isHome ? Copy.home : (tab.url?.host ?? ""))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            if tab.id == model.activeID { Image(systemName: "checkmark") }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture { model.select(tab.id); showTabs = false }
                        .swipeActions {
                            Button(Copy.close, role: .destructive) { model.close(tab.id) }
                        }
                    }
                }
            }
            .navigationTitle("TokiDiscover")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(Copy.close) { showTabs = false }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button(Copy.newTab) { model.newTab(private: false); showTabs = false }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button(Copy.privateTab) { model.newTab(private: true); showTabs = false }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

private struct WebContainer: UIViewRepresentable {
    let webView: WKWebView
    func makeUIView(context: Context) -> WKWebView {
        webView.removeFromSuperview()
        return webView
    }
    func updateUIView(_ uiView: WKWebView, context: Context) {}
}

private struct HomeView: View {
    let engineID: String
    let onEngine: (String) -> Void
    let onSubmit: (String) -> Void
    @State private var query = ""

    var body: some View {
        VStack(spacing: 18) {
            Text("TokiDiscover")
                .font(.largeTitle.weight(.semibold))
            Text(Copy.tagline)
                .foregroundStyle(.secondary)
            TextField(Copy.searchHint, text: $query)
                .textFieldStyle(.plain)
                .padding(16)
                .background(.ultraThinMaterial, in: Capsule())
                .submitLabel(.search)
                .onSubmit { onSubmit(query) }
            Text(Copy.searchEngine)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)
            FlowEngines(selected: engineID, onSelect: onEngine)
            Text("0.1.0")
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
        .padding(28)
        .frame(maxWidth: 560)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct FlowEngines: View {
    let selected: String
    let onSelect: (String) -> Void
    var body: some View {
        HStack(spacing: 8) {
            ForEach(SearchEngine.all) { engine in
                Button(engine.label) { onSelect(engine.id) }
                    .buttonStyle(.glass)
                    .tint(engine.id == selected ? Color.accentColor : nil)
            }
        }
    }
}

private struct ChromeGlass: ViewModifier {
    let reduceTransparency: Bool
    @ViewBuilder
    func body(content: Content) -> some View {
        if reduceTransparency {
            content.background(.regularMaterial, in: Capsule())
        } else {
            content.glassEffect(.regular.interactive(), in: .capsule)
        }
    }
}

