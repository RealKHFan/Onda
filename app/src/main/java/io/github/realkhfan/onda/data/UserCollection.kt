package io.github.realkhfan.onda.data

import android.content.Context
import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.core.RemoteAlbum
import io.github.realkhfan.onda.core.RemoteArtist
import io.github.realkhfan.onda.core.Source
import io.github.realkhfan.onda.core.SpotifyImport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Catalog artists you follow, catalog albums you saved, and artists you marked as AI. */
object UserCollection {

    private val _followed = MutableStateFlow<List<RemoteArtist>>(emptyList())
    val followed: StateFlow<List<RemoteArtist>> = _followed.asStateFlow()

    private val _albums = MutableStateFlow<List<RemoteAlbum>>(emptyList())
    val albums: StateFlow<List<RemoteAlbum>> = _albums.asStateFlow()

    /** Your own "This is AI" marks (normalised names). */
    private val _aiMarked = MutableStateFlow<Set<String>>(emptySet())
    val aiMarked: StateFlow<Set<String>> = _aiMarked.asStateFlow()

    /** Community list of AI-generated "artists" (CennoxX/spotify-ai-blocker, MIT), shipped with the app. */
    private var aiList: Set<String> = emptySet()

    /** "Not interested": songs (keys) and artists (normalised names) that mixes and suggestions skip. */
    private val _hidden = MutableStateFlow<Set<String>>(emptySet())
    val hidden: StateFlow<Set<String>> = _hidden.asStateFlow()
    private val _blocked = MutableStateFlow<Set<String>>(emptySet())
    val blocked: StateFlow<Set<String>> = _blocked.asStateFlow()

    fun toggleHidden(key: String) {
        _hidden.value = if (key in _hidden.value) _hidden.value - key else _hidden.value + key
        write()
    }
    /** Library songs you hid (files and server songs; catalog songs you marked "not interested" stay hidden). */
    fun isLibraryKey(key: String) = key.startsWith("content:") || key.startsWith("file:") || key.startsWith(Server.PREFIX)
    fun showHiddenSongs() { _hidden.value = _hidden.value.filterNot(::isLibraryKey).toSet(); write() }

    fun isBlocked(name: String) = Match.norm(name) in _blocked.value
    fun toggleBlocked(name: String) {
        val k = Match.norm(name)
        _blocked.value = if (k in _blocked.value) _blocked.value - k else _blocked.value + k
        write()
    }
    /** A suggestion to leave out: hidden song, artist you don't want suggested, or an AI "artist". */
    fun notWanted(r: io.github.realkhfan.onda.core.RemoteTrack) = r.key in _hidden.value || isBlocked(r.artist) || isAi(r.artist)

    private var file: File? = null

    fun load(context: Context) {
        aiList = runCatching { parseAiList(context.assets.open("ai_artists.csv").bufferedReader().readText()) }.getOrDefault(emptySet())
        file = File(context.filesDir, "collection.json")
        read()
    }

    fun parseAiList(csv: String): Set<String> =
        SpotifyImport.parseCsv(csv).drop(1).mapNotNull { r -> r.firstOrNull()?.let(Match::norm)?.takeIf { it.isNotEmpty() } }.toSet()

    fun onCommunityList(name: String) = Match.norm(name) in aiList
    fun isAi(name: String): Boolean = Match.norm(name).let { it.isNotEmpty() && (it in aiList || it in _aiMarked.value) }

    fun toggleAi(name: String) {
        val k = Match.norm(name)
        _aiMarked.value = if (k in _aiMarked.value) _aiMarked.value - k else _aiMarked.value + k
        write()
    }

    fun isFollowed(a: RemoteArtist) = _followed.value.any { it.id == a.id }
    fun toggleFollow(a: RemoteArtist) {
        _followed.value = if (isFollowed(a)) _followed.value.filterNot { it.id == a.id } else listOf(a) + _followed.value
        write()
    }

    private fun albumKey(a: RemoteAlbum) = a.source.prefix + a.ref.ifEmpty { a.id.toString() }
    fun isSaved(a: RemoteAlbum) = _albums.value.any { albumKey(it) == albumKey(a) }
    fun toggleAlbum(a: RemoteAlbum) {
        _albums.value = if (isSaved(a)) _albums.value.filterNot { albumKey(it) == albumKey(a) } else listOf(a) + _albums.value
        write()
    }

    /** After syncing with your PC; songs hidden from your library here stay as they are. */
    fun fromSync(followed: List<RemoteArtist>, albums: List<RemoteAlbum>, notInterested: List<String>, blocked: List<String>, ai: List<String>) {
        _followed.value = followed
        _albums.value = albums
        _hidden.value = _hidden.value.filter(::isLibraryKey).toSet() + notInterested
        _blocked.value = blocked.toSet()
        _aiMarked.value = ai.toSet()
        write()
    }

    private fun read() {
        val f = file ?: return
        if (!f.exists()) return
        try {
            val root = JSONObject(f.readText())
            _followed.value = objects(root.optJSONArray("artists")).map {
                RemoteArtist(it.optLong("id"), it.optString("name"), it.optString("pic").ifBlank { null })
            }
            _albums.value = objects(root.optJSONArray("albums")).map {
                RemoteAlbum(it.optLong("id"), it.optString("title"), it.optString("artist"), it.optString("cover").ifBlank { null },
                    it.optString("date").ifBlank { null }, it.optString("type").ifBlank { null }, it.optLong("artistId"),
                    Source.entries.firstOrNull { s -> s.name == it.optString("src") } ?: Source.DEEZER, it.optString("ref"))
            }
            val ai = root.optJSONArray("ai") ?: JSONArray()
            _aiMarked.value = (0 until ai.length()).map { ai.getString(it) }.toSet()
            _hidden.value = strings(root.optJSONArray("hidden"))
            _blocked.value = strings(root.optJSONArray("blocked"))
        } catch (_: Exception) {
            runCatching { f.copyTo(File(f.path + ".bad"), overwrite = true) } // never silently lose the user's collection
        }
    }

    private fun write() {
        val f = file ?: return
        runCatching {
            val root = JSONObject()
                .put("artists", JSONArray(_followed.value.map { JSONObject().put("id", it.id).put("name", it.name).put("pic", it.picture) }))
                .put("albums", JSONArray(_albums.value.map {
                    JSONObject().put("id", it.id).put("title", it.title).put("artist", it.artist).put("cover", it.cover).put("date", it.date)
                        .put("type", it.type).put("artistId", it.artistId).put("src", it.source.name).put("ref", it.ref)
                }))
                .put("ai", JSONArray(_aiMarked.value.toList()))
                .put("hidden", JSONArray(_hidden.value.toList()))
                .put("blocked", JSONArray(_blocked.value.toList()))
            val tmp = File(f.path + ".tmp")
            tmp.writeText(root.toString())
            tmp.renameTo(f)
        }
    }

    private fun strings(a: JSONArray?): Set<String> = if (a == null) emptySet() else (0 until a.length()).map { a.getString(it) }.toSet()

    private fun objects(a: JSONArray?): List<JSONObject> = if (a == null) emptyList() else (0 until a.length()).mapNotNull { a.optJSONObject(it) }
}
