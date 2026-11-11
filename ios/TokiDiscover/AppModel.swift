import Foundation
import SwiftUI
import TokiDiscoverCore
/// Owns tabs + library (UserDefaults JSON). Private tabs never write history.
@MainActor final class AppModel: ObservableObject {
    @Published var tabs: [BrowserTab] = [BrowserTab()]
    @Published var activeTabID: UUID = UUID()
    @Published var bookmarks: [Bookmark] = [] { didSet { persist() } }
    @Published var history: [HistoryEntry] = [] { didSet { persist() } }
    @Published var downloads: [DownloadRecord] = []
    @Published var blockedMessage: String?
    @AppStorage("tokidiscover_engine") var engineName: String = SearchEngine.default.rawValue
    @AppStorage("tokidiscover_custom_template") var customTemplate: String = ""
    @AppStorage("tokidiscover_desktop_ua") var desktopUA: Bool = false
    @AppStorage("tokidiscover_js") var javaScriptEnabled: Bool = true
    init() { activeTabID = tabs[0].id; load() }
    var engine: SearchEngine { SearchEngine(rawValue: engineName) ?? .default }
    var activeTab: BrowserTab { tabs.first { $0.id == activeTabID } ?? tabs[0] }
    func newTab(isPrivate: Bool = false) { let t = BrowserTab(isPrivate: isPrivate); tabs.append(t); activeTabID = t.id }
    func closeTab(_ id: UUID) { guard let i = tabs.firstIndex(where: { $0.id == id }) else { return }; tabs.remove(at: i)
        if tabs.isEmpty { let t = BrowserTab(); tabs.append(t); activeTabID = t.id } else if activeTabID == id { activeTabID = tabs[min(i, tabs.count - 1)].id } }
    func submitOmnibox(_ input: String) { switch UrlNormalizer.normalize(input, engine: engine, customTemplate: customTemplate) {
        case .url(let u), .search(let u, _): navigate(u); case .newTab: updateActive { $0.url = "about:blank"; $0.title = "新标签页" }
        case .blocked(let reason): blockedMessage = reason } }
    func navigate(_ url: String) { updateActive { $0.url = url } }
    func pageCommitted(title: String, url: String) { updateActive { $0.title = title.isEmpty ? url : title; $0.url = url }
        if !activeTab.isPrivate && url != "about:blank" { history.insert(HistoryEntry(title: title.isEmpty ? url : title, url: url), at: 0); history = Array(history.prefix(500)) } }
    func updateActive(_ mutate: (inout BrowserTab) -> Void) { guard let i = tabs.firstIndex(where: { $0.id == activeTabID }) else { return }; mutate(&tabs[i]) }
    private func persist() { let enc = JSONEncoder(); if let d = try? enc.encode(bookmarks) { UserDefaults.standard.set(d, forKey: "tokidiscover_bookmarks") }
        if let d = try? enc.encode(history) { UserDefaults.standard.set(d, forKey: "tokidiscover_history") } }
    private func load() { let dec = JSONDecoder(); if let d = UserDefaults.standard.data(forKey: "tokidiscover_bookmarks"), let v = try? dec.decode([Bookmark].self, from: d) { bookmarks = v }
        if let d = UserDefaults.standard.data(forKey: "tokidiscover_history"), let v = try? dec.decode([HistoryEntry].self, from: d) { history = v } }
}
