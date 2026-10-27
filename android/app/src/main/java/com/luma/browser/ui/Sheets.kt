@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.luma.browser.ui

import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luma.browser.BuildConfig
import com.luma.browser.browser.BrowserController
import com.luma.browser.browser.BrowserTab
import com.luma.browser.data.HistoryEntry
import com.luma.browser.data.HistoryStore
import com.luma.browser.data.LumaSettings
import com.luma.browser.data.SearchEngine
import com.luma.browser.data.ThemeMode
import com.luma.browser.data.ToolbarEdge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsSheet(
    settings: LumaSettings,
    history: HistoryStore,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val webViewVersion = remember {
        runCatching { WebView.getCurrentWebViewPackage()?.versionName }.getOrNull()
            ?: "系统 WebView"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
        ) {
            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "把 Luma 调成你顺手的样子。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SettingsSection(
                title = "工具栏位置",
                description = "地址栏和工具栏会一起停靠，网页内容自动让出空间。",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PositionOption(
                        label = "顶部",
                        selected = settings.edge == ToolbarEdge.TOP,
                        top = true,
                        modifier = Modifier.weight(1f),
                        onClick = { settings.updateEdge(ToolbarEdge.TOP) },
                    )
                    PositionOption(
                        label = "底部",
                        selected = settings.edge == ToolbarEdge.BOTTOM,
                        top = false,
                        modifier = Modifier.weight(1f),
                        onClick = { settings.updateEdge(ToolbarEdge.BOTTOM) },
                    )
                }
            }

            SettingsSection(
                title = "外观",
                description = "明暗模式会立即应用，并同步系统栏图标。",
            ) {
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
                        ) {
                            Text(option.second, maxLines = 1)
                        }
                    }
                }
            }

            SettingsSection(
                title = "搜索引擎",
                description = "地址栏和新标签页共享同一个搜索来源。",
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SearchEngine.entries.forEach { engine ->
                        FilterChip(
                            selected = settings.engine == engine,
                            onClick = { settings.updateEngine(engine) },
                            label = { Text(engine.label) },
                            leadingIcon = if (settings.engine == engine) {
                                {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }

            SettingsSection(
                title = "本地数据",
                description = "历史记录只保存在这台设备上，用于地址栏补全。",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${history.entries.size} 条历史记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "不会上传到任何服务器",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = {
                            history.clear()
                            Toast.makeText(context, "历史记录已清空", Toast.LENGTH_SHORT).show()
                        },
                        enabled = history.entries.isNotEmpty(),
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("清空")
                    }
                }
            }

            SettingsSection(title = "关于 Luma", description = null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Luma ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "浏览器内核  Chromium WebView $webViewVersion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "界面  Jetpack Compose · Material 3 Expressive",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Luma 是独立项目，与 Google 无关。网页由 Android System WebView 渲染。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            FilledTonalButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("完成")
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String?,
    content: @Composable () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )
    if (description != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 17.sp,
        )
    }
    Spacer(Modifier.height(12.dp))
    content()
}

@Composable
private fun PositionOption(
    label: String,
    selected: Boolean,
    top: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(modifier = Modifier.padding(9.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.78f)
                        .fillMaxHeight(0.58f)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                )
                Box(
                    modifier = Modifier
                        .align(if (top) Alignment.TopCenter else Alignment.BottomCenter)
                        .fillMaxWidth(0.88f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                        ),
                )
            }
            Spacer(Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun TabsSheet(
    controller: BrowserController,
    active: BrowserTab?,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "标签页",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${controller.tabs.size} 个",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(controller.tabs, key = { it.id }) { tab ->
                    TabItem(
                        tab = tab,
                        selectedTab = tab.id == active?.id,
                        onSelect = {
                            controller.selectTab(tab.id)
                            onDismiss()
                        },
                        onClose = { controller.closeTab(tab.id) },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onNewTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("新建标签页")
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun TabItem(
    tab: BrowserTab,
    selectedTab: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(18.dp),
        color = if (selectedTab) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            1.dp,
            if (selectedTab) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val favicon = tab.favicon
                if (favicon != null) {
                    Image(
                        bitmap = favicon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                    )
                } else {
                    Icon(
                        imageVector = if (tab.isNewTab) Icons.Rounded.Add else Icons.Rounded.Tab,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tab.title.ifBlank { "新标签页" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (tab.isNewTab) "新标签页" else tab.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "关闭标签页",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun HistorySheet(
    history: HistoryStore,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "历史记录",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { history.clear() },
                    enabled = history.entries.isNotEmpty(),
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("清空")
                }
            }
            Spacer(Modifier.height(8.dp))
            if (history.entries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "还没有浏览记录",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "你访问过的网页会出现在这里。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(history.entries, key = { it.url + it.visitedAt }) { entry ->
                        HistoryItem(entry = entry, onClick = { onOpen(entry.url) })
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun HistoryItem(entry: HistoryEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = entry.title.take(1).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = SimpleDateFormat("M月d日 HH:mm", Locale.SIMPLIFIED_CHINESE)
                .format(Date(entry.visitedAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        )
    }
}
