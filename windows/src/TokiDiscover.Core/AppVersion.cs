namespace TokiDiscover.Core;
/// <summary>Version is read from the assembly (Directory.Build.props: 0.1.0 / InformationalVersion 0.1.0-beta1), never hardcoded in UI.</summary>
public static class AppVersion
{
    public static string Display => typeof(AppVersion).Assembly.GetName().Version?.ToString() ?? "0.1.0.0";
    public static string Informational => "0.1.0-beta1"; // mirrors Directory.Build.props InformationalVersion
}
