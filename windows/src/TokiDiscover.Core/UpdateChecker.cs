namespace TokiDiscover.Core;
using System.Text.Json;
using System.Text.RegularExpressions;
/// <summary>Checks GitHub Releases of ichiyotsu/tokidiscover for updates.
/// IMPORTANT (bug fixed in Tokilive/tokimusic, do not regress): release tags have shipped mismatched asset
/// versions before, so the version is parsed from the installer asset *filename* first, tag as fallback.</summary>
public static class UpdateChecker
{
    public const string ReleasesApi = "https://api.github.com/repos/ichiyotsu/tokidiscover/releases/latest";
    public static string? ParseVersionFromAssetName(string name) => Regex.Match(name ?? "", @"(\d+\.\d+\.\d+(?:-(?:beta|alpha|rc|preview)[0-9A-Za-z.]*)?)").Groups[1].Value is { Length: > 0 } v ? v : null;
    public static string? ExtractLatestVersion(string releaseJson)
    {
        using var doc = JsonDocument.Parse(releaseJson); var root = doc.RootElement;
        if (root.TryGetProperty("assets", out var assets)) foreach (var a in assets.EnumerateArray())
        { var n = a.TryGetProperty("name", out var np) ? np.GetString() ?? "" : "";
          if (n.EndsWith(".exe", StringComparison.OrdinalIgnoreCase) || n.EndsWith(".msix", StringComparison.OrdinalIgnoreCase) || n.EndsWith(".apk", StringComparison.OrdinalIgnoreCase))
          { var v = ParseVersionFromAssetName(n); if (v != null) return v; } }
        return root.TryGetProperty("tag_name", out var tag) ? tag.GetString()?.TrimStart('v') : null;
    }
    public static bool IsNewer(string remote, string local)
    {
        static int[] Parts(string v) => v.TrimStart('v').Split('-', '.').Select(s => int.TryParse(s, out var n) ? n : 0).ToArray();
        var r = Parts(remote); var l = Parts(local);
        for (var i = 0; i < Math.Max(r.Length, l.Length); i++) { var a = i < r.Length ? r[i] : 0; var b = i < l.Length ? l[i] : 0; if (a != b) return a > b; }
        return false;
    }
}
