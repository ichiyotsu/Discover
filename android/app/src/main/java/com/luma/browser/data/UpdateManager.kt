package com.luma.browser.data

import com.luma.browser.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val tag: String,
    val title: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String,
    val prerelease: Boolean,
    val publishedAt: String,
)

class UpdateManager {
    suspend fun check(channel: UpdateChannel, currentVersion: String = BuildConfig.VERSION_NAME): UpdateInfo? =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12_000
                    readTimeout = 20_000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "Luna-Android/${BuildConfig.VERSION_NAME}")
                }
                try {
                    if (connection.responseCode != 200) return@runCatching null
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val releases = JSONArray(body)
                    var best: UpdateInfo? = null
                    for (index in 0 until releases.length()) {
                        val item = releases.optJSONObject(index) ?: continue
                        if (item.optBoolean("draft", false)) continue
                        val prerelease = item.optBoolean("prerelease", false)
                        if (channel == UpdateChannel.STABLE && prerelease) continue
                        val tag = item.optString("tag_name")
                        val version = tag.removePrefix("v").removePrefix("V")
                        if (version.isBlank() || !isNewer(version, currentVersion)) continue
                        val assets = item.optJSONArray("assets") ?: JSONArray()
                        var apkUrl = ""
                        for (assetIndex in 0 until assets.length()) {
                            val asset = assets.optJSONObject(assetIndex) ?: continue
                            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                                apkUrl = asset.optString("browser_download_url")
                                break
                            }
                        }
                        if (apkUrl.isBlank()) continue
                        val candidate = UpdateInfo(
                            version = version,
                            tag = tag,
                            title = item.optString("name").ifBlank { "Luna $version" },
                            notes = item.optString("body"),
                            pageUrl = item.optString("html_url"),
                            apkUrl = apkUrl,
                            prerelease = prerelease,
                            publishedAt = item.optString("published_at"),
                        )
                        if (best == null || compareVersions(candidate.version, best.version) > 0) {
                            best = candidate
                        }
                    }
                    best
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }

    suspend fun download(
        info: UpdateInfo,
        target: File,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val connection = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("User-Agent", "Luna-Android/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("HTTP ${connection.responseCode}")
            }
            val total = connection.contentLengthLong
            target.parentFile?.mkdirs()
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var received = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        received += read
                        if (total > 0) onProgress((received.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            onProgress(1f)
            target
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val RELEASES_URL = "https://api.github.com/repos/ichiyotsu/Discover/releases?per_page=30"

        fun isNewer(candidate: String, current: String): Boolean =
            compareVersions(candidate, current) > 0

        private fun compareVersions(left: String, right: String): Int {
            val leftParts = left.removePrefix("v").split("-", limit = 2)
            val rightParts = right.removePrefix("v").split("-", limit = 2)
            val leftNumbers = leftParts[0].split(".").map { it.toIntOrNull() ?: 0 }
            val rightNumbers = rightParts[0].split(".").map { it.toIntOrNull() ?: 0 }
            for (index in 0 until maxOf(leftNumbers.size, rightNumbers.size)) {
                val diff = (leftNumbers.getOrNull(index) ?: 0) - (rightNumbers.getOrNull(index) ?: 0)
                if (diff != 0) return diff
            }
            val leftPre = leftParts.getOrNull(1)
            val rightPre = rightParts.getOrNull(1)
            return when {
                leftPre == null && rightPre == null -> 0
                leftPre == null -> 1
                rightPre == null -> -1
                else -> leftPre.compareTo(rightPre)
            }
        }
    }
}
