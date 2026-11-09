package dev.ichiyotsu.tokidiscover.browser

import android.webkit.WebView
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONTokener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object PageTranslator {
    suspend fun translate(webView: WebView, targetTag: String): Int {
        val langRaw = webView.eval(WebScripts.PAGE_LANG).trim().trim('"')
        val target = mlLanguage(targetTag) ?: TranslateLanguage.CHINESE
        var source = mlLanguage(langRaw) ?: if (target == TranslateLanguage.ENGLISH) {
            TranslateLanguage.CHINESE
        } else {
            TranslateLanguage.ENGLISH
        }
        if (source == target) {
            source = if (target == TranslateLanguage.ENGLISH) {
                TranslateLanguage.CHINESE
            } else {
                TranslateLanguage.ENGLISH
            }
        }
        val json = webView.eval(WebScripts.EXTRACT)
        val array = JSONArray(json.ifBlank { "[]" })
        if (array.length() == 0) return 0
        val texts = buildList {
            for (i in 0 until array.length()) add(array.getString(i))
        }
        val translated = translateAll(texts, source, target)
        val out = JSONArray()
        translated.forEach { out.put(it) }
        val applied = webView.eval(WebScripts.apply(out.toString()))
        return applied.toIntOrNull() ?: translated.size
    }

    private suspend fun translateAll(texts: List<String>, source: String, target: String): List<String> {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        val client = Translation.getClient(options)
        try {
            suspendTask { client.downloadModelIfNeeded(DownloadConditions.Builder().build()) }
            return texts.map { text ->
                suspendTask { client.translate(text) }
            }
        } finally {
            client.close()
        }
    }

    fun mlLanguage(tag: String): String? {
        if (tag.isBlank()) return null
        val normalized = tag.replace('_', '-').lowercase()
        val base = normalized.substringBefore('-')
        val mapped = when (base) {
            "zh" -> TranslateLanguage.CHINESE
            "en" -> TranslateLanguage.ENGLISH
            "ja" -> TranslateLanguage.JAPANESE
            "ko" -> TranslateLanguage.KOREAN
            "fr" -> TranslateLanguage.FRENCH
            "de" -> TranslateLanguage.GERMAN
            "es" -> TranslateLanguage.SPANISH
            "ru" -> TranslateLanguage.RUSSIAN
            "pt" -> TranslateLanguage.PORTUGUESE
            "it" -> TranslateLanguage.ITALIAN
            "ar" -> TranslateLanguage.ARABIC
            "hi" -> TranslateLanguage.HINDI
            "th" -> TranslateLanguage.THAI
            "vi" -> TranslateLanguage.VIETNAMESE
            else -> TranslateLanguage.fromLanguageTag(base)
        }
        return mapped
    }

    private suspend fun <T> suspendTask(start: () -> com.google.android.gms.tasks.Task<T>): T =
        suspendCancellableCoroutine { cont ->
            start()
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }
}

private suspend fun WebView.eval(script: String): String = suspendCancellableCoroutine { cont ->
    evaluateJavascript(script) { value ->
        if (!cont.isActive) return@evaluateJavascript
        cont.resume(unwrapJs(value))
    }
}

private fun unwrapJs(value: String?): String {
    if (value.isNullOrBlank() || value == "null") return ""
    return try {
        JSONTokener(value).nextValue()?.toString().orEmpty()
    } catch (_: Exception) {
        value
    }
}
