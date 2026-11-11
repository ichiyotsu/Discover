package com.tokidiscover.app.data
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
/** Persists bookmarks + history as JSON in SharedPreferences (DataStore-ready: swap the backing impl, keep this API).
 *  Private tabs are filtered by callers: history is only written for non-private tabs. */
class BrowserStore(context: Context) {
    private val prefs = context.getSharedPreferences("tokidiscover_library", Context.MODE_PRIVATE)
    var bookmarks: List<Bookmark>
        get() = runCatching { val a = JSONArray(prefs.getString("bookmarks", "[]")); List(a.length()) { val o = a.getJSONObject(it)
            Bookmark(o.getString("id"), o.getString("title"), o.getString("url"), o.optString("folder", "默认"), o.optLong("createdAt")) } }.getOrDefault(emptyList())
        set(v) { prefs.edit().putString("bookmarks", JSONArray(v.map { JSONObject().put("id", it.id).put("title", it.title).put("url", it.url).put("folder", it.folder).put("createdAt", it.createdAt) }).toString()).apply() }
    var history: List<HistoryEntry>
        get() = runCatching { val a = JSONArray(prefs.getString("history", "[]")); List(a.length()) { val o = a.getJSONObject(it)
            HistoryEntry(o.getString("id"), o.getString("title"), o.getString("url"), o.optLong("visitedAt")) } }.getOrDefault(emptyList())
        set(v) { prefs.edit().putString("history", JSONArray(v.map { JSONObject().put("id", it.id).put("title", it.title).put("url", it.url).put("visitedAt", it.visitedAt) }).toString()).apply() }
    fun addBookmark(b: Bookmark) { bookmarks = listOf(b) + bookmarks }
    fun removeBookmark(id: String) { bookmarks = bookmarks.filterNot { it.id == id } }
    fun recordHistory(title: String, url: String, isPrivate: Boolean) { if (isPrivate || url == UrlNormalizer.NEW_TAB_URL) return; history = (listOf(HistoryEntry(title = title.ifBlank { url }, url = url)) + history).take(500) }
    fun clearHistory() { history = emptyList() }
}
