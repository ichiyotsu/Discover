package dev.ichiyotsu.tokidiscover.browser

import android.net.Uri
import java.util.Locale

data class SearchEngine(
    val id: String,
    val label: String,
    val template: String,
)

object SearchEngines {
    val all = listOf(
        SearchEngine("google", "Google", "https://www.google.com/search?q={query}"),
        SearchEngine("bing", "Bing", "https://www.bing.com/search?q={query}"),
        SearchEngine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q={query}"),
        SearchEngine("baidu", "百度", "https://www.baidu.com/s?wd={query}"),
    )

    fun byId(id: String): SearchEngine = all.firstOrNull { it.id == id } ?: all.first()
}

object Omnibox {
    const val HOME = "toki://home"

    fun resolve(raw: String, engine: SearchEngine): String {
        val text = raw.trim()
        if (text.isEmpty() || text.equals(HOME, true) || text.equals("about:home", true)) {
            return HOME
        }
        val candidate = if (text.startsWith("//")) "https:$text" else text
        if ("://" in candidate) {
            val uri = Uri.parse(candidate)
            val scheme = uri.scheme?.lowercase(Locale.US)
            if ((scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()) {
                return candidate
            }
        }
        val looksLikeHost = !text.contains(' ') && !text.contains('　') &&
            (text.startsWith("localhost") || '.' in text || text.contains(':'))
        if (looksLikeHost) {
            return "https://$text"
        }
        return engine.template.replace("{query}", Uri.encode(text))
    }

    fun onlineTranslate(pageUrl: String, targetTag: String): String {
        val tl = when {
            targetTag.startsWith("zh") -> "zh-CN"
            targetTag.isBlank() -> "en"
            else -> targetTag
        }
        return "https://translate.google.com/translate?sl=auto&tl=${Uri.encode(tl)}&u=${Uri.encode(pageUrl)}"
    }
}
