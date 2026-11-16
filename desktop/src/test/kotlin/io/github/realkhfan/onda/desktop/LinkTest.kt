package io.github.realkhfan.onda.desktop

import io.github.realkhfan.onda.core.LinkCrypto
import io.github.realkhfan.onda.core.LinkSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The phone's side of the link, spoken by hand against the PC's server. */
class LinkTest {
    private fun post(path: String, body: ByteArray, phone: String? = null): Pair<Int, ByteArray?> {
        val c = URL("http://127.0.0.1:${LinkCrypto.PORT}$path").openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true
        phone?.let { c.setRequestProperty("X-Onda-Phone", it) }
        c.outputStream.use { it.write(body) }
        return c.responseCode to (if (c.responseCode == 200) c.inputStream.readBytes() else null)
    }

    @Test fun pairsSyncsAndRefusesStrangers() {
        val state = runBlocking { withContext(Dispatchers.Swing) { AppState(CoroutineScope(Dispatchers.Swing)) } }
        runBlocking(Dispatchers.Swing) { Link.load(state); Link.turn(true) }
        try {
            assertTrue(Link.on, Link.problem ?: "")
            // Found on the network.
            DatagramSocket().use { s ->
                s.soTimeout = 2000
                val ask = "ONDA-DISCOVER-1".toByteArray()
                s.send(DatagramPacket(ask, ask.size, InetAddress.getLoopbackAddress(), LinkCrypto.PORT))
                val p = DatagramPacket(ByteArray(256), 256); s.receive(p)
                assertTrue(String(p.data, 0, p.length).startsWith("ONDA1|"))
            }
            // No code shown: pairing is refused; a wrong code too.
            runBlocking(Dispatchers.Swing) { Link.newCode() }
            val code = Link.code!!
            val wrong = if (code == "000000") "111111" else "000000"
            assertEquals(403, post("/v1/pair", LinkCrypto.seal(LinkCrypto.pairKey(wrong), "{}".toByteArray(), "/v1/pair")).first)
            val key = LinkCrypto.pairKey(code)
            val (status, bytes) = post("/v1/pair", LinkCrypto.seal(key, JSONObject().put("id", "test-phone").put("name", "Test phone").toString().toByteArray(), "/v1/pair"))
            assertEquals(200, status)
            val token = JSONObject(String(LinkCrypto.open(key, bytes!!, "/v1/pair#r")!!)).getString("token")
            val link = LinkCrypto.linkKey(token)
            fun call(path: String, req: JSONObject = JSONObject()): Pair<Int, JSONObject?> {
                val (st, b) = post(path, LinkCrypto.seal(link, req.put("t", System.currentTimeMillis()).toString().toByteArray(), path), "test-phone")
                return st to b?.let { JSONObject(String(LinkCrypto.open(link, it, "$path#r")!!)) }
            }
            assertEquals(Link.name, call("/v1/hello").second!!.getString("name"))
            // Sync: read, then write back with a new playlist.
            val got = call("/v1/sync/get").second!!
            val snap = LinkSync.fromJson(got.getJSONObject("snapshot"))
            val withNew = snap.copy(playlists = snap.playlists + LinkSync.Playlist("link-test", "From the phone", "", listOf(LinkSync.Song("dz:5", "Tides", "Lumen Trio"), LinkSync.Song(null, "Only On The Phone", "Nobody Here"))),
                catalog = snap.catalog + io.github.realkhfan.onda.core.RemoteTrack(5, "Tides", "Lumen Trio", "Sea", null, null, 200))
            assertEquals(200, call("/v1/sync/put", JSONObject().put("version", got.getString("version")).put("snapshot", LinkSync.toJson(withNew))).first)
            assertTrue(state.collection.playlists.any { it.id == "link-test" && it.trackIds.first() == "dz:5" })
            // The song this PC doesn't have is kept and listed (greyed out), and never played.
            val missing = state.playlistTracks("link-test").last()
            assertTrue(LinkSync.isMissing(missing.id))
            assertEquals("Only On The Phone", missing.title)
            // The same old version again: changed meanwhile, so the phone must merge again.
            assertEquals(409, call("/v1/sync/put", JSONObject().put("version", got.getString("version")).put("snapshot", LinkSync.toJson(snap))).first)
            assertNotNull(call("/v1/player/state").second)
            // A stranger (or a changed message) gets nothing.
            assertEquals(401, post("/v1/sync/get", LinkCrypto.seal(LinkCrypto.linkKey("nope"), "{}".toByteArray(), "/v1/sync/get"), "test-phone").first)
            assertEquals(401, post("/v1/sync/get", LinkCrypto.seal(link, "{}".toByteArray(), "/v1/sync/get"), "someone-else").first)
            // Covers and folders: a cover the phone sends is kept by its hash and given back the same.
            val photo = ByteArray(5000) { (it * 7).toByte() }
            val hash = LinkSync.hashOf(photo)
            assertEquals(400, call("/v1/cover/put", JSONObject().put("hash", "wrong").put("data", java.util.Base64.getEncoder().encodeToString(photo))).first)
            assertEquals(200, call("/v1/cover/put", JSONObject().put("hash", hash).put("data", java.util.Base64.getEncoder().encodeToString(photo))).first)
            val got2 = call("/v1/sync/get").second!!
            val snap2 = LinkSync.fromJson(got2.getJSONObject("snapshot"))
            val withFolder = snap2.copy(folders = snap2.folders + LinkSync.Folder("link-folder", "Test folder"),
                playlists = snap2.playlists.map { if (it.id == "link-test") it.copy(cover = hash, folder = "link-folder") else it })
            assertEquals(200, call("/v1/sync/put", JSONObject().put("version", got2.getString("version")).put("snapshot", LinkSync.toJson(withFolder))).first)
            val pl = state.collection.playlists.first { it.id == "link-test" }
            assertEquals("link-folder", pl.folder)
            assertEquals(hash, pl.cover?.let { LinkSync.hashOf(java.io.File(it).readBytes()) })
            val (cst, cbytes) = post("/v1/cover/get", LinkCrypto.seal(link, JSONObject().put("hash", hash).put("t", System.currentTimeMillis()).toString().toByteArray(), "/v1/cover/get"), "test-phone")
            assertEquals(200, cst)
            assertTrue(LinkCrypto.open(link, cbytes!!, "/v1/cover/get#r")!!.contentEquals(photo))
            // A song the phone sends lands in "Onda from phone" in the first music folder, in order only.
            val music = java.nio.file.Files.createTempDirectory("onda-music").toFile()
            val before = state.settings.folders
            runBlocking(Dispatchers.Swing) { before.forEach(state::removeFolder); state.addFolder(music.path) }
            val song = LinkSync.Song(null, "Sent Song", "Phone Artist")
            fun put(offset: Long, data: ByteArray) = call("/v1/file/put", JSONObject().put("song", LinkSync.songToJson(song)).put("ext", "mp3")
                .put("offset", offset).put("size", 6L).put("data", java.util.Base64.getEncoder().encodeToString(data))).first
            assertEquals(200, put(0, byteArrayOf(1, 2, 3)))
            assertEquals(409, put(5, byteArrayOf(9))) // a gap: refused
            assertEquals(200, put(3, byteArrayOf(4, 5, 6)))
            assertTrue(java.io.File(music, "Onda from phone/Phone Artist - Sent Song.mp3").readBytes().contentEquals(byteArrayOf(1, 2, 3, 4, 5, 6)))
            runBlocking(Dispatchers.Swing) { state.removeFolder(music.path); before.forEach(state::addFolder) }
            // A song that isn't a file here can't be read.
            assertEquals(false, call("/v1/file/stat", JSONObject().put("song", LinkSync.songToJson(LinkSync.Song(null, "Nope", "Nobody")))).second!!.getBoolean("found"))
            // An old message (replayed later) is refused.
            val old = JSONObject().put("t", System.currentTimeMillis() - 3_600_000L).toString().toByteArray()
            assertEquals(408, post("/v1/hello", LinkCrypto.seal(link, old, "/v1/hello"), "test-phone").first)
        } finally {
            runBlocking(Dispatchers.Swing) {
                // Leave the real collection as it was.
                state.applyLink(state.linkSnapshot(emptyMap(), HashMap()).let { s -> s.copy(playlists = s.playlists.filterNot { it.id == "link-test" }, folders = s.folders.filterNot { it.id == "link-folder" }) }, HashMap())
                Link.phones.filter { it.id == "test-phone" }.forEach(Link::forget)
                Link.turn(false)
            }
        }
    }

    /** Details edited on either side reach the other (found by the file's own artist and title), and so does undoing them. */
    @Test fun editedDetailsSyncBothWays() = runBlocking(Dispatchers.Swing) {
        val state = AppState(CoroutineScope(Dispatchers.Swing))
        val before = state.local
        val id = "file:/tmp/onda-edit-sync-test.mp3"
        val mine = io.github.realkhfan.onda.core.Track(id, id, "tides demo", "lumen trio", "", "lumen trio", 0, 200_000, "audio/mpeg", null, "onda-edit-sync-test.mp3")
        val key = LinkSync.idOf(mine.artist, mine.title)
        try {
            state.local = before + mine
            // Edited on the PC: the phone gets it.
            io.github.realkhfan.onda.data.Overrides.edit(mine, "Tides", "Lumen Trio", "Sea")
            val snap = state.linkSnapshot(emptyMap(), HashMap())
            assertEquals("Tides", snap.edits[key]?.title)
            // Changed on the phone: this PC shows that.
            state.applyLink(snap.copy(edits = snap.edits + (key to LinkSync.Edit("Tides (Live)", "Lumen Trio", "Sea", null, 0))), HashMap())
            assertEquals("Tides (Live)", io.github.realkhfan.onda.data.Overrides.meta.value[id]?.title)
            // Original tags restored on the phone: restored here too.
            state.applyLink(snap.copy(edits = snap.edits - key), HashMap())
            assertEquals(null, io.github.realkhfan.onda.data.Overrides.meta.value[id])
        } finally {
            io.github.realkhfan.onda.data.Overrides.restore(id)
            state.local = before
        }
    }
}
