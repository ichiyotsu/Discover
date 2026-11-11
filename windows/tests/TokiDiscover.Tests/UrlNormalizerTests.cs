using TokiDiscover.Core;
using Xunit;
/// <summary>Mirrors the Android (Kotlin) and iOS (Swift) suites — keep the three in sync.</summary>
public class UrlNormalizerTests
{
    [Fact] public void BlankIsNewTab() { Assert.IsType<NavResult.NewTab>(UrlNormalizer.Normalize("")); Assert.IsType<NavResult.NewTab>(UrlNormalizer.Normalize("about:blank")); }
    [Fact] public void BareDomainGetsHttps() => Assert.Equal("https://example.com", Assert.IsType<NavResult.Url>(UrlNormalizer.Normalize("example.com")).Value);
    [Fact] public void HttpKept() => Assert.Equal("http://example.com", Assert.IsType<NavResult.Url>(UrlNormalizer.Normalize("http://example.com")).Value);
    [Fact] public void WordsBecomeSearch() { var r = Assert.IsType<NavResult.Search>(UrlNormalizer.Normalize("hello world")); Assert.Equal("https://www.bing.com/search?q=hello+world", r.Value); }
    [Fact] public void SingleWordWithoutDotSearches() => Assert.IsType<NavResult.Search>(UrlNormalizer.Normalize("kotlin"));
    [Fact] public void DangerousBlocked() { Assert.IsType<NavResult.Blocked>(UrlNormalizer.Normalize("javascript:alert(1)")); Assert.IsType<NavResult.Blocked>(UrlNormalizer.Normalize("data:text/html,hi")); }
    [Fact] public void Engines() { Assert.Equal("https://www.google.com/search?q=a+b", SearchEngine.Google.SearchUrl("a b"));
        Assert.Equal("https://duckduckgo.com/?q=a+b", SearchEngine.DuckDuckGo.SearchUrl("a b"));
        Assert.Equal("https://www.baidu.com/s?wd=a+b", SearchEngine.Baidu.SearchUrl("a b"));
        Assert.Equal("https://s.example/?q=a+b", SearchEngine.Custom.SearchUrl("a b", "https://s.example/?q={query}")); }
    [Fact] public void AssetVersion() => Assert.Equal("0.2.0", UpdateChecker.ParseVersionFromAssetName("TokiDiscover-0.2.0-win-x64.exe"));
    [Fact] public void AssetVersionBetaApk() => Assert.Equal("0.1.0-beta1", UpdateChecker.ParseVersionFromAssetName("TokiDiscover-0.1.0-beta1.apk"));
    [Fact] public void TabManagerPrivateTab() { var m = new TabManager(); var t = m.NewTab(isPrivate: true); Assert.True(t.IsPrivate); Assert.Equal(t.Id, m.ActiveTabId); m.CloseTab(t.Id); Assert.Single(m.Tabs); }
}
