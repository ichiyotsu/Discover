using Microsoft.UI.Windowing;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using System.Runtime.InteropServices;
using TokiDiscover.Core;
using Windows.Graphics;
using Windows.System;
namespace TokiDiscover;
public sealed partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();
        Title = "TokiDiscover";
        // Transparent title bar: content extends into it, caption buttons transparent, drag region kept.
        ExtendsContentIntoTitleBar = true;
        AppWindow.TitleBar.ButtonBackgroundColor = Microsoft.UI.Colors.Transparent;
        AppWindow.TitleBar.ButtonInactiveBackgroundColor = Microsoft.UI.Colors.Transparent;
        AppWindow.Resize(new SizeInt32(1100, 760));
        SetMinSize(640, 480); // minimum window 640x480 DIP (Tokinote convention)
        RefreshTabList();
        ContentFrame.Navigate(typeof(Views.BrowserPage));
    }
    public void RefreshTabList()
    {
        Nav.MenuItems.Clear();
        foreach (var t in App.Browser.Tabs.Tabs)
            Nav.MenuItems.Add(new NavigationViewItem { Content = (t.IsPrivate ? "无痕 · " : "") + t.Title, Tag = t.Id.ToString() });
        Omnibox.Text = App.Browser.Tabs.Active.Url == UrlNormalizer.NewTabUrl ? "" : App.Browser.Tabs.Active.Url;
    }
    public void NavigateOmniboxInput()
    {
        switch (UrlNormalizer.Normalize(Omnibox.Text, App.Browser.Engine, App.Browser.CustomTemplate))
        {
            case NavResult.Url u: App.Browser.Tabs.Active.Url = u.Value; break;
            case NavResult.Search s: App.Browser.Tabs.Active.Url = s.Value; break;
            case NavResult.NewTab: App.Browser.Tabs.Active.Url = UrlNormalizer.NewTabUrl; break;
            case NavResult.Blocked b: Omnibox.Text = ""; Omnibox.PlaceholderText = b.Reason; return;
        }
        ContentFrame.Navigate(typeof(Views.BrowserPage));
        RefreshTabList();
    }
    private void Omnibox_KeyDown(object sender, KeyRoutedEventArgs e) { if (e.Key == VirtualKey.Enter) NavigateOmniboxInput(); }
    private void Back_Click(object sender, RoutedEventArgs e) => Views.BrowserPage.Current?.GoBack();
    private void Forward_Click(object sender, RoutedEventArgs e) => Views.BrowserPage.Current?.GoForward();
    private void Reload_Click(object sender, RoutedEventArgs e) => Views.BrowserPage.Current?.ReloadOrStop();
    private void NewTab_Click(object sender, RoutedEventArgs e) { App.Browser.Tabs.NewTab(); ContentFrame.Navigate(typeof(Views.BrowserPage)); RefreshTabList(); }
    private void Tabs_Click(object sender, RoutedEventArgs e) => ContentFrame.Navigate(typeof(Views.BrowserPage)); // tab overview lives in the NavigationView pane + BrowserPage grid toggle (see BrowserPage)
    private void Library_Click(object sender, RoutedEventArgs e) => ContentFrame.Navigate(typeof(Views.LibraryPage));
    private void Settings_Click(object sender, RoutedEventArgs e) => ContentFrame.Navigate(typeof(Views.SettingsPage));
    private void Nav_SelectionChanged(NavigationView sender, NavigationViewSelectionChangedEventArgs args)
    {
        if (args.SelectedItem is NavigationViewItem item && item.Tag is string tag && Guid.TryParse(tag, out var id))
        { App.Browser.Tabs.SelectTab(id); ContentFrame.Navigate(typeof(Views.BrowserPage)); RefreshTabList(); }
    }
    public void SetProgress(int value) { LoadProgress.Visibility = value is > 0 and < 100 ? Visibility.Visible : Visibility.Collapsed; LoadProgress.Value = value; }

    // --- Min size via WM_GETMINMAXINFO subclass (same approach as Tokinote WindowSizing) ---
    [DllImport("user32.dll")] private static extern bool SetWindowSubclass(IntPtr hWnd, SubclassProc proc, nuint id, IntPtr data);
    private delegate bool SubclassProc(IntPtr hWnd, uint msg, IntPtr w, IntPtr l, nuint id, IntPtr data);
    [StructLayout(LayoutKind.Sequential)] private struct PointL { public int X, Y; }
    [StructLayout(LayoutKind.Sequential)] private struct MinMaxInfo { public PointL Reserved, MaxSize, MaxPos, MinTrack, MaxTrack; }
    [DllImport("user32.dll")] private static extern bool DefSubclassProc(IntPtr hWnd, uint msg, IntPtr w, IntPtr l);
    private SubclassProc? _proc; private int _minW = 640, _minH = 480;
    private void SetMinSize(int wDip, int hDip)
    {
        var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(this);
        var dpi = (int)Microsoft.UI.Win32Interop.GetDpiForWindow(hwnd);
        _minW = wDip * dpi / 96; _minH = hDip * dpi / 96;
        _proc = (h, msg, w, l, id, data) => { if (msg == 0x0024) { var m = Marshal.PtrToStructure<MinMaxInfo>(l); m.MinTrack = new PointL { X = _minW, Y = _minH }; Marshal.StructureToPtr(m, l, false); } return DefSubclassProc(h, msg, w, l); };
        SetWindowSubclass(hwnd, _proc, 1, IntPtr.Zero);
    }
}
