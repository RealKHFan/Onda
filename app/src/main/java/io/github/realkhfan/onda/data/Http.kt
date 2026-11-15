package io.github.realkhfan.onda.data

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Plain GETs for the catalog APIs. Call from Dispatchers.IO. */
internal object Http {
    fun get(url: String, readTimeoutMs: Int = 8000): String = getOrNull(url, readTimeoutMs) ?: throw IOException("HTTP 404")

    /** Like [get], but "not found" (404) is null instead of an error. */
    fun getOrNull(url: String, readTimeoutMs: Int = 8000): String? {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = readTimeoutMs
        c.setRequestProperty("User-Agent", "Onda") // the default agent would send the phone model
        try {
            val code = c.responseCode
            if (code == 404) return null
            if (code != 200) throw IOException("HTTP $code")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}

/** A non-blank string, or null for missing / JSON null / "". */
internal fun JSONObject.str(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
