using System.Text.Json;

namespace TokiDiscover.Browser;

internal sealed record SearchEngine(string Id, string Label, string Template);

internal static class Engines
{
    public static readonly SearchEngine[] All =
    [
        new("google", "Google", "https://www.google.com/search?q={query}"),
        new("bing", "Bing", "https://www.bing.com/search?q={query}"),
        new("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q={query}"),
        new("baidu", "百度", "https://www.baidu.com/s?wd={query}"),
    ];

    public static SearchEngine ById(string? id) =>
        All.FirstOrDefault(engine => engine.Id == id) ?? All[0];
}

internal static class Omnibox
{
    public const string Home = "toki://home";

    public static string Resolve(string raw, SearchEngine engine)
    {
        var text = raw.Trim();
        if (text.Length == 0 || text.Equals(Home, StringComparison.OrdinalIgnoreCase) ||
            text.Equals("about:home", StringComparison.OrdinalIgnoreCase))
        {
            return Home;
        }

        var candidate = text.StartsWith("//", StringComparison.Ordinal) ? "https:" + text : text;
        if (Uri.TryCreate(candidate, UriKind.Absolute, out var absolute) &&
            (absolute.Scheme == Uri.UriSchemeHttp || absolute.Scheme == Uri.UriSchemeHttps) &&
            !string.IsNullOrEmpty(absolute.Host))
        {
            return absolute.ToString();
        }

        var looksLikeHost = !text.Contains(' ') && !text.Contains('　') &&
                            (text.StartsWith("localhost", StringComparison.OrdinalIgnoreCase) || text.Contains('.') || text.Contains(':'));
        if (looksLikeHost)
        {
            return "https://" + text;
        }

        return engine.Template.Replace("{query}", Uri.EscapeDataString(text), StringComparison.Ordinal);
    }

    public static string TranslateUrl(string pageUrl)
    {
        var culture = CultureInfoName();
        var tl = culture.StartsWith("zh", StringComparison.OrdinalIgnoreCase) ? "zh-CN" : culture;
        if (string.IsNullOrWhiteSpace(tl)) tl = "en";
        return "https://translate.google.com/translate?sl=auto&tl=" + Uri.EscapeDataString(tl) +
               "&u=" + Uri.EscapeDataString(pageUrl);
    }

    private static string CultureInfoName() =>
        System.Globalization.CultureInfo.CurrentUICulture.Name;
}

internal sealed class SettingsStore
{
    private readonly string _path;
    public string EngineId { get; private set; } = "google";

    public SettingsStore()
    {
        var dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "TokiDiscover");
        Directory.CreateDirectory(dir);
        _path = Path.Combine(dir, "settings.json");
        try
        {
            if (File.Exists(_path))
            {
                using var doc = JsonDocument.Parse(File.ReadAllText(_path));
                if (doc.RootElement.TryGetProperty("engine", out var engine))
                {
                    EngineId = engine.GetString() ?? "google";
                }
            }
        }
        catch
        {
            EngineId = "google";
        }
    }

    public void SetEngine(string id)
    {
        EngineId = id;
        File.WriteAllText(_path, JsonSerializer.Serialize(new { engine = id }));
    }
}
