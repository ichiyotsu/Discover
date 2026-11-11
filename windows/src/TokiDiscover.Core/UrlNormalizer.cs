namespace TokiDiscover.Core;
public abstract record NavResult
{
    public sealed record Url(string Value) : NavResult;
    public sealed record Search(string Value, string Query, SearchEngine Engine) : NavResult;
    public sealed record NewTab : NavResult;
    public sealed record Blocked(string Reason) : NavResult;
}
/// <summary>Omnibox input resolution — identical logic on Android / iOS (TokiDiscoverCore) / Windows (TokiDiscover.Core).</summary>
public static class UrlNormalizer
{
    public const string NewTabUrl = "about:blank";
    private static readonly string[] Dangerous = { "javascript:", "data:", "vbscript:", "file:", "blob:" };
    public static NavResult Normalize(string raw, SearchEngine engine = SearchEngineExtensions.Default, string customTemplate = "")
    {
        var input = (raw ?? "").Trim();
        if (input.Length == 0 || input.Equals(NewTabUrl, StringComparison.OrdinalIgnoreCase)) return new NavResult.NewTab();
        var lower = input.ToLowerInvariant();
        var bad = Dangerous.FirstOrDefault(d => lower.StartsWith(d));
        if (bad != null) return new NavResult.Blocked($"已拦截危险协议: {bad}");
        if (lower.StartsWith("about:")) return new NavResult.NewTab();
        if (lower.StartsWith("http://") || lower.StartsWith("https://")) return new NavResult.Url(input);
        if (input.Any(char.IsWhiteSpace)) return new NavResult.Search(engine.SearchUrl(input, customTemplate), input, engine);
        var host = input.Split('/', '?', '#', ':')[0];
        if (host.Contains('.') || host.Equals("localhost", StringComparison.OrdinalIgnoreCase)) return new NavResult.Url("https://" + input);
        return new NavResult.Search(engine.SearchUrl(input, customTemplate), input, engine);
    }
    public static bool IsSafeScheme(string url) { var l = (url ?? "").ToLowerInvariant(); return l.StartsWith("http://") || l.StartsWith("https://") || l == NewTabUrl; }
}
