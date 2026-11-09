using Microsoft.UI.Composition.SystemBackdrops;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Media;
using Windows.UI;

namespace TokiDiscover.Browser;

internal static class Backdrop
{
    public static void Apply(Window window, Microsoft.UI.Xaml.Controls.Grid root)
    {
        // Windows 11: Mica. Windows 10: acrylic when the controller exists, otherwise a solid fill.
        if (MicaController.IsSupported())
        {
            window.SystemBackdrop = new MicaBackdrop { Kind = MicaKind.Base };
            root.Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent);
        }
        else if (DesktopAcrylicController.IsSupported())
        {
            window.SystemBackdrop = new DesktopAcrylicBackdrop();
            root.Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent);
        }
        else
        {
            window.SystemBackdrop = null;
            root.Background = (Brush)Application.Current.Resources["SolidBackgroundFillColorBaseBrush"];
        }

        try
        {
            var titleBar = window.AppWindow.TitleBar;
            titleBar.BackgroundColor = Colors.Transparent;
            titleBar.InactiveBackgroundColor = Colors.Transparent;
            titleBar.ButtonBackgroundColor = Colors.Transparent;
            titleBar.ButtonInactiveBackgroundColor = Colors.Transparent;
        }
        catch
        {
            // Title bar colors are unavailable on some Windows 10 builds.
        }
    }
}
