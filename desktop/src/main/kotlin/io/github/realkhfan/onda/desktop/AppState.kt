package io.github.realkhfan.onda.desktop

import io.github.realkhfan.onda.core.LinkSync

import io.github.realkhfan.onda.core.SpotifyImport
import io.github.realkhfan.onda.core.RemoteArtist
import io.github.realkhfan.onda.core.RemoteAlbum
import io.github.realkhfan.onda.core.NewReleases
import io.github.realkhfan.onda.core.Discovery
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
import io.github.realkhfan.onda.data.ListenBrainz
import io.github.realkhfan.onda.data.Lrclib
import io.github.realkhfan.onda.data.Overrides
import io.github.realkhfan.onda.data.Subsonic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.UUID

/** Everything the window shows: settings, your files, your server, your collection. */
class AppState(private val scope: CoroutineScope) {
    var settings by mutableStateOf(Store.settings()); private set
    private var localCache by mutableStateOf(Store.localCache())
    /** When each file was added (its modification time), for "Recently added". */
    val addedAt by derivedStateOf { localCache.values.associate { it.second.id to it.first } }
    var local by mutableStateOf(localCache.values.map { it.second }); internal set
    var serverSongs by mutableStateOf(if (settings.server != null) Store.serverSongs() else emptyList()); private set
    var serverPlaylists by mutableStateOf(if (settings.server != null) Store.serverPlaylists() else emptyList()); private set
    var collection by mutableStateOf(Store.collection()); private set
    var scanning by mutableStateOf(false); private set
    var syncing by mutableStateOf(false); private set
    var serverNote by mutableStateOf<String?>(null)
    var recent by mutableStateOf(Store.recent()); private set
    var mixes by mutableStateOf(Store.mixes()); private set
    var building by mutableStateOf(setOf<Discovery.Kind>()); private set
    var releases by mutableStateOf(Store.releases()); private set
    /** Your listening history (this PC only): for mixes, Most played and Your stats. */
    var plays by mutableStateOf(Store.plays()); private set

    /** Community list of AI-generated "artists" (CennoxX/spotify-ai-blocker, MIT), shipped with the app. */
    private val aiList: Set<String> = runCatching {
        SpotifyImport.parseCsv(javaClass.getResourceAsStream("/ai_artists.csv")!!.bufferedReader().readText()).drop(1)
            .mapNotNull { r -> r.firstOrNull()?.let(Match::norm)?.takeIf { it.isNotEmpty() } }.toSet()
    }.getOrDefault(emptySet())

    /** Catalog songs shown or played this run, so the heart and menus can save them. */
    private val seen = HashMap<String, RemoteTrack>()

    init { accentState.value = androidx.compose.ui.graphics.Color(settings.accent.toInt()); Lrclib.init(Store.cache); Overrides.load(Store.data); ListenBrainz.load(Store.data) }

    /** Details accepted from the online match (shown instead of the file's tags) and artist photos found with it. */
    var meta by mutableStateOf(Overrides.meta.value); private set
    var artistPics by mutableStateOf(Overrides.artistPics.value); private set

    private val serverTracks by derivedStateOf { Navidrome.tracks(serverSongs, settings.server, settings.kbps) }

    /** Your files and server songs; a local copy wins. */
    /** Hidden songs (duplicates you hid) stay out of the library; playlists that hold them still play them. */
    val library by derivedStateOf { Overrides.apply(Match.mergeLibrary(local, serverTracks), meta).filterNot { it.id in collection.hidden } }

    /** Every song by id, server copies of your files included, so saved ids always resolve. */
    val byId by derivedStateOf { Overrides.apply(serverTracks + local, meta).associateBy { it.id } }

    /** Albums from your folders, newest files first. */
    val recentlyAdded by derivedStateOf {
        val added = localCache.values.associate { it.second.id to it.first }
        io.github.realkhfan.onda.core.Grouping.albums(Overrides.apply(local, meta)).sortedByDescending { a -> a.tracks.maxOf { added[it.id] ?: 0L } }.take(14)
    }

    fun played(i: Player.Item) {
        if (recent.firstOrNull()?.id == i.id) return
        recent = (listOf(i) + recent.filterNot { it.id == i.id }).take(20)
        Store.saveRecent(recent)
        plays = (plays + Discovery.Play(i.id, i.artist, i.title, System.currentTimeMillis())).takeLast(LinkSync.MAX_PLAYS)
        Store.savePlays(plays)
    }

    /* ---------- Taste, filters, follows ---------- */

    fun onAiList(name: String) = Match.norm(name) in aiList
    fun isAi(name: String) = Match.norm(name).let { it.isNotEmpty() && (it in aiList || it in collection.ai) }
    fun toggleAi(name: String) = Match.norm(name).let { k -> change(collection.copy(ai = if (k in collection.ai) collection.ai - k else collection.ai + k)) }
    fun isBlocked(name: String) = Match.norm(name) in collection.blocked
    fun toggleBlocked(name: String) = Match.norm(name).let { k -> change(collection.copy(blocked = if (k in collection.blocked) collection.blocked - k else collection.blocked + k)) }
    fun isHidden(key: String) = key in collection.hidden
    /** Brings back library songs hidden as duplicates (suggestions you hid stay hidden). */
    fun showHiddenSongs() = change(collection.copy(hidden = collection.hidden.filterNot { it.startsWith("file:") || it.startsWith(Navidrome.PREFIX) }))
    fun toggleHidden(key: String) = change(collection.copy(hidden = if (key in collection.hidden) collection.hidden - key else collection.hidden + key))
    /** Left out of suggestions: a hidden song, an artist you don't want suggested, or an AI "artist". */
    fun notWanted(r: RemoteTrack) = isHidden(r.key) || isBlocked(r.artist) || isAi(r.artist)

    fun isFollowed(a: RemoteArtist) = collection.followed.any { it.id == a.id }
    fun toggleFollow(a: RemoteArtist) = change(collection.copy(followed = if (isFollowed(a)) collection.followed.filterNot { it.id == a.id } else listOf(a) + collection.followed))
    fun isSaved(a: RemoteAlbum) = collection.albums.any { it.id == a.id && it.source == a.source && it.ref == a.ref }
    fun toggleAlbum(a: RemoteAlbum) = change(collection.copy(albums = if (isSaved(a)) collection.albums.filterNot { it.id == a.id && it.ref == a.ref } else listOf(a) + collection.albums))

    fun setAutoplay(on: Boolean) = update(settings.copy(autoplay = on))
    fun setBackground(on: Boolean) = update(settings.copy(background = on))
    fun setReleaseAlerts(on: Boolean) = update(settings.copy(releaseAlerts = on))
    fun setApple(on: Boolean) = update(settings.copy(apple = on))
    fun setListenBrainz(on: Boolean) = update(settings.copy(listenBrainz = on))
    fun setBackupDir(dir: String?) = update(settings.copy(backupDir = dir))
    fun setEqualizer(gains: List<Double>, preset: String) = update(settings.copy(eq = gains, eqPreset = preset))
    fun setSoftware(on: Boolean) = update(settings.copy(software = on))
    fun setSongNotify(on: Boolean) = update(settings.copy(songNotify = on))
    fun rememberSearch(q: String) {
        val t = q.trim().takeIf { it.length >= 2 } ?: return
        update(settings.copy(searches = (listOf(t) + settings.searches.filterNot { it.equals(t, ignoreCase = true) }).take(10)))
    }
    fun forgetSearch(q: String?) = update(settings.copy(searches = if (q == null) emptyList() else settings.searches - q))

    /** Name, description and cover of a playlist; a new cover image is copied, so moving the original doesn't lose it. */
    fun editPlaylist(id: String, name: String, description: String, coverFrom: java.io.File?, removeCover: Boolean) {
        val pl = collection.playlists.firstOrNull { it.id == id } ?: return
        var cover = if (removeCover) null else pl.cover
        if (removeCover || coverFrom != null) pl.cover?.let { java.io.File(it).delete() }
        if (coverFrom != null) cover = runCatching {
            val dir = java.io.File(Store.data, "playlist_covers").apply { mkdirs() }
            java.io.File(dir, "$id-${System.currentTimeMillis()}.${coverFrom.extension.lowercase().ifBlank { "jpg" }}").also { coverFrom.copyTo(it) }.path
        }.getOrNull()
        change(collection.copy(playlists = collection.playlists.map {
            if (it.id == id) it.copy(name = name.trim().ifBlank { pl.name }, description = description.trim(), cover = cover) else it
        }))
    }

    /** Songs like a playlist's (its most frequent artists), not already in it; kept for this session. */
    private val recommended = HashMap<String, List<RemoteTrack>>()
    suspend fun recommendFor(playlistId: String, refresh: Boolean = false): List<RemoteTrack> {
        if (!refresh) recommended[playlistId]?.let { return it }
        val tracks = playlistTracks(playlistId)
        val inIt = tracks.flatMap { listOf(it.id, Match.norm(it.artist) + "|" + Match.norm(it.title)) }.toSet()
        val artists = tracks.groupingBy { it.artist }.eachCount().entries.sortedByDescending { it.value }.map { it.key }.shuffled().take(5)
        if (artists.isEmpty()) return emptyList()
        val found = runCatching {
            Discover.similar(artists, settings.listenBrainz, skip = { it.key in inIt || Match.norm(it.artist) + "|" + Match.norm(it.title) in inIt || notWanted(it) }, n = 10)
        }.getOrDefault(emptyList())
        found.forEach { seen[it.key] = it }
        recommended[playlistId] = found
        return found
    }

    fun setAlbumGain(on: Boolean) = update(settings.copy(albumGain = on))
    fun setOutput(device: String?) = update(settings.copy(output = device))

    /** Ids of the songs on the same album, for album-wide loudness. */
    private val albumIds by derivedStateOf { library.groupBy { it.albumKey }.mapValues { e -> e.value.map { it.id } } }
    fun albumMates(id: String): List<String> = if (!settings.albumGain) emptyList() else byId[id]?.let { albumIds[it.albumKey] }.orEmpty()

    /** Where long songs (over 20 minutes) were left: id to ms, the newest 200. */
    private val resumeFile = java.io.File(Store.data, "resume.json")
    private val resume = runCatching { org.json.JSONObject(resumeFile.readText()).let { o -> o.keySet().associateWith { o.getLong(it) } } }.getOrDefault(emptyMap()).toMutableMap()
    fun resumeAt(id: String): Long = resume[id] ?: 0
    fun leftAt(id: String, pos: Long, dur: Long) {
        if (dur < 20 * 60_000) return
        if (pos in 60_000..(dur - 60_000)) resume[id] = pos else resume.remove(id) // near the start or end: start over next time
        while (resume.size > 200) resume.remove(resume.keys.first())
        runCatching { java.io.File(resumeFile.path + ".tmp").apply { writeText(org.json.JSONObject(resume as Map<*, *>).toString()) }.renameTo(resumeFile) }
    }

    /** Built-in playlists from your own library and history. */
    enum class Smart(val title: String, val about: String) {
        ADDED("Recently added", "The newest files in your music folders"),
        TOP("Most played", "Your 100 most played songs"),
        NEVER("Never played", "Songs in your library you haven't played yet"),
        FORGOTTEN("Forgotten favourites", "Liked songs you haven't played in two months"),
        REPEAT("On repeat", "What you've played most in the last 30 days"),
    }

    fun smart(kind: Smart): List<Track> {
        val counts = playCounts
        val last = plays.groupBy { it.key }.mapValues { e -> e.value.maxOf { it.atMs } }
        return when (kind) {
            Smart.ADDED -> library.filter { it.id in addedAt }.sortedByDescending { addedAt[it.id] }.take(100)
            Smart.TOP -> library.filter { (counts[it.id] ?: 0) > 0 }.sortedByDescending { counts[it.id] }.take(100)
            Smart.NEVER -> library.filter { it.id !in counts }.sortedBy { it.title.lowercase() }
            Smart.REPEAT -> (System.currentTimeMillis() - 30 * 86_400_000L).let { since ->
                plays.filter { it.atMs >= since }.groupingBy { it.key }.eachCount().entries.filter { it.value >= 2 }
                    .sortedByDescending { it.value }.take(30).mapNotNull { trackFor(it.key) }
            }
            Smart.FORGOTTEN -> (System.currentTimeMillis() - 60 * 86_400_000L).let { since ->
                collection.liked.mapNotNull(::trackFor).filter { (last[it.id] ?: 0) < since }.sortedBy { last[it.id] ?: 0 }
            }
        }
    }

    fun setCache(on: Boolean) { update(settings.copy(cache = on)); if (!on) SongCache.clear() }
    fun setAccent(argb: Long) { update(settings.copy(accent = argb)); accentState.value = androidx.compose.ui.graphics.Color(argb.toInt()) }
    fun setCompact(on: Boolean) = update(settings.copy(compact = on))
    fun setPlaylistOrder(order: String) = update(settings.copy(playlistOrder = order))
    fun setLyricsScale(scale: Float) = update(settings.copy(lyricsScale = (Math.round(scale * 10) / 10f).coerceIn(0.6f, 1.8f)))

    /** A copy of a playlist ("… (copy)"), with its own copy of a custom cover. */
    fun duplicatePlaylist(id: String): String? {
        val pl = collection.playlists.firstOrNull { it.id == id } ?: return null
        val newId = UUID.randomUUID().toString()
        val cover = pl.cover?.let { c -> runCatching { java.io.File(c).let { f -> java.io.File(f.parentFile, "$newId.${f.extension}").also { f.copyTo(it) }.path } }.getOrNull() }
        change(collection.copy(playlists = collection.playlists + pl.copy(id = newId, name = "${pl.name} (copy)", cover = cover)))
        return newId
    }

    /** Your playlists in the sidebar's order: pinned first, then by your choice. */
    val sidebarPlaylists by derivedStateOf {
        val pins = settings.pinned
        val ordered = when (settings.playlistOrder) {
            "A–Z" -> collection.playlists.sortedBy { it.name.lowercase() }
            "Most songs" -> collection.playlists.sortedByDescending { it.trackIds.size }
            else -> collection.playlists
        }
        ordered.sortedBy { pins.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }

    /** How many library songs you hid (as duplicates or on purpose). */
    val hiddenSongs get() = collection.hidden.count { it.startsWith("file:") || it.startsWith(Navidrome.PREFIX) }

    fun setWindow(box: List<Int>, maximized: Boolean) { if (box != settings.window || maximized != settings.maximized) update(settings.copy(window = box, maximized = maximized)) }

    /** Files from outside your library (opened from the file manager, dropped on the window) as songs to play. */
    fun itemsForFiles(uris: List<String>): List<Player.Item> = uris.mapNotNull { u ->
        byId[u]?.let(::item) ?: runCatching { java.io.File(java.net.URI(u)) }.getOrNull()?.takeIf(Scanner::isAudio)?.let { item(Scanner.read(it, it.parentFile?.name ?: "")) }
    }

    /** Your most played artists of the last three months (names), for the top mixes. */
    fun topArtists(n: Int): List<String> = (System.currentTimeMillis() - 90 * 86_400_000L).let { since ->
        // A file without an artist tag has no artist to make a mix of.
        plays.filter { it.atMs >= since && !isBlocked(it.artist) && Match.norm(it.artist) !in setOf("", "unknown artist", "unknown", "various artists") }.groupingBy { it.artist }.eachCount().entries.sortedByDescending { it.value }.take(n).map { it.key }
    }

    /** An artist's mix: their songs in your library mixed with songs of similar artists (for this session). */
    private val artistMixes = HashMap<String, List<Track>>()
    suspend fun artistMix(name: String): List<Track> {
        artistMixes[name]?.let { return it }
        val own = library.filter { Match.norm(it.artist) == Match.norm(name) }.shuffled().take(10)
        val ownKeys = own.map { Match.norm(it.artist) + "|" + Match.norm(it.title) }.toSet()
        val similar = runCatching { Discover.similar(listOf(name), settings.listenBrainz, skip = { notWanted(it) || Match.norm(it.artist) + "|" + Match.norm(it.title) in ownKeys }, n = 20) }
            .getOrDefault(emptyList()).onEach { seen[it.key] = it }.map { own(it) ?: asTrack(it) }
        val mixed = ArrayList<Track>()
        val a = own.iterator(); val b = similar.iterator()
        while (a.hasNext() || b.hasNext()) { if (a.hasNext()) mixed += a.next(); repeat(2) { if (b.hasNext()) mixed += b.next() } }
        return mixed.distinctBy { it.id }.also { artistMixes[name] = it }
    }

    /** New releases as songs: the tracks of the newest releases of your artists (for this session). */
    private var radar: List<RemoteTrack>? = null
    suspend fun releaseRadar(): List<RemoteTrack> {
        radar?.let { return it }
        val tracks = releases.items.take(12).flatMap { a -> runCatching { Deezer.album(a.id).second }.getOrDefault(emptyList()).take(3) }
            .filterNot(::notWanted).onEach { seen[it.key] = it }
        return tracks.also { if (it.isNotEmpty()) radar = it }
    }

    fun setGapless(on: Boolean) = update(settings.copy(gapless = on))
    fun togglePin(id: String) = update(settings.copy(pinned = if (id in settings.pinned) settings.pinned - id else listOf(id) + settings.pinned))

    /** Lyrics you picked for a song (LRCLIB id, 0 = the one found) and how far they're shifted (ms; + = shown earlier). */
    fun lyricsPick(id: String): Pair<Long, Long> = settings.picks[id] ?: (0L to 0L)
    fun setLyricsPick(id: String, lrclibId: Long, shiftMs: Long) =
        update(settings.copy(picks = if (lrclibId == 0L && shiftMs == 0L) settings.picks - id else settings.picks + (id to (lrclibId to shiftMs))))

    /** Starts with your desktop session ([background]: without the window, like after closing it with music on). */
    private val autostartFile = java.io.File(System.getenv("XDG_CONFIG_HOME") ?: (System.getProperty("user.home") + "/.config"), "autostart/io.github.realkhfan.Onda.desktop")
    var autostart by mutableStateOf(autostartFile.isFile); private set
    fun setAutostart(on: Boolean, background: Boolean = true) {
        runCatching {
            if (!on) autostartFile.delete()
            else {
                val exe = ProcessHandle.current().info().command().orElse(System.getProperty("user.home") + "/.local/bin/onda")
                autostartFile.parentFile.mkdirs()
                autostartFile.writeText("[Desktop Entry]\nType=Application\nName=Onda\nExec=\"$exe\"" + (if (background) " --background" else "") +
                    "\nIcon=io.github.realkhfan.Onda\nX-GNOME-Autostart-enabled=true\n")
            }
        }
        autostart = autostartFile.isFile
    }

    private fun afterLibraryChange() {
        measureLoudness()
        val got = cleanWant()
        if (got.isNotEmpty()) Discover.notify(if (got.size == 1) "“${got[0].title}” is in your library now" else "${got.size} songs you wanted are in your library now",
            "Removed from Want to listen.")
    }

    /** Songs from Want to listen that are now in your library (a download, a scan, a match) leave the list. */
    fun cleanWant(): List<RemoteTrack> {
        if (library.isEmpty() || collection.want.isEmpty()) return emptyList()
        val got = collection.want.filter { w -> library.any { Match.same(it.title, it.artist, w.title, w.artist) } }
        if (got.isNotEmpty()) change(collection.copy(want = collection.want - got.toSet()))
        return got
    }

    fun setZoom(z: Float) = update(settings.copy(zoom = (Math.round(z * 10) / 10f).coerceIn(0.7f, 1.6f)))

    fun setNormalize(on: Boolean) { update(settings.copy(normalize = on)); if (on) measureLoudness() else Loudness.stop() }

    /** Measures the loudness of library songs not measured yet (files directly, server songs through their download link). */
    fun measureLoudness() {
        if (!settings.normalize) return
        val cfg = settings.server
        Loudness.measure(scope, library.mapNotNull { t ->
            if (t.id.startsWith("file:")) t.id to t.uri else Downloads.forTrack(t, cfg)?.let { t.id to it.url }
        })
    }

    /** Your artists, strongest first: followed ones, then plays, likes and your library (the phone's rule). */
    fun seeds(): List<String> =
        (collection.followed.map { it.name } + Discovery.seedArtists(plays, likedTracks.filterNot(::isPreviewRow), collection.want, library, System.currentTimeMillis()))
            .distinctBy(Match::norm).filterNot { isAi(it) || isBlocked(it) }

    /** Builds each mix whose day / week has changed (or all, with [force]). */
    fun refreshMixes(force: Boolean = false) {
        val today = java.time.LocalDate.now()
        val due = Discovery.Kind.entries.filter { k -> force || mixes[k]?.period != Discovery.period(k, today) }.filter { it !in building }
        if (due.isEmpty()) return
        val seeds = seeds()
        if (seeds.isEmpty()) return
        val known = (library.map { it.artist } + plays.map { it.artist }).map(Match::norm).toSet()
        val playedKeys = plays.map { it.key }.toSet()
        building = building + due
        scope.launch {
            for (k in due) {
                try {
                    val m = Discover.mix(k, Discovery.period(k, today), seeds, known, settings.listenBrainz) { r -> r.key in playedKeys || own(r) != null || notWanted(r) }
                    if (m.tracks.isNotEmpty()) { mixes = mixes + (k to m); withContext(Dispatchers.IO) { Store.saveMixes(mixes) } }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                } finally {
                    building = building - k
                }
            }
        }
    }

    /** New releases of your artists (every 6 hours at most); a notification on release day if allowed. */
    fun checkReleases() {
        val r = releases
        if (System.currentTimeMillis() - r.lastCheck < 6 * 3_600_000L) return
        val artists = seeds()
        if (artists.isEmpty()) return
        scope.launch {
            val today = java.time.LocalDate.now()
            val found = runCatching { Discover.releases(artists) }.getOrNull() ?: return@launch
            val recent = NewReleases.recent(found, today).filterNot { isAi(it.artist) || isBlocked(it.artist) }
            if (settings.releaseAlerts) {
                NewReleases.toNotify(recent, r.seen, today, firstCheck = r.lastCheck == 0L).take(5).forEach {
                    Discover.notify("New from ${it.artist}", "${it.title} (${it.type ?: "release"})")
                }
            }
            releases = Store.Releases(recent, recent.map { it.id }.toSet(), System.currentTimeMillis())
            withContext(Dispatchers.IO) { Store.saveReleases(releases) }
        }
    }

    /** On the last song: add about 10 similar songs (your own copies in full, others as previews or free full copies). */
    /** Song radio: songs like this artist's, for after the song you started it from. */
    suspend fun radio(artist: String, skipKey: String): List<Player.Item> =
        runCatching { Discover.similar(listOf(artist), settings.listenBrainz, skip = { it.key == skipKey || notWanted(it) }, n = 25) }
            .getOrDefault(emptyList()).mapNotNull(::item)

    /** How often each song was played (your own history, on this PC). */
    val playCounts by derivedStateOf { plays.groupingBy { it.key }.eachCount() }

    /** The mini player window is open. */
    var mini by mutableStateOf(false)

    /** A playlist file (M3U) becomes a playlist; returns its name and how many songs were found. */
    fun importPlaylistFile(file: java.io.File): Pair<String, Int> {
        val (name, ids) = M3u.read(file, library)
        if (ids.isNotEmpty()) createPlaylist(name, ids)
        return name to ids.size
    }

    fun extend(player: Player) {
        if (!settings.autoplay) return
        val inQueue = player.queue.map { it.id }.toSet()
        val last = player.queue.takeLast(5).reversed().map { it.artist }
        scope.launch {
            val more = runCatching { Discover.similar(last, settings.listenBrainz, skip = { it.key in inQueue || notWanted(it) }) }.getOrDefault(emptyList()).mapNotNull(::item)
            if (more.isNotEmpty() && player.index == player.queue.lastIndex) player.append(more)
        }
    }

    /* ---------- Backup ---------- */

    private val backupFiles = listOf("collection.json", "plays.json", "recent.json", "overrides.json")

    /** One ZIP with playlists, likes, history and settings; not your server login or folders (they belong to this PC). */
    fun backup(to: java.io.File) {
        java.util.zip.ZipOutputStream(to.outputStream()).use { z ->
            backupFiles.map { java.io.File(Store.data, it) }.filter { it.isFile }.forEach { f ->
                z.putNextEntry(java.util.zip.ZipEntry(f.name)); f.inputStream().use { it.copyTo(z) }; z.closeEntry()
            }
            val s = settings
            z.putNextEntry(java.util.zip.ZipEntry("settings.json"))
            z.write(org.json.JSONObject().put("kbps", s.kbps).put("volume", s.volume).put("autoplay", s.autoplay)
                .put("background", s.background).put("releaseAlerts", s.releaseAlerts).put("apple", s.apple)
                .put("listenBrainz", s.listenBrainz).put("normalize", s.normalize)
                .put("eq", org.json.JSONArray(s.eq)).put("eqPreset", s.eqPreset).put("songNotify", s.songNotify).put("zoom", s.zoom.toDouble()).toString().toByteArray())
            z.closeEntry()
        }
    }

    /** Once a day into the chosen folder, keeping the newest 5 of Onda's own backups there. Call off the UI thread. */
    fun dailyBackup() {
        val dir = settings.backupDir?.let { java.io.File(it) }?.takeIf { it.isDirectory } ?: return
        fun mine() = dir.listFiles { f: java.io.File -> f.isFile && f.name.startsWith("onda-backup-") && f.name.endsWith(".zip") }.orEmpty().sortedByDescending { it.lastModified() }
        if (mine().firstOrNull()?.let { System.currentTimeMillis() - it.lastModified() < 20 * 3_600_000L } == true) return
        runCatching { backup(java.io.File(dir, "onda-backup-${java.time.LocalDate.now()}.zip")) }
        mine().drop(5).forEach { it.delete() }
    }

    /** Replaces playlists, likes, history and settings with the backup's (a copy of the current ones is kept). Returns false if it isn't one. */
    fun restore(from: java.io.File): Boolean {
        val found = HashMap<String, String>()
        java.util.zip.ZipInputStream(from.inputStream()).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                if (e.name !in backupFiles && e.name != "settings.json") continue
                val bytes = z.readNBytes(32 shl 20)
                val text = String(bytes)
                if (runCatching { org.json.JSONTokener(text).nextValue() }.isSuccess) found[e.name] = text
            }
        }
        if (found.isEmpty()) return false
        backup(java.io.File(Store.data, "before-restore.zip"))
        found.forEach { (name, text) ->
            if (name == "settings.json") {
                val o = org.json.JSONObject(text)
                update(settings.copy(kbps = o.optInt("kbps", settings.kbps), volume = o.optDouble("volume", settings.volume),
                    autoplay = o.optBoolean("autoplay", settings.autoplay), background = o.optBoolean("background", settings.background),
                    releaseAlerts = o.optBoolean("releaseAlerts", settings.releaseAlerts), apple = o.optBoolean("apple", settings.apple),
                    listenBrainz = o.optBoolean("listenBrainz", settings.listenBrainz), normalize = o.optBoolean("normalize", settings.normalize),
                    eq = o.optJSONArray("eq")?.let { a -> List(10) { a.optDouble(it, 0.0) } } ?: settings.eq, eqPreset = o.optString("eqPreset", settings.eqPreset),
                    songNotify = o.optBoolean("songNotify", settings.songNotify), zoom = o.optDouble("zoom", settings.zoom.toDouble()).toFloat()))
            } else {
                java.io.File(Store.data, "$name.tmp").apply { writeText(text) }.renameTo(java.io.File(Store.data, name))
            }
        }
        collection = Store.collection(); plays = Store.plays(); recent = Store.recent(); Overrides.load(Store.data)
        return true
    }

    /** A saved catalog song as a list row: your own copy once you have it, else the preview. */
    fun trackFor(id: String): Track? = byId[id] ?: collection.remote.firstOrNull { it.key == id }?.let { r -> own(r) ?: asTrack(r) }

    private fun own(r: RemoteTrack) = library.firstOrNull { Match.same(it.title, it.artist, r.title, r.artist) }
    fun asTrack(r: RemoteTrack) = Track(r.key, "", r.title, r.artist, r.album, r.artist, 0, r.durationSec * 1000L, PREVIEW, r.cover)
    fun isPreviewRow(t: Track) = t.folderName == PREVIEW && t.uri.isEmpty()

    /** Can the heart / playlists hold this id? (Your songs, or a catalog song seen this run.) */
    /** The catalog song behind a row: a saved preview, or the Deezer match you accepted for a file. */
    fun remoteFor(t: Track): RemoteTrack? = collection.remote.firstOrNull { it.key == t.id } ?: seen[t.id]
        ?: meta[t.id]?.deezerId?.takeIf { it > 0 }?.let { RemoteTrack(it, t.title, t.artist, t.album, null, null, 0) }

    fun remember(r: RemoteTrack) { seen[r.key] = r }
    fun canSave(id: String) = byId.containsKey(id) || seen.containsKey(id) || collection.remote.any { it.key == id }

    private fun withRemote(id: String, c: Store.Collection): Store.Collection {
        val r = seen[id] ?: return c
        return if (c.remote.any { it.key == id }) c else c.copy(remote = c.remote + r.copy(preview = null))
    }

    fun start() {
        scope.launch { Overrides.meta.collect { meta = it } }
        scope.launch { Overrides.artistPics.collect { artistPics = it } }
        if (settings.folders.isNotEmpty()) rescan()
        if (settings.server != null) sync()
        Downloads.resume(this)
    }

    private fun update(s: Store.Settings) { settings = s; Store.save(s) }
    private fun change(c: Store.Collection) { collection = c; Store.saveCollection(c) }

    /* ---------- Phone link (library sync) ---------- */

    /** Your files and server songs with their own tags (no edits). */
    val rawTracks: List<Track> get() = serverTracks + local

    /** Your collection as the phone can read it ([foreign]: edits for songs this PC doesn't have, kept from last time). */
    fun linkSnapshot(foreign: Map<String, LinkSync.Edit>, known: MutableMap<String, LinkSync.Song>): LinkSync.Snapshot {
        val c = collection
        val lib = LinkSync.Library(byId.values.toList(), rawTracks, c.remote.associateBy { it.key }, known)
        return LinkSync.Snapshot(
            playlists = c.playlists.map { p -> LinkSync.Playlist(p.id, p.name, p.description, p.trackIds.mapNotNull(lib::song)) },
            liked = c.liked.mapNotNull(lib::song),
            want = c.want, catalog = c.remote, followed = c.followed, albums = c.albums,
            notInterested = c.hidden.filter { Source.of(it) != null }, blocked = c.blocked, ai = c.ai,
            plays = plays.map { it.copy(key = it.key.takeIf { k -> Source.of(k) != null }.orEmpty()) },
            edits = foreign + Overrides.forSync(rawTracks),
        ).also { known += lib.learned }
    }

    /** The merged collection from the phone; returns the edits for songs that aren't on this PC. */
    fun applyLink(s: LinkSync.Snapshot, known: MutableMap<String, LinkSync.Song>): Map<String, LinkSync.Edit> {
        val raw = rawTracks
        val foreign = Overrides.fromSync(raw, s.edits)
        // Songs are found by the details shown after the edits above.
        val shown = Overrides.apply(raw, Overrides.meta.value)
        val lib = LinkSync.Library(shown, raw, emptyMap())
        val c = collection
        change(c.copy(
            playlists = s.playlists.map { p -> Store.Playlist(p.id, p.name, p.songs.map(lib::id).distinct(), p.description, c.playlists.firstOrNull { it.id == p.id }?.cover) },
            liked = s.liked.map(lib::id).distinct(),
            want = s.want, remote = (s.catalog + c.remote).distinctBy { it.key }, followed = s.followed, albums = s.albums,
            hidden = c.hidden.filter { Source.of(it) == null } + s.notInterested, blocked = s.blocked, ai = s.ai,
        ))
        plays = s.plays.map { p -> p.copy(key = p.key.ifEmpty { lib.id(LinkSync.Song(null, p.title, p.artist)) }) }.takeLast(LinkSync.MAX_PLAYS)
        Store.savePlays(plays)
        known += lib.learned
        return foreign
    }

    /* ---------- Folders ---------- */

    fun addFolder(path: String) {
        if (path in settings.folders) return
        update(settings.copy(folders = settings.folders + path)); rescan()
    }

    fun removeFolder(path: String) { update(settings.copy(folders = settings.folders - path)); rescan() }

    fun rescan() {
        if (scanning) return
        scope.launch { rescanNow() }
    }

    /** Reads your folders again (only changed files are opened); waits for a scan already running. */
    suspend fun rescanNow() {
        while (scanning) kotlinx.coroutines.delay(100)
        scanning = true
        try {
            val found = withContext(Dispatchers.IO) { Scanner.scan(settings.folders, localCache).also(Store::saveLocal) }
            localCache = found
            local = found.values.map { it.second }
        } finally {
            scanning = false
        }
        afterLibraryChange()
    }

    fun launch(block: suspend CoroutineScope.() -> Unit) = scope.launch(block = block)

    /** Spotify import: songs into the playlist of that name (made if needed), plus likes, Want to listen and history. */
    fun importSpotify(lists: List<Pair<String, List<String>>>, catalog: List<RemoteTrack>, liked: List<String>,
                      want: List<RemoteTrack>, newPlays: List<Discovery.Play>) {
        var c = collection.copy(remote = (collection.remote + catalog.map { it.copy(preview = null) }).distinctBy { it.key })
        for ((name, ids) in lists) {
            val pl = c.playlists.firstOrNull { it.name == name }
            c = if (pl == null) c.copy(playlists = c.playlists + Store.Playlist(UUID.randomUUID().toString(), name, ids.distinct()))
            else c.copy(playlists = c.playlists.map { if (it.id == pl.id) it.copy(trackIds = (it.trackIds + ids).distinct()) else it })
        }
        c = c.copy(liked = liked.filterNot { it in c.liked }.distinct() + c.liked,
            want = c.want + want.filterNot { w -> c.want.any { it.key == w.key } }.distinctBy { it.key })
        change(c)
        plays = (plays + newPlays).distinctBy { Triple(it.artist, it.title, it.atMs / 60_000) }.sortedBy { it.atMs }.takeLast(3000)
        Store.savePlays(plays)
    }

    /** A file was renamed or a server song downloaded: likes, playlists, history and matches move to the new id. */
    fun migrateIds(moved: Map<String, String>) {
        if (moved.isEmpty()) return
        fun m(id: String) = moved[id] ?: id
        change(collection.copy(liked = collection.liked.map(::m).distinct(),
            playlists = collection.playlists.map { it.copy(trackIds = it.trackIds.map(::m)) }))
        // A file's id is its file URI, so a moved song plays from its new place.
        recent = recent.map { i -> moved[i.id]?.let { i.copy(id = it, uri = it) } ?: i }.distinctBy { it.id }; Store.saveRecent(recent)
        plays = plays.map { it.copy(key = m(it.key)) }; Store.savePlays(plays)
        Overrides.migrateIds(moved)
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
                afterLibraryChange()
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

    /** Undo: puts a deleted playlist back, with the catalog songs only it held. */
    fun restorePlaylist(pl: Store.Playlist, remotes: List<RemoteTrack>) = change(collection.copy(
        playlists = collection.playlists + pl, remote = (collection.remote + remotes).distinctBy { it.key }))
    fun setPlaylistTracks(id: String, ids: List<String>) = change(collection.copy(playlists = collection.playlists.map { if (it.id == id) it.copy(trackIds = ids) else it }))

    fun deletePlaylist(id: String) = change(collection.copy(playlists = collection.playlists.filterNot { it.id == id }))

    fun addToPlaylist(id: String, trackId: String) = change(withRemote(trackId, collection.copy(playlists = collection.playlists.map {
        if (it.id == id && trackId !in it.trackIds) it.copy(trackIds = it.trackIds + trackId) else it
    })))

    /** [from] / [to] are positions as shown (songs that can't be shown right now are skipped there). */
    fun movePlaylistTrack(id: String, from: Int, to: Int) = change(collection.copy(playlists = collection.playlists.map { pl ->
        val shown = pl.trackIds.filter { trackFor(it) != null }
        if (pl.id != id || from !in shown.indices || to !in shown.indices) return@map pl
        val ids = pl.trackIds.toMutableList()
        val target = ids.indexOf(shown[to])
        ids.add(target, ids.removeAt(ids.indexOf(shown[from]))) // lands where the target was; the target moves aside
        pl.copy(trackIds = ids)
    }))

    /** Keyboard shortcuts that change the page ("search", "back"); the window picks them up. */
    var navRequest by mutableStateOf<String?>(null)

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

    /** Full-copy lookups, by Deezer id: started once and kept, so a slow one still helps the next time the song plays. */
    private val fullCopies = HashMap<Long, Deferred<Player.Resolved?>>()
    private val previewLinks = HashMap<Long, Pair<Long, String>>() // Deezer id -> (fetched at, link); links expire

    private fun fullCopy(id: Long): Deferred<Player.Resolved?> = synchronized(fullCopies) {
        fullCopies.getOrPut(id) {
            scope.async(Dispatchers.IO) {
                runCatching { FullCopies.find(id, audius = true, jamendoId = "") }.getOrNull()
                    ?.let { c -> FreeCatalogs.stream(c)?.let { Player.Resolved(it, c.source.label) } }
            }
        }
    }

    private suspend fun previewLink(id: Long): String? {
        synchronized(previewLinks) { previewLinks[id] }?.takeIf { System.currentTimeMillis() - it.first < 10 * 60_000 }?.let { return it.second }
        return Deezer.previewNow(id)?.also { synchronized(previewLinks) { previewLinks[id] = System.currentTimeMillis() to it } }
    }

    /** For the player: a Deezer song plays as a free full copy when the artist's own account has one, else a fresh preview. */
    suspend fun resolve(i: Player.Item): Player.Resolved = coroutineScope {
        if (i.id.startsWith(Navidrome.PREFIX) && settings.cache) {
            SongCache.get(i.id)?.let { return@coroutineScope Player.Resolved(it.toURI().toString()) }
            SongCache.keep(scope, i.id, i.uri) // streams now; kept for next time
        }
        if (i.deezerId <= 0) return@coroutineScope Player.Resolved(i.uri)
        val full = fullCopy(i.deezerId)
        val preview = async { previewLink(i.deezerId) } // asked at the same time, so a preview never waits for the lookup
        // Never hold a preview up for long: after 1.5 s it plays the preview, and the lookup finishes for next time.
        val copy = if (full.isCompleted) full.await() else withTimeoutOrNull(1500) { full.await() }
        copy?.also { preview.cancel() } ?: Player.Resolved(preview.await() ?: throw IOException("No preview"))
    }

    /** The next song's link is looked up while this one plays, so it starts at once. */
    fun prefetch(i: Player.Item) {
        if (i.deezerId <= 0) return
        scope.launch(Dispatchers.IO) { runCatching { fullCopy(i.deezerId).await(); previewLink(i.deezerId) } }
    }
}
