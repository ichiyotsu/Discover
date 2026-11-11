package com.tokidiscover.app.data
import android.content.Context
/** Browser settings persisted in SharedPreferences. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("tokidiscover_settings", Context.MODE_PRIVATE)
    var searchEngine: String get() = prefs.getString("engine", SearchEngine.Default.name) ?: SearchEngine.Default.name
        set(v) { prefs.edit().putString("engine", v).apply() }
    var customTemplate: String get() = prefs.getString("custom_template", "") ?: ""
        set(v) { prefs.edit().putString("custom_template", v).apply() }
    var themeMode: String get() = prefs.getString("theme_mode", "System") ?: "System"
        set(v) { prefs.edit().putString("theme_mode", v).apply() }
    var desktopUa: Boolean get() = prefs.getBoolean("desktop_ua", false); set(v) { prefs.edit().putBoolean("desktop_ua", v).apply() }
    var javaScriptEnabled: Boolean get() = prefs.getBoolean("js", true); set(v) { prefs.edit().putBoolean("js", v).apply() }
    var homepage: String get() = prefs.getString("homepage", UrlNormalizer.NEW_TAB_URL) ?: UrlNormalizer.NEW_TAB_URL
        set(v) { prefs.edit().putString("homepage", v).apply() }
    fun engine(): SearchEngine = runCatching { SearchEngine.valueOf(searchEngine) }.getOrDefault(SearchEngine.Default)
}
