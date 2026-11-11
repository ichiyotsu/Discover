import Foundation
/// Search engines. Default is Bing (matches the Windows ecosystem); changeable in Settings.
public enum SearchEngine: String, CaseIterable {
    case google = "Google", bing = "Bing", duckDuckGo = "DuckDuckGo", baidu = "百度", custom = "自定义"
    public var template: String { switch self {
        case .google: return "https://www.google.com/search?q={query}"
        case .bing: return "https://www.bing.com/search?q={query}"
        case .duckDuckGo: return "https://duckduckgo.com/?q={query}"
        case .baidu: return "https://www.baidu.com/s?wd={query}"
        case .custom: return "{query}" } }
    public func searchURL(_ query: String, customTemplate: String = "") -> String {
        let tpl = (self == .custom && !customTemplate.isEmpty) ? customTemplate : template
        // Encode per word and join with "+" so "hello world" -> "hello+world" (matches URLEncoder / WebUtility on the other platforms).
        let enc = query.split(separator: " ").map { w in String(w).addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? String(w) }.joined(separator: "+")
        return tpl.contains("{query}") ? tpl.replacingOccurrences(of: "{query}", with: enc) : tpl + enc
    }
    public static let `default`: SearchEngine = .bing
}
public enum NavResult: Equatable {
    case url(String), search(url: String, query: String), newTab, blocked(String)
}
/// Omnibox input resolution — identical logic on Android / iOS (TokiDiscoverCore) / Windows (TokiDiscover.Core).
public enum UrlNormalizer {
    public static let newTabURL = "about:blank"
    private static let dangerous = ["javascript:", "data:", "vbscript:", "file:", "blob:"]
    public static func normalize(_ raw: String, engine: SearchEngine = .default, customTemplate: String = "") -> NavResult {
        let input = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if input.isEmpty || input.lowercased() == newTabURL { return .newTab }
        let lower = input.lowercased()
        if let bad = dangerous.first(where: { lower.hasPrefix($0) }) { return .blocked("已拦截危险协议: \(bad)") }
        if lower.hasPrefix("about:") { return .newTab }
        if lower.hasPrefix("http://") || lower.hasPrefix("https://") { return .url(input) }
        if input.contains(where: { $0.isWhitespace }) { return .search(url: engine.searchURL(input, customTemplate: customTemplate), query: input) }
        let host = input.split(whereSeparator: { "/?#:".contains($0) }).first.map(String.init) ?? input
        if host.contains(".") || host.lowercased() == "localhost" { return .url("https://" + input) }
        return .search(url: engine.searchURL(input, customTemplate: customTemplate), query: input)
    }
    public static func isSafeScheme(_ url: String) -> Bool { let l = url.lowercased(); return l.hasPrefix("http://") || l.hasPrefix("https://") || l == newTabURL }
}
/// Update check: version is parsed from the release *asset filename* first, tag second —
/// the same false-update bug fixed in Tokilive/tokimusic (tags shipped mismatched asset versions).
public enum UpdateChecker {
    public static func parseVersion(fromAssetName name: String) -> String? {
        guard let re = try? NSRegularExpression(pattern: #"(\d+\.\d+\.\d+(?:-(?:beta|alpha|rc|preview)[0-9A-Za-z.]*)?)"#),
              let m = re.firstMatch(in: name, range: NSRange(name.startIndex..., in: name)),
              let r = Range(m.range(at: 1), in: name) else { return nil }
        return String(name[r])
    }
}
