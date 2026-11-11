package com.tokidiscover.app.ui
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tokidiscover.app.TokiDiscoverApplication
import com.tokidiscover.app.data.UrlNormalizer
import com.tokidiscover.app.ui.browser.*
import com.tokidiscover.app.ui.components.FloatingNavBar
import com.tokidiscover.app.ui.components.FloatingOmnibox
import com.tokidiscover.app.ui.theme.TokiDiscoverTheme
import com.tokidiscover.app.ui.theme.ThemeMode
import com.tokidiscover.app.ui.theme.tokiNavGlass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
@Composable fun TokiDiscoverApp() {
    val context = LocalContext.current
    val app = context.applicationContext as TokiDiscoverApplication
    val vm: BrowserViewModel = viewModel()
    var themeMode by remember { mutableStateOf(runCatching { ThemeMode.valueOf(app.settings.themeMode) }.getOrDefault(ThemeMode.System)) }
    TokiDiscoverTheme(themeMode) {
        var dest by remember { mutableStateOf(Dest.Browser) }
        val hazeState = remember { HazeState() }; val density = LocalDensity.current
        var barHeight by remember { mutableStateOf(0.dp) }
        val bottomPadding = barHeight + 16.dp
        var currentWebView by remember { mutableStateOf<WebView?>(null) }
        Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
                    when (dest) {
                        Dest.Browser -> BrowserScreen(vm, hazeState, bottomPadding, onOpenTabs = { dest = Dest.Tabs }) { wv -> currentWebView = wv }
                        Dest.Tabs -> TabsScreen(vm, bottomPadding) { dest = Dest.Browser }
                        Dest.Library -> LibraryScreen(vm, bottomPadding) { url -> vm.navigate(url); dest = Dest.Browser }
                        Dest.Settings -> SettingsScreen(vm, bottomPadding, themeMode) { themeMode = it; app.settings.themeMode = it.name }
                    }
                }
                // Floating stack: omnibox above the glass nav bar on the Browser dest (8dp gap, Tokinote convention).
                Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp, start = 12.dp, end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dest == Dest.Browser && vm.activeTab.url != UrlNormalizer.NEW_TAB_URL) {
                        FloatingOmnibox(tab = vm.activeTab, isLoading = vm.activeTab.progress in 1..99,
                            onSubmit = { vm.submitOmnibox(it) },
                            onBack = { currentWebView?.goBack() }, onForward = { currentWebView?.goForward() },
                            onReloadOrStop = { if (vm.activeTab.progress in 1..99) currentWebView?.stopLoading() else currentWebView?.reload() },
                            onTabs = { dest = Dest.Tabs }, tabCount = vm.tabs.size, glass = Modifier.tokiNavGlass(hazeState))
                    }
                    FloatingNavBar(dest, { dest = it }, modifier = Modifier.onSizeChanged { barHeight = with(density) { it.height.toDp() } }, glass = Modifier.tokiNavGlass(hazeState))
                }
                vm.blockedMessage?.let { msg -> AlertDialog(onDismissRequest = { vm.blockedMessage = null }, confirmButton = { TextButton(onClick = { vm.blockedMessage = null }) { Text("知道了") } }, title = { Text("已拦截") }, text = { Text(msg) }) }
            }
        }
    }
}
