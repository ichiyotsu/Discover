package dev.ichiyotsu.tokidiscover.ui

import java.util.Locale

internal object Copy {
    private val zh: Boolean
        get() = Locale.getDefault().language.startsWith("zh")

    val tagline get() = if (zh) "轻量发现" else "A lighter way to look around"
    val searchHint get() = if (zh) "搜索或输入网址" else "Search or type a URL"
    val back get() = if (zh) "后退" else "Back"
    val forward get() = if (zh) "前进" else "Forward"
    val reload get() = if (zh) "刷新" else "Reload"
    val home get() = if (zh) "主页" else "Home"
    val translate get() = if (zh) "翻译" else "Translate"
    val translateOnline get() = if (zh) "在线翻译此页" else "Translate this page online"
    val tabs get() = if (zh) "标签页" else "Tabs"
    val menu get() = if (zh) "菜单" else "Menu"
    val newTab get() = if (zh) "新标签页" else "New tab"
    val privateTab get() = if (zh) "无痕标签页" else "Private tab"
    val privateLabel get() = if (zh) "无痕" else "Private"
    val close get() = if (zh) "关闭" else "Close"
    val searchEngine get() = if (zh) "搜索引擎" else "Search engine"
    val translateLanguage get() = if (zh) "翻译目标语言" else "Translation language"
    val followSystem get() = if (zh) "跟随系统" else "System language"
    val address get() = if (zh) "地址栏" else "Address bar"
    val expand get() = if (zh) "展开地址栏" else "Expand address bar"
    val emptyTabs get() = if (zh) "没有其他标签页" else "No other tabs"
}
