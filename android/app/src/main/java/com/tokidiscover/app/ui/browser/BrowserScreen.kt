package com.tokidiscover.app.ui.browser
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tokidiscover.app.data.UrlNormalizer
import com.tokidiscover.app.ui.BrowserViewModel
import dev.chrisbanes.haze.HazeState
private const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
/** Engine: Android system WebView (Chromium-based, updated via Play Store — version/behaviour varies by device).
 *  Limitations: no extension support; find/download are the WebView APIs below; one WebView instance per tab. */
@SuppressLint("SetJavaScriptEnabled")
@Composable fun BrowserScreen(vm: BrowserViewModel, hazeState: HazeState, bottomPadding: androidx.compose.ui.unit.Dp, onOpenTabs: () -> Unit, bottomBar: @Composable (WebView?) -> Unit) {
    val tab = vm.activeTab; val context = LocalContext.current
    var webView by remember(tab.id) { mutableStateOf<WebView?>(null) }
    var isLoading by remember(tab.id) { mutableStateOf(false) }
    var findVisible by remember { mutableStateOf(false) }; var findQuery by remember { mutableStateOf("") }
    if (tab.url == UrlNormalizer.NEW_TAB_URL && vm.pendingNavigation == null) {
        HomeScreen(vm, bottomPadding); return
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            AssistChip(onClick = { vm.toggleBookmark() }, label = { Text(if (vm.isBookmarked(tab.url)) "已收藏" else "收藏") },
                leadingIcon = { Icon(if (vm.isBookmarked(tab.url)) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, null, Modifier.size(16.dp)) })
            Spacer(Modifier.width(8.dp))
            AssistChip(onClick = { findVisible = !findVisible }, label = { Text("页内查找") }, leadingIcon = { Icon(Icons.Filled.FindInPage, null, Modifier.size(16.dp)) })
            Spacer(Modifier.width(8.dp))
            AssistChip(onClick = { val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, tab.url); context.startActivity(Intent.createChooser(i, "分享当前页")) },
                label = { Text("分享") }, leadingIcon = { Icon(Icons.Filled.Share, null, Modifier.size(16.dp)) })
            if (tab.isPrivate) { Spacer(Modifier.width(8.dp)); AssistChip(onClick = {}, label = { Text("无痕") }) }
        }
        if (findVisible) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            TextField(value = findQuery, onValueChange = { findQuery = it; webView?.findAllAsync(it) }, singleLine = true, placeholder = { Text("在页面中查找") }, modifier = Modifier.weight(1f))
            TextButton(onClick = { webView?.findNext(true) }) { Text("下一个") }
            TextButton(onClick = { webView?.clearMatches(); findVisible = false }) { Text("关闭") }
        }
        AndroidView(factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                settings.javaScriptEnabled = vm.settings.javaScriptEnabled
                settings.domStorageEnabled = true; settings.loadWithOverviewMode = true; settings.useWideViewPort = true
                if (vm.settings.desktopUa) settings.userAgentString = DESKTOP_UA
                if (tab.isPrivate) { settings.cacheMode = WebSettings.LOAD_NO_CACHE; CookieManager.getInstance().setAcceptCookie(false) }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean = !UrlNormalizer.isSafeScheme(r.url.toString()) // block javascript:/data: etc.
                    override fun onPageStarted(v: WebView, url: String, fav: Bitmap?) { isLoading = true; tab.url = url }
                    override fun onPageFinished(v: WebView, url: String) { isLoading = false; tab.progress = 100
                        tab.canGoBack = v.canGoBack(); tab.canGoForward = v.canGoForward(); vm.onPageCommitted(tab, v.title ?: "", url) }
                }
                webChromeClient = object : WebChromeClient() { override fun onProgressChanged(v: WebView, p: Int) { tab.progress = p } }
                setDownloadListener { url, _, contentDisposition, _, total ->
                    val name = URLUtil.guessFileName(url, contentDisposition, null); vm.onDownloadStarted(name, url, total) } // record only; system DownloadManager handoff is a TODO (see ARCHITECTURE.md)
                webView = this
                val initial = vm.pendingNavigation ?: tab.url; if (initial != UrlNormalizer.NEW_TAB_URL) loadUrl(initial)
            }
        }, update = { w ->
            val target = vm.pendingNavigation
            if (target != null && target != UrlNormalizer.NEW_TAB_URL && w.url != target) w.loadUrl(target)
            if (target != null) vm.pendingNavigation = null
        }, modifier = Modifier.weight(1f))
        bottomBar(webView)
    }
}
