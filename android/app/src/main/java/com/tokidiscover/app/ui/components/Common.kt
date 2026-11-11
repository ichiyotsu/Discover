package com.tokidiscover.app.ui.components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tokidiscover.app.ui.theme.ExpressiveCardShape
import com.tokidiscover.app.ui.theme.ExpressiveChipShape
/** PillGroup segmented control (Tokinote): used by Library (书签/历史/下载) and Settings pickers. */
@Composable fun PillGroup(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(ExpressiveChipShape).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Surface(shape = ExpressiveChipShape, color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.height(48.dp).clickable { onSelect(i) }) {
                Box(Modifier.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) } } } }
}
@Composable fun ExpressiveCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = ExpressiveCardShape, color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier) { Column(Modifier.padding(16.dp), content = content) }
}
@Composable fun EmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis) }
}
