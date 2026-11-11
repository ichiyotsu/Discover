package com.tokidiscover.app.ui.browser
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.tokidiscover.app.BuildConfig
import com.tokidiscover.app.data.SearchEngine
import com.tokidiscover.app.data.UpdateChecker
import com.tokidiscover.app.ui.BrowserViewModel
import com.tokidiscover.app.ui.components.PillGroup
import com.tokidiscover.app.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.LocalLifecycleOwner
/** Settings: search engine, homepage, theme, desktop UA, JavaScript, per-item clear-data, About (real version + update check). */
@Composable fun SettingsScreen(vm: BrowserViewModel, bottomPadding: androidx.compose.ui.unit.Dp, themeMode: ThemeMode, onThemeMode: (ThemeMode) -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var engine by remember { mutableStateOf(vm.settings.engine()) }
    var desktopUa by remember { mutableStateOf(vm.settings.desktopUa) }
    var js by remember { mutableStateOf(vm.settings.javaScriptEnabled) }
    var updateText by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = bottomPadding)) {
        Text("设置", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(20.dp))
        Text("搜索引擎（默认 Bing）", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
        PillGroup(SearchEngine.entries.map { it.displayName }, SearchEngine.entries.indexOf(engine)) { i -> engine = SearchEngine.entries[i]; vm.settings.searchEngine = engine.name }
        if (engine == SearchEngine.Custom) { Spacer(Modifier.height(8.dp))
            var tpl by remember { mutableStateOf(vm.settings.customTemplate) }
            TextField(value = tpl, onValueChange = { tpl = it; vm.settings.customTemplate = it }, label = { Text("自定义模板（含 {query}）") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(20.dp)); Text("主题", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
        PillGroup(listOf("跟随系统", "浅色", "深色"), ThemeMode.entries.indexOf(themeMode)) { onThemeMode(ThemeMode.entries[it]) }
        Spacer(Modifier.height(20.dp)); Text("浏览", style = MaterialTheme.typography.titleMedium)
        ListItem(headlineContent = { Text("桌面版 UA") }, supportingContent = { Text("以桌面浏览器标识请求网页，切换后需刷新") },
            trailingContent = { Switch(checked = desktopUa, onCheckedChange = { desktopUa = it; vm.settings.desktopUa = it }) })
        ListItem(headlineContent = { Text("JavaScript") }, supportingContent = { Text("关闭后部分网站无法正常显示，切换后需刷新") },
            trailingContent = { Switch(checked = js, onCheckedChange = { js = it; vm.settings.javaScriptEnabled = it }) })
        ListItem(headlineContent = { Text("新标签页") }, supportingContent = { Text("固定为 TokiDiscover 主页（胶囊搜索 + 常用站点），暂不支持自定义主页 URL") })
        Spacer(Modifier.height(20.dp)); Text("清除浏览数据", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush() }) { Text("Cookie") }
            OutlinedButton(onClick = { WebStorage.getInstance().deleteAllData(); WebView(context).clearCache(true) }) { Text("缓存") }
            OutlinedButton(onClick = { vm.clearHistory() }) { Text("历史") }
        }
        Spacer(Modifier.height(20.dp)); Text("关于", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
        // Version read from BuildConfig (real package version), never hardcoded.
        Text("TokiDiscover 浏览器 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）", style = MaterialTheme.typography.bodyLarge)
        Text("引擎：Android 系统 WebView · 仓库 ichiyotsu/tokidiscover（待用户创建后检查更新）", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { updateText = "检查中…"; scope.launch { val latest = UpdateChecker.fetchLatestVersion()
            updateText = when { latest == null -> "暂时无法检查（仓库未创建或无网络）"; UpdateChecker.isNewer(latest, "0.1.0") -> "发现新版本 $latest"; else -> "已是最新版本" } } }) { Text("检查更新") }
        updateText?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
