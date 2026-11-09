using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media.Animation;
using Microsoft.Web.WebView2.Core;
using TokiDiscover.Browser;
using Windows.System;

namespace TokiDiscover;

public sealed partial class MainWindow : Window
{
    private readonly SettingsStore _settings = new();
    private readonly List<BrowserTab> _tabs = [];
    private BrowserTab? _active;
    private bool _addressFocused;
    private bool _chromeExpanded = true;
    private CoreWebView2Environment? _normalEnvironment;
    private CoreWebView2Environment? _privateEnvironment;
    private string? _privateFolder;

    public MainWindow()
    {
        InitializeComponent();
        Title = "TokiDiscover";
        Backdrop.Apply(this, Root);
        ExtendsContentIntoTitleBar = true;
        SetTitleBar(TitleBarDrag);
        AppWindow.Resize(new Windows.Graphics.SizeInt32(1180, 780));
        ApplyText();
        _ = CreateTab(privateMode: false, url: null);
    }

    private void ApplyText()
    {
        TitleText.Text = Loc.App;
        PrivateBadgeText.Text = Loc.PrivateBadge;
        AddressBox.PlaceholderText = Loc.AddressHint;
        HomeBox.PlaceholderText = Loc.AddressHint;
        HomeTagline.Text = Loc.Tagline;
        EngineLabel.Text = Loc.SearchEngine;
        ToolTipService.SetToolTip(BackButton, Loc.Back);
        ToolTipService.SetToolTip(ForwardButton, Loc.Forward);
        ToolTipService.SetToolTip(ReloadButton, Loc.Reload);
        ToolTipService.SetToolTip(HomeButton, Loc.Home);
        ToolTipService.SetToolTip(TranslateButton, Loc.Translate);
        ToolTipService.SetToolTip(MenuButton, Loc.Menu);
        RebuildEngines();
    }

    private void RebuildEngines()
    {
        EngineList.Children.Clear();
        foreach (var engine in Engines.All)
        {
            var radio = new RadioButton
            {
                Content = engine.Label,
                GroupName = "engine",
                IsChecked = engine.Id == _settings.EngineId,
                Tag = engine.Id,
            };
            radio.Checked += (_, _) =>
            {
                if (radio.Tag is string id) _settings.SetEngine(id);
            };
            EngineList.Children.Add(radio);
        }
    }

    private async Task CreateTab(bool privateMode, string? url)
    {
        var web = new WebView2 { Visibility = Visibility.Collapsed };
        WebHost.Children.Add(web);
        var tab = new BrowserTab(web, privateMode);
        _tabs.Add(tab);
        AddTabChip(tab);
        Select(tab);
        try
        {
            var environment = await EnvironmentFor(privateMode);
            await web.EnsureCoreWebView2Async(environment);
            var settings = web.CoreWebView2.Settings;
            settings.IsStatusBarEnabled = false;
            settings.AreDefaultContextMenusEnabled = true;
            settings.IsPasswordAutosaveEnabled = !privateMode;
            settings.IsGeneralAutofillEnabled = !privateMode;
            settings.UserAgent += " TokiDiscover/0.1.0";
            await web.CoreWebView2.AddScriptToExecuteOnDocumentCreatedAsync(ScrollScript);
            web.CoreWebView2.DocumentTitleChanged += (_, _) => DispatcherQueue.TryEnqueue(() =>
            {
                tab.Title = web.CoreWebView2.DocumentTitle;
                if (string.IsNullOrWhiteSpace(tab.Title)) tab.Title = "TokiDiscover";
                RefreshChrome();
            });
            web.CoreWebView2.SourceChanged += (_, _) => DispatcherQueue.TryEnqueue(() => SyncAddress(tab));
            web.CoreWebView2.HistoryChanged += (_, _) => DispatcherQueue.TryEnqueue(UpdateNavButtons);
            web.CoreWebView2.NavigationCompleted += (_, _) => DispatcherQueue.TryEnqueue(() => SyncAddress(tab));
            web.CoreWebView2.NewWindowRequested += (sender, args) =>
            {
                args.Handled = true;
                var targetUrl = args.Uri;
                var isPrivate = tab.IsPrivate;
                DispatcherQueue.TryEnqueue(() =>
                {
                    _ = CreateTab(isPrivate, targetUrl);
                });
            };
            web.CoreWebView2.WebMessageReceived += (_, args) =>
            {
                var message = args.TryGetWebMessageAsString();
                DispatcherQueue.TryEnqueue(() =>
                {
                    if (_active != tab || _addressFocused || tab.IsHome) return;
                    if (message == "collapse") SetChrome(false);
                    else if (message == "expand") SetChrome(true);
                });
            };
            if (string.IsNullOrEmpty(url) || url == Omnibox.Home) ShowHome(tab);
            else web.CoreWebView2.Navigate(url);
        }
        catch (Exception ex)
        {
            Notice.Message = Loc.WebViewMissing + " " + ex.Message;
            Notice.IsOpen = true;
            ShowHome(tab);
        }
    }

    private async Task<CoreWebView2Environment> EnvironmentFor(bool privateMode)
    {
        if (!privateMode)
        {
            if (_normalEnvironment != null) return _normalEnvironment;
            var folder = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "TokiDiscover", "WebView2");
            Directory.CreateDirectory(folder);
            _normalEnvironment = await CoreWebView2Environment.CreateWithOptionsAsync(null!, folder, null!);
            return _normalEnvironment;
        }

        if (_privateEnvironment != null) return _privateEnvironment;
        _privateFolder = Path.Combine(Path.GetTempPath(), "TokiDiscover", "private-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(_privateFolder);
        var options = new CoreWebView2EnvironmentOptions();
        _privateEnvironment = await CoreWebView2Environment.CreateWithOptionsAsync(null!, _privateFolder, options);
        return _privateEnvironment;
    }

    private void AddTabChip(BrowserTab tab)
    {
        var button = new Button
        {
            MaxWidth = 180,
            Height = 32,
            CornerRadius = new CornerRadius(10),
            Tag = tab,
        };
        var text = new TextBlock { TextTrimming = TextTrimming.CharacterEllipsis, MaxWidth = 140 };
        button.Content = text;
        button.Click += (_, _) => Select(tab);
        tab.Chip = button;
        tab.ChipText = text;
        TabStrip.Children.Add(button);
    }

    private void Select(BrowserTab tab)
    {
        _active = tab;
        foreach (var item in _tabs)
        {
            item.Web.Visibility = item == tab && !item.IsHome ? Visibility.Visible : Visibility.Collapsed;
        }
        HomeLayer.Visibility = tab.IsHome ? Visibility.Visible : Visibility.Collapsed;
        PrivateBadge.Visibility = tab.IsPrivate ? Visibility.Visible : Visibility.Collapsed;
        RefreshChrome();
        UpdateNavButtons();
        SetChrome(true);
    }

    private void ShowHome(BrowserTab tab)
    {
        tab.IsHome = true;
        tab.Title = "TokiDiscover";
        tab.Url = "";
        tab.Web.Visibility = Visibility.Collapsed;
        if (_active == tab)
        {
            HomeLayer.Visibility = Visibility.Visible;
            AddressBox.Text = "";
            RefreshChrome();
            SetChrome(true);
        }
    }

    private void Navigate(BrowserTab tab, string input)
    {
        var target = Omnibox.Resolve(input, Engines.ById(_settings.EngineId));
        if (target == Omnibox.Home)
        {
            ShowHome(tab);
            return;
        }
        if (tab.Web.CoreWebView2 == null)
        {
            Notice.Message = Loc.WebViewMissing;
            Notice.IsOpen = true;
            return;
        }
        tab.IsHome = false;
        HomeLayer.Visibility = Visibility.Collapsed;
        tab.Web.Visibility = Visibility.Visible;
        tab.Web.CoreWebView2.Navigate(target);
        SetChrome(true);
    }

    private void SyncAddress(BrowserTab tab)
    {
        if (tab.Web.CoreWebView2 == null) return;
        tab.Url = tab.Web.CoreWebView2.Source;
        tab.IsHome = false;
        if (_active == tab && !_addressFocused)
        {
            AddressBox.Text = tab.Url;
            HomeLayer.Visibility = Visibility.Collapsed;
            tab.Web.Visibility = Visibility.Visible;
        }
        RefreshChrome();
        UpdateNavButtons();
    }

    private void RefreshChrome()
    {
        foreach (var tab in _tabs)
        {
            var label = tab.IsPrivate ? "◉ " : "";
            label += string.IsNullOrWhiteSpace(tab.Title) ? "TokiDiscover" : tab.Title;
            if (tab.ChipText != null) tab.ChipText.Text = label;
            if (tab.Chip != null)
            {
                tab.Chip.Opacity = tab == _active ? 1 : 0.65;
            }
        }
        if (_active != null)
        {
            PillText.Text = string.IsNullOrWhiteSpace(_active.Title) ? "TokiDiscover" : _active.Title;
        }
    }

    private void UpdateNavButtons()
    {
        var core = _active?.Web.CoreWebView2;
        BackButton.IsEnabled = core?.CanGoBack == true || _active is { IsHome: false };
        ForwardButton.IsEnabled = core?.CanGoForward == true;
        ReloadButton.IsEnabled = _active is { IsHome: false };
    }

    private void SetChrome(bool expanded)
    {
        if (_chromeExpanded == expanded) return;
        _chromeExpanded = expanded;
        var ease = new CubicEase { EasingMode = EasingMode.EaseOut };
        var duration = new Duration(TimeSpan.FromMilliseconds(expanded ? 220 : 160));
        var animation = new DoubleAnimation
        {
            To = expanded ? 1 : 0.96,
            Duration = duration,
            EasingFunction = ease,
        };
        Storyboard.SetTarget(animation, ChromeScale);
        Storyboard.SetTargetProperty(animation, "ScaleX");
        var storyboard = new Storyboard();
        storyboard.Children.Add(animation);
        storyboard.Begin();
        ChromeCard.Visibility = expanded ? Visibility.Visible : Visibility.Collapsed;
        ChromePill.Visibility = expanded ? Visibility.Collapsed : Visibility.Visible;
    }

    private void CloseTab(BrowserTab tab)
    {
        tab.Web.Close();
        WebHost.Children.Remove(tab.Web);
        if (tab.Chip != null) TabStrip.Children.Remove(tab.Chip);
        _tabs.Remove(tab);
        if (tab.IsPrivate && _tabs.All(item => !item.IsPrivate))
        {
            _privateEnvironment = null;
            if (_privateFolder != null)
            {
                try { Directory.Delete(_privateFolder, true); } catch { /* files may still be locked */ }
                _privateFolder = null;
            }
        }
        if (_tabs.Count == 0) _ = CreateTab(false, null);
        else if (_active == tab) Select(_tabs[^1]);
    }

    private void Back_Click(object sender, RoutedEventArgs e)
    {
        if (_active == null) return;
        if (_active.Web.CoreWebView2?.CanGoBack == true) _active.Web.CoreWebView2.GoBack();
        else ShowHome(_active);
    }

    private void Forward_Click(object sender, RoutedEventArgs e) => _active?.Web.CoreWebView2?.GoForward();

    private void Reload_Click(object sender, RoutedEventArgs e)
    {
        if (_active is { IsHome: false }) _active.Web.CoreWebView2?.Reload();
    }

    private void Home_Click(object sender, RoutedEventArgs e)
    {
        if (_active != null) ShowHome(_active);
    }

    private void Translate_Click(object sender, RoutedEventArgs e)
    {
        if (_active == null || _active.IsHome || string.IsNullOrWhiteSpace(_active.Url)) return;
        Navigate(_active, Omnibox.TranslateUrl(_active.Url));
    }

    private void Menu_Click(object sender, RoutedEventArgs e)
    {
        var flyout = new MenuFlyout();
        flyout.Items.Add(Item(Loc.NewTab, () => _ = CreateTab(false, null)));
        flyout.Items.Add(Item(Loc.PrivateTab, () => _ = CreateTab(true, null)));
        flyout.Items.Add(Item(Loc.CloseTab, () => { if (_active != null) CloseTab(_active); }));
        flyout.Items.Add(new MenuFlyoutSeparator());
        foreach (var engine in Engines.All)
        {
            var id = engine.Id;
            var item = new ToggleMenuFlyoutItem
            {
                Text = engine.Label,
                IsChecked = id == _settings.EngineId,
            };
            item.Click += (_, _) =>
            {
                _settings.SetEngine(id);
                RebuildEngines();
            };
            flyout.Items.Add(item);
        }
        flyout.Items.Add(new MenuFlyoutSeparator());
        flyout.Items.Add(new MenuFlyoutItem { Text = "TokiDiscover " + Loc.Version, IsEnabled = false });
        flyout.ShowAt(MenuButton);
    }

    private static MenuFlyoutItem Item(string text, Action action)
    {
        var item = new MenuFlyoutItem { Text = text };
        item.Click += (_, _) => action();
        return item;
    }

    private void Address_KeyDown(object sender, KeyRoutedEventArgs e)
    {
        if (e.Key == VirtualKey.Enter && _active != null)
        {
            Navigate(_active, AddressBox.Text);
            e.Handled = true;
        }
    }

    private void HomeBox_KeyDown(object sender, KeyRoutedEventArgs e)
    {
        if (e.Key == VirtualKey.Enter && _active != null)
        {
            Navigate(_active, HomeBox.Text);
            HomeBox.Text = "";
            e.Handled = true;
        }
    }

    private void Address_GotFocus(object sender, RoutedEventArgs e) => _addressFocused = true;
    private void Address_LostFocus(object sender, RoutedEventArgs e) => _addressFocused = false;

    private void Pill_Click(object sender, RoutedEventArgs e) => SetChrome(true);

    private void Root_KeyDown(object sender, KeyRoutedEventArgs e)
    {
        var ctrl = (Microsoft.UI.Input.InputKeyboardSource.GetKeyStateForCurrentThread(VirtualKey.Control) &
                    Windows.UI.Core.CoreVirtualKeyStates.Down) == Windows.UI.Core.CoreVirtualKeyStates.Down;
        var shift = (Microsoft.UI.Input.InputKeyboardSource.GetKeyStateForCurrentThread(VirtualKey.Shift) &
                     Windows.UI.Core.CoreVirtualKeyStates.Down) == Windows.UI.Core.CoreVirtualKeyStates.Down;
        if (!ctrl) return;
        if (e.Key == VirtualKey.T) { _ = CreateTab(shift, null); e.Handled = true; }
        else if (e.Key == VirtualKey.W && _active != null) { CloseTab(_active); e.Handled = true; }
        else if (e.Key == VirtualKey.L) { AddressBox.Focus(FocusState.Programmatic); AddressBox.SelectAll(); e.Handled = true; }
        else if (e.Key == VirtualKey.R) { Reload_Click(this, new RoutedEventArgs()); e.Handled = true; }
    }

    private const string ScrollScript = """
        (() => {
          if (window.__tokiScroll) return;
          window.__tokiScroll = true;
          let last = window.scrollY || 0;
          window.addEventListener('scroll', () => {
            const y = window.scrollY || 0;
            if (y > last + 28 && y > 72) chrome.webview.postMessage('collapse');
            else if (last > y + 28 || y <= 0) chrome.webview.postMessage('expand');
            last = y;
          }, { passive: true });
        })();
        """;

    private sealed class BrowserTab
    {
        public BrowserTab(WebView2 web, bool isPrivate)
        {
            Web = web;
            IsPrivate = isPrivate;
        }

        public WebView2 Web { get; }
        public bool IsPrivate { get; }
        public bool IsHome { get; set; } = true;
        public string Title { get; set; } = "TokiDiscover";
        public string Url { get; set; } = "";
        public Button? Chip { get; set; }
        public TextBlock? ChipText { get; set; }
    }
}
