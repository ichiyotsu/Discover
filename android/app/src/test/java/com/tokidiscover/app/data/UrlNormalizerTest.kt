package com.tokidiscover.app.data
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
/** Mirrors TokiDiscoverCore (Swift) and TokiDiscover.Core (C#) test cases — keep the three suites in sync. */
class UrlNormalizerTest {
    @Test fun blankIsNewTab() { assertEquals(NavResult.NewTab, UrlNormalizer.normalize("")); assertEquals(NavResult.NewTab, UrlNormalizer.normalize("about:blank")) }
    @Test fun bareDomainGetsHttps() { assertEquals(NavResult.Url("https://example.com"), UrlNormalizer.normalize("example.com"))
        assertEquals(NavResult.Url("https://example.com/path?q=1"), UrlNormalizer.normalize("example.com/path?q=1")) }
    @Test fun httpKept() { assertEquals(NavResult.Url("http://example.com"), UrlNormalizer.normalize("http://example.com")) }
    @Test fun wordsBecomeSearch() { val r = UrlNormalizer.normalize("hello world"); assertTrue(r is NavResult.Search)
        assertEquals("https://www.bing.com/search?q=hello+world", (r as NavResult.Search).url) }
    @Test fun singleWordWithoutDotSearches() { val r = UrlNormalizer.normalize("kotlin"); assertTrue(r is NavResult.Search) }
    @Test fun dangerousSchemesBlocked() { assertTrue(UrlNormalizer.normalize("javascript:alert(1)") is NavResult.Blocked)
        assertTrue(UrlNormalizer.normalize("data:text/html,hi") is NavResult.Blocked) }
    @Test fun engines() { assertEquals("https://www.google.com/search?q=a+b", SearchEngine.Google.searchUrl("a b"))
        assertEquals("https://duckduckgo.com/?q=a+b", SearchEngine.DuckDuckGo.searchUrl("a b"))
        assertEquals("https://www.baidu.com/s?wd=a+b", SearchEngine.Baidu.searchUrl("a b"))
        assertEquals("https://s.example/?q=a+b", SearchEngine.Custom.searchUrl("a b", "https://s.example/?q={query}")) }
    @Test fun updateVersionFromAssetName() { assertEquals("0.2.0", UpdateChecker.parseVersionFromAssetName("TokiDiscover-0.2.0-android.apk")); assertEquals("0.1.0-beta1", UpdateChecker.parseVersionFromAssetName("tokidiscover-0.1.0-beta1.apk")) }
}
