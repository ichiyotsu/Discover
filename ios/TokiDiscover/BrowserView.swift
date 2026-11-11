import SwiftUI
import WebKit
import TokiDiscoverCore
/// Engine: WKWebView (system WebKit). Limitations: no extensions; progress via KVO on estimatedProgress;
/// downloads are delegate callbacks only (no built-in download manager UI); find uses WKWebView.find.
struct WebView: UIViewRepresentable {
    var urlString: String; var desktopUA: Bool; var javaScriptEnabled: Bool; var isPrivate: Bool
    var onCommit: (String, String) -> Void; var onProgress: (Double) -> Void
    func makeUIView(context: Context) -> WKWebView {
        let cfg = WKWebViewConfiguration()
        cfg.websiteDataStore = isPrivate ? .nonPersistent() : .default()
        cfg.defaultWebpagePreferences.allowsContentJavaScript = javaScriptEnabled
        let wv = WKWebView(frame: .zero, configuration: cfg)
        wv.navigationDelegate = context.coordinator
        wv.customUserAgent = desktopUA ? "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15" : nil
        context.coordinator.observe(wv)
        if urlString != "about:blank", let u = URL(string: urlString) { wv.load(URLRequest(url: u)) }
        context.coordinator.webView = wv
        return wv
    }
    func updateUIView(_ wv: WKWebView, context: Context) {
        if let target = context.coordinator.pendingURL, target != "about:blank", wv.url?.absoluteString != target, let u = URL(string: target) { wv.load(URLRequest(url: u)); context.coordinator.pendingURL = nil }
    }
    func makeCoordinator() -> Coordinator { Coordinator(parent: self) }
    final class Coordinator: NSObject, WKNavigationDelegate {
        var parent: WebView; weak var webView: WKWebView?; var pendingURL: String?; private var observation: NSKeyValueObservation?
        init(parent: WebView) { self.parent = parent }
        func observe(_ wv: WKWebView) { observation = wv.observe(\.estimatedProgress) { [weak self] v, _ in DispatchQueue.main.async { self?.parent.onProgress(v.estimatedProgress) } } }
        func webView(_ wv: WKWebView, decidePolicyFor action: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            guard let u = action.request.url?.absoluteString else { decisionHandler(.cancel); return }
            decisionHandler(UrlNormalizer.isSafeScheme(u) || u.hasPrefix("about:") ? .allow : .cancel) // block javascript:/data:
        }
        func webView(_ wv: WKWebView, didFinish nav: WKNavigation!) { parent.onCommit(wv.title ?? "", wv.url?.absoluteString ?? "") }
    }
}
