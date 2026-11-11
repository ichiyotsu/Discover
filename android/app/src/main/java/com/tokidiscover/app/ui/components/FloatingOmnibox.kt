package com.tokidiscover.app.ui.components
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tokidiscover.app.data.BrowserTab
/** Bottom floating glass capsule omnibox (36dp, same glass recipe as FloatingNavBar — the browser's primary
 *  control lives at the bottom, per the Toki floating-capsule language). Shows site title + host when not editing. */
@Composable fun FloatingOmnibox(tab: BrowserTab, isLoading: Boolean, onSubmit: (String) -> Unit, onBack: () -> Unit, onForward: () -> Unit,
    onReloadOrStop: () -> Unit, onTabs: () -> Unit, tabCount: Int, glass: Modifier = Modifier, modifier: Modifier = Modifier) {
    var editing by remember(tab.id) { mutableStateOf(false) }
    var text by remember(tab.id, tab.url) { mutableStateOf(if (tab.url == "about:blank") "" else tab.url) }
    Column(modifier) {
        if (tab.progress in 1..99) LinearProgressIndicator(progress = { tab.progress / 100f }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.primary)
        Row(modifier = Modifier.clip(RoundedCornerShape(36.dp)).then(glass).padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = tab.canGoBack) { Icon(Icons.Filled.ArrowBack, "后退") }
            IconButton(onClick = onForward, enabled = tab.canGoForward) { Icon(Icons.Filled.ArrowForward, "前进") }
            if (editing) {
                TextField(value = text, onValueChange = { text = it }, singleLine = true, placeholder = { Text("搜索或输入网址") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onSubmit(text); editing = false }),
                    colors = TextFieldDefaults.colors(focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent, unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent, focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent, unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent),
                    modifier = Modifier.weight(1f))
            } else {
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(tab.title.ifBlank { "新标签页" }, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(if (tab.url == "about:blank") "点此搜索或输入网址" else tab.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = { text = if (tab.url == "about:blank") "" else tab.url; editing = true }) { Icon(Icons.Outlined.Search, "搜索") }
            }
            IconButton(onClick = onReloadOrStop) { Icon(if (isLoading) Icons.Filled.Close else Icons.Filled.Refresh, if (isLoading) "停止" else "刷新") }
            BadgedBox(badge = { Badge { Text("$tabCount") } }) { IconButton(onClick = onTabs) { Icon(Icons.Outlined.Tab, "标签页") } }
        }
    }
}
