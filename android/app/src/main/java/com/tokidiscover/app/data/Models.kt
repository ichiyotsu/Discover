package com.tokidiscover.app.data
import java.util.UUID
/** Tab model: private tabs never write history / search records. */
data class BrowserTab(val id: String = UUID.randomUUID().toString(), var url: String = UrlNormalizer.NEW_TAB_URL,
    var title: String = "新标签页", val isPrivate: Boolean = false, var canGoBack: Boolean = false, var canGoForward: Boolean = false, var progress: Int = 0)
data class Bookmark(val id: String = UUID.randomUUID().toString(), val title: String, val url: String, val folder: String = "默认", val createdAt: Long = System.currentTimeMillis())
data class HistoryEntry(val id: String = UUID.randomUUID().toString(), val title: String, val url: String, val visitedAt: Long = System.currentTimeMillis())
data class DownloadRecord(val id: String = UUID.randomUUID().toString(), val fileName: String, val url: String, val totalBytes: Long = -1, val status: String = "已开始", val startedAt: Long = System.currentTimeMillis())
data class SpeedDialSite(val title: String, val url: String)
val DefaultSpeedDial = listOf(
    SpeedDialSite("Bilibili", "https://www.bilibili.com"), SpeedDialSite("GitHub", "https://github.com"),
    SpeedDialSite("Wikipedia", "https://www.wikipedia.org"), SpeedDialSite("Bing", "https://www.bing.com"),
    SpeedDialSite("知乎", "https://www.zhihu.com"), SpeedDialSite("YouTube", "https://www.youtube.com"))
