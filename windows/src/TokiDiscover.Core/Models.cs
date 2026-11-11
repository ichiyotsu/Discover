namespace TokiDiscover.Core;
/// <summary>Tab model: private tabs never write history / search records.</summary>
public sealed class BrowserTab
{
    public Guid Id { get; init; } = Guid.NewGuid();
    public string Url { get; set; } = UrlNormalizer.NewTabUrl;
    public string Title { get; set; } = "新标签页";
    public bool IsPrivate { get; init; }
    public bool CanGoBack { get; set; }
    public bool CanGoForward { get; set; }
    public int Progress { get; set; }
}
public sealed record Bookmark(Guid Id, string Title, string Url, string Folder = "默认", DateTimeOffset CreatedAt = default);
public sealed record HistoryEntry(Guid Id, string Title, string Url, DateTimeOffset VisitedAt);
public sealed record DownloadRecord(Guid Id, string FileName, string Url, long TotalBytes = -1, string Status = "已记录");
public sealed record SpeedDialSite(string Title, string Url);
public static class DefaultSpeedDial
{
    public static readonly IReadOnlyList<SpeedDialSite> Sites = new[]
    {
        new SpeedDialSite("Bilibili", "https://www.bilibili.com"), new SpeedDialSite("GitHub", "https://github.com"),
        new SpeedDialSite("Wikipedia", "https://www.wikipedia.org"), new SpeedDialSite("Bing", "https://www.bing.com"),
        new SpeedDialSite("知乎", "https://www.zhihu.com"), new SpeedDialSite("YouTube", "https://www.youtube.com"),
    };
}
/// <summary>In-memory tab manager (UI-agnostic, unit-testable).</summary>
public sealed class TabManager
{
    public List<BrowserTab> Tabs { get; } = new() { new BrowserTab() };
    public Guid ActiveTabId { get; private set; }
    public TabManager() => ActiveTabId = Tabs[0].Id;
    public BrowserTab Active => Tabs.FirstOrDefault(t => t.Id == ActiveTabId) ?? Tabs[0];
    public BrowserTab NewTab(bool isPrivate = false) { var t = new BrowserTab { IsPrivate = isPrivate }; Tabs.Add(t); ActiveTabId = t.Id; return t; }
    public void CloseTab(Guid id)
    {
        var i = Tabs.FindIndex(t => t.Id == id); if (i < 0) return; Tabs.RemoveAt(i);
        if (Tabs.Count == 0) { var t = new BrowserTab(); Tabs.Add(t); ActiveTabId = t.Id; }
        else if (ActiveTabId == id) ActiveTabId = Tabs[Math.Min(i, Tabs.Count - 1)].Id;
    }
    public void SelectTab(Guid id) { if (Tabs.Any(t => t.Id == id)) ActiveTabId = id; }
}
