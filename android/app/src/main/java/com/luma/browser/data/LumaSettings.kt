package com.luma.browser.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.URLEncoder

enum class ToolbarEdge { TOP, BOTTOM }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class TabLayout { GRID, LIST }

enum class UpdateChannel { STABLE, BETA }

enum class ToolbarAction { BACK, FORWARD, RELOAD, TABS, TRANSLATE }

data class SearchProvider(
    val id: String,
    val label: String,
    val template: String,
)

enum class SearchEngine(
    val id: String,
    val label: String,
    val template: String,
) {
    BING("bing", "Bing", "https://www.bing.com/search?q={query}"),
    GOOGLE("google", "Google", "https://www.google.com/search?q={query}"),
    BAIDU("baidu", "百度", "https://www.baidu.com/s?wd={query}"),
    DUCKDUCKGO("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q={query}"),
    CUSTOM("custom", "自定义", ""),
    ;

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

    var customSearchName by mutableStateOf(
        prefs.getString(KEY_CUSTOM_SEARCH_NAME, null)?.takeIf { it.isNotBlank() } ?: "自定义",
    )
        private set

    var customSearchTemplate by mutableStateOf(
        prefs.getString(KEY_CUSTOM_SEARCH_TEMPLATE, null)?.takeIf { it.isNotBlank() }
            ?: "https://www.google.com/search?q={query}",
    )
        private set

    var autoCollapseToolbar by mutableStateOf(prefs.getBoolean(KEY_AUTO_COLLAPSE, true))
        private set

    var tabLayout by mutableStateOf(
        if (prefs.getString(KEY_TAB_LAYOUT, null) == "list") TabLayout.LIST else TabLayout.GRID,
    )
        private set

    var tabPreviews by mutableStateOf(prefs.getBoolean(KEY_TAB_PREVIEWS, true))
        private set

    var backgroundUri by mutableStateOf(prefs.getString(KEY_BACKGROUND_URI, null))
        private set

    var updateChannel by mutableStateOf(
        if (prefs.getString(KEY_UPDATE_CHANNEL, null) == "beta") UpdateChannel.BETA else UpdateChannel.STABLE,
    )
        private set

    var autoCheckUpdates by mutableStateOf(prefs.getBoolean(KEY_AUTO_CHECK_UPDATES, true))
        private set

    var toolbarBack by mutableStateOf(prefs.getBoolean(KEY_TOOLBAR_BACK, true))
        private set
    var toolbarForward by mutableStateOf(prefs.getBoolean(KEY_TOOLBAR_FORWARD, false))
        private set
    var toolbarReload by mutableStateOf(prefs.getBoolean(KEY_TOOLBAR_RELOAD, true))
        private set
    var toolbarTabs by mutableStateOf(prefs.getBoolean(KEY_TOOLBAR_TABS, true))
        private set
    var toolbarTranslate by mutableStateOf(prefs.getBoolean(KEY_TOOLBAR_TRANSLATE, false))
        private set

    val searchProvider: SearchProvider
        get() = if (engine == SearchEngine.CUSTOM) {
            SearchProvider(
                id = SearchEngine.CUSTOM.id,
                label = customSearchName.ifBlank { "自定义" },
                template = customSearchTemplate,
            )
        } else {
            SearchProvider(engine.id, engine.label, engine.template)
        }

    fun searchUrl(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val template = searchProvider.template
            .takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?: "https://www.google.com/search?q={query}"
        return when {
            template.contains("{query}") -> template.replace("{query}", encoded)
            template.contains("%s") -> template.replace("%s", encoded)
            else -> template + encoded
        }
    }

    fun toolbarActionEnabled(action: ToolbarAction): Boolean = when (action) {
        ToolbarAction.BACK -> toolbarBack
        ToolbarAction.FORWARD -> toolbarForward
        ToolbarAction.RELOAD -> toolbarReload
        ToolbarAction.TABS -> toolbarTabs
        ToolbarAction.TRANSLATE -> toolbarTranslate
    }

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

    fun updateCustomSearch(name: String, template: String) {
        customSearchName = name.trim().ifBlank { "自定义" }
        customSearchTemplate = template.trim()
        engine = SearchEngine.CUSTOM
        prefs.edit()
            .putString(KEY_CUSTOM_SEARCH_NAME, customSearchName)
            .putString(KEY_CUSTOM_SEARCH_TEMPLATE, customSearchTemplate)
            .putString(KEY_ENGINE, SearchEngine.CUSTOM.id)
            .apply()
    }

    fun updateAutoCollapse(value: Boolean) {
        autoCollapseToolbar = value
        prefs.edit().putBoolean(KEY_AUTO_COLLAPSE, value).apply()
    }

    fun updateTabLayout(value: TabLayout) {
        tabLayout = value
        prefs.edit().putString(KEY_TAB_LAYOUT, if (value == TabLayout.LIST) "list" else "grid").apply()
    }

    fun updateTabPreviews(value: Boolean) {
        tabPreviews = value
        prefs.edit().putBoolean(KEY_TAB_PREVIEWS, value).apply()
    }

    fun updateBackgroundUri(value: String?) {
        backgroundUri = value
        prefs.edit().apply {
            if (value.isNullOrBlank()) remove(KEY_BACKGROUND_URI) else putString(KEY_BACKGROUND_URI, value)
        }.apply()
    }

    fun updateUpdateChannel(value: UpdateChannel) {
        updateChannel = value
        prefs.edit().putString(KEY_UPDATE_CHANNEL, if (value == UpdateChannel.BETA) "beta" else "stable").apply()
    }

    fun updateAutoCheckUpdates(value: Boolean) {
        autoCheckUpdates = value
        prefs.edit().putBoolean(KEY_AUTO_CHECK_UPDATES, value).apply()
    }

    fun updateToolbarAction(action: ToolbarAction, enabled: Boolean) {
        when (action) {
            ToolbarAction.BACK -> {
                toolbarBack = enabled
                prefs.edit().putBoolean(KEY_TOOLBAR_BACK, enabled).apply()
            }
            ToolbarAction.FORWARD -> {
                toolbarForward = enabled
                prefs.edit().putBoolean(KEY_TOOLBAR_FORWARD, enabled).apply()
            }
            ToolbarAction.RELOAD -> {
                toolbarReload = enabled
                prefs.edit().putBoolean(KEY_TOOLBAR_RELOAD, enabled).apply()
            }
            ToolbarAction.TABS -> {
                toolbarTabs = enabled
                prefs.edit().putBoolean(KEY_TOOLBAR_TABS, enabled).apply()
            }
            ToolbarAction.TRANSLATE -> {
                toolbarTranslate = enabled
                prefs.edit().putBoolean(KEY_TOOLBAR_TRANSLATE, enabled).apply()
            }
        }
    }

    private companion object {
        const val KEY_EDGE = "toolbar_edge"
        const val KEY_THEME = "theme_mode"
        const val KEY_ENGINE = "search_engine"
        const val KEY_CUSTOM_SEARCH_NAME = "custom_search_name"
        const val KEY_CUSTOM_SEARCH_TEMPLATE = "custom_search_template"
        const val KEY_AUTO_COLLAPSE = "auto_collapse_toolbar"
        const val KEY_TAB_LAYOUT = "tab_layout"
        const val KEY_TAB_PREVIEWS = "tab_previews"
        const val KEY_BACKGROUND_URI = "background_uri"
        const val KEY_UPDATE_CHANNEL = "update_channel"
        const val KEY_AUTO_CHECK_UPDATES = "auto_check_updates"
        const val KEY_TOOLBAR_BACK = "toolbar_back"
        const val KEY_TOOLBAR_FORWARD = "toolbar_forward"
        const val KEY_TOOLBAR_RELOAD = "toolbar_reload"
        const val KEY_TOOLBAR_TABS = "toolbar_tabs"
        const val KEY_TOOLBAR_TRANSLATE = "toolbar_translate"
    }
}
