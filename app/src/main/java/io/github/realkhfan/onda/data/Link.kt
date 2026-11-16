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

    private class Reply(val status: Int, val json: JSONObject?)

    /** A request to the PC; if it moved to another address on the network, it's found again by its id. */
    private suspend fun call(path: String, req: JSONObject = JSONObject(), timeoutMs: Int = 10_000): Reply? = withContext(Dispatchers.IO) {
        val pc = _pc.value ?: return@withContext null
        val key = LinkCrypto.linkKey(token ?: return@withContext null)
        val body = LinkCrypto.seal(key, req.put("t", System.currentTimeMillis()).toString().toByteArray(), path)
        val r = post(pc.address, path, body, timeoutMs) ?: discover().firstOrNull { it.id == pc.id }?.let { moved ->
            _pc.value = pc.copy(address = moved.address); prefs.edit().putString("address", moved.address).apply()
            post(moved.address, path, body, timeoutMs)
        } ?: return@withContext null
        Reply(r.first, r.second?.let { LinkCrypto.open(key, it, "$path#r") }?.let { JSONObject(String(it)) })
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
                val got = call("/v1/sync/get", timeoutMs = 30_000)?.takeIf { it.status == 200 }?.json ?: return done("Couldn't reach ${_pc.value?.name ?: "your PC"}.")
                val stored = withContext(Dispatchers.IO) { runCatching { JSONObject(File(dir, "link-base.json").readText()) }.getOrNull() }
                val base = stored?.optJSONObject("base")?.let(LinkSync::fromJson)
                val known = HashMap(LinkSync.songsFromJson(stored?.optJSONObject("known")))
                val mine = snapshot(base, known)
                val merged = withContext(Dispatchers.Default) { LinkSync.merge(base, mine, LinkSync.fromJson(got.getJSONObject("snapshot"))) }
                val put = call("/v1/sync/put", JSONObject().put("version", got.getString("version")).put("snapshot", LinkSync.toJson(merged)), 30_000)
                    ?: return done("Couldn't reach ${_pc.value?.name ?: "your PC"}.")
                if (put.status == 409) return@repeat // changed on the PC meanwhile: merge again
                if (put.status != 200) return done("The PC refused the sync (${put.status}).")
                apply(merged, known)
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
        val here = raw.map { LinkSync.idOf(it.artist, it.title) }.toSet()
        return LinkSync.Snapshot(
            playlists = PlaylistStore.playlists.value.map { p -> LinkSync.Playlist(p.id, p.name, p.description, p.trackIds.mapNotNull(lib::song)) },
            liked = PlaylistStore.liked.value.mapNotNull(lib::song),
            want = PlaylistStore.want.value, catalog = PlaylistStore.remote.value.values.toList(),
            followed = UserCollection.followed.value, albums = UserCollection.albums.value,
            notInterested = UserCollection.hidden.value.filter { Source.of(it) != null },
            blocked = UserCollection.blocked.value.toList(), ai = UserCollection.aiMarked.value.toList(),
            plays = PlayLog.plays.value.map { it.copy(key = it.key.takeIf { k -> Source.of(k) != null }.orEmpty()) },
            // Edits for songs this phone doesn't have stay as they were agreed last time.
            edits = base?.edits.orEmpty().filterKeys { it !in here } + Overrides.forSync(raw),
        ).also { known += lib.learned }
    }

    private fun apply(s: LinkSync.Snapshot, known: MutableMap<String, LinkSync.Song>) {
        val raw = raw()
        Overrides.fromSync(raw, s.edits)
        val shown = AppData.everyTrack(MusicLibrary.tracks.value, AppData.serverTracks(), Overrides.meta.value)
        val lib = LinkSync.Library(shown, raw, emptyMap())
        PlaylistStore.fromSync(
            s.playlists.map { p -> PlaylistStore.Playlist(p.id, p.name, p.songs.map(lib::id).distinct(), p.description) },
            s.liked.map(lib::id).distinct(), s.want, s.catalog,
        )
        UserCollection.fromSync(s.followed, s.albums, s.notInterested, s.blocked, s.ai)
        PlayLog.replace(s.plays.map { p -> p.copy(key = p.key.ifEmpty { lib.id(LinkSync.Song(null, p.title, p.artist)) }) })
        known += lib.learned
    }

    /* ---------- Play on… ---------- */

    /** What plays on the PC (null: not reachable). */
    suspend fun state(): JSONObject? = call("/v1/player/state", timeoutMs = 4_000)?.json

    suspend fun command(cmd: String, extra: JSONObject.() -> Unit = {}): JSONObject? = call("/v1/player/cmd", JSONObject().put("cmd", cmd).apply(extra), 4_000)?.json

    /** Plays these on the PC; returns how many songs the PC doesn't have, or null if it couldn't be reached. */
    suspend fun playThere(q: LinkSync.Queue): Int? = call("/v1/player/play", JSONObject().put("queue", LinkSync.queueToJson(q)), 15_000)?.json?.optInt("missing")

    /** Takes over what the PC plays (it pauses there). */
    suspend fun take(): LinkSync.Queue? = call("/v1/player/take", timeoutMs = 8_000)?.json?.optJSONObject("queue")?.let(LinkSync::queueFromJson)

    /** Waits (up to 20 s) for the PC to send something to play here: null when the PC can't be reached, no songs when nothing came. */
    suspend fun inbox(): LinkSync.Queue? = call("/v1/inbox", timeoutMs = 30_000)?.takeIf { it.status == 200 }
        ?.let { r -> r.json?.optJSONObject("play")?.let(LinkSync::queueFromJson) ?: LinkSync.Queue(emptyList(), emptyList(), 0) }

    /** Songs to play here: yours where found, catalog songs as previews; returns those found and how many weren't. */
    fun resolve(q: LinkSync.Queue, library: List<Track>): Pair<List<androidx.media3.common.MediaItem>, Int> {
        val lib = LinkSync.Library(library, raw(), emptyMap())
        val catalog = q.catalog.associateBy { it.key }
        val byId = library.associateBy { it.id }
        val items = q.songs.mapNotNull { s ->
            if (s.key != null) catalog[s.key]?.let { r -> library.firstOrNull { Match.same(it.title, it.artist, r.title, r.artist) }?.toMediaItem() ?: r.toPreviewItem() }
            else byId[lib.id(s)]?.toMediaItem()
        }
        return items to (q.songs.size - items.size)
    }
}
