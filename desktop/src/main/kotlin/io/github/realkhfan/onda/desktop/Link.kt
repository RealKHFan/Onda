package io.github.realkhfan.onda.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.realkhfan.onda.core.LinkCrypto
import io.github.realkhfan.onda.core.LinkSync
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
        val res: JSONObject = when (path) {
            "/v1/hello" -> JSONObject().put("name", name).put("pc", pcId)
            "/v1/sync/get" -> runBlocking(Dispatchers.Swing) {
                val snap = LinkSync.toJson(state.linkSnapshot(foreignEdits, known))
                JSONObject().put("version", version(snap)).put("snapshot", snap)
            }
            "/v1/sync/put" -> runBlocking(Dispatchers.Swing) {
                // Changed here since the phone looked: it merges again.
                if (req.optString("version") != version(LinkSync.toJson(state.linkSnapshot(foreignEdits, known)))) return@runBlocking null
                foreignEdits = state.applyLink(LinkSync.fromJson(req.getJSONObject("snapshot")), known)
                lastSync = System.currentTimeMillis()
                save()
                JSONObject().put("ok", true)
            } ?: return ex.sendResponseHeaders(409, -1)
            "/v1/player/state" -> runBlocking(Dispatchers.Swing) { playerState() }
            "/v1/player/cmd" -> runBlocking(Dispatchers.Swing) { command(req); playerState() }
            "/v1/player/play" -> runBlocking(Dispatchers.Swing) {
                JSONObject().put("missing", playHere(LinkSync.queueFromJson(req.getJSONObject("queue"))))
            }
            "/v1/player/take" -> runBlocking(Dispatchers.Swing) {
                // The phone takes over: what plays here, from here on.
                val q = queueHere()
                player?.pause()
                JSONObject().apply { q?.let { put("queue", LinkSync.queueToJson(it)) } }
            }
            "/v1/inbox" -> inbox.getOrPut(phone.id) { LinkedBlockingQueue() }.poll(20, TimeUnit.SECONDS) ?: JSONObject()
            else -> return ex.sendResponseHeaders(404, -1)
        }
        val out = LinkCrypto.seal(key, res.put("t", System.currentTimeMillis()).toString().toByteArray(), "$path#r")
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
        return JSONObject().put("has", true).put("playing", p.playing).put("title", cur.title).put("artist", cur.artist).put("album", cur.album)
            .put("art", cur.art?.takeIf { it.startsWith("http") }).put("pos", p.positionMs).put("dur", p.durationMs).put("volume", p.volume)
    }

    private fun command(req: JSONObject) {
        val p = player ?: return
        when (req.optString("cmd")) {
            "toggle" -> p.toggle()
            "next" -> p.next()
            "prev" -> p.previous()
            "seek" -> p.seek(req.optLong("ms"))
            "volume" -> { p.changeVolume(req.optDouble("v", p.volume)); state.setVolume(p.volume) }
        }
    }

    /** What plays here, from the current song on. */
    private fun queueHere(): LinkSync.Queue? {
        val p = player ?: return null
        val cur = p.current ?: return null
        val items = listOf(cur) + p.upcoming().map { it.second }
        val lib = LinkSync.Library(state.library, state.rawTracks, state.collection.remote.associateBy { it.key })
        val songs = items.map { i -> lib.song(i.id)?.takeIf { it.title.isNotEmpty() } ?: LinkSync.Song(i.id.takeIf { io.github.realkhfan.onda.core.Source.of(it) != null }, i.title, i.artist, i.album, i.durationMs) }
        val catalog = items.mapNotNull { i -> LinkSync.remoteOf(i.id, i.title, i.artist, i.album, i.art, i.durationMs)?.let { r -> if (i.deezerId > 0) r.copy(id = i.deezerId) else r } }
        return LinkSync.Queue(songs, catalog, p.positionMs)
    }

    /** Plays songs from the phone here; returns how many aren't on this PC (left out). */
    private fun playHere(q: LinkSync.Queue): Int {
        val p = player ?: return q.songs.size
        val lib = LinkSync.Library(state.library, state.rawTracks, state.collection.remote.associateBy { it.key })
        val catalog = q.catalog.associateBy { it.key }
        val items = q.songs.map { s ->
            if (s.key != null) catalog[s.key]?.let(state::item) else state.byId[lib.id(s)]?.let(state::item)
        }
        val found = items.filterNotNull()
        if (found.isEmpty()) return q.songs.size
        p.play(found, 0)
        // The first song goes on where the phone was (if it's the same song).
        if (items.first() != null && q.positionMs > 3000) state.launch { delay(800); p.seek(q.positionMs) }
        return q.songs.size - found.size
    }
}
