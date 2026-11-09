package dev.ichiyotsu.tokidiscover.browser

import android.app.Activity
import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.AndroidViewModel
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference
import java.util.UUID

class TabState(
    val id: String,
    val isPrivate: Boolean,
    val webView: WebView,
) {
    var title by mutableStateOf("TokiDiscover")
    var url by mutableStateOf("")
    var isHome by mutableStateOf(true)
    var loading by mutableStateOf(false)
    var progress by mutableIntStateOf(0)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
}

class BrowserViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val tabs: SnapshotStateList<TabState> = emptyList<TabState>().toMutableStateList()

    var activeId by mutableStateOf("")
        private set
    var chromeExpanded by mutableStateOf(true)
    var engineId by mutableStateOf(prefs.getString(KEY_ENGINE, "google") ?: "google")
    var translateTarget by mutableStateOf(prefs.getString(KEY_TL, "") ?: "")
    var message by mutableStateOf<String?>(null)
    var activityRef: WeakReference<Activity>? = null

    var fileChooser: ((ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams) -> Unit)? = null

    private val _translating = MutableStateFlow(false)
    val translating: StateFlow<Boolean> = _translating

    val activeTab: TabState?
        get() = tabs.firstOrNull { it.id == activeId } ?: tabs.firstOrNull()

    val engine: SearchEngine
        get() = SearchEngines.byId(engineId)

    init {
        newTab(isPrivate = false)
    }

    fun chooseEngine(id: String) {
        engineId = id
        prefs.edit().putString(KEY_ENGINE, id).apply()
    }

    fun chooseTranslateTarget(tag: String) {
        translateTarget = tag
        prefs.edit().putString(KEY_TL, tag).apply()
    }

    fun newTab(isPrivate: Boolean, select: Boolean = true): TabState {
        val tab = TabState(
            id = UUID.randomUUID().toString(),
            isPrivate = isPrivate,
            webView = createWebView(isPrivate),
        )
        tabs.add(tab)
        if (select) activeId = tab.id
        chromeExpanded = true
        return tab
    }

    fun select(id: String) {
        if (tabs.any { it.id == id }) {
            activeId = id
            chromeExpanded = true
        }
    }

    fun close(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        val tab = tabs.removeAt(index)
        tab.webView.apply {
            stopLoading()
            destroy()
        }
        if (tabs.isEmpty()) {
            newTab(isPrivate = false)
        } else if (activeId == id) {
            val next = tabs.getOrNull(index.coerceAtMost(tabs.lastIndex)) ?: tabs.last()
            activeId = next.id
        }
        if (tab.isPrivate) clearPrivateDataIfIdle()
    }

    fun navigateActive(input: String) {
        val tab = activeTab ?: return
        val target = Omnibox.resolve(input, engine)
        if (target == Omnibox.HOME) {
            showHome(tab)
        } else {
            tab.isHome = false
            tab.webView.loadUrl(target)
        }
        chromeExpanded = true
    }

    fun showHome() {
        val tab = activeTab ?: return
        showHome(tab)
    }

    fun showHome(tab: TabState) {
        tab.webView.stopLoading()
        tab.isHome = true
        tab.loading = false
        tab.url = ""
        tab.title = "TokiDiscover"
        tab.canGoBack = tab.webView.canGoBack()
        chromeExpanded = true
    }

    fun back() {
        val tab = activeTab ?: return
        if (tab.isHome) return
        if (tab.webView.canGoBack()) tab.webView.goBack() else showHome(tab)
    }

    fun forward() {
        val tab = activeTab ?: return
        if (tab.webView.canGoForward()) {
            tab.isHome = false
            tab.webView.goForward()
        }
    }

    fun reload() {
        val tab = activeTab ?: return
        if (tab.isHome) return
        tab.webView.reload()
    }

    fun onScroll(dy: Int, y: Int) {
        val tab = activeTab ?: return
        if (tab.isHome) return
        if (dy > 18 && y > 48) chromeExpanded = false
        else if (dy < -18 || y <= 0) chromeExpanded = true
    }

    fun openExternal(url: String?) {
        if (url.isNullOrBlank()) return
        navigateActive(url)
    }

    suspend fun translateActive() {
        val tab = activeTab ?: return
        if (tab.isHome) {
            message = if (zh()) "先打开一个网页" else "Open a page first"
            return
        }
        val tag = translateTarget.ifBlank {
            java.util.Locale.getDefault().toLanguageTag()
        }
        _translating.value = true
        try {
            val count = PageTranslator.translate(tab.webView, tag)
            message = if (count > 0) {
                if (zh()) "已翻译约 $count 段" else "Translated about $count blocks"
            } else {
                if (zh()) "这一页没有可翻译的文本" else "Nothing to translate"
            }
        } catch (error: Exception) {
            message = if (zh()) {
                "设备端翻译失败，可改用在线翻译"
            } else {
                "On-device translation failed. Try online translate."
            }
        } finally {
            _translating.value = false
        }
    }

    fun translateOnline() {
        val tab = activeTab ?: return
        val page = tab.url
        if (tab.isHome || page.isBlank()) return
        val tag = translateTarget.ifBlank { java.util.Locale.getDefault().toLanguageTag() }
        navigateActive(Omnibox.onlineTranslate(page, tag))
    }

    override fun onCleared() {
        tabs.forEach { it.webView.destroy() }
        tabs.clear()
        super.onCleared()
    }

    private fun createWebView(isPrivate: Boolean): WebView {
        val context = getApplication<Application>()
        val webView = WebView(context)
        if (isPrivate && WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            runCatching {
                ProfileStore.getInstance().getOrCreateProfile(PRIVATE_PROFILE)
                WebViewCompat.setProfile(webView, PRIVATE_PROFILE)
            }
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = if (isPrivate) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            javaScriptCanOpenWindowsAutomatically = true
            userAgentString = userAgentString + " TokiDiscover/0.1.0"
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, !isPrivate)
        webView.setDownloadListener { url, userAgent, contentDisposition, mime, _ ->
            enqueueDownload(url, userAgent, contentDisposition, mime)
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                val tab = tabOf(view) ?: return
                tab.isHome = false
                tab.loading = true
                tab.url = url.orEmpty()
            }

            override fun onPageFinished(view: WebView, url: String?) {
                val tab = tabOf(view) ?: return
                tab.loading = false
                tab.progress = 100
                tab.url = url.orEmpty()
                tab.title = view.title?.takeIf { it.isNotBlank() } ?: tab.title
                tab.canGoBack = view.canGoBack()
                tab.canGoForward = view.canGoForward()
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                val tab = tabOf(view) ?: return
                tab.canGoBack = view.canGoBack()
                tab.canGoForward = view.canGoForward()
                if (!tab.isHome && !url.isNullOrBlank()) tab.url = url
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                val uri = request.url ?: return false
                val scheme = uri.scheme?.lowercase().orEmpty()
                if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data") {
                    return false
                }
                if (scheme == "intent" || scheme == "mailto" || scheme == "tel" || scheme == "geo") {
                    val activity = activityRef?.get() ?: return true
                    runCatching {
                        val intent = if (scheme == "intent") {
                            Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                        } else {
                            Intent(Intent.ACTION_VIEW, uri)
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        activity.startActivity(intent)
                    }
                    return true
                }
                return true
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                val tab = view?.let { tabOf(it) } ?: return
                tab.progress = newProgress
                tab.loading = newProgress in 1..99
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                val tab = view?.let { tabOf(it) } ?: return
                if (!title.isNullOrBlank() && !tab.isHome) tab.title = title
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?,
            ): Boolean {
                val params = fileChooserParams ?: return false
                val chooser = fileChooser ?: return false
                chooser.invoke(filePathCallback, params)
                return true
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?,
            ): Boolean {
                val tab = view?.let { tabOf(it) } ?: return false
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                val child = createWebView(tab.isPrivate)
                val created = TabState(UUID.randomUUID().toString(), tab.isPrivate, child)
                tabs.add(created)
                activeId = created.id
                transport.webView = child
                resultMsg.sendToTarget()
                return true
            }
        }
        webView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (tabOf(webView)?.id == activeId) onScroll(scrollY - oldScrollY, scrollY)
        }
        return webView
    }

    private fun enqueueDownload(url: String, userAgent: String, contentDisposition: String?, mime: String?) {
        val context: Context = activityRef?.get() ?: getApplication()
        runCatching {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                if (!mime.isNullOrBlank()) setMimeType(mime)
                addRequestHeader("User-Agent", userAgent)
                val name = URLUtil.guessFileName(url, contentDisposition, mime)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                setTitle(name)
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            message = if (zh()) "已开始下载" else "Download started"
        }.onFailure {
            message = if (zh()) "无法下载此文件" else "Could not download this file"
        }
    }

    private fun clearPrivateDataIfIdle() {
        if (tabs.any { it.isPrivate }) return
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) return
        runCatching {
            val profile = ProfileStore.getInstance().getProfile(PRIVATE_PROFILE) ?: return
            profile.cookieManager.removeAllCookies(null)
            profile.cookieManager.flush()
            profile.webStorage.deleteAllData()
        }
        runCatching { WebStorage.getInstance() }
    }

    private fun tabOf(view: WebView): TabState? = tabs.firstOrNull { it.webView === view }

    private fun zh(): Boolean = java.util.Locale.getDefault().language.startsWith("zh")

    companion object {
        const val PRIVATE_PROFILE = "toki-private"
        private const val PREFS = "tokidiscover"
        private const val KEY_ENGINE = "engine"
        private const val KEY_TL = "translate"
    }
}
