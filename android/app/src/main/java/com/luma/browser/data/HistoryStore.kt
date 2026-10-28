package com.luma.browser.data

import android.content.Context
import android.text.Html
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val url: String,
    val title: String,
    val visitedAt: Long,
)

data class HistoryImportResult(
    val added: Int,
    val skipped: Int,
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
        while (entries.size > MAX_ENTRIES) entries.removeAt(entries.lastIndex)
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

    fun remove(entry: HistoryEntry) {
        val index = entries.indexOfFirst { it.url == entry.url && it.visitedAt == entry.visitedAt }
        if (index >= 0) {
            entries.removeAt(index)
            persist()
        }
    }

    fun clearRange(fromInclusive: Long, toInclusive: Long = Long.MAX_VALUE) {
        val removed = entries.filter { it.visitedAt in fromInclusive..toInclusive }
        if (removed.isEmpty()) return
        entries.removeAll(removed.toSet())
        persist()
    }

    fun countRange(fromInclusive: Long, toInclusive: Long = Long.MAX_VALUE): Int =
        entries.count { it.visitedAt in fromInclusive..toInclusive }

    fun clear() {
        entries.clear()
        prefs.edit().remove(KEY_ENTRIES).apply()
    }

    fun exportJson(): String {
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
        return JSONObject().apply {
            put("format", "luma-history")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("entries", array)
        }.toString(2)
    }

    /**
     * Imports JSON, Chrome/Edge history HTML exports, Chrome Takeout BrowserHistory.json,
     * or a plain list of URLs. Returns how many valid entries were added.
     */
    fun importContent(raw: String): HistoryImportResult {
        val parsed = mutableListOf<HistoryEntry>()
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            runCatching { parseJson(trimmed) }.getOrNull()?.let(parsed::addAll)
        }
        if (parsed.isEmpty() && trimmed.contains("<a", ignoreCase = true)) {
            parseHtml(trimmed, parsed)
        }
        if (parsed.isEmpty()) {
            parsePlainUrls(trimmed, parsed)
        }

        val seen = entries.map { it.url }.toMutableSet()
        var added = 0
        var skipped = 0
        val now = System.currentTimeMillis()
        parsed.forEachIndexed { index, entry ->
            if (!entry.url.startsWith("http") || !seen.add(entry.url)) {
                skipped++
                return@forEachIndexed
            }
            entries.add(
                HistoryEntry(
                    url = entry.url,
                    title = entry.title.ifBlank { hostOf(entry.url) },
                    visitedAt = if (entry.visitedAt > 0) entry.visitedAt else now - index * 1000L,
                ),
            )
            added++
        }
        if (added > 0) {
            entries.sortByDescending { it.visitedAt }
            while (entries.size > MAX_ENTRIES) entries.removeAt(entries.lastIndex)
            persist()
        }
        return HistoryImportResult(added, skipped)
    }

    private fun parseJson(raw: String): List<HistoryEntry> {
        val result = mutableListOf<HistoryEntry>()
        val root = runCatching { JSONArray(raw) }.getOrNull()
        val array = if (root != null) {
            root
        } else {
            val obj = JSONObject(raw)
            obj.optJSONArray("entries")
                ?: obj.optJSONArray("Browser History")
                ?: obj.optJSONArray("history")
                ?: JSONArray()
        }
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val url = item.optString("url").takeIf { it.startsWith("http") } ?: continue
            val title = item.optString("title")
            val visitedAt = when {
                item.has("visitedAt") -> item.optLong("visitedAt")
                item.has("time_usec") -> chromeTimeToEpoch(item.optLong("time_usec"))
                item.has("last_visit_time") -> chromeTimeToEpoch(item.optLong("last_visit_time"))
                else -> 0L
            }
            result.add(HistoryEntry(url, title, visitedAt))
        }
        return result
    }

    private fun parseHtml(raw: String, result: MutableList<HistoryEntry>) {
        val anchor = Regex(
            """<a\s+[^>]*href=["']([^"']+)["'][^>]*>(.*?)</a>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        anchor.findAll(raw).forEach { match ->
            val url = match.groupValues[1].replace("&amp;", "&")
            if (!url.startsWith("http")) return@forEach
            val title = Html.fromHtml(match.groupValues[2], Html.FROM_HTML_MODE_LEGACY)
                .toString()
                .replace(Regex("\\s+"), " ")
                .trim()
            result.add(HistoryEntry(url, title, 0L))
        }
    }

    private fun parsePlainUrls(raw: String, result: MutableList<HistoryEntry>) {
        val urlPattern = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
        urlPattern.findAll(raw).forEach { match ->
            val url = match.value.trimEnd('.', ',', ')', ']', '"', '\'')
            result.add(HistoryEntry(url, hostOf(url), 0L))
        }
    }

    private fun chromeTimeToEpoch(value: Long): Long {
        if (value <= 0) return 0L
        val millis = if (value > 10_000_000_000_000L) value / 1000L else value
        val epoch = millis - CHROME_EPOCH_OFFSET_MS
        return if (epoch > 0) epoch else 0L
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
        const val MAX_ENTRIES = 2000
        const val CHROME_EPOCH_OFFSET_MS = 11_644_473_600_000L
    }
}
