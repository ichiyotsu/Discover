using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using TokiDiscover.Core;
using Windows.ApplicationModel.DataTransfer;
using Windows.System;
namespace TokiDiscover.Views;
/// <summary>Engine: WebView2 (Evergreen). Limitations: runtime must be installed (Evergreen bootstrapper in installer);
/// downloads surface via DownloadStarting (recorded, state tracked by WebView2); find = window.find() via ExecuteScriptAsync.</summary>
public sealed partial class BrowserPage : Page
{
    public static BrowserPage? Current { get; private set; }
    private bool _loading;
    public BrowserPage()
    {
        InitializeComponent(); Current = this;
        SpeedDial.ItemsSource = DefaultSpeedDial.Sites;
        Loaded += async (_, _) => await InitAsync();
    }
    private async System.Threading.Tasks.Task InitAsync()
    {
        var tab = App.Browser.Tabs.Active;
        PrivateHint.Visibility = tab.IsPrivate ? Visibility.Visible : Visibility.Collapsed;
        if (tab.Url == UrlNormalizer.NewTabUrl) { HomePanel.Visibility = Visibility.Visible; Web.Visibility = Visibility.Collapsed; return; }
        HomePanel.Visibility = Visibility.Collapsed; Web.Visibility = Visibility.Visible;
        await Web.EnsureCoreWebView2Async();
        Web.CoreWebView2.Settings.AreDevToolsEnabled = true;
        Web.CoreWebView2.Settings.IsScriptEnabled = App.Browser.JavaScriptEnabled;
        if (App.Browser.Tabs.Active.IsPrivate) { /* WebView2 inPrivate equivalent: separate CoreWebView2Environment per private session is a TODO — see ARCHITECTURE.md */ }
        Web.CoreWebView2.NavigationStarting += (_, e) => { if (!UrlNormalizer.IsSafeScheme(e.Uri)) e.Cancel = true; _loading = true; };
        Web.CoreWebView2.NavigationCompleted += (_, _) => { _loading = false; tab.CanGoBack = Web.CanGoBack; tab.CanGoForward = Web.CanGoForward;
            App.Browser.RecordHistory(tab, Web.CoreWebView2.DocumentTitle, Web.Source?.ToString() ?? tab.Url); App.Window?.RefreshTabList(); };
        Web.CoreWebView2.DownloadStarting += (_, e) => { App.Browser.Downloads.Insert(0, new DownloadRecord(Guid.NewGuid(), System.IO.Path.GetFileName(e.DownloadOperation.Uri), e.DownloadOperation.Uri)); };
        if (Web.Source?.ToString() != tab.Url && System.Uri.TryCreate(tab.Url, System.UriKind.Absolute, out var uri)) Web.Source = uri;
    }
    public void GoBack() { if (Web.CoreWebView2 != null && Web.CanGoBack) Web.GoBack(); }
    public void GoForward() { if (Web.CoreWebView2 != null && Web.CanGoForward) Web.GoForward(); }
    public void ReloadOrStop() { if (Web.CoreWebView2 == null) return; if (_loading) Web.CoreWebView2.Stop(); else Web.Reload(); }
    private void HomeSearch_KeyDown(object sender, KeyRoutedEventArgs e)
    {
        if (e.Key != VirtualKey.Enter) return;
        switch (UrlNormalizer.Normalize(HomeSearch.Text, App.Browser.Engine, App.Browser.CustomTemplate))
        { case NavResult.Url u: App.Browser.Tabs.Active.Url = u.Value; break; case NavResult.Search s: App.Browser.Tabs.Active.Url = s.Value; break; case NavResult.Blocked: return; }
        _ = InitAsync(); App.Window?.RefreshTabList();
    }
    private void SpeedDial_Click(object sender, RoutedEventArgs e) { if (sender is Button b && b.Tag is string url) { App.Browser.Tabs.Active.Url = url; _ = InitAsync(); App.Window?.RefreshTabList(); } }
    private void Bookmark_Click(object sender, RoutedEventArgs e) => App.Browser.ToggleBookmark(App.Browser.Tabs.Active);
    private void Find_Click(object sender, RoutedEventArgs e) => FindBox.Visibility = Visibility.Visible;
    private void FindBox_KeyDown(object sender, KeyRoutedEventArgs e)
    {
        if (e.Key != VirtualKey.Enter || Web.CoreWebView2 == null) return;
        // Native CoreWebView2.Find has no constructible FindOptions in the WinRT projection,
        // so find-in-page goes through window.find() (wrapAround=true: repeated Enter cycles through matches).
        var term = FindBox.Text.Replace("\\", "\\\\").Replace("'", "\\'").Replace("\r", " ").Replace("\n", " ");
        _ = Web.CoreWebView2.ExecuteScriptAsync($"window.find('{term}', false, false, true, false, false, false)");
    }
    private void Share_Click(object sender, RoutedEventArgs e)
    {
        var tab = App.Browser.Tabs.Active; var pkg = new DataPackage(); pkg.SetText(tab.Url);
        pkg.Properties.Title = tab.Title; Clipboard.SetContent(pkg); // system share UI (DataTransferManager) requires a window handle interop call — clipboard is the 0.1.0 fallback, noted in ARCHITECTURE.md
    }
    private void PrivateTab_Click(object sender, RoutedEventArgs e) { App.Browser.Tabs.NewTab(isPrivate: true); _ = InitAsync(); App.Window?.RefreshTabList(); }
}
