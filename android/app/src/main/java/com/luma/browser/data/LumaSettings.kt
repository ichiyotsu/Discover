package com.luma.browser.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.URLEncoder

enum class ToolbarEdge { TOP, BOTTOM }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class SearchEngine(
    val id: String,
    val label: String,
    private val baseUrl: String,
) {
    BING("bing", "Bing", "https://www.bing.com/search?q="),
    GOOGLE("google", "Google", "https://www.google.com/search?q="),
    BAIDU("baidu", "百度", "https://www.baidu.com/s?wd="),
    DUCKDUCKGO("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=");

    fun searchUrl(query: String): String = baseUrl + URLEncoder.encode(query, "UTF-8")

    companion object {
        fun fromId(id: String?): SearchEngine = entries.firstOrNull { it.id == id } ?: BING
    }
}

class LumaSettings(context: Context) {
    private val prefs = context.getSharedPreferences("luma.settings", Context.MODE_PRIVATE)

    var edge by mutableStateOf(
        if (prefs.getString(KEY_EDGE, null) == "bottom") ToolbarEdge.BOTTOM else ToolbarEdge.TOP,
    )
        private set

    var theme by mutableStateOf(
        when (prefs.getString(KEY_THEME, null)) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        },
    )
        private set

    var engine by mutableStateOf(SearchEngine.fromId(prefs.getString(KEY_ENGINE, null)))
        private set

    fun updateEdge(value: ToolbarEdge) {
        if (edge == value) return
        edge = value
        prefs.edit().putString(KEY_EDGE, if (value == ToolbarEdge.TOP) "top" else "bottom").apply()
    }

    fun updateTheme(value: ThemeMode) {
        if (theme == value) return
        theme = value
        val stored = when (value) {
            ThemeMode.SYSTEM -> "system"
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK -> "dark"
        }
        prefs.edit().putString(KEY_THEME, stored).apply()
    }

    fun updateEngine(value: SearchEngine) {
        if (engine == value) return
        engine = value
        prefs.edit().putString(KEY_ENGINE, value.id).apply()
    }

    private companion object {
        const val KEY_EDGE = "toolbar_edge"
        const val KEY_THEME = "theme_mode"
        const val KEY_ENGINE = "search_engine"
    }
}

