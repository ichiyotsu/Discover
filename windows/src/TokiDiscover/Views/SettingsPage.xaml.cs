using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using TokiDiscover.Core;
namespace TokiDiscover.Views;
public sealed partial class SettingsPage : Page
{
    public SettingsPage()
    {
        InitializeComponent();
        EngineBox.ItemsSource = Enum.GetValues<SearchEngine>().Select(e => e.DisplayName()).ToList();
        EngineBox.SelectedIndex = (int)App.Browser.Engine;
        JsSwitch.IsOn = App.Browser.JavaScriptEnabled;
        AboutText.Text = $"TokiDiscover 浏览器 {AppVersion.Informational}（程序集 {AppVersion.Display}）\n引擎：WebView2（Evergreen Chromium）· 仓库 ichiyotsu/tokidiscover（待用户创建后检查更新）"; // version from assembly, never hardcoded in UI text
    }
    private void EngineBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
    { App.Browser.Engine = (SearchEngine)EngineBox.SelectedIndex; CustomTemplateBox.Visibility = App.Browser.Engine == SearchEngine.Custom ? Visibility.Visible : Visibility.Collapsed; }
    private void CustomTemplateBox_TextChanged(object sender, TextChangedEventArgs e) => App.Browser.CustomTemplate = CustomTemplateBox.Text;
    private void JsSwitch_Toggled(object sender, RoutedEventArgs e) => App.Browser.JavaScriptEnabled = JsSwitch.IsOn;
    private async void ClearWebData_Click(object sender, RoutedEventArgs e)
    { /* WebView2 profile clear: CoreWebView2.Profile.ClearBrowsingDataAsync — wired from BrowserPage's live WebView2 in a later pass; button kept honest: */
      UpdateText.Text = "请在浏览页打开任意网页后使用（需要活动的 WebView2 配置文件）— 0.1.0-beta1 暂未跨页面接线，见 ARCHITECTURE.md"; await System.Threading.Tasks.Task.CompletedTask; }
    private void ClearHistory_Click(object sender, RoutedEventArgs e) { App.Browser.History.Clear(); UpdateText.Text = "历史已清空"; }
    private async void CheckUpdate_Click(object sender, RoutedEventArgs e)
    {
        UpdateText.Text = "检查中…";
        try { using var http = new System.Net.Http.HttpClient(); http.DefaultRequestHeaders.UserAgent.ParseAdd("TokiDiscover");
            var json = await http.GetStringAsync(UpdateChecker.ReleasesApi); var latest = UpdateChecker.ExtractLatestVersion(json);
            UpdateText.Text = latest == null ? "暂时无法解析版本" : UpdateChecker.IsNewer(latest, "0.1.0") ? $"发现新版本 {latest}" : "已是最新版本";
        } catch { UpdateText.Text = "暂时无法检查（仓库未创建或无网络）"; }
    }
}
