package io.github.realkhfan.onda.data

import android.content.Context
import io.github.realkhfan.onda.core.LinkCrypto
import io.github.realkhfan.onda.core.LinkSync
import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.player.toMediaItem
import io.github.realkhfan.onda.player.toPreviewItem
import io.github.realkhfan.onda.core.Source
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.scan.MusicLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.UUID

/**
 * Onda on your PC, over your Wi-Fi only (no cloud): library sync and "Play on…". Everything is encrypted with a key
 * from pairing (the 6-digit code the PC shows). The phone asks; the PC answers.
 */
object Link {
    data class Pc(val address: String, val id: String, val name: String)

    private val _pc = MutableStateFlow<Pc?>(null)
    val pc: StateFlow<Pc?> = _pc.asStateFlow()
    /** The last sync: when, and how it went. */
    val status = MutableStateFlow<String?>(null)
    val syncing = MutableStateFlow(false)
    private var token: String? = null
    private var phoneId = ""
    private lateinit var dir: File
    private lateinit var prefs: android.content.SharedPreferences
    private val mutex = Mutex()
    /** Syncs outlive the screen that started them. */
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Main.immediate)

    fun syncLater(onDone: (String) -> Unit = {}) { scope.launch { onDone(sync()) } }

    fun load(context: Context) {
        dir = context.filesDir
        prefs = context.getSharedPreferences("link", Context.MODE_PRIVATE)
        phoneId = prefs.getString("phone", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("phone", it).apply() }
        token = prefs.getString("token", null)
        val addr = prefs.getString("address", null)
        if (token != null && addr != null) _pc.value = Pc(addr, prefs.getString("pc", "")!!, prefs.getString("name", "Your PC")!!)
        status.value = prefs.getString("status", null)
        watch()
    }

    /* ---------- finding and pairing ---------- */

    /** PCs on this network with Onda's link on (they answer one broadcast). */
    suspend fun discover(): List<Pc> = withContext(Dispatchers.IO) {
        val found = LinkedHashMap<String, Pc>()
        runCatching {
            DatagramSocket().use { s ->
                s.broadcast = true
                s.soTimeout = 400
                val ask = "ONDA-DISCOVER-1".toByteArray()
                repeat(3) {
                    runCatching { s.send(DatagramPacket(ask, ask.size, InetAddress.getByName("255.255.255.255"), LinkCrypto.PORT)) }
                    val until = System.currentTimeMillis() + 500
                    while (System.currentTimeMillis() < until) {
                        val buf = ByteArray(256)
                        val p = DatagramPacket(buf, buf.size)
                        if (runCatching { s.receive(p) }.isFailure) break
                        val parts = String(p.data, 0, p.length).split('|')
                        if (parts.size >= 3 && parts[0] == "ONDA1") found[parts[1]] = Pc(p.address.hostAddress ?: continue, parts[1], parts[2])
                    }
                }
            }
        }
        found.values.toList()
    }

    /** Null when it worked, else what went wrong. */
    suspend fun pair(address: String, code: String, phoneName: String): String? = withContext(Dispatchers.IO) {
        val key = LinkCrypto.pairKey(code.filter(Char::isDigit))
        val body = LinkCrypto.seal(key, JSONObject().put("id", phoneId).put("name", phoneName).toString().toByteArray(), "/v1/pair")
        val (status, bytes) = post(address.trim(), "/v1/pair", body, 8_000) ?: return@withContext "Can't reach $address. Is Onda open on the PC, with the link on?"
        if (status == 403) return@withContext "That code didn't work. Make a new one on the PC and try again."
        val res = bytes?.let { LinkCrypto.open(key, it, "/v1/pair#r") }?.let { JSONObject(String(it)) } ?: return@withContext "The PC answered something unexpected."
        token = res.getString("token")
        _pc.value = Pc(address.trim(), res.optString("pc"), res.optString("name").ifBlank { "Your PC" })
        prefs.edit().putString("token", token).putString("address", address.trim()).putString("pc", res.optString("pc")).putString("name", _pc.value!!.name).apply()
        null
    }

    fun unlink() {
        token = null; _pc.value = null; status.value = null
        prefs.edit().remove("token").remove("address").remove("pc").remove("name").remove("status").apply()
        File(dir, "link-base.json").delete()
    }

    /* ---------- requests ---------- */

    private class Reply(val status: Int, val bytes: ByteArray?) {
        val json: JSONObject? get() = bytes?.let { runCatching { JSONObject(String(it)) }.getOrNull() }
    }

    /** A request to the PC; if it moved to another address on the network, it's found again by its id. */
    private suspend fun call(path: String, req: JSONObject = JSONObject(), timeoutMs: Int = 10_000): Reply? = withContext(Dispatchers.IO) { callNow(path, req, timeoutMs) }

    /** The same, on the calling thread (the player's loading thread, a download). */
    private fun callNow(path: String, req: JSONObject, timeoutMs: Int): Reply? {
        val pc = _pc.value ?: return null
        val key = LinkCrypto.linkKey(token ?: return null)
        val body = LinkCrypto.seal(key, req.put("t", System.currentTimeMillis()).toString().toByteArray(), path)
        val r = post(pc.address, path, body, timeoutMs) ?: kotlinx.coroutines.runBlocking { discover() }.firstOrNull { it.id == pc.id }?.let { moved ->
            _pc.value = pc.copy(address = moved.address); prefs.edit().putString("address", moved.address).apply()
            post(moved.address, path, body, timeoutMs)
        } ?: return null
        return Reply(r.first, r.second?.let { LinkCrypto.open(key, it, "$path#r") })
    }

    private fun post(address: String, path: String, body: ByteArray, timeoutMs: Int): Pair<Int, ByteArray?>? = runCatching {
        val host = if (':' in address) address else "$address:${LinkCrypto.PORT}"
        val c = URL("http://$host$path").openConnection() as HttpURLConnection
        c.connectTimeout = 3_000; c.readTimeout = timeoutMs
        c.requestMethod = "POST"; c.doOutput = true
        c.setRequestProperty("X-Onda-Phone", phoneId)
        c.setFixedLengthStreamingMode(body.size)
        c.outputStream.use { it.write(body) }
        val code = c.responseCode
        code to (if (code == 200) c.inputStream.use { it.readBytes() } else null).also { c.disconnect() }
    }.getOrNull()

    /* ---------- library sync ---------- */

    /** Merges this phone's collection with the PC's (both end up the same). Returns what to tell you. */
    suspend fun sync(): String = mutex.withLock {
        if (_pc.value == null) return "Not linked with a PC."
        if (MusicLibrary.scanning.value) return "Your music is still being scanned. Try again in a moment."
        syncing.value = true
        try {
            repeat(3) {
                val reply = call("/v1/sync/get", timeoutMs = 30_000) ?: return done("Couldn't reach ${_pc.value?.name ?: "your PC"}.")
                if (reply.status == 423) return done("${_pc.value?.name ?: "Your PC"} is still scanning its music. Try again in a moment.")
                val got = reply.takeIf { it.status == 200 }?.json ?: return done("Couldn't reach ${_pc.value?.name ?: "your PC"}.")
                val stored = withContext(Dispatchers.IO) { runCatching { JSONObject(File(dir, "link-base.json").readText()) }.getOrNull() }
                val base = stored?.optJSONObject("base")?.let(LinkSync::fromJson)
                val known = HashMap(LinkSync.songsFromJson(stored?.optJSONObject("known")))
                val mine = snapshot(base, known)
                val theirs = LinkSync.fromJson(got.getJSONObject("snapshot"))
                val merged = withContext(Dispatchers.Default) { LinkSync.merge(base, mine, theirs) }
                if (!swapCovers(merged, theirs)) return done("Couldn't send a playlist cover to ${_pc.value?.name ?: "your PC"}.")
                val put = call("/v1/sync/put", JSONObject().put("version", got.getString("version")).put("snapshot", LinkSync.toJson(merged)), 30_000)
                    ?: return done("Couldn't reach ${_pc.value?.name ?: "your PC"}.")
                if (put.status == 409) return@repeat // changed on the PC meanwhile: merge again
                if (put.status == 423) return done("${_pc.value?.name ?: "Your PC"} is still scanning its music. Try again in a moment.")
                if (put.status != 200) return done("The PC refused the sync (${put.status}).")
                apply(merged, known)
                Overrides.synced()
                withContext(Dispatchers.IO) {
                    val f = File(dir, "link-base.json")
                    File(f.path + ".tmp").apply { writeText(JSONObject().put("base", LinkSync.toJson(merged)).put("known", LinkSync.songsToJson(known)).toString()) }.renameTo(f)
                }
                return done("Synced " + java.time.LocalTime.now().withNano(0).withSecond(0))
            }
            done("Your PC kept changing; try again.")
        } finally {
            syncing.value = false
        }
    }

    private fun done(msg: String): String {
        status.value = msg
        prefs.edit().putString("status", msg).apply()
        return msg
    }

    private fun raw(): List<Track> = MusicLibrary.tracks.value + AppData.serverTracks()

    private fun snapshot(base: LinkSync.Snapshot?, known: MutableMap<String, LinkSync.Song>): LinkSync.Snapshot {
        val raw = raw()
        val shown = AppData.everyTrack(MusicLibrary.tracks.value, AppData.serverTracks(), Overrides.meta.value)
        val lib = LinkSync.Library(shown, raw, PlaylistStore.remote.value, known)
        return LinkSync.Snapshot(
            playlists = PlaylistStore.playlists.value.map { p ->
                LinkSync.Playlist(p.id, p.name, p.description, p.trackIds.mapNotNull(lib::song), p.cover?.let(::coverHash), p.folder)
            },
            folders = PlaylistStore.folders.value.map { LinkSync.Folder(it.id, it.name) },
            liked = PlaylistStore.liked.value.mapNotNull(lib::song),
            want = PlaylistStore.want.value, catalog = PlaylistStore.remote.value.values.toList(),
            followed = UserCollection.followed.value, albums = UserCollection.albums.value,
            notInterested = UserCollection.hidden.value.filter { Source.of(it) != null },
            blocked = UserCollection.blocked.value.toList(), ai = UserCollection.aiMarked.value.toList(),
            plays = PlayLog.plays.value.map { it.copy(key = it.key.takeIf { k -> Source.of(k) != null }.orEmpty()) },
            // Agreed last time plus yours; only a restore here removes one (a song missing its edit for a moment keeps it).
            edits = Overrides.forSync(raw, base?.edits.orEmpty()),
        ).also { known += lib.learned }
    }

    private fun apply(s: LinkSync.Snapshot, known: MutableMap<String, LinkSync.Song>) {
        val raw = raw()
        Overrides.fromSync(raw, s.edits)
        val shown = AppData.everyTrack(MusicLibrary.tracks.value, AppData.serverTracks(), Overrides.meta.value)
        val lib = LinkSync.Library(shown, raw, emptyMap())
        val coverFiles = coverFiles()
        PlaylistStore.fromSync(
            s.playlists.map { p -> PlaylistStore.Playlist(p.id, p.name, p.songs.map(lib::id).distinct(), p.description, p.cover?.let { coverFiles[it] }, p.folder) },
            s.folders.map { PlaylistStore.Folder(it.id, it.name) },
            s.liked.map(lib::id).distinct(), s.want, s.catalog,
        )
        UserCollection.fromSync(s.followed, s.albums, s.notInterested, s.blocked, s.ai)
        PlayLog.replace(s.plays.map { p -> p.copy(key = p.key.ifEmpty { lib.id(LinkSync.Song(null, p.title, p.artist)) }) })
        known += lib.learned
        synced = fingerprint() // what just came from the PC isn't a change to send back
    }

    /* ---------- playlist covers ---------- */

    private val hashes = HashMap<String, String>() // cover file -> its hash (the files never change; a new photo is a new file)

    private fun coverHash(path: String): String? = hashes[path] ?: runCatching { LinkSync.hashOf(File(path).readBytes()) }.getOrNull()?.also { hashes[path] = it }

    /** Every cover photo here by its hash. */
    private fun coverFiles(): Map<String, String> =
        File(dir, "covers").listFiles().orEmpty().mapNotNull { f -> coverHash(f.path)?.let { it to f.path } }.toMap()

    /** Before the merged collection is kept: covers the PC doesn't have go to it, those this phone doesn't have come here. */
    private suspend fun swapCovers(merged: LinkSync.Snapshot, theirs: LinkSync.Snapshot): Boolean = withContext(Dispatchers.IO) {
        val here = coverFiles()
        val there = theirs.playlists.mapNotNull { it.cover }.toSet()
        for (p in merged.playlists) {
            val h = p.cover ?: continue
            if (h !in there) {
                val bytes = here[h]?.let { runCatching { File(it).readBytes() }.getOrNull() } ?: continue
                val r = callNow("/v1/cover/put", JSONObject().put("hash", h).put("data", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)), 30_000)
                if (r?.status != 200) return@withContext false
            }
            if (h !in here) {
                // Checked against its hash: only the photo that was asked for is kept.
                val bytes = callNow("/v1/cover/get", JSONObject().put("hash", h), 30_000)?.takeIf { it.status == 200 }?.bytes
                if (bytes != null && LinkSync.hashOf(bytes) == h) {
                    File(dir, "covers").mkdirs()
                    File(dir, "covers/${p.id}-$h.img").writeBytes(bytes)
                }
            }
        }
        true
    }

    /* ---------- sync as soon as something changes ---------- */

    @Volatile private var synced = 0

    /** What a sync carries, as one number (history aside: it waits for the next sync). */
    private fun fingerprint(): Int = listOf<Any?>(
        PlaylistStore.playlists.value, PlaylistStore.folders.value, PlaylistStore.liked.value, PlaylistStore.want.value,
        UserCollection.followed.value, UserCollection.albums.value, UserCollection.hidden.value, UserCollection.blocked.value,
        UserCollection.aiMarked.value, Overrides.meta.value,
    ).hashCode()

    /** Changes here reach the PC a few seconds later (while Onda runs). */
    private fun watch() {
        synced = fingerprint()
        val flows = listOf<kotlinx.coroutines.flow.Flow<Any?>>(
            PlaylistStore.playlists, PlaylistStore.folders, PlaylistStore.liked, PlaylistStore.want, UserCollection.followed, UserCollection.albums,
            UserCollection.hidden, UserCollection.blocked, UserCollection.aiMarked, Overrides.meta,
        )
        scope.launch {
            kotlinx.coroutines.flow.combine(flows) { fingerprint() }.collectLatest { f ->
                kotlinx.coroutines.delay(4_000)
                if (_pc.value != null && f != synced && !syncing.value) syncLater()
            }
        }
    }

    /* ---------- songs from the PC: copy or stream ---------- */

    /** A song's file on the PC: its size and extension, or null when the PC doesn't have it as a file. */
    fun stat(song: LinkSync.Song): Pair<Long, String>? =
        callNow("/v1/file/stat", JSONObject().put("song", LinkSync.songToJson(song)), 10_000)?.json
            ?.takeIf { it.optBoolean("found") }?.let { it.getLong("size") to it.optString("ext", "mp3") }

    /** Up to [length] bytes of the song's file from [offset] (the player's loading thread and downloads call this). */
    fun read(song: LinkSync.Song, offset: Long, length: Int): ByteArray? =
        callNow("/v1/file/read", JSONObject().put("song", LinkSync.songToJson(song)).put("offset", offset).put("length", length), 30_000)
            ?.takeIf { it.status == 200 }?.bytes

    /** Sends one of this phone's songs to the PC (it asked: "Get from your phone"). */
    private suspend fun send(context: Context, song: LinkSync.Song) = withContext(Dispatchers.IO) {
        val lib = LinkSync.Library(AppData.everyTrack(MusicLibrary.tracks.value, AppData.serverTracks(), Overrides.meta.value), raw(), emptyMap())
        val t = raw().firstOrNull { it.id == lib.id(song) }?.takeIf { it.uri.startsWith("content:") } ?: return@withContext
        val uri = android.net.Uri.parse(t.uri)
        val size = runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { c -> if (c.moveToFirst()) c.getLong(0) else -1L }
        }.getOrNull() ?: -1L
        if (size <= 0) return@withContext
        val ext = t.fileName.substringAfterLast('.', "mp3").lowercase()
        runCatching {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                val buf = ByteArray(CHUNK)
                var offset = 0L
                while (offset < size) {
                    var n = 0
                    while (n < buf.size) { val k = input.read(buf, n, buf.size - n); if (k < 0) break; n += k }
                    if (n <= 0) break
                    val r = callNow("/v1/file/put", JSONObject().put("song", LinkSync.songToJson(song)).put("ext", ext).put("offset", offset).put("size", size)
                        .put("data", android.util.Base64.encodeToString(buf, 0, n, android.util.Base64.NO_WRAP)), 60_000)
                    if (r?.status != 200) break
                    offset += n
                }
            }
        }
    }

    const val CHUNK = 1024 * 1024

    /* ---------- messages from the PC ---------- */

    /** Play this here (the PC's "Play on… your phone"). */
    var onPlay: ((LinkSync.Queue) -> Unit)? = null

    /** The PC as this phone's remote: "cmd" (toggle, next, prev, seek, shuffle, repeat), "add", "handover"; and "pcPlaying". */
    var onMessage: ((JSONObject) -> Unit)? = null

    fun onWifi(context: Context): Boolean = context.getSystemService(android.net.ConnectivityManager::class.java).let { cm ->
        cm.getNetworkCapabilities(cm.activeNetwork)?.let {
            it.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) || it.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
        } == true
    }

    /** Tells the PC what plays here, so it shows it and can control it (only when linked and on Wi-Fi). */
    suspend fun reportNow(context: Context, now: JSONObject) {
        if (_pc.value == null || !onWifi(context)) return
        call("/v1/phone/now", now, 4_000)
    }

    /**
     * Waits for the PC (up to 20 s per request) while the playback service runs: what to play, a sync, a song to send.
     * Only on Wi-Fi, where the PC can be.
     */
    suspend fun listen(context: Context) {
        while (true) {
            if (_pc.value == null || !onWifi(context)) { kotlinx.coroutines.delay(30_000); continue }
            val r = call("/v1/inbox", timeoutMs = 30_000)?.takeIf { it.status == 200 }?.json
            if (r == null) { kotlinx.coroutines.delay(20_000); continue } // the PC isn't reachable right now
            r.optJSONObject("play")?.let(LinkSync::queueFromJson)?.let { q -> onPlay?.invoke(q) }
            if (r.optBoolean("sync")) syncLater()
            r.optJSONObject("send")?.let { send(context, LinkSync.songFromJson(it)) }
            if (r.has("cmd") || r.has("add") || r.optBoolean("handover") || r.optBoolean("pcPlaying")) onMessage?.invoke(r)
        }
    }

    /* ---------- Play on… ---------- */

    /** What plays on the PC (null: not reachable). */
    suspend fun state(): JSONObject? = call("/v1/player/state", timeoutMs = 4_000)?.json

    suspend fun command(cmd: String, extra: JSONObject.() -> Unit = {}): JSONObject? = call("/v1/player/cmd", JSONObject().put("cmd", cmd).apply(extra), 4_000)?.json

    /** Plays these on the PC; returns how many songs the PC doesn't have, or null if it couldn't be reached. */
    suspend fun playThere(q: LinkSync.Queue): Int? = call("/v1/player/play", JSONObject().put("queue", LinkSync.queueToJson(q)), 15_000)?.json?.optInt("missing")

    /** Adds songs to the PC's queue ([next]: right after the song playing). */
    suspend fun addThere(q: LinkSync.Queue, next: Boolean): Boolean =
        call("/v1/player/add", JSONObject().put("queue", LinkSync.queueToJson(q)).put("next", next), 10_000)?.status == 200

    /** Asks the PC for one of its songs (for "Get it from your PC": Onda copies it into your music folder). */
    fun linkUri(t: Track): String = io.github.realkhfan.onda.player.LinkDataSource.uriFor(LinkSync.Song(null, t.title, t.artist))

    /** Takes over what the PC plays (it pauses there). */
    suspend fun take(): LinkSync.Queue? = call("/v1/player/take", timeoutMs = 8_000)?.json?.optJSONObject("queue")?.let(LinkSync::queueFromJson)



    /** Songs to play here: yours where found, catalog songs as previews; returns those found and how many weren't. */
    /** [start]: where the queue's start song is among [items] (songs not here are left out). */
    data class Resolved(val items: List<androidx.media3.common.MediaItem>, val start: Int, val missing: Int)

    fun resolve(q: LinkSync.Queue, library: List<Track>): Resolved {
        val lib = LinkSync.Library(library, raw(), emptyMap())
        val catalog = q.catalog.associateBy { it.key }
        val byId = library.associateBy { it.id }
        val all = q.songs.map { s ->
            if (s.key != null) catalog[s.key]?.let { r -> library.firstOrNull { Match.same(it.title, it.artist, r.title, r.artist) }?.toMediaItem() ?: r.toPreviewItem() }
            else byId[lib.id(s)]?.toMediaItem()
        }
        val items = all.filterNotNull()
        val start = all.take(q.start.coerceIn(0, all.size)).count { it != null }.coerceAtMost((items.size - 1).coerceAtLeast(0))
        return Resolved(items, start, q.songs.size - items.size)
    }
}
