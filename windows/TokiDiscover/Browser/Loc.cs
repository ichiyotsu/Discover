using System.Globalization;

namespace TokiDiscover.Browser;

internal static class Loc
{
    public static bool Zh => CultureInfo.CurrentUICulture.TwoLetterISOLanguageName.Equals("zh", StringComparison.OrdinalIgnoreCase);

    public static string App => "TokiDiscover";
    public static string Tagline => Zh ? "轻量发现" : "A lighter way to look around";
    public static string AddressHint => Zh ? "搜索或输入网址" : "Search or type a URL";
    public static string Back => Zh ? "后退" : "Back";
    public static string Forward => Zh ? "前进" : "Forward";
    public static string Reload => Zh ? "刷新" : "Reload";
    public static string Home => Zh ? "主页" : "Home";
    public static string Translate => Zh ? "翻译此页" : "Translate page";
    public static string NewTab => Zh ? "新标签页" : "New tab";
    public static string PrivateTab => Zh ? "无痕标签页" : "Private tab";
    public static string CloseTab => Zh ? "关闭标签页" : "Close tab";
    public static string SearchEngine => Zh ? "搜索引擎" : "Search engine";
    public static string PrivateBadge => Zh ? "无痕" : "Private";
    public static string Menu => Zh ? "菜单" : "Menu";
    public static string Version => "0.1.0";
    public static string WebViewMissing => Zh
        ? "需要安装 Microsoft Edge WebView2 运行时后才能浏览网页。Windows 11 通常已自带。"
        : "The Microsoft Edge WebView2 Runtime is required. Windows 11 includes it.";
}
