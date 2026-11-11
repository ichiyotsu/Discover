package com.tokidiscover.app.data
/** Omnibox input resolution — identical logic on Android / iOS (TokiDiscoverCore) / Windows (TokiDiscover.Core). */
sealed interface NavResult {
    data class Url(val url: String) : NavResult
    data class Search(val url: String, val query: String, val engine: SearchEngine) : NavResult
    data object NewTab : NavResult
    data class Blocked(val reason: String) : NavResult
}
object UrlNormalizer {
    const val NEW_TAB_URL = "about:blank"
    private val dangerous = listOf("javascript:", "data:", "vbscript:", "file:", "blob:")
    fun normalize(raw: String, engine: SearchEngine = SearchEngine.Default, customTemplate: String = ""): NavResult {
        val input = raw.trim()
        if (input.isEmpty() || input.equals(NEW_TAB_URL, ignoreCase = true)) return NavResult.NewTab
        val lower = input.lowercase()
        dangerous.firstOrNull { lower.startsWith(it) }?.let { return NavResult.Blocked("已拦截危险协议: $it") }
        if (lower.startsWith("about:")) return NavResult.NewTab
        if (lower.startsWith("http://") || lower.startsWith("https://")) return NavResult.Url(input)
        if (input.any { it.isWhitespace() }) return search(input, engine, customTemplate)
        val host = input.substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':')
        if (host.contains('.') || host.equals("localhost", ignoreCase = true)) return NavResult.Url("https://$input")
        return search(input, engine, customTemplate)
    }
    private fun search(q: String, e: SearchEngine, tpl: String) = NavResult.Search(e.searchUrl(q, tpl), q, e)
    fun isSafeScheme(url: String): Boolean { val l = url.lowercase(); return l.startsWith("http://") || l.startsWith("https://") || l == NEW_TAB_URL }
}
