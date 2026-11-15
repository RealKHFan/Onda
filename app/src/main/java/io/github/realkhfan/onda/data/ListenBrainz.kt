package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.Match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Similar artists from ListenBrainz (open listening data), a second opinion next to Deezer's. Only artist names are sent. */
object ListenBrainz {
    // MusicBrainz asks apps to name themselves with a contact; this is the project page, nothing about the user.
    private const val AGENT = "Onda/1 ( https://github.com/RealKHFan/Onda )"
    private const val ALGO = "session_based_days_7500_session_300_contribution_5_threshold_10_limit_100_filter_True_skip_30"
    private val mbids = HashMap<String, String>() // normalised name -> MusicBrainz id ("" = none)
    private val lock = Mutex()
    private var file: File? = null

    fun load(dir: File) {
        file = File(dir, "mbids.json")
        runCatching { JSONObject(file!!.readText()).let { o -> o.keys().forEach { mbids[it] = o.getString(it) } } }
    }

    /** Names of artists like [artist], most similar first; empty when unknown or unreachable. */
    suspend fun similar(artist: String, limit: Int): List<String> = runCatching {
        val id = mbid(artist) ?: return emptyList()
        val json = withContext(Dispatchers.IO) {
            Http.get("https://labs.api.listenbrainz.org/similar-artists/json?artist_mbids=$id&algorithm=$ALGO", agent = AGENT)
        }
        parseSimilar(json).take(limit)
    }.getOrDefault(emptyList())

    private suspend fun mbid(name: String): String? = lock.withLock {
        val k = Match.norm(name)
        if (k.isEmpty()) return null
        mbids[k]?.let { return it.ifEmpty { null } }
        val json = withContext(Dispatchers.IO) {
            Http.get("https://musicbrainz.org/ws/2/artist/?query=${Http.enc("artist:\"$name\"")}&fmt=json&limit=3", agent = AGENT)
        }
        delay(1100) // MusicBrainz allows one request per second
        val id = parseMbid(json, name).orEmpty()
        mbids[k] = id
        file?.let { f -> runCatching { File(f.path + ".tmp").apply { writeText(JSONObject(mbids as Map<*, *>).toString()) }.renameTo(f) } }
        id.ifEmpty { null }
    }

    /** The exact-name, high-score match only: a wrong artist would send the mixes somewhere else entirely. */
    fun parseMbid(json: String, name: String): String? {
        val arr = JSONObject(json).optJSONArray("artists") ?: return null
        return (0 until arr.length()).map { arr.getJSONObject(it) }
            .firstOrNull { it.optInt("score") >= 90 && Match.norm(it.optString("name")) == Match.norm(name) }?.optString("id")
    }

    fun parseSimilar(json: String): List<String> {
        val arr = JSONArray(json)
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("name")?.takeIf { n -> n.isNotBlank() } }
    }
}
