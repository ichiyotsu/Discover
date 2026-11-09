import Foundation
import UIKit
import WebKit

@MainActor
final class BrowserTab: Identifiable, ObservableObject {
    let id = UUID()
    let isPrivate: Bool
    let webView: WKWebView
    @Published var title = "TokiDiscover"
    @Published var url: URL?
    @Published var isHome = true
    @Published var loading = false
    @Published var progress: Double = 0
    @Published var canGoBack = false
    @Published var canGoForward = false
    private var observations: [NSKeyValueObservation] = []
    var onScroll: ((CGFloat, CGFloat) -> Void)?

    init(isPrivate: Bool, dataStore: WKWebsiteDataStore) {
        self.isPrivate = isPrivate
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = dataStore
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        let view = WKWebView(frame: .zero, configuration: configuration)
        view.allowsBackForwardNavigationGestures = true
        self.webView = view
        super.init()
        observations = [
            view.observe(\.title, options: [.new]) { [weak self] web, _ in
                let title = web.title
                Task { @MainActor in self?.title = (title?.isEmpty == false ? title! : "TokiDiscover") }
            },
            view.observe(\.url, options: [.new]) { [weak self] web, _ in
                let url = web.url
                Task { @MainActor in
                    guard let self else { return }
                    if !self.isHome { self.url = url }
                }
            },
            view.observe(\.isLoading, options: [.new]) { [weak self] web, _ in
                let loading = web.isLoading
                Task { @MainActor in self?.loading = loading }
            },
            view.observe(\.estimatedProgress, options: [.new]) { [weak self] web, _ in
                let progress = web.estimatedProgress
                Task { @MainActor in self?.progress = progress }
            },
            view.observe(\.canGoBack, options: [.new]) { [weak self] web, _ in
                let value = web.canGoBack
                Task { @MainActor in self?.canGoBack = value }
            },
            view.observe(\.canGoForward, options: [.new]) { [weak self] web, _ in
                let value = web.canGoForward
                Task { @MainActor in self?.canGoForward = value }
            },
            view.scrollView.observe(\.contentOffset, options: [.old, .new]) { [weak self] scroll, change in
                let y = scroll.contentOffset.y
                let old = change.oldValue?.y ?? y
                Task { @MainActor in self?.onScroll?(y, y - old) }
            },
        ]
        view.navigationDelegate = NavigationBridge.shared
        view.uiDelegate = NavigationBridge.shared
    }

    func navigate(_ url: URL) {
        isHome = false
        self.url = url
        webView.load(URLRequest(url: url))
    }

    func showHome() {
        webView.stopLoading()
        isHome = true
        url = nil
        title = "TokiDiscover"
        loading = false
    }
}

@MainActor
final class BrowserModel: ObservableObject {
    @Published var tabs: [BrowserTab] = []
    @Published var activeID: UUID?
    @Published var chromeExpanded = true
    @Published var addressFocused = false
    @Published var engineID: String {
        didSet { UserDefaults.standard.set(engineID, forKey: "engine") }
    }
    @Published var status: String?
    @Published var translationConfiguration: TranslationConfigurationBox?
    @Published var pendingChunks: [String] = []

    private var privateStore: WKWebsiteDataStore?

    var active: BrowserTab? { tabs.first { $0.id == activeID } ?? tabs.first }
    var engine: SearchEngine { SearchEngine.named(engineID) }

    init() {
        engineID = UserDefaults.standard.string(forKey: "engine") ?? "google"
        NavigationBridge.shared.model = self
        newTab(private: false)
    }

    func newTab(private isPrivate: Bool) {
        let tab = BrowserTab(isPrivate: isPrivate, dataStore: store(private: isPrivate))
        tab.onScroll = { [weak self] y, dy in
            guard let self, self.active?.id == tab.id, !self.addressFocused, !tab.isHome else { return }
            if dy > 12, y > 40 { self.chromeExpanded = false }
            else if dy < -12 || y <= 0 { self.chromeExpanded = true }
        }
        tabs.append(tab)
        activeID = tab.id
        chromeExpanded = true
    }

    func select(_ id: UUID) {
        activeID = id
        chromeExpanded = true
    }

    func close(_ id: UUID) {
        guard let index = tabs.firstIndex(where: { $0.id == id }) else { return }
        let tab = tabs.remove(at: index)
        tab.webView.stopLoading()
        if tabs.isEmpty { newTab(private: false) }
        else if activeID == id { activeID = tabs[min(index, tabs.count - 1)].id }
        if tab.isPrivate, tabs.allSatisfy({ !$0.isPrivate }) { privateStore = nil }
    }

    func submit(_ text: String) {
        guard let tab = active else { return }
        guard let url = Omnibox.resolve(text, engine: engine) else {
            tab.showHome()
            chromeExpanded = true
            return
        }
        tab.navigate(url)
        chromeExpanded = true
    }

    func goBack() {
        guard let tab = active else { return }
        if tab.webView.canGoBack { tab.webView.goBack() }
        else { tab.showHome() }
    }

    func goForward() { active?.webView.goForward() }
    func reload() { if active?.isHome == false { active?.webView.reload() } }
    func home() { active?.showHome(); chromeExpanded = true }

    func open(url: URL, private isPrivate: Bool) {
        if active?.isPrivate == isPrivate, let tab = active {
            tab.navigate(url)
        } else {
            newTab(private: isPrivate)
            active?.navigate(url)
        }
    }

    func prepareTranslation() async {
        guard let tab = active, !tab.isHome else {
            status = Copy.zh ? "先打开一个网页" : "Open a page first"
            return
        }
        let script = """
        (function(){
          if (!document.body) return [];
          var skip = {SCRIPT:1,STYLE:1,NOSCRIPT:1,TEXTAREA:1,CODE:1};
          var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
          var out = [], n;
          while ((n = walker.nextNode()) && out.length < 24) {
            var p = n.parentElement;
            if (!p || skip[p.tagName]) continue;
            var t = (n.nodeValue || '').replace(/\\s+/g,' ').trim();
            if (t.length < 2 || t.length > 280) continue;
            out.push(t);
          }
          return out;
        })();
        """
        do {
            let result = try await tab.webView.evaluateJavaScript(script)
            let chunks = (result as? [String]) ?? []
            if chunks.isEmpty {
                status = Copy.zh ? "这一页没有可翻译的文本" : "Nothing to translate"
                return
            }
            pendingChunks = chunks
            let target = Locale.current.language
            var box = translationConfiguration ?? TranslationConfigurationBox(target: target)
            box.target = target
            box.token += 1
            translationConfiguration = box
        } catch {
            status = Copy.zh ? "无法读取页面文本" : "Could not read the page"
        }
    }

    func applyTranslation(_ lines: [String]) async {
        guard let tab = active, !lines.isEmpty else { return }
        let data = try? JSONSerialization.data(withJSONObject: lines)
        let json = data.flatMap { String(data: $0, encoding: .utf8) } ?? "[]"
        let script = """
        (function(){
          var parsed = \(json);
          if (!document.body) return 0;
          var skip = {SCRIPT:1,STYLE:1,NOSCRIPT:1,TEXTAREA:1,CODE:1};
          var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
          var i = 0, n, applied = 0;
          while ((n = walker.nextNode()) && i < parsed.length) {
            var p = n.parentElement;
            if (!p || skip[p.tagName]) continue;
            var raw = n.nodeValue || '';
            var t = raw.replace(/\\s+/g,' ').trim();
            if (t.length < 2 || t.length > 280) continue;
            if (parsed[i] && parsed[i] !== t) { n.nodeValue = raw.replace(t, parsed[i]); applied++; }
            i++;
          }
          return applied;
        })();
        """
        _ = try? await tab.webView.evaluateJavaScript(script)
        status = Copy.zh ? "已使用系统翻译" : "Translated with the system"
    }

    func translateOnline() {
        guard let tab = active, let page = tab.url, let target = Omnibox.translateURL(for: page) else { return }
        tab.navigate(target)
    }

    private func store(private isPrivate: Bool) -> WKWebsiteDataStore {
        if !isPrivate { return .default() }
        if privateStore == nil { privateStore = .nonPersistent() }
        return privateStore!
    }
}

/// Wraps the translation trigger so SwiftUI can observe a new request.
struct TranslationConfigurationBox: Equatable {
    var target: Locale.Language
    var token: Int = 0
}

final class NavigationBridge: NSObject, WKNavigationDelegate, WKUIDelegate {
    static let shared = NavigationBridge()
    weak var model: BrowserModel?

    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction) async -> WKNavigationActionPolicy {
        guard let url = navigationAction.request.url else { return .cancel }
        let scheme = url.scheme?.lowercased() ?? ""
        if scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data" { return .allow }
        await MainActor.run {
            UIApplication.shared.open(url)
        }
        return .cancel
    }

    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration, for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        if navigationAction.targetFrame == nil, let url = navigationAction.request.url {
            Task { @MainActor in
                let isPrivate = self.model?.active?.isPrivate ?? false
                self.model?.open(url: url, private: isPrivate)
            }
        }
        return nil
    }
}
