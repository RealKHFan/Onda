package io.github.realkhfan.onda.data

import androidx.test.core.app.ApplicationProvider
import io.github.realkhfan.onda.core.LinkCrypto
import io.github.realkhfan.onda.core.LinkSync
import io.github.realkhfan.onda.core.RemoteTrack
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetAddress
import java.net.ServerSocket

/** The phone's link client against a stand-in PC that speaks the same protocol as the desktop app. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LinkClientTest {
    @Test fun pairsAndSyncsBothWays() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        AppData.load(ctx)
        // Onda shows its saved song list first (on the main thread); a sync waits for it.
        val loading = System.currentTimeMillis() + 5_000
        while (!io.github.realkhfan.onda.scan.MusicLibrary.loaded.value && System.currentTimeMillis() < loading) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle(); Thread.sleep(10)
        }
        val phoneCover = java.io.File(ctx.filesDir, "covers").apply { mkdirs() }.resolve("phone-pl-1.img").apply { writeBytes(ByteArray(2000) { 7 }) }
        PlaylistStore.fromSync(listOf(PlaylistStore.Playlist("phone-pl", "On the phone", listOf("dz:1"), cover = phoneCover.path)), emptyList(), emptyList(), emptyList(),
            listOf(RemoteTrack(1, "Paper Moon", "Velvet Hours", "Moons", null, null, 180)))
        var token = ""
        val pcCover = ByteArray(3000) { (it % 251).toByte() }
        val pcCoverHash = LinkSync.hashOf(pcCover)
        val covers = HashMap<String, ByteArray>().apply { put(pcCoverHash, pcCover) }
        val songFile = ByteArray(1_300_000) { (it * 31 % 256).toByte() }
        var pcSnapshot = LinkSync.Snapshot(playlists = listOf(LinkSync.Playlist("pc-pl", "On the PC", "", listOf(LinkSync.Song("dz:2", "Tides", "Lumen Trio")), cover = pcCoverHash, folder = "pc-f")),
            folders = listOf(LinkSync.Folder("pc-f", "From the PC")),
            catalog = listOf(RemoteTrack(2, "Tides", "Lumen Trio", "Sea", null, null, 200)), blocked = listOf("someone"),
            // What played last on the PC, later than anything here: this phone picks it up (paused) at the same moment.
            last = LinkSync.LastPlay(LinkSync.Queue(listOf(LinkSync.Song("dz:2", "Tides", "Lumen Trio")),
                listOf(RemoteTrack(2, "Tides", "Lumen Trio", "Sea", null, "https://example.invalid/p.mp3", 200)), 42_000, 0, false, 1), System.currentTimeMillis()))
        var duringPut: (() -> Unit)? = null // something done on the phone while it waits for the PC's answer
        // A bare HTTP server (the JDK's isn't on Android's test classpath): one request per connection.
        fun handle(path: String, body: ByteArray): Pair<Int, ByteArray?> {
            val (key, aad) = if (path == "/v1/pair") LinkCrypto.pairKey("123456") to "/v1/pair" else LinkCrypto.linkKey(token) to path
            val req = LinkCrypto.open(key, body, aad)?.let { JSONObject(String(it)) } ?: return 403 to null
            val res: Any = when (path) {
                "/v1/pair" -> { token = LinkCrypto.newToken(); JSONObject().put("token", token).put("pc", "pc-1").put("name", "Test PC") }
                "/v1/sync/get" -> JSONObject().put("version", "v1").put("snapshot", LinkSync.toJson(pcSnapshot))
                "/v1/sync/put" -> { duringPut?.invoke(); duringPut = null; pcSnapshot = LinkSync.fromJson(req.getJSONObject("snapshot")); JSONObject().put("ok", true) }
                "/v1/cover/get" -> covers[req.getString("hash")] ?: return 404 to null
                "/v1/cover/put" -> { covers[req.getString("hash")] = java.util.Base64.getDecoder().decode(req.getString("data")); JSONObject() }
                "/v1/file/stat" -> JSONObject().put("found", true).put("size", songFile.size).put("ext", "flac")
                "/v1/file/read" -> {
                    val o = req.getLong("offset").toInt(); val n = minOf(req.getInt("length"), songFile.size - o)
                    songFile.copyOfRange(o, o + maxOf(n, 0))
                }
                else -> JSONObject()
            }
            return 200 to LinkCrypto.seal(key, if (res is ByteArray) res else res.toString().toByteArray(), "$path#r")
        }
        val server = ServerSocket(LinkCrypto.PORT, 8, InetAddress.getByName("127.0.0.1"))
        Thread {
            while (!server.isClosed) runCatching {
                server.accept().use { sock ->
                    val input = java.io.BufferedInputStream(sock.getInputStream())
                    fun line() = generateSequence { input.read().takeIf { it >= 0 } }.takeWhile { it != '\n'.code }.map { it.toChar() }.joinToString("").trimEnd('\r')
                    val path = line().split(' ')[1]
                    var length = 0
                    while (true) { val h = line(); if (h.isEmpty()) break; if (h.lowercase().startsWith("content-length:")) length = h.substringAfter(':').trim().toInt() }
                    val body = ByteArray(length).also { var n = 0; while (n < length) n += input.read(it, n, length - n) }
                    val (status, out) = handle(path, body)
                    val head = "HTTP/1.1 $status X\r\nContent-Length: ${out?.size ?: 0}\r\nConnection: close\r\n\r\n"
                    sock.getOutputStream().apply { write(head.toByteArray()); out?.let(::write); flush() }
                }
            }
        }.apply { isDaemon = true; start() }
        try {
            runBlocking {
                assertTrue(Link.pair("127.0.0.1", "000000", "Test phone")!!.contains("code"))
                assertNull(Link.pair("127.0.0.1", "123 456", "Test phone"))
                assertEquals("Test PC", Link.pc.value?.name)
                assertTrue(Link.sync(), Link.status.value!!.startsWith("Synced"))
            }
            // Both sides now have both playlists, and the PC's settings.
            assertEquals(setOf("phone-pl", "pc-pl"), PlaylistStore.playlists.value.map { it.id }.toSet())
            assertEquals(setOf("phone-pl", "pc-pl"), pcSnapshot.playlists.map { it.id }.toSet())
            assertEquals(listOf("dz:2"), PlaylistStore.playlists.value.first { it.id == "pc-pl" }.trackIds)
            assertTrue(UserCollection.isBlocked("someone"))
            io.github.realkhfan.onda.player.LastSession.current!!.let { last ->
                assertEquals(listOf("dz:2"), last.items.map { it.mediaId })
                assertEquals(42_000L, last.positionMs); assertEquals(1, last.repeat)
            }
            assertTrue(java.io.File(ctx.filesDir, "session.json").exists()) // Onda's player wasn't running: it starts from it
            // Covers went both ways (checked by their hash), and the PC's folder came here.
            assertTrue(LinkSync.hashOf(phoneCover.readBytes()) in covers)
            val fromPc = PlaylistStore.playlists.value.first { it.id == "pc-pl" }
            assertEquals("pc-f", fromPc.folder)
            assertEquals(pcCoverHash, LinkSync.hashOf(java.io.File(fromPc.cover!!).readBytes()))
            assertEquals(listOf("From the PC"), PlaylistStore.folders.value.map { it.name })
            // A song played straight from the PC: read in pieces, every byte right.
            val src = io.github.realkhfan.onda.player.LinkDataSource()
            val uri = android.net.Uri.parse(io.github.realkhfan.onda.player.LinkDataSource.uriFor(LinkSync.Song(null, "Glass", "Other Band")))
            assertEquals(songFile.size.toLong(), src.open(androidx.media3.datasource.DataSpec(uri)))
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(100_000)
            while (true) { val n = src.read(buf, 0, buf.size); if (n < 0) break; out.write(buf, 0, n) }
            src.close()
            assertTrue(out.toByteArray().contentEquals(songFile))
            // From the middle (a seek).
            assertEquals(1000L, src.open(androidx.media3.datasource.DataSpec.Builder().setUri(uri).setPosition(songFile.size - 1000L).build()))
            src.close()
            // Removed on the PC since: removed here at the next sync.
            pcSnapshot = pcSnapshot.copy(playlists = pcSnapshot.playlists.filterNot { it.id == "phone-pl" })
            runBlocking { Link.sync() }
            assertEquals(listOf("pc-pl"), PlaylistStore.playlists.value.map { it.id })
            // A playlist made here while a sync waited for the PC: kept (it was overwritten by the answer), and sent next time.
            duringPut = { PlaylistStore.createPlaylist("Made meanwhile") }
            runBlocking { Link.sync() }
            assertEquals(listOf("pc-pl", "Made meanwhile"), PlaylistStore.playlists.value.map { if (it.id == "pc-pl") it.id else it.name })
            // A second sync starts by itself and takes it to the PC.
            val until = System.currentTimeMillis() + 15_000
            while ("Made meanwhile" !in pcSnapshot.playlists.map { it.name } && System.currentTimeMillis() < until) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle(); Thread.sleep(20)
            }
            assertTrue("Made meanwhile" in pcSnapshot.playlists.map { it.name })
        } finally {
            server.close()
            Link.unlink()
        }
    }
}
