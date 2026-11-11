package com.tokidiscover.app.data
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
/** Checks GitHub Releases of ichiyotsu/tokidiscover for updates.
 *  IMPORTANT (bug fixed in Tokilive/tokimusic, do not regress): release tags have shipped mismatched asset
 *  versions before, so the version is parsed from the APK asset *filename* first, falling back to the tag. */
object UpdateChecker {
    const val RELEASES_API = "https://api.github.com/repos/ichiyotsu/tokidiscover/releases/latest"
    fun parseVersionFromAssetName(name: String): String? = Regex("""(\d+\.\d+\.\d+(?:-(?:beta|alpha|rc|preview)[0-9A-Za-z.]*)?)""").find(name)?.groupValues?.get(1)
    fun extractLatestVersion(releaseJson: String): String? {
        val o = JSONObject(releaseJson); val assets = o.optJSONArray("assets")
        if (assets != null) for (i in 0 until assets.length()) { val n = assets.getJSONObject(i).optString("name")
            if (n.endsWith(".apk", true)) parseVersionFromAssetName(n)?.let { return it } }
        return o.optString("tag_name").trimStart('v').ifBlank { null }
    }
    fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.removePrefix("v").split("-", ".").map { it.toIntOrNull() ?: 0 }
        val r = parts(remote); val l = parts(local); for (i in 0 until maxOf(r.size, l.size)) { val a = r.getOrElse(i) { 0 }; val b = l.getOrElse(i) { 0 }; if (a != b) return a > b }
        return remote.contains("beta") && !local.contains("beta") == false && remote != local && remote.substringAfter("-", "") > local.substringAfter("-", "")
    }
    suspend fun fetchLatestVersion(): String? = runCatching {
        val c = URL(RELEASES_API).openConnection() as HttpURLConnection
        c.setRequestProperty("Accept", "application/vnd.github+json"); extractLatestVersion(c.inputStream.bufferedReader().readText())
    }.getOrNull()
}
