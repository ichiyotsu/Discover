import Foundation
struct BrowserTab: Identifiable, Equatable { let id = UUID(); var url: String = "about:blank"; var title: String = "新标签页"; var isPrivate: Bool = false; var progress: Double = 0 }
struct Bookmark: Identifiable, Codable { var id = UUID(); var title: String; var url: String; var folder: String = "默认" }
struct HistoryEntry: Identifiable, Codable { var id = UUID(); var title: String; var url: String; var visitedAt: Date = Date() }
struct DownloadRecord: Identifiable { let id = UUID(); var fileName: String; var url: String; var status: String = "已记录" }
struct SpeedDialSite { var title: String; var url: String }
let defaultSpeedDial = [SpeedDialSite(title: "Bilibili", url: "https://www.bilibili.com"), SpeedDialSite(title: "GitHub", url: "https://github.com"),
    SpeedDialSite(title: "Wikipedia", url: "https://www.wikipedia.org"), SpeedDialSite(title: "Bing", url: "https://www.bing.com"),
    SpeedDialSite(title: "知乎", url: "https://www.zhihu.com"), SpeedDialSite(title: "YouTube", url: "https://www.youtube.com")]
