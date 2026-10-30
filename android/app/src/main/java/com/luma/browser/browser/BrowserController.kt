package com.luma.browser.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luma.browser.R
import com.luma.browser.data.HistoryStore
import com.luma.browser.data.LumaSettings
import com.luma.browser.data.ToolbarEdge
import org.json.JSONTokener
import java.net.URLEncoder

data class PageError(
    val code: Int,
    val description: String,
    val url: String,
)

class BrowserTab(
    val id: Long,
    val webView: WebView,
) {
    var title by mutableStateOf("新标签页")
    var url by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var progress by mutableStateOf(0f)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var isNewTab by mutableStateOf(true)
    var favicon by mutableStateOf<Bitmap?>(null)
    var error by mutableStateOf<PageError?>(null)
    var preview by mutableStateOf<Bitmap?>(null)

    val displayUrl: String
        get() = if (isNewTab) "" else url
}

class BrowserController(
    private val context: Context,
    private val settings: LumaSettings,
    private val onInternalCommand: (String) -> Unit = {},
) {
    val tabs = mutableStateListOf<BrowserTab>()
    val history = HistoryStore(context)

    var activeTabId by mutableStateOf<Long?>(null)
        private set

    var toolbarCollapsed by mutableStateOf(false)
        private set

    val activeTab: BrowserTab?
        get() = tabs.firstOrNull { it.id == activeTabId }

    private var container: FrameLayout? = null
    private var nextId = 1L
    private var contentTopPx = 0
    private var contentBottomPx = 0

    init {
        newTab()
    }

    fun newTab(initialUrl: String? = null, select: Boolean = true): BrowserTab {
        val webView = buildWebView()
        val tab = BrowserTab(nextId++, webView)
        configureWebView(tab)

        if (!initialUrl.isNullOrBlank()) {
            tab.isNewTab = false
            tab.url = initialUrl
            tab.title = hostOf(initialUrl)
            tab.isLoading = true
        }

        tabs.add(tab)
        if (select) selectTab(tab.id)

        if (!initialUrl.isNullOrBlank()) {
            if (select) syncAttachment()
            loadUrl(tab, initialUrl)
        }
        return tab
    }

    fun selectTab(id: Long) {
        if (tabs.none { it.id == id }) return
        if (activeTabId == id) {
            syncAttachment()
            return
        }
        activeTab?.let { previous ->
            if (!previous.isNewTab) capturePreview(previous)
        }
        activeTabId = id
        toolbarCollapsed = false
        syncAttachment()
    }

    fun expandToolbar() {
        toolbarCollapsed = false
    }

    fun collapseToolbar() {
        if (settings.edge == ToolbarEdge.BOTTOM && settings.autoCollapseToolbar) {
            toolbarCollapsed = true
        }
    }

    fun setContentInsets(top: Int, bottom: Int) {
        if (contentTopPx == top && contentBottomPx == bottom) return
        contentTopPx = top
        contentBottomPx = bottom
        tabs.forEach { injectContentInsets(it) }
    }

    fun capturePreview(tab: BrowserTab) {
        if (tab.isNewTab || tab.error != null) return
        val width = tab.webView.width
        val height = tab.webView.height
        if (width <= 0 || height <= 0) return
        runCatching {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            tab.webView.draw(Canvas(bitmap))
            val targetWidth = 360
            val targetHeight = (height.toFloat() / width * targetWidth).toInt().coerceAtLeast(1)
            tab.preview = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            if (tab.preview !== bitmap) bitmap.recycle()
        }
    }

    fun translateCurrentPage() {
        val tab = activeTab ?: return
        if (tab.isNewTab || tab.url.isBlank()) {
            Toast.makeText(context, "先打开一个网页再翻译", Toast.LENGTH_SHORT).show()
            return
        }
        val encoded = URLEncoder.encode(tab.url, "UTF-8")
        newTab(
            "https://translate.google.com/translate?sl=auto&tl=zh-CN&u=$encoded",
            select = true,
        )
    }

    fun translateSelectionOrPage() {
        val webView = activeTab?.webView
        if (webView == null || activeTab?.isNewTab == true) {
            translateCurrentPage()
            return
        }
        webView.evaluateJavascript(
            "(function(){return window.getSelection ? window.getSelection().toString() : '';})()",
        ) { raw ->
            val selected = runCatching {
                (JSONTokener(raw).nextValue() as? String).orEmpty()
            }.getOrNull()?.trim().orEmpty()
            if (selected.isBlank()) {
                translateCurrentPage()
            } else {
                val encoded = URLEncoder.encode(selected, "UTF-8")
                newTab(
                    "https://translate.google.com/?sl=auto&tl=zh-CN&text=$encoded&op=translate",
                    select = true,
                )
            }
        }
    }

    fun closeTab(id: Long) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        val tab = tabs[index]
        tabs.removeAt(index)

        tab.webView.stopLoading()
        (tab.webView.parent as? ViewGroup)?.removeView(tab.webView)
        tab.webView.destroy()

        if (activeTabId == id) {
            activeTabId = null
            val next = tabs.getOrNull(index) ?: tabs.getOrNull(index - 1)
            if (next != null) selectTab(next.id) else newTab()
        } else {
            syncAttachment()
        }
    }

    fun closeActiveTab() {
        activeTabId?.let(::closeTab)
    }

    fun navigate(raw: String) {
        val input = raw.trim()
        if (input.isEmpty()) return

        val scheme = when {
            input.startsWith("luna://", ignoreCase = true) -> "luna://"
            input.startsWith("luma://", ignoreCase = true) -> "luma://"
            else -> null
        }
        if (scheme != null) {
            val command = input.removePrefix(scheme).trim('/').lowercase()
            if (command.startsWith("settings") || command.startsWith("history") || command.startsWith("newtab")) {
                onInternalCommand(command.substringBefore('/'))
                return
            }
        }

        val url = resolveInput(input) ?: return
        val tab = activeTab ?: newTab()
        openInTab(tab, url)
    }

    fun openInTab(tab: BrowserTab, url: String) {
        tab.isNewTab = false
        tab.error = null
        tab.url = url
        tab.title = hostOf(url)
        tab.isLoading = true
        tab.progress = 0.08f
        toolbarCollapsed = false
        if (activeTabId == tab.id) syncAttachment()
        loadUrl(tab, url)
    }

    fun showNewTabPage() {
        val tab = activeTab ?: return
        tab.webView.stopLoading()
        tab.isNewTab = true
        tab.url = ""
        tab.title = "新标签页"
        tab.isLoading = false
        tab.progress = 0f
        tab.error = null
        tab.canGoBack = false
        tab.canGoForward = false
        tab.preview = null
        toolbarCollapsed = false
        syncAttachment()
    }

    fun goBack() {
        val tab = activeTab ?: return
        if (tab.webView.canGoBack()) {
            tab.webView.goBack()
        } else if (!tab.isNewTab) {
            showNewTabPage()
        }
    }

    fun goForward() {
        val tab = activeTab ?: return
        if (tab.webView.canGoForward()) tab.webView.goForward()
    }

    fun reload() {
        val tab = activeTab ?: return
        if (tab.isNewTab) return
        if (tab.isLoading) {
            tab.webView.stopLoading()
            tab.isLoading = false
        } else if (tab.error != null) {
            retry()
        } else {
            tab.webView.reload()
        }
    }

    fun retry() {
        val tab = activeTab ?: return
        val url = tab.error?.url ?: tab.url
        if (url.isNotBlank()) openInTab(tab, url)
    }

    fun stopLoading() {
        activeTab?.webView?.stopLoading()
    }

    fun attachContainer(frame: FrameLayout) {
        if (container === frame) return
        container?.removeAllViews()
        container = frame
        syncAttachment()
    }

    fun detachContainer(frame: FrameLayout) {
        if (container !== frame) return
        frame.removeAllViews()
        container = null
    }

    fun destroy() {
        tabs.forEach { tab ->
            (tab.webView.parent as? ViewGroup)?.removeView(tab.webView)
            tab.webView.stopLoading()
            tab.webView.destroy()
        }
        tabs.clear()
        activeTabId = null
    }

    private fun syncAttachment() {
        val frame = container ?: return
        while (frame.childCount > 0) frame.removeViewAt(0)
        val tab = activeTab ?: return
        if (tab.isNewTab) return
        (tab.webView.parent as? ViewGroup)?.removeView(tab.webView)
        frame.addView(
            tab.webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    private fun loadUrl(tab: BrowserTab, url: String) {
        runCatching { tab.webView.loadUrl(url) }
            .onFailure {
                tab.isLoading = false
                Toast.makeText(context, "无法打开这个地址", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resolveInput(input: String): String? {
        val normalized = input.trim()
        if (normalized.startsWith("javascript:", true) ||
            normalized.startsWith("data:", true) ||
            normalized.startsWith("file:", true) ||
            normalized.startsWith("vbscript:", true)
        ) {
            Toast.makeText(context, "出于安全考虑，不允许打开这个地址", Toast.LENGTH_SHORT).show()
            return null
        }
        if (normalized.startsWith("http://", true) || normalized.startsWith("https://", true)) {
            return normalized
        }
        if (LOCALHOST_PATTERN.matches(normalized)) return "http://$normalized"
        if (DOMAIN_PATTERN.matches(normalized)) return "https://$normalized"
        return settings.searchUrl(normalized)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildWebView(): WebView {
        val webView = WebView(context)
        webView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
        webView.setBackgroundColor(android.graphics.Color.WHITE)
        webView.overScrollMode = WebView.OVER_SCROLL_IF_CONTENT_SCROLLS
        webView.scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT
            @Suppress("DEPRECATION")
            forceDark = WebSettings.FORCE_DARK_OFF
            allowFileAccess = false
            allowContentAccess = false
            userAgentString = WebSettings.getDefaultUserAgent(context).replace("; wv", "")
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        return webView
    }

    private fun configureWebView(tab: BrowserTab) {
        val webView = tab.webView
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val uri = request?.url ?: return false
                return handleExternalUri(uri)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
            ): WebResourceResponse? {
                val requestUrl = request?.url?.toString() ?: return null
                if (requestUrl != FONT_URL) return null
                return runCatching {
                    WebResourceResponse(
                        "font/ttf",
                        "UTF-8",
                        200,
                        "OK",
                        mapOf(
                            "Access-Control-Allow-Origin" to "*",
                            "Cache-Control" to "public, max-age=31536000",
                        ),
                        context.resources.openRawResource(R.font.google_sans_flex),
                    )
                }.getOrNull()
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                injectFontOverride(tab)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                tab.error = null
                tab.isLoading = true
                tab.progress = 0.08f
                if (!url.isNullOrBlank() && url != "about:blank") {
                    tab.url = url
                    tab.isNewTab = false
                    tab.title = hostOf(url)
                    tab.favicon = favicon
                }
                updateNavigation(tab)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                tab.isLoading = false
                tab.progress = 1f
                if (!url.isNullOrBlank() && url != "about:blank") {
                    tab.url = url
                    tab.isNewTab = false
                    if (tab.title.isBlank() || tab.title == "新标签页") {
                        tab.title = hostOf(url)
                    }
                    val lowerTitle = tab.title.lowercase()
                    val failedPage = tab.error != null ||
                        lowerTitle.contains("webpage not available") ||
                        lowerTitle.contains("this site can’t be reached") ||
                        lowerTitle.contains("this site can't be reached") ||
                        lowerTitle.contains("无法访问此网站")
                    if (!failedPage) {
                        history.record(url, tab.title)
                    }
                }
                updateNavigation(tab)
                injectContentInsets(tab)
                injectFontOverride(tab)
                webView.postDelayed({
                    if (tab.id == activeTabId) capturePreview(tab)
                }, 420)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame != true || error == null) return
                if (error.errorCode == WebViewClient.ERROR_UNKNOWN) return
                tab.isLoading = false
                tab.progress = 0f
                tab.error = PageError(
                    code = error.errorCode,
                    description = error.description?.toString().orEmpty(),
                    url = request.url?.toString() ?: tab.url,
                )
                updateNavigation(tab)
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: android.net.http.SslError?,
            ) {
                handler?.cancel()
                tab.isLoading = false
                tab.error = PageError(
                    code = -201,
                    description = "SSL 证书不受信任",
                    url = error?.url ?: tab.url,
                )
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                if (!url.isNullOrBlank() && url != "about:blank") {
                    tab.url = url
                    tab.isNewTab = false
                }
                updateNavigation(tab)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                tab.progress = (newProgress / 100f).coerceIn(0f, 1f)
                tab.isLoading = newProgress < 100
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                    tab.title = title
                    if (tab.url.startsWith("http")) history.updateTitle(tab.url, title)
                }
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                tab.favicon = icon
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?,
            ): Boolean {
                if (resultMsg == null) return false
                val newTab = newTab(select = true)
                newTab.isNewTab = false
                newTab.title = "正在加载…"
                syncAttachment()
                val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                transport.webView = newTab.webView
                resultMsg.sendToTarget()
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.deny()
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?,
            ) {
                callback?.invoke(origin, false, false)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?,
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                return false
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            enqueueDownload(url, userAgent, contentDisposition, mimeType)
        }

        val density = context.resources.displayMetrics.density
        val collapseThreshold = (30 * density).toInt().coerceAtLeast(24)
        var scrollAccumulator = 0
        webView.setOnScrollChangeListener { _: View?, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->
            if (tab.id != activeTabId) return@setOnScrollChangeListener
            if (!settings.autoCollapseToolbar || settings.edge != ToolbarEdge.BOTTOM) {
                toolbarCollapsed = false
                return@setOnScrollChangeListener
            }
            val dy = scrollY - oldScrollY
            if (scrollY <= 0) {
                toolbarCollapsed = false
                scrollAccumulator = 0
                return@setOnScrollChangeListener
            }
            if (dy > 0) {
                scrollAccumulator += dy
                if (scrollAccumulator > collapseThreshold && scrollY > collapseThreshold * 3) {
                    toolbarCollapsed = true
                    scrollAccumulator = 0
                }
            } else if (dy < 0) {
                scrollAccumulator += dy
                if (scrollAccumulator < -collapseThreshold / 2) {
                    toolbarCollapsed = false
                    scrollAccumulator = 0
                }
            }
        }
    }

    private fun injectFontOverride(tab: BrowserTab) {
        if (tab.isNewTab) return
        tab.webView.evaluateJavascript(FONT_SCRIPT, null)
    }

    private fun injectContentInsets(tab: BrowserTab) {
        if (tab.isNewTab) return
        val top = contentTopPx
        val bottom = contentBottomPx
        if (top <= 0 && bottom <= 0) return
        val script = """
            (function() {
              try {
                if (!document.body) return;
                var oldTop = document.getElementById('__luma_top_inset');
                var oldBottom = document.getElementById('__luma_bottom_inset');
                if (oldTop) oldTop.remove();
                if (oldBottom) oldBottom.remove();
                function add(id, height, atStart) {
                  if (!height || height <= 0) return;
                  var el = document.createElement('div');
                  el.id = id;
                  el.style.cssText = 'display:block!important;width:100%!important;height:' + height + 'px!important;pointer-events:none!important;flex:none!important;';
                  if (atStart && document.body.firstChild) document.body.insertBefore(el, document.body.firstChild);
                  else document.body.appendChild(el);
                }
                add('__luma_top_inset', $top, true);
                add('__luma_bottom_inset', $bottom, false);
                document.documentElement.style.scrollPaddingTop = '${top}px';
                document.documentElement.style.scrollPaddingBottom = '${bottom}px';
              } catch (e) {}
            })();
        """.trimIndent()
        tab.webView.evaluateJavascript(script, null)
    }

    private fun handleExternalUri(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme == "http" || scheme == "https" || scheme == "about") return false
        return try {
            when (scheme) {
                "mailto", "tel", "sms" -> {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
                "intent" -> {
                    val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                    context.startActivity(intent)
                }
                else -> return true
            }
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "没有可以处理这个链接的应用", Toast.LENGTH_SHORT).show()
            true
        } catch (_: Exception) {
            true
        }
    }

    private fun enqueueDownload(
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?,
    ) {
        runCatching {
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
                setTitle(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(context, "已开始下载 $fileName", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateNavigation(tab: BrowserTab) {
        tab.canGoBack = runCatching { tab.webView.canGoBack() }.getOrDefault(false)
        tab.canGoForward = runCatching { tab.webView.canGoForward() }.getOrDefault(false)
    }

    private fun hostOf(url: String): String {
        if (url.isBlank()) return "新标签页"
        return runCatching {
            val parsed = Uri.parse(url)
            parsed.host?.removePrefix("www.").orEmpty().ifBlank { url }
        }.getOrDefault(url)
    }

    private companion object {
        const val FONT_URL = "https://luna.invalid/fonts/google_sans_flex.ttf"

        val FONT_SCRIPT = """
            (function() {
              try {
                if (document.getElementById('luna-font-override')) return;
                var style = document.createElement('style');
                style.id = 'luna-font-override';
                style.textContent = `
                  @font-face {
                    font-family: 'Google Sans';
                    src: url('$FONT_URL') format('truetype');
                    font-weight: 100 900;
                    font-style: normal;
                    font-display: swap;
                  }
                  html, body {
                    font-family: 'Google Sans', 'Noto Sans SC', 'PingFang SC', sans-serif !important;
                  }
                  body *:not(i):not(svg):not(path):not([class*="icon"]):not([class*="material-icons"]):not([class*="material-symbols"]):not([class*="fa-"]):not([class*="fa "]):not([class*="glyphicon"]):not([class*="codicon"]):not([class*="octicon"]):not([data-icon]):not([aria-hidden="true"]) {
                    font-family: 'Google Sans', 'Noto Sans SC', 'PingFang SC', sans-serif !important;
                  }
                `;
                (document.head || document.documentElement).appendChild(style);
              } catch (e) {}
            })();
        """.trimIndent()

        val DOMAIN_PATTERN = Regex(
            """^(?:[\w-]+\.)+[A-Za-z]{2,}(?::\d+)?(?:[/?#].*)?$""",
        )
        val LOCALHOST_PATTERN = Regex(
            """^(?:localhost|127\.0\.0\.1|\[::1\])(?::\d+)?(?:[/?#].*)?$""",
            RegexOption.IGNORE_CASE,
        )
    }
}
