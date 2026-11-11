package com.tokidiscover.app.ui.browser
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tokidiscover.app.data.DefaultSpeedDial
import com.tokidiscover.app.ui.BrowserViewModel
import com.tokidiscover.app.ui.components.EmptyState
import com.tokidiscover.app.ui.components.ExpressiveCard
/** New-tab Home: centered capsule search (upper area) + Speed Dial ExpressiveCard grid + recent history shortcuts. */
@Composable fun HomeScreen(vm: BrowserViewModel, bottomPadding: androidx.compose.ui.unit.Dp) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(bottom = bottomPadding)) {
        Spacer(Modifier.height(72.dp))
        Text("TokiDiscover", style = MaterialTheme.typography.displayMedium)
        Text(if (vm.activeTab.isPrivate) "无痕标签页 — 不会记录历史" else "搜索或输入网址", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        TextField(value = query, onValueChange = { query = it }, singleLine = true, placeholder = { Text("搜索或输入网址") },
            shape = MaterialTheme.shapes.extraLarge, colors = TextFieldDefaults.colors(focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent, unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go), keyboardActions = KeyboardActions(onGo = { vm.submitOmnibox(query) }),
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(28.dp))
        Text("常用站点", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            items(DefaultSpeedDial) { site -> ExpressiveCard(onClick = { vm.navigate(site.url) }) { Text(site.title, style = MaterialTheme.typography.titleSmall); Text(site.url.removePrefix("https://"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) } }
        }
        if (vm.history.isNotEmpty() && !vm.activeTab.isPrivate) {
            Text("最近访问", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
            vm.history.take(3).forEach { h -> ListItem(headlineContent = { Text(h.title, maxLines = 1) }, supportingContent = { Text(h.url, maxLines = 1) },
                modifier = Modifier.padding(vertical = 2.dp)) }
        } else if (!vm.activeTab.isPrivate) EmptyState("暂无历史", "访问过的页面会显示在这里")
    }
}
