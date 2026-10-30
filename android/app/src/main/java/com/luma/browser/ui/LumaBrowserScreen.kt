@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalLayoutApi::class,
)

package com.luma.browser.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import com.luma.browser.R
import com.luma.browser.browser.BrowserController
import com.luma.browser.browser.BrowserTab
import com.luma.browser.browser.PageError
import com.luma.browser.data.HistoryEntry
import com.luma.browser.data.LumaSettings
import com.luma.browser.data.ToolbarEdge
import com.luma.browser.data.UpdateInfo
import com.luma.browser.data.UpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

private enum class ChromeMode { EXPANDED, PILL, CORNER_LEFT, CORNER_RIGHT }

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
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager() }

    var showSettings by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var addressEditing by remember { mutableStateOf(false) }
    var addressQuery by remember { mutableStateOf("") }
    var cornerMode by remember { mutableStateOf<ChromeMode?>(null) }

    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var downloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    val active = controller.activeTab
    val isTop = settings.edge == ToolbarEdge.TOP
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val chromeHeight = 64.dp
    val scrollCollapsed = !isTop &&
        settings.autoCollapseToolbar &&
        controller.toolbarCollapsed &&
        !addressEditing
    val chromeMode = when {
        cornerMode != null -> cornerMode!!
        scrollCollapsed -> ChromeMode.PILL
        else -> ChromeMode.EXPANDED
    }

    fun checkForUpdates(manual: Boolean) {
        if (checkingUpdate || downloadingUpdate) return
        checkingUpdate = true
        updateMessage = null
        scope.launch {
            val result = runCatching { updateManager.check(settings.updateChannel) }.getOrNull()
            checkingUpdate = false
            updateInfo = result
            if (result != null) {
                updateMessage = "发现新版本 ${result.version}"
                if (!manual) showUpdateDialog = true
            } else if (manual) {
                updateMessage = "当前已是最新版本"
            }
        }
    }

    fun installUpdate() {
        val info = updateInfo ?: return
        val existing = downloadedFile
        if (existing != null && existing.exists()) {
            launchInstaller(context, existing)
            return
        }
        if (downloadingUpdate) return
        downloadingUpdate = true
        downloadProgress = 0f
        updateMessage = "正在下载 ${info.version}…"
        scope.launch {
            try {
                val directory = File(context.cacheDir, "updates").apply { mkdirs() }
                val file = File(directory, "Luna-${info.version}.apk")
                updateManager.download(info, file) { downloadProgress = it }
                downloadedFile = file
                downloadingUpdate = false
                updateMessage = "下载完成，正在打开安装器"
                launchInstaller(context, file)
            } catch (error: Exception) {
                downloadingUpdate = false
                updateMessage = "下载失败：${error.message ?: "网络错误"}"
            }
        }
    }

    LaunchedEffect(Unit) {
        if (settings.autoCheckUpdates) checkForUpdates(false)
    }

    LaunchedEffect(isTop, topInset, bottomInset) {
        val density = context.resources.displayMetrics.density
        val overlay = ((64 + 16) * density).toInt()
        controller.setContentInsets(
            top = (topInset.value * density).toInt() + if (isTop) overlay else 0,
            bottom = if (isTop) 0 else overlay,
        )
    }

    LaunchedEffect(addressEditing) {
        if (addressEditing) {
            cornerMode = null
            controller.expandToolbar()
        }
    }

    LaunchedEffect(controller.activeTabId) {
        cornerMode = null
    }

    LaunchedEffect(Unit) {
        delay(320)
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

    fun leaveAddressMode() {
        addressEditing = false
        addressQuery = ""
        focusManager.clearFocus(force = true)
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
            cornerMode != null -> cornerMode = null
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
            isTop = isTop,
            topInset = topInset,
            onFocusAddress = {
                controller.expandToolbar()
                addressEditing = true
            },
            onOpenUrl = { url ->
                leaveAddressMode()
                controller.navigate(url)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomInset)
                .zIndex(1f),
        )

        val swipeModifier = if (!isTop && !addressEditing) {
            Modifier.pointerInput(isTop, addressEditing) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    var dx = 0f
                    var dy = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val delta = change.position - change.previousPosition
                        dx += delta.x
                        dy += delta.y
                        if (abs(dx) > 56.dp.toPx() && abs(dx) > abs(dy) * 1.2f) {
                            cornerMode = if (dx < 0) ChromeMode.CORNER_LEFT else ChromeMode.CORNER_RIGHT
                            event.changes.forEach { it.consume() }
                            break
                        }
                        if (abs(dx) > viewConfiguration.touchSlop && abs(dx) > abs(dy)) {
                            change.consume()
                        }
                    }
                }
            }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .align(if (isTop) Alignment.TopCenter else Alignment.BottomCenter)
                .then(if (isTop) Modifier.statusBarsPadding() else Modifier.navigationBarsPadding())
                .padding(horizontal = 12.dp)
                .padding(
                    top = if (isTop) 8.dp else 0.dp,
                    bottom = if (isTop) 0.dp else 8.dp,
                )
                .fillMaxWidth()
                .zIndex(10f)
                .then(swipeModifier),
        ) {
            AnimatedContent(
                targetState = chromeMode,
                transitionSpec = {
                    (
                        fadeIn(tween(180)) +
                            slideInVertically(tween(220)) { if (isTop) -it / 2 else it / 2 }
                        ).togetherWith(
                        fadeOut(tween(120)) +
                            slideOutVertically(tween(160)) { if (isTop) -it / 2 else it / 2 },
                    )
                },
                label = "browserChrome",
            ) { mode ->
                when (mode) {
                    ChromeMode.CORNER_LEFT -> Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        CornerExpandButton(
                            onClick = {
                                cornerMode = null
                                controller.expandToolbar()
                            },
                        )
                    }

                    ChromeMode.CORNER_RIGHT -> Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        CornerExpandButton(
                            onClick = {
                                cornerMode = null
                                controller.expandToolbar()
                            },
                        )
                    }

                    ChromeMode.PILL -> Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CollapsedChrome(
                            active = active,
                            onExpand = { controller.expandToolbar() },
                            modifier = Modifier.fillMaxWidth(0.76f),
                        )
                    }

                    ChromeMode.EXPANDED -> BrowserChrome(
                        controller = controller,
                        active = active,
                        settings = settings,
                        editing = addressEditing,
                        onOpenTabs = {
                            leaveAddressMode()
                            cornerMode = null
                            controller.expandToolbar()
                            showTabs = true
                        },
                        onOpenSettings = {
                            leaveAddressMode()
                            cornerMode = null
                            controller.expandToolbar()
                            showSettings = true
                        },
                        onOpenHistory = {
                            leaveAddressMode()
                            cornerMode = null
                            controller.expandToolbar()
                            showHistory = true
                        },
                        onTranslate = { controller.translateSelectionOrPage() },
                        onEditingChanged = { editing ->
                            addressEditing = editing
                            if (!editing) addressQuery = ""
                        },
                        onQueryChanged = { addressQuery = it },
                        onSubmit = ::navigateFromAddress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (addressEditing && (addressQuery.isNotBlank() || controller.history.suggestions("").isNotEmpty())) {
            AddressSuggestions(
                query = addressQuery,
                settings = settings,
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
                        top = if (isTop) topInset + chromeHeight + 12.dp else 0.dp,
                        bottom = if (isTop) 0.dp else bottomInset + chromeHeight + 12.dp,
                    )
                    .fillMaxWidth()
                    .zIndex(9f),
            )
        }
    }

    if (showSettings) {
        SettingsScreen(
            settings = settings,
            history = controller.history,
            updateInfo = updateInfo,
            checkingUpdate = checkingUpdate,
            downloadingUpdate = downloadingUpdate,
            downloadProgress = downloadProgress,
            updateMessage = updateMessage,
            onCheckUpdate = { checkForUpdates(true) },
            onInstallUpdate = { installUpdate() },
            onOpenReleasePage = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/ichiyotsu/Discover/releases"),
                        ),
                    )
                }
            },
            onOpenHistory = {
                showSettings = false
                showHistory = true
            },
            onClose = { showSettings = false },
        )
    }

    if (showTabs) {
        TabSwitcherScreen(
            controller = controller,
            settings = settings,
            onClose = { showTabs = false },
            onNewTab = {
                showTabs = false
                controller.newTab()
            },
        )
    }

    if (showHistory) {
        HistoryScreen(
            history = controller.history,
            onClose = { showHistory = false },
            onOpenUrl = { url -> controller.navigate(url) },
        )
    }

    StatusBarFrost(
        modifier = Modifier
            .fillMaxWidth()
            .height(topInset)
            .zIndex(100f),
    )

    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            onDismissRequest = { if (!downloadingUpdate) showUpdateDialog = false },
            title = { Text("Luna ${info.version} 可用") },
            text = {
                Column {
                    Text(
                        text = info.notes.take(500).ifBlank { "新版本已发布。" },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (downloadingUpdate) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { installUpdate() },
                    enabled = !downloadingUpdate,
                ) {
                    Text(if (downloadingUpdate) "下载中…" else "下载并安装")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUpdateDialog = false },
                    enabled = !downloadingUpdate,
                ) { Text("稍后") }
            },
        )
    }
}

@Composable
private fun CornerExpandButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
        shadowElevation = 12.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_luna_mark),
                contentDescription = "展开底栏",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(7.dp)
                    .size(12.dp),
            )
        }
    }
}

@Composable
private fun PageSurface(
    controller: BrowserController,
    active: BrowserTab?,
    settings: LumaSettings,
    isTop: Boolean,
    topInset: Dp,
    onFocusAddress: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pageColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val frame = remember(pageColor) {
        FrameLayout(context).apply {
            setBackgroundColor(pageColor.toArgb())
        }
    }

    DisposableEffect(frame) {
        controller.attachContainer(frame)
        onDispose { controller.detachContainer(frame) }
    }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
        AndroidView(
            factory = { frame },
            modifier = Modifier.fillMaxSize(),
        )

        if (active?.isNewTab == true) {
            NewTabPage(
                settings = settings,
                onOpen = onOpenUrl,
                onFocusAddress = onFocusAddress,
                topContentPadding = topInset + if (isTop) 78.dp else 0.dp,
                bottomContentPadding = if (isTop) 0.dp else 78.dp,
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
                    .height(3.dp)
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
        -201 -> "这个网站的安全证书不受信任，Luna 已停止加载。"
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
    settings: LumaSettings,
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
                    subtitle = "使用 ${settings.searchProvider.label} 搜索",
                    onClick = { onSearch(settings.searchUrl(query)) },
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

private fun launchInstaller(context: Context, file: File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !context.packageManager.canRequestPackageInstalls()
    ) {
        Toast.makeText(context, "请允许 Luna 安装未知应用后重试", Toast.LENGTH_LONG).show()
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
        }
        return
    }
    runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "无法打开安装器：${it.message}", Toast.LENGTH_LONG).show()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
