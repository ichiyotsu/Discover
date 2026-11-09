import Foundation

struct SearchEngine: Identifiable, Hashable {
    let id: String
    let label: String
    let template: String

    static let all: [SearchEngine] = [
        SearchEngine(id: "google", label: "Google", template: "https://www.google.com/search?q={query}"),
        SearchEngine(id: "bing", label: "Bing", template: "https://www.bing.com/search?q={query}"),
        SearchEngine(id: "duckduckgo", label: "DuckDuckGo", template: "https://duckduckgo.com/?q={query}"),
        SearchEngine(id: "baidu", label: "百度", template: "https://www.baidu.com/s?wd={query}"),
    ]

    static func named(_ id: String) -> SearchEngine {
        all.first { $0.id == id } ?? all[0]
    }
}

enum Omnibox {
    static func resolve(_ raw: String, engine: SearchEngine) -> URL? {
        let text = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.isEmpty { return nil }
        if text.lowercased() == "toki://home" || text.lowercased() == "about:home" { return nil }
        let candidate = text.hasPrefix("//") ? "https:\(text)" : text
        if let url = URL(string: candidate),
           let scheme = url.scheme?.lowercased(),
           scheme == "http" || scheme == "https",
           url.host?.isEmpty == false {
            return url
        }
        let looksLikeHost = !text.contains(" ") && !text.contains("　") &&
            (text.hasPrefix("localhost") || text.contains(".") || text.contains(":"))
        if looksLikeHost, let url = URL(string: "https://\(text)") {
            return url
        }
        let encoded = text.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? text
        return URL(string: engine.template.replacingOccurrences(of: "{query}", with: encoded))
    }

    static func translateURL(for page: URL) -> URL? {
        let language = Locale.current.language.languageCode?.identifier ?? "en"
        let target = language == "zh" ? "zh-CN" : language
        var parts = URLComponents(string: "https://translate.google.com/translate")
        parts?.queryItems = [
            URLQueryItem(name: "sl", value: "auto"),
            URLQueryItem(name: "tl", value: target),
            URLQueryItem(name: "u", value: page.absoluteString),
        ]
        return parts?.url
    }
}

enum Copy {
    static var zh: Bool { Locale.current.language.languageCode?.identifier == "zh" }
    static var tagline: String { zh ? "轻量发现" : "A lighter way to look around" }
    static var searchHint: String { zh ? "搜索或输入网址" : "Search or type a URL" }
    static var back: String { zh ? "后退" : "Back" }
    static var forward: String { zh ? "前进" : "Forward" }
    static var reload: String { zh ? "刷新" : "Reload" }
    static var home: String { zh ? "主页" : "Home" }
    static var translate: String { zh ? "翻译" : "Translate" }
    static var translateOnline: String { zh ? "在线翻译" : "Translate online" }
    static var tabs: String { zh ? "标签页" : "Tabs" }
    static var newTab: String { zh ? "新标签页" : "New tab" }
    static var privateTab: String { zh ? "无痕标签页" : "Private tab" }
    static var searchEngine: String { zh ? "搜索引擎" : "Search engine" }
    static var expand: String { zh ? "展开地址栏" : "Expand address bar" }
    static var close: String { zh ? "关闭" : "Close" }
    static var privateLabel: String { zh ? "无痕" : "Private" }
}
