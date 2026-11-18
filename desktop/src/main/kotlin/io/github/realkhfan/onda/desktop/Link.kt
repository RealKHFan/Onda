package io.github.realkhfan.onda.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.realkhfan.onda.core.LinkCrypto
import io.github.realkhfan.onda.core.LinkSync
import io.github.realkhfan.onda.data.Overrides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Your phone and this PC, over your Wi-Fi only (no cloud): the phone syncs your library with this PC and can play
 * here or take over what plays here. Off until you turn it on in Settings; only phones you paired get in.
 */
object Link {
    data class Phone(val id: String, val name: String, val token: String)

    private val file = File(Store.data, "link.json")
    var on by mutableStateOf(false); private set
    var phones by mutableStateOf<List<Phone>>(emptyList()); private set
    /** The pairing code shown in Settings (valid 2 minutes, for one phone). */
    var code by mutableStateOf<String?>(null); private set
    var problem by mutableStateOf<String?>(null); private set
    /** When each phone was last in touch (it asks every few seconds while Onda is open on it). */
    var seen by mutableStateOf<Map<String, Long>>(emptyMap()); private set
    var lastSync by mutableLongStateOf(0L); private set
    private var pcId = UUID.randomUUID().toString()
    /** The edits agreed at the last sync (by each song's own tags); kept in link.json as "foreign". */
    private var foreignEdits: Map<String, LinkSync.Edit> = emptyMap()
    private val known = HashMap<String, LinkSync.Song>()
    private var codeUntil = 0L
    private var codeTries = 0
    private var server: HttpServer? = null
    private var udp: DatagramSocket? = null
    private val inbox = java.util.concurrent.ConcurrentHashMap<String, LinkedBlockingQueue<JSONObject>>()

    lateinit var state: AppState
    var player: Player? = null
    val name: String get() = runCatching { java.net.InetAddress.getLocalHost().hostName }.getOrNull()?.takeIf { it.isNotBlank() && it != "localhost" } ?: "Onda on your PC"

    fun load(s: AppState) {
        state = s
        runCatching {
            val o = JSONObject(file.readText())
            pcId = o.optString("pc").ifBlank { pcId }
            phones = (0 until (o.optJSONArray("phones")?.length() ?: 0)).map { o.getJSONArray("phones").getJSONObject(it) }
                .map { Phone(it.getString("id"), it.optString("name"), it.getString("token")) }
            foreignEdits = LinkSync.fromJson(JSONObject().put("edits", o.optJSONObject("foreign") ?: JSONObject())).edits
            known += LinkSync.songsFromJson(o.optJSONObject("known"))
            if (o.optBoolean("on")) start()
        }
    }

    private fun save() = runCatching {
        val o = JSONObject().put("on", on).put("pc", pcId)
            .put("phones", JSONArray(phones.map { JSONObject().put("id", it.id).put("name", it.name).put("token", it.token) }))
            .put("foreign", LinkSync.toJson(LinkSync.Snapshot(edits = foreignEdits)).getJSONObject("edits"))
            .put("known", LinkSync.songsToJson(known))
        val tmp = File(file.path + ".tmp")
        tmp.writeText(o.toString())
        // The tokens are secrets: only you can read this file.
        runCatching { java.nio.file.Files.setPosixFilePermissions(tmp.toPath(), java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")) }
        tmp.renameTo(file)
    }

    fun turn(enabled: Boolean) {
        if (enabled) start() else stop()
        save()
    }

    fun newCode() { code = LinkCrypto.newCode(); codeUntil = System.currentTimeMillis() + 120_000; codeTries = 0 }

    fun forget(p: Phone) { phones = phones - p; save() }

    /** This PC's addresses on your network, for typing into the phone if it doesn't find the PC. */
    fun addresses(): List<String> = runCatching {
        java.net.NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .flatMap { it.inetAddresses.toList() }.filterIsInstance<java.net.Inet4Address>().map { it.hostAddress }
    }.getOrDefault(emptyList())

    private fun start() {
        if (server != null) return
        try {
            server = HttpServer.create(InetSocketAddress(LinkCrypto.PORT), 16).apply {
                createContext("/v1/") { ex -> runCatching { handle(ex) }.onFailure { runCatching { ex.sendResponseHeaders(500, -1) } }; ex.close() }
                executor = Executors.newFixedThreadPool(6) { r -> Thread(r, "onda-link").apply { isDaemon = true } }
                start()
            }
            // Phones find this PC by asking the whole network once ("anyone running Onda?").
            udp = DatagramSocket(LinkCrypto.PORT).also { sock ->
                Thread({
                    val buf = ByteArray(256)
                    while (!sock.isClosed) runCatching {
                        val p = DatagramPacket(buf, buf.size)
                        sock.receive(p)
                        if (String(p.data, 0, p.length) == "ONDA-DISCOVER-1") {
                            val reply = "ONDA1|$pcId|${name.take(60)}".toByteArray()
                            sock.send(DatagramPacket(reply, reply.size, p.address, p.port))
                        }
                    }
                }, "onda-link-find").apply { isDaemon = true }.start()
            }
            on = true; problem = null
        } catch (e: Exception) {
            stop()
            problem = "Couldn't open port ${LinkCrypto.PORT}: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    private fun stop() {
        server?.stop(0); server = null
        udp?.close(); udp = null
        on = false; code = null
    }

    /** Puts what plays here on a phone; it starts there the next time it checks in (within a few seconds). */
    fun playOnPhone(p: Phone): Boolean {
        val q = queueHere() ?: return false
        inbox.getOrPut(p.id) { LinkedBlockingQueue() }.offer(JSONObject().put("play", LinkSync.queueToJson(q)))
        player?.pause()
        return true
    }

    fun connected(p: Phone) = System.currentTimeMillis() - (seen[p.id] ?: 0) < 40_000

    /* ---------- your phone playing (this PC is its remote) ---------- */

    /** What a phone said it plays ([s]: has, playing, key, title, artist, album, art, pos, dur, shuffle, repeat), and when. */
    data class PhoneNow(val phoneId: String, val name: String, val s: JSONObject, val at: Long, val playedAt: Long) {
        val playing get() = s.optBoolean("playing")
        /** Where the song is now (it went on playing since the phone said). */
        fun position(now: Long = System.currentTimeMillis()) = (s.optLong("pos") + if (playing) now - at else 0).coerceAtMost(s.optLong("dur").takeIf { it > 0 } ?: Long.MAX_VALUE)
    }
    var phoneNow by mutableStateOf<PhoneNow?>(null); private set

    /**
     * The phone that plays right now, while this PC doesn't: it says so at least every 10 s while it plays; paused, it stays
     * the place to play for 10 minutes after it last played, like Spotify's devices.
     */
    fun activePhone(now: Long = System.currentTimeMillis()): PhoneNow? = phoneNow?.takeIf { n ->
        on && player?.playing != true && (if (n.playing) now - n.at < 30_000 else now - n.playedAt < 10 * 60_000)
    }

    private fun notPlaying(p: Phone) { if (phoneNow?.phoneId == p.id) phoneNow = null }

    private fun tell(phoneId: String, msg: JSONObject) { inbox.getOrPut(phoneId) { LinkedBlockingQueue() }.offer(msg) }

    /** Play/pause, next, previous, seek (ms), shuffle (on), repeat (mode 0 off, 1 all, 2 one) on the phone. */
    fun phoneCommand(cmd: String, extra: JSONObject.() -> Unit = {}) {
        val n = phoneNow ?: return
        val msg = JSONObject().put("cmd", cmd).apply(extra)
        tell(n.phoneId, msg)
        // Shown at once; the phone confirms within a second.
        val s = JSONObject(n.s.toString()).put("pos", n.position())
        when (cmd) {
            "toggle" -> s.put("playing", !n.playing)
            "seek" -> s.put("pos", msg.optLong("ms"))
            "shuffle" -> s.put("shuffle", msg.optBoolean("on"))
            "repeat" -> s.put("repeat", msg.optInt("mode"))
            "remove", "move", "clear" -> {
                val next = s.optJSONArray("next")?.let { a -> (0 until a.length()).map { a.get(it) } }.orEmpty().toMutableList()
                when (cmd) {
                    "remove" -> msg.optInt("i", -1).takeIf { it in next.indices }?.let { next.removeAt(it) }
                    "move" -> { val f = msg.optInt("from", -1); val t = msg.optInt("to", -1); if (f in next.indices && t in next.indices) next.add(t, next.removeAt(f)) }
                    else -> next.clear()
                }
                s.put("next", JSONArray(next))
            }
        }
        phoneNow = n.copy(s = s, at = System.currentTimeMillis())
    }

    /** "Sync now" here: the phone syncs (it always merges both sides). */
    fun syncNow(p: Phone) = tell(p.id, JSONObject().put("sync", true))

    /** "This PC": the phone hands what it plays over to this PC (and becomes its remote). */
    fun takeFromPhone() { phoneNow?.let { tell(it.phoneId, JSONObject().put("handover", true)) } }

    /** Plays a list on the phone that's playing (instead of here); false when none is (a paused one leaves it to this PC). */
    fun playOnActivePhone(items: List<Player.Item>, start: Int): Boolean {
        val n = activePhone()?.takeIf { it.playing } ?: return false
        // The phone's shuffle and repeat stay; shuffled: the chosen song first, the rest mixed (the phone keeps the order sent).
        val shuffle = n.s.optBoolean("shuffle")
        val list = if (shuffle) listOf(items[start]) + items.filterIndexed { i, _ -> i != start }.shuffled() else items
        tell(n.phoneId, JSONObject().put("play", LinkSync.queueToJson(queueOf(list, if (shuffle) 0 else start, 0).copy(shuffle = shuffle, repeat = n.s.optInt("repeat")))))
        return true
    }

    /** Adds a song to the queue of the phone that's playing; false when none is. */
    fun addOnActivePhone(item: Player.Item, next: Boolean): Boolean {
        val n = activePhone()?.takeIf { it.playing } ?: return false
        tell(n.phoneId, JSONObject().put("add", LinkSync.queueToJson(queueOf(listOf(item), 0, 0))).put("next", next))
        return true
    }

    /** This PC started playing: a linked phone that isn't playing shows it (it becomes the remote). */
    fun playingHere() {
        phoneNow = null // this PC is the place to play now
        if (!on) return
        phones.filter(::connected).forEach { tell(it.id, JSONObject().put("pcPlaying", true)) }
    }

    /* ---------- requests ---------- */

    private fun handle(ex: HttpExchange) {
        val path = ex.requestURI.path
        if (ex.requestMethod != "POST") return ex.sendResponseHeaders(405, -1)
        val body = ex.requestBody.use { it.readNBytes(32 * 1024 * 1024) }
        if (path == "/v1/pair") return pair(ex, body)
        val phone = phones.firstOrNull { it.id == ex.requestHeaders.getFirst("X-Onda-Phone") } ?: return ex.sendResponseHeaders(401, -1)
        val key = LinkCrypto.linkKey(phone.token)
        val req = LinkCrypto.open(key, body, path)?.let { JSONObject(String(it)) } ?: return ex.sendResponseHeaders(401, -1)
        if (kotlin.math.abs(System.currentTimeMillis() - req.optLong("t")) > LinkCrypto.MAX_SKEW_MS) return ex.sendResponseHeaders(408, -1)
        runBlocking(Dispatchers.Swing) { seen = seen + (phone.id to System.currentTimeMillis()) }
        val res: Any = when (path) {
            "/v1/hello" -> JSONObject().put("name", name).put("pc", pcId)
            // Not while the music is being scanned: songs could look gone for a moment.
            "/v1/sync/get" -> runBlocking(Dispatchers.Swing) {
                if (state.scanning) return@runBlocking null
                val snap = LinkSync.toJson(state.linkSnapshot(foreignEdits, known))
                JSONObject().put("version", version(snap)).put("snapshot", snap)
            } ?: return ex.sendResponseHeaders(423, -1)
            "/v1/sync/put" -> {
                val status = runBlocking(Dispatchers.Swing) {
                    if (state.scanning) return@runBlocking 423
                    // Changed here since the phone looked: it merges again.
                    if (req.optString("version") != version(LinkSync.toJson(state.linkSnapshot(foreignEdits, known)))) return@runBlocking 409
                    val merged = LinkSync.fromJson(req.getJSONObject("snapshot"))
                    state.applyLink(merged, known)
                    // All edits agreed now (not only those for songs this PC doesn't have): one missing here later keeps it.
                    foreignEdits = merged.edits
                    Overrides.synced()
                    told = state.linkFingerprint() // what came from the phone isn't news for it
                    lastSync = System.currentTimeMillis()
                    save()
                    200
                }
                if (status != 200) return ex.sendResponseHeaders(status, -1)
                JSONObject().put("ok", true)
            }
            "/v1/player/state" -> runBlocking(Dispatchers.Swing) { playerState() }
            "/v1/player/cmd" -> runBlocking(Dispatchers.Swing) { notPlaying(phone); command(req); playerState() }
            "/v1/player/play" -> runBlocking(Dispatchers.Swing) {
                notPlaying(phone)
                JSONObject().put("missing", playHere(LinkSync.queueFromJson(req.getJSONObject("queue"))))
            }
            "/v1/player/take" -> runBlocking(Dispatchers.Swing) {
                notPlaying(phone)
                // The phone takes over: what plays here, from here on.
                val q = queueHere()
                player?.pause()
                JSONObject().apply { q?.let { put("queue", LinkSync.queueToJson(it)) } }
            }
            "/v1/player/add" -> runBlocking(Dispatchers.Swing) {
                val q = LinkSync.queueFromJson(req.getJSONObject("queue"))
                val items = resolveHere(q).filterNotNull()
                player?.let { p -> if (req.optBoolean("next")) items.reversed().forEach(p::playNext) else items.forEach(p::enqueue) }
                JSONObject().put("added", items.size)
            }
            "/v1/inbox" -> inbox.getOrPut(phone.id) { LinkedBlockingQueue() }.poll(20, TimeUnit.SECONDS) ?: JSONObject()
            // What the phone plays (when it changes, and every few seconds while it plays).
            "/v1/phone/now" -> runBlocking(Dispatchers.Swing) {
                val now = System.currentTimeMillis()
                val before = phoneNow?.takeIf { it.phoneId == phone.id }
                phoneNow = if (req.optBoolean("has")) PhoneNow(phone.id, phone.name, req, now, if (req.optBoolean("playing")) now else before?.playedAt ?: 0)
                    else phoneNow?.takeIf { it.phoneId != phone.id }
                JSONObject()
            }
            // Songs and covers: only files of your library (found by the song), never a path the phone names.
            "/v1/file/stat" -> runBlocking(Dispatchers.Swing) { fileOf(LinkSync.songFromJson(req.getJSONObject("song"))) }
                .let { f -> JSONObject().put("found", f != null).put("size", f?.length() ?: 0).put("ext", f?.extension?.lowercase() ?: "") }
            "/v1/file/read" -> {
                val f = runBlocking(Dispatchers.Swing) { fileOf(LinkSync.songFromJson(req.getJSONObject("song"))) } ?: return ex.sendResponseHeaders(404, -1)
                val offset = req.optLong("offset").coerceAtLeast(0)
                val length = req.optInt("length").coerceIn(0, 4 * 1024 * 1024)
                java.io.RandomAccessFile(f, "r").use { r ->
                    if (offset >= r.length()) ByteArray(0)
                    else ByteArray(minOf(length.toLong(), r.length() - offset).toInt()).also { r.seek(offset); r.readFully(it) }
                }
            }
            "/v1/file/put" -> receive(phone, req) ?: return ex.sendResponseHeaders(409, -1)
            "/v1/cover/get" -> runBlocking(Dispatchers.Swing) { state.coverFiles()[req.optString("hash")] }
                ?.let { runCatching { File(it).readBytes() }.getOrNull() } ?: return ex.sendResponseHeaders(404, -1)
            "/v1/cover/put" -> {
                val bytes = java.util.Base64.getDecoder().decode(req.optString("data"))
                if (bytes.size > 20 * 1024 * 1024 || !runBlocking(Dispatchers.Swing) { state.saveCover(req.optString("hash"), bytes) }) return ex.sendResponseHeaders(400, -1)
                JSONObject().put("ok", true)
            }
            else -> return ex.sendResponseHeaders(404, -1)
        }
        val plain = if (res is ByteArray) res else (res as JSONObject).put("t", System.currentTimeMillis()).toString().toByteArray()
        val out = LinkCrypto.seal(key, plain, "$path#r")
        ex.sendResponseHeaders(200, out.size.toLong())
        ex.responseBody.use { it.write(out) }
    }

    private fun pair(ex: HttpExchange, body: ByteArray) {
        val c = code
        if (c == null || System.currentTimeMillis() > codeUntil || codeTries >= 5) return ex.sendResponseHeaders(403, -1)
        val key = LinkCrypto.pairKey(c)
        val req = LinkCrypto.open(key, body, "/v1/pair")?.let { JSONObject(String(it)) }
        if (req == null) { codeTries++; return ex.sendResponseHeaders(403, -1) }
        val phone = Phone(req.getString("id").take(64), req.optString("name").take(60).ifBlank { "Phone" }, LinkCrypto.newToken())
        runBlocking(Dispatchers.Swing) { phones = phones.filterNot { it.id == phone.id } + phone; code = null; save() }
        val out = LinkCrypto.seal(key, JSONObject().put("token", phone.token).put("pc", pcId).put("name", name).toString().toByteArray(), "/v1/pair#r")
        ex.sendResponseHeaders(200, out.size.toLong())
        ex.responseBody.use { it.write(out) }
    }

    private fun version(snap: JSONObject): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(snap.toString().toByteArray()).take(12).joinToString("") { "%02x".format(it) }

    private fun playerState(): JSONObject {
        val p = player ?: return JSONObject().put("has", false)
        val cur = p.current ?: return JSONObject().put("has", false).put("volume", p.volume)
        // What's next too, for the phone's queue screen while it's the remote.
        val next = JSONArray(p.upcoming().take(50).map { (_, i) ->
            JSONObject().put("key", i.id).put("title", i.title).put("artist", i.artist).put("album", i.album).put("art", i.art?.takeIf { it.startsWith("http") }).put("dur", i.durationMs)
        })
        return JSONObject().put("has", true).put("playing", p.playing).put("key", cur.id).put("title", cur.title).put("artist", cur.artist).put("album", cur.album)
            .put("art", cur.art?.takeIf { it.startsWith("http") }).put("pos", p.positionMs).put("dur", p.durationMs).put("volume", p.volume).put("next", next)
            .put("shuffle", p.shuffle).put("repeat", p.repeat)
    }

    private fun command(req: JSONObject) {
        val p = player ?: return
        when (req.optString("cmd")) {
            "toggle" -> p.toggle()
            "next" -> p.next()
            "prev" -> p.previous()
            "seek" -> p.seek(req.optLong("ms"))
            "volume" -> { p.changeVolume(req.optDouble("v", p.volume)); state.setVolume(p.volume) }
            "shuffle" -> p.shuffleOn(req.optBoolean("on"))
            "repeat" -> p.repeatMode(req.optInt("mode"))
            // The phone's queue screen while it's the remote ([i], [from], [to] count from the first song still to come).
            "jump" -> p.upcoming().getOrNull(req.optInt("i", -1))?.let { p.jumpTo(it.first) }
            "remove" -> repeat(req.optInt("n", 1).coerceIn(1, 50)) { p.upcoming().getOrNull(req.optInt("i", -1))?.let { p.removeAt(it.first) } }
            "move" -> p.moveUpcoming(req.optInt("from", -1), req.optInt("to", -1))
            "clear" -> p.clearUpcoming()
        }
    }

    /** What plays here, from the current song on, with this player's shuffle and repeat. */
    private fun queueHere(): LinkSync.Queue? {
        val p = player ?: return null
        val cur = p.current ?: return null
        return queueOf(listOf(cur) + p.upcoming().map { it.second }, 0, p.positionMs).copy(shuffle = p.shuffle, repeat = p.repeat)
    }

    private fun queueOf(items: List<Player.Item>, start: Int, positionMs: Long): LinkSync.Queue {
        val lib = LinkSync.Library(state.library, state.rawTracks, state.collection.remote.associateBy { it.key })
        val songs = items.map { i -> lib.song(i.id)?.takeIf { it.title.isNotEmpty() } ?: LinkSync.Song(i.id.takeIf { io.github.realkhfan.onda.core.Source.of(it) != null }, i.title, i.artist, i.album, i.durationMs) }
        val catalog = items.mapNotNull { i -> LinkSync.remoteOf(i.id, i.title, i.artist, i.album, i.art, i.durationMs)?.let { r -> if (i.deezerId > 0) r.copy(id = i.deezerId) else r } }
        return LinkSync.Queue(songs, catalog, positionMs, start = start)
    }

    /** Songs from the phone as player items here (null: not on this PC). */
    private fun resolveHere(q: LinkSync.Queue): List<Player.Item?> {
        val lib = LinkSync.Library(state.library, state.rawTracks, state.collection.remote.associateBy { it.key })
        val catalog = q.catalog.associateBy { it.key }
        return q.songs.map { s -> if (s.key != null) catalog[s.key]?.let(state::item) else state.byId[lib.id(s)]?.let(state::item) }
    }

    /** A song's file on this PC (your music folders only; server songs aren't files here). */
    private fun fileOf(song: LinkSync.Song): File? {
        val lib = LinkSync.Library(state.library, state.rawTracks, emptyMap())
        val t = state.byId[lib.id(song)] ?: return null
        return t.id.takeIf { it.startsWith("file:") }?.let { runCatching { File(java.net.URI(it)) }.getOrNull() }?.takeIf { it.isFile }
    }

    /** A song the phone sends ("Get from your phone"), piece by piece, into "Onda from phone" in your first music folder. */
    private fun receive(phone: Phone, req: JSONObject): JSONObject? {
        val song = LinkSync.songFromJson(req.getJSONObject("song"))
        val size = req.optLong("size")
        val ext = req.optString("ext").lowercase().filter { it.isLetterOrDigit() }.take(5).ifEmpty { "mp3" }
        if (size <= 0 || size > 2L * 1024 * 1024 * 1024) return null
        val dir = runBlocking(Dispatchers.Swing) { Downloads.target(state) }?.let { File(it, "Onda from phone") } ?: return null
        val name = io.github.realkhfan.onda.core.FileNames.forSong(song.title, song.artist, ext) ?: return null
        dir.mkdirs()
        val part = File(dir, "$name.part")
        val offset = req.optLong("offset")
        if (offset != (if (offset == 0L) 0L else part.length())) return null // out of order: the phone starts again
        val bytes = java.util.Base64.getDecoder().decode(req.optString("data"))
        java.io.FileOutputStream(part, offset > 0).use { it.write(bytes) }
        receiving = receiving + (phone.id to "${song.title} · ${(part.length() * 100 / size).coerceAtMost(100)}%")
        if (part.length() < size) return JSONObject().put("done", false)
        val final = File(dir, name).takeIf { !it.exists() } ?: File(dir, "${name.substringBeforeLast('.')} (2).$ext")
        part.renameTo(final)
        receiving = receiving - phone.id
        runBlocking(Dispatchers.Swing) { state.rescan() } // the greyed-out song becomes the file once it's scanned
        return JSONObject().put("done", true)
    }

    /** Songs coming from the phone right now (for Settings). */
    var receiving by mutableStateOf<Map<String, String>>(emptyMap()); private set

    /** Asks the phone for its copy of a song that's greyed out here ("Get from your phone"); it sends it while Onda runs there. */
    fun getFromPhone(songs: List<LinkSync.Song>): Boolean {
        val p = phones.firstOrNull { connected(it) } ?: return false
        songs.forEach { s -> inbox.getOrPut(p.id) { LinkedBlockingQueue() }.offer(JSONObject().put("send", LinkSync.songToJson(s))) }
        return true
    }

    /** Your phones hear when something changed here (a few seconds later), so they sync while both are open. */
    private var told = 0
    fun changed(fingerprint: Int) {
        if (fingerprint == told) return
        told = fingerprint
        phones.filter(::connected).forEach { inbox.getOrPut(it.id) { LinkedBlockingQueue() }.offer(JSONObject().put("sync", true)) }
    }

    /** Plays songs from the phone here; returns how many aren't on this PC (left out). */
    private fun playHere(q: LinkSync.Queue): Int {
        val p = player ?: return q.songs.size
        val items = resolveHere(q)
        val found = items.filterNotNull()
        if (found.isEmpty()) return q.songs.size
        val start = items.take(q.start.coerceIn(0, items.size)).count { it != null }.coerceAtMost(found.lastIndex)
        p.play(found, start, here = true)
        p.adopt(q.shuffle, q.repeat)
        // The song goes on where the phone was (if it's the same song).
        if (items.getOrNull(q.start) != null && q.positionMs > 3000) state.launch { delay(800); p.seek(q.positionMs) }
        return q.songs.size - found.size
    }
}
