package com.tokidiscover.app.ui
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.tokidiscover.app.TokiDiscoverApplication
import com.tokidiscover.app.data.*
/** Owns the in-memory tab list + library state. One WebView per tab is created by BrowserScreen and kept alive while its tab exists. */
class BrowserViewModel(app: Application) : AndroidViewModel(app) {
    val settings: SettingsStore get() = getApplication<TokiDiscoverApplication>().settings
    val library: BrowserStore get() = getApplication<TokiDiscoverApplication>().library
    val tabs = mutableStateListOf<BrowserTab>().apply { add(BrowserTab()) }
    var activeTabId by mutableStateOf(tabs.first().id); private set
    val activeTab: BrowserTab get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
    var bookmarks by mutableStateOf(library.bookmarks); private set
    var history by mutableStateOf(library.history); private set
    val downloads = mutableStateListOf<DownloadRecord>()
    var pendingNavigation by mutableStateOf<String?>(null) // consumed by BrowserScreen's WebView
    var blockedMessage by mutableStateOf<String?>(null)
    fun newTab(isPrivate: Boolean = false, url: String? = null) { val t = BrowserTab(isPrivate = isPrivate); tabs.add(t); activeTabId = t.id; if (url != null) navigate(url) }
    fun closeTab(id: String) { val i = tabs.indexOfFirst { it.id == id }; if (i < 0) return; tabs.removeAt(i)
        if (tabs.isEmpty()) { val t = BrowserTab(); tabs.add(t); activeTabId = t.id }
        else if (activeTabId == id) activeTabId = tabs[minOf(i, tabs.lastIndex)].id }
    fun selectTab(id: String) { if (tabs.any { it.id == id }) activeTabId = id }
    /** Resolve omnibox input through UrlNormalizer; blocked schemes surface a message instead of navigating. */
    fun submitOmnibox(input: String) { when (val r = UrlNormalizer.normalize(input, settings.engine(), settings.customTemplate)) {
        is NavResult.Url -> navigate(r.url); is NavResult.Search -> navigate(r.url)
        NavResult.NewTab -> { activeTab.url = UrlNormalizer.NEW_TAB_URL; activeTab.title = "新标签页"; pendingNavigation = UrlNormalizer.NEW_TAB_URL }
        is NavResult.Blocked -> blockedMessage = r.reason } }
    fun navigate(url: String) { activeTab.url = url; pendingNavigation = url }
    fun onPageCommitted(tab: BrowserTab, title: String, url: String) { tab.title = title.ifBlank { url }; tab.url = url; library.recordHistory(title, url, tab.isPrivate); history = library.history }
    fun toggleBookmark() { val t = activeTab; if (t.url == UrlNormalizer.NEW_TAB_URL) return
        val existing = bookmarks.firstOrNull { it.url == t.url }
        if (existing != null) library.removeBookmark(existing.id) else library.addBookmark(Bookmark(title = t.title, url = t.url))
        bookmarks = library.bookmarks }
    fun isBookmarked(url: String) = bookmarks.any { it.url == url }
    fun removeBookmark(id: String) { library.removeBookmark(id); bookmarks = library.bookmarks }
    fun clearHistory() { library.clearHistory(); history = library.history }
    fun onDownloadStarted(fileName: String, url: String, totalBytes: Long) { downloads.add(0, DownloadRecord(fileName = fileName, url = url, totalBytes = totalBytes)) }
}
