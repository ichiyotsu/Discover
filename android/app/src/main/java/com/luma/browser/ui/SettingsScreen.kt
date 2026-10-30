@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.luma.browser.ui

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luma.browser.BuildConfig
import com.luma.browser.data.HistoryStore
import com.luma.browser.data.LumaSettings
import com.luma.browser.data.SearchEngine
import com.luma.browser.data.TabLayout
import com.luma.browser.data.ThemeMode
import com.luma.browser.data.ToolbarAction
import com.luma.browser.data.ToolbarEdge
import com.luma.browser.data.UpdateChannel
import com.luma.browser.data.UpdateInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SettingsPage { ROOT, APPEARANCE, TOOLBAR, SEARCH, TABS, DATA, UPDATE, ABOUT }

@Composable
fun SettingsScreen(
    settings: LumaSettings,
    history: HistoryStore,
    updateInfo: UpdateInfo?,
    checkingUpdate: Boolean,
    downloadingUpdate: Boolean,
    downloadProgress: Float,
    updateMessage: String?,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onOpenHistory: () -> Unit,
    onClose: () -> Unit,
) {
    var page by remember { mutableStateOf(SettingsPage.ROOT) }
    val context = LocalContext.current
    var dataMessage by remember { mutableStateOf<String?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.onSuccess { text ->
            if (text.isNullOrBlank()) {
                dataMessage = "文件为空或无法读取"
                return@onSuccess
            }
            val result = history.importContent(text)
            dataMessage = "已导入 ${result.added} 条，跳过 ${result.skipped} 条"
            Toast.makeText(context, dataMessage, Toast.LENGTH_LONG).show()
        }.onFailure {
            dataMessage = "导入失败：${it.message ?: "无法读取文件"}"
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(history.exportJson())
            }
        }.onSuccess {
            Toast.makeText(context, "历史记录已导出", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "导出失败：${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler {
        if (page == SettingsPage.ROOT) onClose() else page = SettingsPage.ROOT
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { if (page == SettingsPage.ROOT) onClose() else page = SettingsPage.ROOT }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = when (page) {
                        SettingsPage.ROOT -> "设置"
                        SettingsPage.APPEARANCE -> "外观"
                        SettingsPage.TOOLBAR -> "工具栏"
                        SettingsPage.SEARCH -> "搜索引擎"
                        SettingsPage.TABS -> "标签页"
                        SettingsPage.DATA -> "数据"
                        SettingsPage.UPDATE -> "应用更新"
                        SettingsPage.ABOUT -> "关于"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Luna ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            when (page) {
                SettingsPage.ROOT -> SettingsRoot(
                    settings = settings,
                    updateAvailable = updateInfo != null,
                    onOpen = { page = it },
                )

                SettingsPage.APPEARANCE -> AppearancePage(settings)
                SettingsPage.TOOLBAR -> ToolbarSettingsPage(settings)
                SettingsPage.SEARCH -> SearchSettingsPage(settings)
                SettingsPage.TABS -> TabsSettingsPage(settings)
                SettingsPage.DATA -> DataPage(
                    history = history,
                    message = dataMessage,
                    onExport = { exportLauncher.launch("Luna-history-${fileDate()}.json") },
                    onImport = {
                        importLauncher.launch(
                            arrayOf("text/html", "application/json", "text/plain", "application/octet-stream"),
                        )
                    },
                    onOpenHistory = onOpenHistory,
                    onClearAll = { confirmClearAll = true },
                )
                SettingsPage.UPDATE -> UpdatePage(
                    settings = settings,
                    updateInfo = updateInfo,
                    checking = checkingUpdate,
                    downloading = downloadingUpdate,
                    progress = downloadProgress,
                    message = updateMessage,
                    onCheck = onCheckUpdate,
                    onInstall = onInstallUpdate,
                    onOpenReleasePage = onOpenReleasePage,
                )
                SettingsPage.ABOUT -> AboutPage()
            }
        }
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("清空全部历史记录？") },
            text = { Text("当前共有 ${history.entries.size} 条记录。清空后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        history.clear()
                        confirmClearAll = false
                        dataMessage = "历史记录已清空"
                    },
                ) { Text("确认清空") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SettingsRoot(
    settings: LumaSettings,
    updateAvailable: Boolean,
    onOpen: (SettingsPage) -> Unit,
) {
    val entries = listOf(
        SettingsEntry(
            icon = Icons.Rounded.Palette,
            title = "外观",
            summary = when (settings.theme) {
                ThemeMode.SYSTEM -> "跟随系统"
                ThemeMode.LIGHT -> "浅色"
                ThemeMode.DARK -> "深色"
            },
            page = SettingsPage.APPEARANCE,
        ),
        SettingsEntry(
            icon = Icons.Rounded.Tune,
            title = "工具栏",
            summary = if (settings.edge == ToolbarEdge.TOP) {
                "顶部"
            } else {
                "底部 · " + if (settings.autoCollapseToolbar) "滚动自动收起" else "始终展开"
            },
            page = SettingsPage.TOOLBAR,
        ),
        SettingsEntry(
            icon = Icons.Rounded.Search,
            title = "搜索引擎",
            summary = settings.searchProvider.label,
            page = SettingsPage.SEARCH,
        ),
        SettingsEntry(
            icon = Icons.Rounded.Tab,
            title = "标签页",
            summary = "${if (settings.tabLayout == TabLayout.GRID) "网格" else "列表"} · " +
                if (settings.tabPreviews) "实时预览" else "仅标题",
            page = SettingsPage.TABS,
        ),
        SettingsEntry(
            icon = Icons.Rounded.Storage,
            title = "数据",
            summary = "历史导入 / 导出 / 清理",
            page = SettingsPage.DATA,
        ),
        SettingsEntry(
            icon = Icons.Rounded.Refresh,
            title = "应用更新",
            summary = if (updateAvailable) "有新版本可用" else
                if (settings.updateChannel == UpdateChannel.BETA) "Beta 通道" else "正式版通道",
            page = SettingsPage.UPDATE,
            badge = updateAvailable,
        ),
        SettingsEntry(
            icon = Icons.Rounded.Info,
            title = "关于",
            summary = "Luna ${BuildConfig.VERSION_NAME}",
            page = SettingsPage.ABOUT,
        ),
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(entries.size) { index ->
            val entry = entries[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(entry.page) }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            entry.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        if (entry.badge) {
                            Spacer(Modifier.width(7.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Text(
                                    text = "新",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = entry.summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (index != entries.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f),
                )
            }
        }
    }
}

private data class SettingsEntry(
    val icon: ImageVector,
    val title: String,
    val summary: String,
    val page: SettingsPage,
    val badge: Boolean = false,
)

@Composable
private fun AppearancePage(settings: LumaSettings) {
    val context = LocalContext.current
    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            settings.updateBackgroundUri(uri.toString())
        }
    }
    SettingsPageContent {
        SettingsLabel("明暗模式", "界面和系统栏会立即切换。")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = listOf(
                ThemeMode.SYSTEM to "跟随系统",
                ThemeMode.LIGHT to "浅色",
                ThemeMode.DARK to "深色",
            )
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = settings.theme == option.first,
                    onClick = { settings.updateTheme(option.first) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    modifier = Modifier.weight(1f),
                ) { Text(option.second, maxLines = 1) }
            }
        }
        Spacer(Modifier.height(24.dp))
        SettingsLabel("主页背景", "默认沿用当前渐变，也可以从相册选择一张图片。")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { backgroundLauncher.launch(arrayOf("image/*")) },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (settings.backgroundUri == null) "选择图片" else "更换图片")
            }
            if (settings.backgroundUri != null) {
                TextButton(
                    onClick = {
                        settings.backgroundUri?.let { uri ->
                            runCatching {
                                context.contentResolver.releasePersistableUriPermission(
                                    Uri.parse(uri),
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                                )
                            }
                        }
                        settings.updateBackgroundUri(null)
                    },
                ) { Text("恢复默认") }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "全局排版使用 Google Sans Flex，圆角、层级和玻璃质感参照 TokiUI；桌面端的主题色种子暂未在手机端开放。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ToolbarSettingsPage(settings: LumaSettings) {
    SettingsPageContent {
        SettingsLabel("停靠位置", "网页全屏显示，工具栏悬浮在网页之上。")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(ToolbarEdge.TOP to "顶部", ToolbarEdge.BOTTOM to "底部").forEachIndexed { index, option ->
                SegmentedButton(
                    selected = settings.edge == option.first,
                    onClick = { settings.updateEdge(option.first) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                    modifier = Modifier.weight(1f),
                ) { Text(option.second) }
            }
        }
        Spacer(Modifier.height(20.dp))
        SettingsSwitchRow(
            title = "向下滑动时收起",
            description = "底栏收起为只显示网址的小胶囊；向上滑动或点击后展开。",
            checked = settings.autoCollapseToolbar,
            onCheckedChange = settings::updateAutoCollapse,
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
        SettingsLabel("工具栏项目", "地址栏和菜单始终显示。")
        ToolbarSwitch("后退", settings.toolbarBack) { settings.updateToolbarAction(ToolbarAction.BACK, it) }
        ToolbarSwitch("前进", settings.toolbarForward) { settings.updateToolbarAction(ToolbarAction.FORWARD, it) }
        ToolbarSwitch("刷新 / 停止", settings.toolbarReload) { settings.updateToolbarAction(ToolbarAction.RELOAD, it) }
        ToolbarSwitch("标签页", settings.toolbarTabs) { settings.updateToolbarAction(ToolbarAction.TABS, it) }
        ToolbarSwitch("翻译", settings.toolbarTranslate) { settings.updateToolbarAction(ToolbarAction.TRANSLATE, it) }
    }
}

@Composable
private fun ToolbarSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SearchSettingsPage(settings: LumaSettings) {
    var name by remember { mutableStateOf(settings.customSearchName) }
    var template by remember { mutableStateOf(settings.customSearchTemplate) }
    val validTemplate = template.trim().let {
        (it.startsWith("http://") || it.startsWith("https://")) && it.length > 12
    }
    SettingsPageContent {
        SettingsLabel("预设搜索引擎", "点击后立即生效。")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchEngine.entries.filter { it != SearchEngine.CUSTOM }.forEach { engine ->
                FilterChip(
                    selected = settings.engine == engine,
                    onClick = { settings.updateEngine(engine) },
                    label = { Text(engine.label) },
                    leadingIcon = if (settings.engine == engine) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(15.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        SettingsLabel("自定义搜索引擎", "模板里用 {query} 代表搜索词。")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = template,
            onValueChange = { template = it },
            label = { Text("搜索 URL 模板") },
            placeholder = { Text("https://www.google.com/search?q={query}") },
            singleLine = false,
            minLines = 2,
            isError = !validTemplate,
            supportingText = {
                Text(if (validTemplate) "支持 {query} 或 %s 占位符" else "请输入以 http(s):// 开头的模板")
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { settings.updateCustomSearch(name, template) },
            enabled = validTemplate,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("保存并使用") }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "当前：${settings.searchProvider.label}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TabsSettingsPage(settings: LumaSettings) {
    SettingsPageContent {
        SettingsLabel("标签页排版", "点开标签页后全屏展示。")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(TabLayout.GRID to "网格", TabLayout.LIST to "列表").forEachIndexed { index, option ->
                SegmentedButton(
                    selected = settings.tabLayout == option.first,
                    onClick = { settings.updateTabLayout(option.first) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                    modifier = Modifier.weight(1f),
                ) { Text(option.second) }
            }
        }
        Spacer(Modifier.height(20.dp))
        SettingsSwitchRow(
            title = "网页预览",
            description = "标签页卡片显示网页截图，而不只是网址。后台标签页会保持 WebView 预加载。",
            checked = settings.tabPreviews,
            onCheckedChange = settings::updateTabPreviews,
        )
    }
}

@Composable
private fun DataPage(
    history: HistoryStore,
    message: String?,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onOpenHistory: () -> Unit,
    onClearAll: () -> Unit,
) {
    SettingsPageContent {
        SettingsLabel("浏览记录", "共 ${history.entries.size} 条，保存在本机。")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onImport, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("导入")
            }
            Button(onClick = onExport, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("导出")
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "支持 Luna JSON、Chrome / Edge 导出的历史记录 HTML，以及 Chrome Takeout 的 BrowserHistory.json。Google Drive / OneDrive 可通过系统文件选择器直接导入导出。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (message != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        SettingsClickRow("查看完整历史记录", "按条目删除、按时间范围清空", onOpenHistory)
        SettingsClickRow("清空全部历史记录", "需要二次确认", onClearAll, danger = true)
    }
}

@Composable
private fun UpdatePage(
    settings: LumaSettings,
    updateInfo: UpdateInfo?,
    checking: Boolean,
    downloading: Boolean,
    progress: Float,
    message: String?,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    onOpenReleasePage: () -> Unit,
) {
    SettingsPageContent {
        SettingsLabel("更新通道", "正式版只包含稳定版本；Beta 会接收预发布版本。")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(UpdateChannel.STABLE to "正式版", UpdateChannel.BETA to "Beta").forEachIndexed { index, option ->
                SegmentedButton(
                    selected = settings.updateChannel == option.first,
                    onClick = { settings.updateUpdateChannel(option.first) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                    modifier = Modifier.weight(1f),
                ) { Text(option.second) }
            }
        }
        Spacer(Modifier.height(14.dp))
        SettingsSwitchRow(
            title = "每次启动检查更新",
            description = "从 GitHub Releases 读取最新版本。",
            checked = settings.autoCheckUpdates,
            onCheckedChange = settings::updateAutoCheckUpdates,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCheck, enabled = !checking && !downloading, modifier = Modifier.fillMaxWidth()) {
            if (checking) {
                Text("正在检查…")
            } else {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("立即检查")
            }
        }
        if (message != null) {
            Spacer(Modifier.height(10.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (updateInfo != null) {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${updateInfo.title}${if (updateInfo.prerelease) " · Beta" else ""}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = updateInfo.notes.take(280).ifBlank { "新版本已发布。" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onInstall, enabled = !downloading, modifier = Modifier.fillMaxWidth()) {
                if (downloading) {
                    Text("下载中 ${(progress * 100).toInt()}%")
                } else {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("下载并安装")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onOpenReleasePage, modifier = Modifier.fillMaxWidth()) {
            Text("在 GitHub 查看 Release")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "当前版本 ${BuildConfig.VERSION_NAME} · ${if (settings.updateChannel == UpdateChannel.BETA) "Beta" else "正式版"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutPage() {
    val webViewVersion = remember {
        runCatching { WebView.getCurrentWebViewPackage()?.versionName }.getOrNull() ?: "系统 WebView"
    }
    val context = LocalContext.current
    SettingsPageContent {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Luna", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Material 3 Expressive 浏览器 · ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        SettingsInfoRow("浏览器内核", "Chromium WebView $webViewVersion")
        SettingsInfoRow("界面", "Jetpack Compose · Material 3")
        SettingsInfoRow("设计参考", "md3e · Material 3 Expressive")
        SettingsInfoRow("图标", "Streamline · ai-edit-spark-solid")
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Luna 是独立项目，与 Google 无关。网页由 Android System WebView 渲染。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ichiyotsu/Discover")),
                    )
                }
            },
        ) { Text("GitHub · ichiyotsu/Discover") }
    }
}

@Composable
private fun SettingsPageContent(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        content()
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SettingsLabel(title: String, description: String? = null) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    if (description != null) {
        Spacer(Modifier.height(3.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsClickRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun fileDate(): String =
    SimpleDateFormat("yyyyMMdd-HHmm", Locale.SIMPLIFIED_CHINESE).format(Date())
