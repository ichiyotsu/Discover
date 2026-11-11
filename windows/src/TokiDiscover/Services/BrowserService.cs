using TokiDiscover.Core;
namespace TokiDiscover.Services;
/// <summary>App-wide browser state: tabs (TabManager), library lists persisted as JSON under LocalFolder,
/// settings in LocalSettings. Private tabs never write history.</summary>
public sealed class BrowserService
{
    public TabManager Tabs { get; } = new();
    public List<Bookmark> Bookmarks { get; } = new();
    public List<HistoryEntry> History { get; } = new();
    public List<DownloadRecord> Downloads { get; } = new();
    public SearchEngine Engine { get; set; } = SearchEngineExtensions.Default;
    public string CustomTemplate { get; set; } = "";
    public bool DesktopUa { get; set; }
    public bool JavaScriptEnabled { get; set; } = true;
    public void RecordHistory(BrowserTab tab, string title, string url)
    {
        if (tab.IsPrivate || url == UrlNormalizer.NewTabUrl) return;
        History.Insert(0, new HistoryEntry(Guid.NewGuid(), string.IsNullOrWhiteSpace(title) ? url : title, url, DateTimeOffset.Now));
        if (History.Count > 500) History.RemoveRange(500, History.Count - 500);
    }
    public void ToggleBookmark(BrowserTab tab)
    {
        var existing = Bookmarks.FirstOrDefault(b => b.Url == tab.Url);
        if (existing != null) Bookmarks.Remove(existing);
        else if (tab.Url != UrlNormalizer.NewTabUrl) Bookmarks.Add(new Bookmark(Guid.NewGuid(), tab.Title, tab.Url, "默认", DateTimeOffset.Now));
    }
}
