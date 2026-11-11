import XCTest
@testable import TokiDiscoverCore
/// Mirrors the Android (Kotlin) and Windows (C#) suites — keep the three in sync.
final class UrlNormalizerTests: XCTestCase {
    func testBlankIsNewTab() { XCTAssertEqual(UrlNormalizer.normalize(""), .newTab); XCTAssertEqual(UrlNormalizer.normalize("about:blank"), .newTab) }
    func testBareDomainGetsHttps() { XCTAssertEqual(UrlNormalizer.normalize("example.com"), .url("https://example.com")) }
    func testHttpKept() { XCTAssertEqual(UrlNormalizer.normalize("http://example.com"), .url("http://example.com")) }
    func testWordsBecomeSearch() { guard case .search(let url, _) = UrlNormalizer.normalize("hello world") else { return XCTFail() }
        XCTAssertEqual(url, "https://www.bing.com/search?q=hello+world") }
    func testDangerousBlocked() { if case .blocked = UrlNormalizer.normalize("javascript:alert(1)") {} else { XCTFail() } }
    func testEngines() { XCTAssertEqual(SearchEngine.google.searchURL("a b"), "https://www.google.com/search?q=a+b")
        XCTAssertEqual(SearchEngine.custom.searchURL("a b", customTemplate: "https://s.example/?q={query}"), "https://s.example/?q=a+b") }
    func testAssetVersion() { XCTAssertEqual(UpdateChecker.parseVersion(fromAssetName: "TokiDiscover-0.2.0-ios.ipa"), "0.2.0") }
}
