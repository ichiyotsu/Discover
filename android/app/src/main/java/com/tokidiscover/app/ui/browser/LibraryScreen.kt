package com.tokidiscover.app.ui.browser
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tokidiscover.app.ui.BrowserViewModel
import com.tokidiscover.app.ui.components.EmptyState
import com.tokidiscover.app.ui.components.PillGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
/** Library: PillGroup switches 书签 / 历史 / 下载.
 *  Downloads on Android: entries recorded from WebView's DownloadListener (name/url/size/status).
 *  Actually saving files via DownloadManager is not wired in 0.1.0-beta1 — see ARCHITECTURE.md (honest limitation). */
@Composable fun LibraryScreen(vm: BrowserViewModel, bottomPadding: androidx.compose.ui.unit.Dp, onOpenUrl: (String) -> Unit) {
    var section by remember { mutableStateOf(0) }
    val fmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    Column(Modifier.fillMaxSize().padding(20.dp).padding(bottom = bottomPadding)) {
        Text("书库", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(16.dp))
        PillGroup(listOf("书签", "历史", "下载"), section) { section = it }; Spacer(Modifier.height(16.dp))
        when (section) {
            0 -> if (vm.bookmarks.isEmpty()) EmptyState("暂无书签", "在浏览页点「收藏」添加当前页，默认存入「默认」文件夹")
                else LazyColumn { items(vm.bookmarks, key = { it.id }) { b ->
                    ListItem(headlineContent = { Text(b.title, maxLines = 1) }, supportingContent = { Text("${b.folder} · ${b.url}", maxLines = 1) },
                        trailingContent = { IconButton(onClick = { vm.removeBookmark(b.id) }) { Icon(Icons.Filled.Delete, "删除书签") } },
                        modifier = Modifier.padding(vertical = 2.dp))
                    HorizontalDivider() } }
            1 -> Column { if (vm.history.isNotEmpty()) TextButton(onClick = { vm.clearHistory() }) { Text("清空历史") }
                if (vm.history.isEmpty()) EmptyState("暂无历史", "无痕标签页不会产生历史记录")
                else LazyColumn { items(vm.history, key = { it.id }) { h ->
                    ListItem(headlineContent = { Text(h.title, maxLines = 1) }, supportingContent = { Text("${fmt.format(Date(h.visitedAt))} · ${h.url}", maxLines = 1) }) ; HorizontalDivider() } } }
            else -> if (vm.downloads.isEmpty()) EmptyState("暂无下载", "网页触发下载时会在此记录（文件名/网址/大小）；文件保存走系统下载为后续版本计划")
                else LazyColumn { items(vm.downloads, key = { it.id }) { d ->
                    ListItem(headlineContent = { Text(d.fileName, maxLines = 1) },
                        supportingContent = { Text("${d.status} · ${if (d.totalBytes > 0) "${d.totalBytes / 1024} KB" else "大小未知"} · ${d.url}", maxLines = 2) }) ; HorizontalDivider() } }
        }
    }
}
