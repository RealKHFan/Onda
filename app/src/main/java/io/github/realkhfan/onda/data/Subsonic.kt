package io.github.realkhfan.onda.data

import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.security.MessageDigest

/** OpenSubsonic API (Navidrome, Gonic, Airsonic…): token login, full-library listing, stream and cover URLs. */
object Subsonic {

    data class Config(val url: String, val user: String, val salt: String, val token: String)

    data class Song(
        val id: String,
        val title: String,
        val artist: String,
        val album: String,
        val albumArtist: String,
        val track: Int,
        val durationSec: Int,
        val coverArt: String?,
    )

    /** The server's own refusal. 40 = wrong login, 41 = token login not supported (LDAP/proxy setups). */
    class ApiError(val code: Int, message: String) : IOException(message)

    /** md5(password + salt). Onda stores this token, never the password. */
    fun token(password: String, salt: String): String = md5(password + salt)

    // One salt per login (not per request) keeps cover URLs stable, so the image cache works.
    private fun auth(c: Config) = "u=${Http.enc(c.user)}&t=${c.token}&s=${c.salt}&v=1.16.1&c=Onda&f=json"

    fun url(c: Config, method: String, params: String = ""): String =
        "${c.url}/rest/$method?" + (if (params.isEmpty()) "" else "$params&") + auth(c)

    /** [maxKbps] 0 streams the original file; otherwise the server converts to MP3 at that rate. */
    fun streamUrl(c: Config, id: String, maxKbps: Int): String =
        url(c, "stream", "id=${Http.enc(id)}" + if (maxKbps > 0) "&maxBitRate=$maxKbps&format=mp3" else "")

    /** The original file, unconverted. */
    fun downloadUrl(c: Config, id: String): String = url(c, "download", "id=${Http.enc(id)}")

    fun coverUrl(c: Config, coverArt: String): String = url(c, "getCoverArt", "id=${Http.enc(coverArt)}&size=600")

    /** The inner response object; throws [ApiError] with the server's reason when it says no. */
    fun unwrap(json: String): JSONObject {
        val r = JSONObject(json).optJSONObject("subsonic-response") ?: throw IOException("Not a Subsonic/Navidrome server")
        if (r.optString("status") != "ok") {
            val e = r.optJSONObject("error")
            throw ApiError(e?.optInt("code") ?: 0, e?.optString("message").orEmpty().ifBlank { "Server refused the request" })
        }
        return r
    }

    fun parseSongs(r: JSONObject): List<Song> {
        val arr = r.optJSONObject("searchResult3")?.optJSONArray("song") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            if (o.optBoolean("isDir")) return@mapNotNull null
            val artist = o.str("artist") ?: o.str("displayArtist") ?: ""
            Song(
                id = o.str("id") ?: return@mapNotNull null,
                title = o.str("title") ?: return@mapNotNull null,
                artist = artist,
                album = o.str("album") ?: "",
                albumArtist = o.str("displayAlbumArtist") ?: artist,
                track = o.optInt("track"),
                durationSec = o.optInt("duration"),
                coverArt = o.str("coverArt"),
            )
        }
    }

    /** "192.168.1.20:4533" -> "http://192.168.1.20:4533"; a pasted web-UI link like ".../app/#/album/x" is trimmed too. */
    fun normalizeUrl(input: String): String {
        var u = input.trim().trimEnd('/')
        if (!u.contains("://")) u = "http://$u"
        u = u.substringBefore("/app/").substringBefore("/app#").removeSuffix("/app").removeSuffix("/rest")
        return u.trimEnd('/')
    }

    /** Addresses that stay inside your home or VPN: LAN ranges, localhost, .local names, bare host names, Tailscale. */
    fun isPrivateHost(url: String): Boolean {
        val h = runCatching { URI(url).host }.getOrNull()?.lowercase() ?: return false
        if (h.startsWith("[")) return h.startsWith("[fd") || h.startsWith("[fc") || h.startsWith("[fe80") || h == "[::1]"
        if (h == "localhost" || !h.contains('.') || listOf(".local", ".lan", ".home.arpa", ".internal").any { h.endsWith(it) }) return true
        val p = h.split('.').map { it.toIntOrNull() ?: return false }
        if (p.size != 4) return false
        return p[0] == 10 || p[0] == 127 || (p[0] == 192 && p[1] == 168) || (p[0] == 172 && p[1] in 16..31) ||
            (p[0] == 100 && p[1] in 64..127)
    }

    private fun md5(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
