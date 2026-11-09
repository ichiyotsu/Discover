package dev.ichiyotsu.tokidiscover.ui

import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ichiyotsu.tokidiscover.browser.BrowserViewModel
import dev.ichiyotsu.tokidiscover.browser.SearchEngines
import dev.ichiyotsu.tokidiscover.browser.TabState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowserScreen(model: BrowserViewModel) {
    val tab = model.activeTab ?: return
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val density = LocalDensity.current
    val context = LocalContext.current
    var chromePx by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var addressFocused by remember { mutableStateOf(false) }
    var draft by remember(tab.id) { mutableStateOf(if (tab.isHome) "" else tab.url) }
    val translating by model.translating.collectAsStateWithLifecycle()
    val statusTop = WindowInsets.statusBars
    val expanded = model.chromeExpanded || addressFocused || tab.isHome

    DisposableEffect(context) {
        model.activityRef = java.lang.ref.WeakReference(context as? android.app.Activity)
        onDispose { }
    }
    LaunchedEffect(tab.url, tab.isHome, addressFocused) {
        if (!addressFocused) draft = if (tab.isHome) "" else tab.url
    }
    LaunchedEffect(expanded) {
        if (!expanded) focus.clearFocus()
    }
    LaunchedEffect(model.message) {
        val text = model.message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        model.message = null
    }
    BackHandler(enabled = !tab.isHome && (tab.canGoBack || tab.webView.canGoBack())) {
        model.back()
    }

    val chromeDp = with(density) { chromePx.toDp() }
    val topInset = WindowInsets.statusBars

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1f)
                .onSizeChanged { chromePx = it.height }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(bottom = 10.dp),
        ) {
            FloatingChrome(
                model = model,
                tab = tab,
                expanded = expanded,
                draft = draft,
                onDraft = { draft = it },
                onFocus = { addressFocused = it },
                menu = menu,
                onMenu = { menu = it },
                onOpenSheet = { sheet = true },
                onTranslate = { scope.launch { model.translateActive() } },
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = chromeDp)
                .windowInsetsPadding(topInset),
        ) {
            if (tab.loading || translating) {
                LinearWavyProgressIndicator(
                    progress = { if (translating) 0.35f else (tab.progress / 100f).coerceIn(0.05f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
            }
            Box(Modifier.fillMaxSize()) {
                if (tab.isHome) {
                    HomePane(
                        engineId = model.engineId,
                        onEngine = model::chooseEngine,
                        onSubmit = model::navigateActive,
                    )
                }
                WebSlot(
                    tab = tab,
                    visible = !tab.isHome,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = chromeDp + 8.dp)
                .zIndex(2f),
        )
    }

    if (sheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { sheet = false },
            sheetState = sheetState,
        ) {
            TabSheet(model = model, onDismiss = { sheet = false })
        }
    }
}

@Composable
private fun FloatingChrome(
    model: BrowserViewModel,
    tab: TabState,
    expanded: Boolean,
    draft: String,
    onDraft: (String) -> Unit,
    onFocus: (Boolean) -> Unit,
    menu: Boolean,
    onMenu: (Boolean) -> Unit,
    onOpenSheet: () -> Unit,
    onTranslate: () -> Unit,
) {
    val maxField = 280.dp
    HorizontalFloatingToolbar(
        expanded = expanded,
        floatingActionButton = {
            FloatingToolbarDefaults.VibrantFloatingActionButton(
                onClick = { model.chromeExpanded = true },
            ) {
                Icon(
                    imageVector = if (tab.isPrivate) Icons.Filled.Lock else Icons.Filled.Search,
                    contentDescription = Copy.expand,
                )
            }
        },
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .widthIn(max = 640.dp),
        colors = if (tab.isPrivate) {
            FloatingToolbarDefaults.standardFloatingToolbarColors()
        } else {
            FloatingToolbarDefaults.vibrantFloatingToolbarColors()
        },
        content = {
            NavIcon(Icons.AutoMirrored.Filled.ArrowBack, Copy.back, expanded && tab.canGoBack, model::back)
            NavIcon(Icons.AutoMirrored.Filled.ArrowForward, Copy.forward, expanded && tab.canGoForward, model::forward)
            AddressField(
                value = draft,
                placeholder = Copy.searchHint,
                privateMode = tab.isPrivate,
                onValue = onDraft,
                onFocus = onFocus,
                onSubmit = { model.navigateActive(draft) },
                modifier = Modifier
                    .widthIn(min = 120.dp, max = maxField)
                    .focusProperties { canFocus = expanded },
            )
            NavIcon(Icons.Filled.Refresh, Copy.reload, expanded && !tab.isHome, model::reload)
            Box {
                NavIcon(Icons.Filled.MoreVert, Copy.menu, expanded) { onMenu(true) }
                DropdownMenu(expanded = menu, onDismissRequest = { onMenu(false) }) {
                    DropdownMenuItem(
                        text = { Text(Copy.home) },
                        leadingIcon = { Icon(Icons.Filled.Home, null) },
                        onClick = { onMenu(false); model.showHome() },
                    )
                    DropdownMenuItem(
                        text = { Text(Copy.translate) },
                        onClick = { onMenu(false); onTranslate() },
                    )
                    DropdownMenuItem(
                        text = { Text(Copy.translateOnline) },
                        onClick = { onMenu(false); model.translateOnline() },
                    )
                    DropdownMenuItem(
                        text = { Text(Copy.newTab) },
                        leadingIcon = { Icon(Icons.Filled.Add, null) },
                        onClick = { onMenu(false); model.newTab(false) },
                    )
                    DropdownMenuItem(
                        text = { Text(Copy.privateTab) },
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        onClick = { onMenu(false); model.newTab(true) },
                    )
                    DropdownMenuItem(
                        text = { Text("${Copy.tabs} (${model.tabs.size})") },
                        onClick = { onMenu(false); onOpenSheet() },
                    )
                }
            }
        },
    )
}

@Composable
private fun NavIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.focusProperties { canFocus = enabled },
    ) {
        Icon(icon, contentDescription = label)
    }
}

@Composable
private fun AddressField(
    value: String,
    placeholder: String,
    privateMode: Boolean,
    onValue: (String) -> Unit,
    onFocus: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.height(40.dp),
        shape = CircleShape,
        color = if (privateMode) colors.tertiaryContainer else colors.surfaceContainerHighest,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocus(it.isFocused) },
            )
        }
    }
}

@Composable
private fun WebSlot(tab: TabState, visible: Boolean, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { frame ->
            val web = tab.webView
            if (web.parent !== frame) {
                (web.parent as? ViewGroup)?.removeView(web)
                frame.removeAllViews()
                frame.addView(
                    web,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
            frame.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
            web.visibility = frame.visibility
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomePane(
    engineId: String,
    onEngine: (String) -> Unit,
    onSubmit: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val motion = MaterialTheme.motionScheme
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "TokiDiscover",
            style = MaterialTheme.typography.displaySmallEmphasized,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            Copy.tagline,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 10.dp),
                )
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit(query) }),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    Copy.searchHint,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            Copy.searchEngine,
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchEngines.all.forEach { engine ->
                ToggleButton(
                    checked = engine.id == engineId,
                    onCheckedChange = { onEngine(engine.id) },
                ) {
                    Text(engine.label)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "0.1.0",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(motion.let { 8.dp }))
    }
}

@Composable
private fun TabSheet(model: BrowserViewModel, onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(Copy.tabs, style = MaterialTheme.typography.titleLargeEmphasized)
            Row {
                IconButton(onClick = { model.newTab(false); onDismiss() }) {
                    Icon(Icons.Filled.Add, contentDescription = Copy.newTab)
                }
                IconButton(onClick = { model.newTab(true); onDismiss() }) {
                    Icon(Icons.Filled.Lock, contentDescription = Copy.privateTab)
                }
            }
        }
        Text(
            Copy.searchEngine,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRowEngines(model.engineId, model::chooseEngine)
        Text(
            Copy.translateLanguage,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LanguageRow(model.translateTarget, model::chooseTranslateTarget)
        model.tabs.forEach { item ->
            ListItem(
                headlineContent = {
                    Text(item.title.ifBlank { "TokiDiscover" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    val host = if (item.isHome) Copy.home else item.url
                    Text(
                        (if (item.isPrivate) "${Copy.privateLabel} · " else "") + host,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingContent = {
                    Icon(
                        if (item.isPrivate) Icons.Filled.Lock else Icons.Filled.Search,
                        contentDescription = null,
                    )
                },
                trailingContent = {
                    IconButton(onClick = { model.close(item.id) }) {
                        Icon(Icons.Filled.Close, contentDescription = Copy.close)
                    }
                },
                modifier = Modifier.clickable {
                    model.select(item.id)
                    onDismiss()
                },
                colors = androidx.compose.material3.ListItemDefaults.colors(
                    containerColor = if (item.id == model.activeId) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowEngines(selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SearchEngines.all.forEach { engine ->
            ToggleButton(checked = engine.id == selected, onCheckedChange = { onSelect(engine.id) }) {
                Text(engine.label)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageRow(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        "" to Copy.followSystem,
        "zh" to "中文",
        "en" to "English",
        "ja" to "日本語",
        "ko" to "한국어",
    )
    FlowRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (tag, label) ->
            ToggleButton(checked = selected == tag, onCheckedChange = { onSelect(tag) }) {
                Text(label)
            }
        }
    }
}

@Suppress("unused")
private val fade = fadeIn() to fadeOut()
