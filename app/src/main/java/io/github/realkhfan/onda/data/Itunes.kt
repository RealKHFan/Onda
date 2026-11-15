package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

/** Apple's keyless iTunes Search API: a second source of 30-second previews for songs Deezer lacks. */
object Itunes {
    private const val API = "https://itunes.apple.com"

    /** The phone's region picks the store; Apple sees it anyway from the IP. */
    private fun country(): String = Locale.getDefault().country.lowercase().takeIf { it.length == 2 } ?: "us"

    suspend fun searchSongs(query: String): List<RemoteTrack> = withContext(Dispatchers.IO) {
        parseSongs(Http.get("$API/search?term=${Http.enc(query)}&media=music&entity=song&limit=25&country=${country()}"))
    }

    suspend fun track(id: Long): RemoteTrack? = withContext(Dispatchers.IO) {
        parseSongs(Http.get("$API/lookup?id=$id&country=${country()}")).firstOrNull()
    }

    fun parseSongs(json: String): List<RemoteTrack> {
        val arr = JSONObject(json).optJSONArray("results") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            if (o.optString("wrapperType") != "track" || o.optString("kind") != "song") return@mapNotNull null
            val id = o.optLong("trackId", -1L)
            if (id <= 0L) return@mapNotNull null
            RemoteTrack(
                id = id,
                title = o.str("trackName") ?: return@mapNotNull null,
                artist = o.str("artistName") ?: "",
                album = o.str("collectionName") ?: "",
                cover = o.str("artworkUrl100")?.replace("100x100bb", "600x600bb"), // Apple serves any size by URL
                preview = o.str("previewUrl"),
                durationSec = (o.optLong("trackTimeMillis") / 1000).toInt(),
                source = Source.ITUNES,
            )
        }.distinctBy { it.id }
    }
}
