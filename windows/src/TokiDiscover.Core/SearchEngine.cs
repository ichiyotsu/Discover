namespace TokiDiscover.Core;
using System.Net;
/// <summary>Search engines. Default is Bing (matches the Windows ecosystem); changeable in Settings. Custom uses a template containing {query}.</summary>
public enum SearchEngine { Google, Bing, DuckDuckGo, Baidu, Custom }
public static class SearchEngineExtensions
{
    public const SearchEngine Default = SearchEngine.Bing;
    public static string DisplayName(this SearchEngine e) => e switch { SearchEngine.Google => "Google", SearchEngine.Bing => "Bing", SearchEngine.DuckDuckGo => "DuckDuckGo", SearchEngine.Baidu => "百度", _ => "自定义" };
    public static string Template(this SearchEngine e) => e switch
    {
        SearchEngine.Google => "https://www.google.com/search?q={query}",
        SearchEngine.Bing => "https://www.bing.com/search?q={query}",
        SearchEngine.DuckDuckGo => "https://duckduckgo.com/?q={query}",
        SearchEngine.Baidu => "https://www.baidu.com/s?wd={query}",
        _ => "{query}",
    };
    public static string SearchUrl(this SearchEngine e, string query, string customTemplate = "")
    {
        var tpl = e == SearchEngine.Custom && !string.IsNullOrWhiteSpace(customTemplate) ? customTemplate : e.Template();
        var enc = WebUtility.UrlEncode(query);
        return tpl.Contains("{query}") ? tpl.Replace("{query}", enc) : tpl + enc;
    }
}
