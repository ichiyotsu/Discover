package com.tokidiscover.app.ui.browser
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tokidiscover.app.ui.BrowserViewModel
import com.tokidiscover.app.ui.components.ExpressiveCard
/** Tab overview grid: card per tab (title + URL + close), active tab highlighted, private tabs badged. */
@Composable fun TabsScreen(vm: BrowserViewModel, bottomPadding: androidx.compose.ui.unit.Dp, onPick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp).padding(bottom = bottomPadding)) {
        Text("标签页", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            items(vm.tabs, key = { it.id }) { tab ->
                val active = tab.id == vm.activeTabId
                Surface(shape = com.tokidiscover.app.ui.theme.ExpressiveCardShape,
                    color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(16.dp)) {
                        Row { Text(tab.title.ifBlank { "新标签页" }, style = MaterialTheme.typography.titleSmall, maxLines = 2, modifier = Modifier.weight(1f))
                            IconButton(onClick = { vm.closeTab(tab.id) }) { Icon(Icons.Filled.Close, "关闭标签") } }
                        Text(if (tab.url == "about:blank") "新标签页" else tab.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        if (tab.isPrivate) AssistChip(onClick = {}, label = { Text("无痕") })
                        TextButton(onClick = { vm.selectTab(tab.id); onPick() }) { Text(if (active) "当前标签" else "切换到此标签") }
                    } } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { vm.newTab(); onPick() }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("新建标签") }
            OutlinedButton(onClick = { vm.newTab(isPrivate = true); onPick() }) { Text("新建无痕标签") }
        }
    }
}
