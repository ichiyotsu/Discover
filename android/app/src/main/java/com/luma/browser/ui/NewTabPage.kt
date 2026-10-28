@file:OptIn(ExperimentalLayoutApi::class)

package com.luma.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luma.browser.data.LumaSettings
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class Shortcut(
    val name: String,
    val host: String,
    val url: String,
    val monogram: String,
    val color: Color,
)

private val shortcuts = listOf(
    Shortcut("GitHub", "github.com", "https://github.com", "GH", Color(0xFF24292F)),
    Shortcut("哔哩哔哩", "bilibili.com", "https://www.bilibili.com", "B", Color(0xFFFB7299)),
    Shortcut("YouTube", "youtube.com", "https://www.youtube.com", "YT", Color(0xFFFF0033)),
    Shortcut("知乎", "zhihu.com", "https://www.zhihu.com", "知", Color(0xFF0066FF)),
    Shortcut("维基百科", "wikipedia.org", "https://zh.wikipedia.org", "W", Color(0xFF54595D)),
    Shortcut("少数派", "sspai.com", "https://sspai.com", "少", Color(0xFFD71A1B)),
)

@Composable
fun NewTabPage(
    settings: LumaSettings,
    onOpen: (String) -> Unit,
    onFocusAddress: () -> Unit,
    topContentPadding: Dp = 0.dp,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(15_000)
        }
    }

    val greeting = when (now.hour) {
        in 5..10 -> "早上好"
        in 11..12 -> "中午好"
        in 13..17 -> "下午好"
        in 18..22 -> "晚上好"
        else -> "夜深了"
    }
    val time = remember(now) { now.format(DateTimeFormatter.ofPattern("HH:mm")) }
    val date = remember(now) {
        now.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE))
    }
    val scheme = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        scheme.primaryContainer.copy(alpha = 0.86f),
                        scheme.surface,
                        scheme.tertiaryContainer.copy(alpha = 0.48f),
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 36.dp, end = 0.dp)
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 0.dp)
                .size(260.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            scheme.tertiary.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        val compact = maxWidth < 390.dp
        val columns = if (maxWidth > 620.dp) 3 else 2
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = if (compact) 20.dp else 26.dp,
                    end = if (compact) 20.dp else 26.dp,
                    top = 22.dp + topContentPadding,
                    bottom = 22.dp + bottomContentPadding,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = scheme.primary,
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "Luma",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = scheme.surfaceContainerLowest.copy(alpha = 0.48f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        scheme.outlineVariant.copy(alpha = 0.38f),
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(scheme.primary, CircleShape),
                        )
                        Text(
                            text = "本地新标签页",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(if (compact) 26.dp else 36.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(scheme.primary, CircleShape),
                )
                Text(
                    text = "$greeting · $date",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "$greeting。",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = if (compact) 36.sp else 44.sp,
                    lineHeight = if (compact) 40.sp else 48.sp,
                ),
                color = scheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = "今天想去哪里？",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = if (compact) 36.sp else 44.sp,
                    lineHeight = if (compact) 40.sp else 48.sp,
                ),
                color = scheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )

            Spacer(Modifier.height(26.dp))

            Surface(
                onClick = onFocusAddress,
                shape = RoundedCornerShape(28.dp),
                color = scheme.surfaceContainerLowest.copy(alpha = 0.78f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    scheme.outlineVariant.copy(alpha = 0.42f),
                ),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp),
                    )
                    Text(
                        text = "搜索网页，或输入网址",
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = scheme.primary.copy(alpha = 0.11f),
                    ) {
                        Text(
                            text = settings.searchProvider.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.primary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = scheme.primary,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = scheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Material 3 Expressive", "今日科技", "玻璃拟态 CSS").forEach { idea ->
                    Surface(
                        onClick = { onOpen(settings.searchUrl(idea)) },
                        shape = RoundedCornerShape(999.dp),
                        color = scheme.surfaceContainerLowest.copy(alpha = 0.40f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            scheme.outlineVariant.copy(alpha = 0.38f),
                        ),
                    ) {
                        Text(
                            text = idea,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "常去的地方",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "快捷方式",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))

            shortcuts.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEach { shortcut ->
                        ShortcutTile(
                            shortcut = shortcut,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpen(shortcut.url) },
                        )
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "Luma · Material 3 Expressive · Chromium WebView",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
    }
}

@Composable
private fun ShortcutTile(
    shortcut: Shortcut,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.46f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(shortcut.color, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = shortcut.monogram,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shortcut.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = shortcut.host,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
