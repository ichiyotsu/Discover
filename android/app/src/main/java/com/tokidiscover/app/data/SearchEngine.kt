package com.tokidiscover.app.data
import java.net.URLEncoder
/** Search engines. Default is Bing (matches the Windows ecosystem); changeable in Settings. Custom uses a template containing {query}. */
enum class SearchEngine(val displayName: String, val template: String) {
    Google("Google", "https://www.google.com/search?q={query}"),
    Bing("Bing", "https://www.bing.com/search?q={query}"),
    DuckDuckGo("DuckDuckGo", "https://duckduckgo.com/?q={query}"),
    Baidu("百度", "https://www.baidu.com/s?wd={query}"),
    Custom("自定义", "{query}");
    fun searchUrl(query: String, customTemplate: String = ""): String {
        val tpl = if (this == Custom && customTemplate.isNotBlank()) customTemplate else template
        val enc = URLEncoder.encode(query, "UTF-8")
        return if (tpl.contains("{query}")) tpl.replace("{query}", enc) else tpl + enc
    }
    companion object { val Default = Bing }
}
