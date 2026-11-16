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
            // An old message (replayed later) is refused.
            val old = JSONObject().put("t", System.currentTimeMillis() - 3_600_000L).toString().toByteArray()
            assertEquals(408, post("/v1/hello", LinkCrypto.seal(link, old, "/v1/hello"), "test-phone").first)
        } finally {
            runBlocking(Dispatchers.Swing) {
                // Leave the real collection as it was.
                state.applyLink(state.linkSnapshot(emptyMap(), HashMap()).let { s -> s.copy(playlists = s.playlists.filterNot { it.id == "link-test" }) }, HashMap())
                Link.phones.filter { it.id == "test-phone" }.forEach(Link::forget)
                Link.turn(false)
            }
        }
    }
}
