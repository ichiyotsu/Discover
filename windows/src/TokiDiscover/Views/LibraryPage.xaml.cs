using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
namespace TokiDiscover.Views;
/// <summary>Library: bookmarks / history / downloads. Downloads on Windows are recorded from WebView2 DownloadStarting
/// (name/url); full download management (pause/resume/folder) is a later version — see ARCHITECTURE.md.</summary>
public sealed partial class LibraryPage : Page
{
    private int _section;
    public LibraryPage() { InitializeComponent(); Loaded += (_, _) => Refresh(); }
    private void Section_Click(object sender, RoutedEventArgs e) { _section = int.Parse((string)((Button)sender).Tag); Refresh(); }
    private void ClearHistory_Click(object sender, RoutedEventArgs e) { App.Browser.History.Clear(); Refresh(); }
    private void Refresh()
    {
        ClearHistoryButton.Visibility = _section == 1 && App.Browser.History.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
        switch (_section)
        {
            case 0: Items.ItemsSource = App.Browser.Bookmarks.Select(b => $"{b.Title} — {b.Folder} · {b.Url}").ToList();
                EmptyText.Text = App.Browser.Bookmarks.Count == 0 ? "暂无书签 — 在浏览页点「收藏当前页」添加，默认存入「默认」文件夹" : ""; break;
            case 1: Items.ItemsSource = App.Browser.History.Select(h => $"{h.VisitedAt:MM-dd HH:mm} — {h.Title} · {h.Url}").ToList();
                EmptyText.Text = App.Browser.History.Count == 0 ? "暂无历史 — 无痕标签页不会产生历史记录" : ""; break;
            default: Items.ItemsSource = App.Browser.Downloads.Select(d => $"{d.FileName} — {d.Status} · {d.Url}").ToList();
                EmptyText.Text = App.Browser.Downloads.Count == 0 ? "暂无下载 — 网页触发下载时会在此记录（由 WebView2 完成实际保存）" : ""; break;
        }
    }
}
