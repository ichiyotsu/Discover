using Microsoft.UI.Xaml;
namespace TokiDiscover;
public partial class App : Application
{
    public static MainWindow? Window { get; private set; }
    public static Services.BrowserService Browser { get; } = new();
    public App() => InitializeComponent();
    protected override void OnLaunched(LaunchActivatedEventArgs args) { Window = new MainWindow(); Window.Activate(); }
}
