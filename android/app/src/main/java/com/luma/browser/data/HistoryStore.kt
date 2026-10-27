package com.luma.browser.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val url: String,
    val title: String,
    val visitedAt: Long,
)

class HistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("luma.history", Context.MODE_PRIVATE)
    val entries = mutableStateListOf<HistoryEntry>()

    init {
        load()
    }

    fun record(url: String, title: String) {
        if (!url.startsWith("http")) return
        val cleanTitle = title.trim().ifBlank { hostOf(url) }
        val now = System.currentTimeMillis()
        val existing = entries.indexOfFirst { it.url == url }
        if (existing >= 0) {
            if (now - entries[existing].visitedAt < 60_000) {
                entries[existing] = entries[existing].copy(title = cleanTitle, visitedAt = now)
            } else {
                entries.removeAt(existing)
                entries.add(0, HistoryEntry(url, cleanTitle, now))
            }
        } else {
            entries.add(0, HistoryEntry(url, cleanTitle, now))
        }
        while (entries.size > 120) entries.removeAt(entries.lastIndex)
        persist()
    }

    fun updateTitle(url: String, title: String) {
        val index = entries.indexOfFirst { it.url == url }
        if (index >= 0 && title.isNotBlank()) {
            entries[index] = entries[index].copy(title = title)
            persist()
        }
    }

    fun suggestions(query: String, limit: Int = 5): List<HistoryEntry> {
        val needle = query.trim().lowercase()
        return entries.asSequence()
            .filter { entry ->
                needle.isEmpty() ||
                    entry.title.lowercase().contains(needle) ||
                    entry.url.lowercase().contains(needle)
            }
            .take(limit)
            .toList()
    }

    fun clear() {
        entries.clear()
        prefs.edit().remove(KEY_ENTRIES).apply()
    }

    private fun load() {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return
        runCatching {
            val array = JSONArray(raw)
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val url = item.optString("url")
                if (url.isBlank()) continue
                entries.add(
                    HistoryEntry(
                        url = url,
                        title = item.optString("title").ifBlank { hostOf(url) },
                        visitedAt = item.optLong("visitedAt", 0L),
                    ),
                )
            }
        }
    }

    private fun persist() {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("url", entry.url)
                    put("title", entry.title)
                    put("visitedAt", entry.visitedAt)
                },
            )
        }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    private fun hostOf(url: String): String = runCatching {
        java.net.URI(url).host?.removePrefix("www.").orEmpty()
    }.getOrNull().orEmpty().ifBlank { url }

    private companion object {
        const val KEY_ENTRIES = "entries"
    }
}
