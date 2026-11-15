package io.github.realkhfan.onda.desktop

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Source
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.Deezer
import io.github.realkhfan.onda.data.FreeCatalogs
import io.github.realkhfan.onda.data.FullCopies
import io.github.realkhfan.onda.data.Lrclib
import io.github.realkhfan.onda.data.Subsonic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.UUID

/** Everything the window shows: settings, your files, your server, your collection. */
class AppState(private val scope: CoroutineScope) {
    var settings by mutableStateOf(Store.settings()); private set
    private var localCache = Store.localCache()
    var local by mutableStateOf(localCache.values.map { it.second }); internal set
    var serverSongs by mutableStateOf(if (settings.server != null) Store.serverSongs() else emptyList()); private set
    var serverPlaylists by mutableStateOf(if (settings.server != null) Store.serverPlaylists() else emptyList()); private set
    var collection by mutableStateOf(Store.collection()); private set
    var scanning by mutableStateOf(false); private set
    var syncing by mutableStateOf(false); private set
    var serverNote by mutableStateOf<String?>(null)
    var recent by mutableStateOf(Store.recent()); private set

    /** Catalog songs shown or played this run, so the heart and menus can save them. */
    private val seen = HashMap<String, RemoteTrack>()

    init { Lrclib.init(Store.cache) }

    private val serverTracks by derivedStateOf { Navidrome.tracks(serverSongs, settings.server, settings.kbps) }

    /** Your files and server songs; a local copy wins. */
    val library by derivedStateOf { Match.mergeLibrary(local, serverTracks) }

    /** Every song by id, server copies of your files included, so saved ids always resolve. */
    val byId by derivedStateOf { (serverTracks + local).associateBy { it.id } }

    /** Albums from your folders, newest files first. */
    val recentlyAdded by derivedStateOf {
        val added = localCache.values.associate { it.second.id to it.first }
        io.github.realkhfan.onda.core.Grouping.albums(local).sortedByDescending { a -> a.tracks.maxOf { added[it.id] ?: 0L } }.take(14)
    }

    fun played(i: Player.Item) {
        if (recent.firstOrNull()?.id == i.id) return
        recent = (listOf(i) + recent.filterNot { it.id == i.id }).take(20)
        Store.saveRecent(recent)
    }

    /** A saved catalog song as a list row: your own copy once you have it, else the preview. */
    fun trackFor(id: String): Track? = byId[id] ?: collection.remote.firstOrNull { it.key == id }?.let { r -> own(r) ?: asTrack(r) }

    private fun own(r: RemoteTrack) = library.firstOrNull { Match.same(it.title, it.artist, r.title, r.artist) }
    fun asTrack(r: RemoteTrack) = Track(r.key, "", r.title, r.artist, r.album, r.artist, 0, r.durationSec * 1000L, PREVIEW, r.cover)
    fun isPreviewRow(t: Track) = t.folderName == PREVIEW && t.uri.isEmpty()

    /** Can the heart / playlists hold this id? (Your songs, or a catalog song seen this run.) */
    fun remember(r: RemoteTrack) { seen[r.key] = r }
    fun canSave(id: String) = byId.containsKey(id) || seen.containsKey(id) || collection.remote.any { it.key == id }

    private fun withRemote(id: String, c: Store.Collection): Store.Collection {
        val r = seen[id] ?: return c
        return if (c.remote.any { it.key == id }) c else c.copy(remote = c.remote + r.copy(preview = null))
    }

    fun start() {
        if (settings.folders.isNotEmpty()) rescan()
        if (settings.server != null) sync()
    }

    private fun update(s: Store.Settings) { settings = s; Store.save(s) }
    private fun change(c: Store.Collection) { collection = c; Store.saveCollection(c) }

    /* ---------- Folders ---------- */

    fun addFolder(path: String) {
        if (path in settings.folders) return
        update(settings.copy(folders = settings.folders + path)); rescan()
    }

    fun removeFolder(path: String) { update(settings.copy(folders = settings.folders - path)); rescan() }

    fun rescan() {
        if (scanning) return
        scanning = true
        scope.launch {
            try {
                val found = withContext(Dispatchers.IO) { Scanner.scan(settings.folders, localCache).also(Store::saveLocal) }
                localCache = found
                local = found.values.map { it.second }
            } finally {
                scanning = false
            }
        }
    }

    /* ---------- Server ---------- */

    /** Returns an error to show, or null when connected. */
    suspend fun connect(url: String, user: String, password: String): String? = try {
        update(settings.copy(server = Navidrome.connect(url, user, password)))
        sync(); null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Subsonic.ApiError) {
        if (e.code == 40) "Wrong username or password." else e.message
    } catch (e: Exception) {
        "Couldn't reach the server (${e.message ?: e.javaClass.simpleName})."
    }

    fun disconnect() {
        update(settings.copy(server = null))
        serverSongs = emptyList(); serverPlaylists = emptyList()
        Store.saveServerSongs(emptyList()); Store.saveServerPlaylists(emptyList())
    }

    fun setKbps(k: Int) = update(settings.copy(kbps = k))
    fun setVolume(v: Double) = update(settings.copy(volume = v))

    fun sync() {
        val cfg = settings.server ?: return
        if (syncing) return
        syncing = true; serverNote = null
        scope.launch {
            try {
                val songs = Navidrome.songs(cfg)
                serverSongs = songs
                withContext(Dispatchers.IO) { Store.saveServerSongs(songs) }
                val lists = Navidrome.playlists(cfg)
                serverPlaylists = lists
                withContext(Dispatchers.IO) { Store.saveServerPlaylists(lists) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                serverNote = "Your server isn't reachable right now. Showing the last list."
            } finally {
                syncing = false
            }
        }
    }

    /* ---------- Collection ---------- */

    fun isLiked(id: String) = id in collection.liked
    fun toggleLike(id: String) = change(withRemote(id, collection.copy(liked = if (isLiked(id)) collection.liked - id else listOf(id) + collection.liked)))
    val likedTracks: List<Track> get() = collection.liked.mapNotNull(::trackFor)

    fun createPlaylist(name: String, first: List<String> = emptyList()): String {
        val id = UUID.randomUUID().toString()
        var c = collection.copy(playlists = collection.playlists + Store.Playlist(id, name.ifBlank { "New playlist" }, first))
        first.forEach { c = withRemote(it, c) }
        change(c)
        return id
    }

    fun renamePlaylist(id: String, name: String) =
        change(collection.copy(playlists = collection.playlists.map { if (it.id == id && name.isNotBlank()) it.copy(name = name) else it }))

    fun deletePlaylist(id: String) = change(collection.copy(playlists = collection.playlists.filterNot { it.id == id }))

    fun addToPlaylist(id: String, trackId: String) = change(withRemote(trackId, collection.copy(playlists = collection.playlists.map {
        if (it.id == id && trackId !in it.trackIds) it.copy(trackIds = it.trackIds + trackId) else it
    })))

    fun removeFromPlaylist(id: String, trackId: String) =
        change(collection.copy(playlists = collection.playlists.map { if (it.id == id) it.copy(trackIds = it.trackIds - trackId) else it }))

    fun playlistTracks(id: String): List<Track> = collection.playlists.firstOrNull { it.id == id }?.trackIds?.mapNotNull(::trackFor).orEmpty()
    fun serverPlaylistTracks(p: Subsonic.Playlist): List<Track> = p.songIds.mapNotNull { byId[Navidrome.PREFIX + it] }

    fun isWanted(r: RemoteTrack) = collection.want.any { it.key == r.key }
    fun toggleWant(r: RemoteTrack) = change(collection.copy(want =
        if (isWanted(r)) collection.want.filterNot { it.key == r.key } else listOf(r.copy(preview = null)) + collection.want))

    /* ---------- Playing ---------- */

    fun item(t: Track): Player.Item {
        if (isPreviewRow(t)) collection.remote.firstOrNull { it.key == t.id }?.let { r -> item(r)?.let { return it } }
        return Player.Item(t.id, t.title, t.artist, t.album, t.artUri, t.uri, durationMs = t.durationMs)
    }

    /** A catalog song: your own copy if you have one, a free full song, or a Deezer preview (link fetched when it plays). */
    fun item(r: RemoteTrack): Player.Item? {
        seen[r.key] = r
        library.firstOrNull { Match.same(it.title, it.artist, r.title, r.artist) }?.let { return item(it) }
        if (r.source == Source.DEEZER) {
            return Player.Item(r.key, r.title, r.artist, r.album, r.cover, "", preview = true, deezerId = r.id, durationMs = r.durationSec * 1000L)
        }
        val url = FreeCatalogs.stream(r) ?: r.preview ?: return null
        return Player.Item(r.key, r.title, r.artist, r.album, r.cover, url, preview = !r.source.full, durationMs = r.durationSec * 1000L)
    }

    companion object { const val PREVIEW = "Preview" }

    private val fullCopies = HashMap<Long, Player.Resolved?>()

    /** For the player: a Deezer song plays as a free full copy when the artist's own account has one, else a fresh preview. */
    suspend fun resolve(i: Player.Item): Player.Resolved {
        if (i.deezerId <= 0) return Player.Resolved(i.uri)
        val known = synchronized(fullCopies) { fullCopies.containsKey(i.deezerId) }
        val full = if (known) synchronized(fullCopies) { fullCopies[i.deezerId] } else {
            // Never hold a preview up for long: after 3 s it plays the preview and asks again next time.
            withTimeoutOrNull(3000) {
                runCatching { FullCopies.find(i.deezerId, audius = true, jamendoId = "") }.getOrNull()
                    ?.let { c -> FreeCatalogs.stream(c)?.let { Player.Resolved(it, c.source.label) } }
                    .also { synchronized(fullCopies) { fullCopies[i.deezerId] = it } }
            }
        }
        return full ?: Player.Resolved(Deezer.previewNow(i.deezerId) ?: throw IOException("No preview"))
    }
}
