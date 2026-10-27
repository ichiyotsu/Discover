@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalLayoutApi::class,
)

package com.luma.browser.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.luma.browser.browser.BrowserController
import com.luma.browser.browser.BrowserTab
import com.luma.browser.browser.PageError
import com.luma.browser.data.HistoryEntry
import com.luma.browser.data.LumaSettings
import com.luma.browser.data.SearchEngine
import com.luma.browser.data.ToolbarEdge
import kotlinx.coroutines.delay

@Composable
fun LumaBrowserScreen(
    controller: BrowserController,
    settings: LumaSettings,
    sheetRequest: String?,
    onSheetRequestHandled: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    var showSettings by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var addressEditing by remember { mutableStateOf(false) }
    var addressQuery by remember { mutableStateOf("") }

    val active = controller.activeTab
    val isTop = settings.edge == ToolbarEdge.TOP
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val chromeHeight = 64.dp
    val gap = 8.dp
    val pageGap = 6.dp

    val targetTop = if (isTop) topInset + gap + chromeHeight + pageGap else topInset + gap
    val targetBottom = if (isTop) bottomInset + gap else bottomInset + gap + chromeHeight + pageGap
    val pageTop by animateDpAsState(
        targetValue = targetTop,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 380f),
        label = "pageTop",
    )
    val pageBottom by animateDpAsState(
        targetValue = targetBottom,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 380f),
        label = "pageBottom",
    )

    LaunchedEffect(Unit) {
        delay(280)
        focusManager.clearFocus(force = true)
        keyboard?.hide()
    }

    LaunchedEffect(sheetRequest) {
        val request = sheetRequest ?: return@LaunchedEffect
        when {
            request.startsWith("settings") -> showSettings = true
            request.startsWith("history") -> showHistory = true
            request.startsWith("newtab") -> controller.showNewTabPage()
        }
        onSheetRequestHandled()
    }

    LaunchedEffect(controller.activeTabId) {
        if (!addressEditing) {
            addressQuery = ""
        }
    }

    fun leaveAddressMode() {
        addressEditing = false
        addressQuery = ""
        focusManager.clearFocus()
        keyboard?.hide()
    }

    fun navigateFromAddress(value: String) {
        val input = value.trim()
        if (input.isEmpty()) return
        leaveAddressMode()
        controller.navigate(input)
    }

    BackHandler(enabled = !showSettings && !showTabs && !showHistory) {
        when {
            addressEditing -> leaveAddressMode()
            active?.webView?.canGoBack() == true -> controller.goBack()
            active?.isNewTab == false -> controller.showNewTabPage()
            else -> context.findActivity()?.finish()
        }
    }

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        LumaAmbientBackground(Modifier.fillMaxSize())

        PageSurface(
            controller = controller,
            active = active,
            settings = settings,
            onFocusAddress = { addressEditing = true },
            onOpenUrl = { url ->
                leaveAddressMode()
                controller.navigate(url)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp, top = pageTop, bottom = pageBottom)
                .zIndex(1f),
        )

        BrowserChrome(
            controller = controller,
            active = active,
            settings = settings,
            editing = addressEditing,
            onOpenTabs = {
                leaveAddressMode()
                showTabs = true
            },
            onOpenSettings = {
                leaveAddressMode()
                showSettings = true
            },
            onOpenHistory = {
                leaveAddressMode()
                showHistory = true
            },
            onEditingChanged = { editing ->
                addressEditing = editing
                if (!editing) addressQuery = ""
            },
            onQueryChanged = { addressQuery = it },
            onSubmit = ::navigateFromAddress,
            modifier = Modifier
                .align(if (isTop) Alignment.TopCenter else Alignment.BottomCenter)
                .then(
                    if (isTop) Modifier.statusBarsPadding() else Modifier.navigationBarsPadding(),
                )
                .padding(horizontal = 12.dp)
                .padding(
                    top = if (isTop) gap else 0.dp,
                    bottom = if (isTop) 0.dp else gap,
                )
                .fillMaxWidth()
                .zIndex(10f),
        )

        if (addressEditing && (addressQuery.isNotBlank() || controller.history.suggestions("").isNotEmpty())) {
            AddressSuggestions(
                query = addressQuery,
                engine = settings.engine,
                history = controller.history.suggestions(addressQuery, 4),
                onSearch = { navigateFromAddress(it) },
                onOpen = { url ->
                    leaveAddressMode()
                    controller.navigate(url)
                },
                modifier = Modifier
                    .align(if (isTop) Alignment.TopCenter else Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = if (isTop) topInset + gap + chromeHeight + pageGap else 0.dp,
                        bottom = if (isTop) 0.dp else bottomInset + gap + chromeHeight + pageGap,
                    )
                    .fillMaxWidth()
                    .zIndex(9f),
            )
        }
    }

    if (showSettings) {
        SettingsSheet(
            settings = settings,
            history = controller.history,
            onDismiss = { showSettings = false },
        )
    }

    if (showTabs) {
        TabsSheet(
            controller = controller,
            active = active,
            onDismiss = { showTabs = false },
            onNewTab = {
                showTabs = false
                controller.newTab()
            },
        )
    }

    if (showHistory) {
        HistorySheet(
            history = controller.history,
            onDismiss = { showHistory = false },
            onOpen = { url ->
                showHistory = false
                controller.navigate(url)
            },
        )
    }
}

@Composable
private fun PageSurface(
    controller: BrowserController,
    active: BrowserTab?,
    settings: LumaSettings,
    onFocusAddress: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val shape = RoundedCornerShape(26.dp)
    val pageColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val frame = remember(pageColor) {
        FrameLayout(context).apply {
            clipChildren = true
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    val radius = with(density) { 26.dp.toPx() }
                    outline.setRoundRect(0, 0, view.width, view.height, radius)
                }
            }
            background = GradientDrawable().apply {
                cornerRadius = with(density) { 26.dp.toPx() }
                setColor(pageColor.toArgb())
            }
        }
    }

    DisposableEffect(frame) {
        controller.attachContainer(frame)
        onDispose { controller.detachContainer(frame) }
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 22.dp,
                shape = shape,
                clip = false,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                shape,
            ),
    ) {
        AndroidView(
            factory = { frame },
            modifier = Modifier.fillMaxSize(),
        )

        if (active?.isNewTab == true) {
            NewTabPage(
                settings = settings,
                onOpen = onOpenUrl,
                onFocusAddress = onFocusAddress,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (active?.error != null) {
            ErrorPage(
                error = active.error!!,
                onRetry = { controller.retry() },
                onBack = { controller.goBack() },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (active?.isLoading == true && active.isNewTab.not()) {
            LinearProgressIndicator(
                progress = { active.progress.coerceIn(0.04f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.TopCenter)
                    .zIndex(4f),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            )
        }
    }
}

@Composable
private fun ErrorPage(
    error: PageError,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (error.code) {
        -2 -> "找不到这个网站"
        -6 -> "无法连接到网站"
        -8, -7 -> "连接超时"
        -201 -> "证书不受信任"
        else -> "无法访问此网站"
    }
    val detail = when (error.code) {
        -2 -> "请检查网址是否拼写正确，或换一个网络再试。"
        -6 -> "目标网站拒绝了连接，或网络暂时不可用。"
        -8, -7 -> "网站响应时间过长。请稍后重试。"
        -201 -> "这个网站的安全证书不受信任，Luma 已停止加载。"
        else -> error.description.ifBlank { "页面暂时无法加载，请稍后重试。" }
    }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(74.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "连接没有建立",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = error.url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onRetry) { Text("重新加载") }
                TextButton(onClick = onBack) { Text("返回新标签页") }
            }
        }
    }
}

@Composable
private fun AddressSuggestions(
    query: String,
    engine: SearchEngine,
    history: List<HistoryEntry>,
    onSearch: (String) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (query.isBlank() && history.isEmpty()) return
    Surface(
        modifier = modifier.heightIn(max = 360.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        shadowElevation = 12.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            if (query.isNotBlank()) {
                SuggestionRow(
                    icon = Icons.Rounded.Search,
                    title = query,
                    subtitle = "使用 ${engine.label} 搜索",
                    onClick = { onSearch(engine.searchUrl(query)) },
                )
            }
            history.forEachIndexed { index, entry ->
                if (query.isNotBlank() || index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
                SuggestionRow(
                    icon = Icons.Rounded.History,
                    title = entry.title.ifBlank { entry.url },
                    subtitle = entry.url,
                    onClick = { onOpen(entry.url) },
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(34.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
