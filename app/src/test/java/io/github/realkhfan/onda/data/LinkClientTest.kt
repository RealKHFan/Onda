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
        PlaylistStore.fromSync(listOf(PlaylistStore.Playlist("phone-pl", "On the phone", listOf("dz:1"))), emptyList(), emptyList(),
            listOf(RemoteTrack(1, "Paper Moon", "Velvet Hours", "Moons", null, null, 180)))
        var token = ""
        var pcSnapshot = LinkSync.Snapshot(playlists = listOf(LinkSync.Playlist("pc-pl", "On the PC", "", listOf(LinkSync.Song("dz:2", "Tides", "Lumen Trio")))),
            catalog = listOf(RemoteTrack(2, "Tides", "Lumen Trio", "Sea", null, null, 200)), blocked = listOf("someone"))
        // A bare HTTP server (the JDK's isn't on Android's test classpath): one request per connection.
        fun handle(path: String, body: ByteArray): Pair<Int, ByteArray?> {
            val (key, aad) = if (path == "/v1/pair") LinkCrypto.pairKey("123456") to "/v1/pair" else LinkCrypto.linkKey(token) to path
            val req = LinkCrypto.open(key, body, aad)?.let { JSONObject(String(it)) } ?: return 403 to null
            val res = when (path) {
                "/v1/pair" -> { token = LinkCrypto.newToken(); JSONObject().put("token", token).put("pc", "pc-1").put("name", "Test PC") }
                "/v1/sync/get" -> JSONObject().put("version", "v1").put("snapshot", LinkSync.toJson(pcSnapshot))
                "/v1/sync/put" -> { pcSnapshot = LinkSync.fromJson(req.getJSONObject("snapshot")); JSONObject().put("ok", true) }
                else -> JSONObject()
            }
            return 200 to LinkCrypto.seal(key, res.toString().toByteArray(), "$path#r")
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
            // Removed on the PC since: removed here at the next sync.
            pcSnapshot = pcSnapshot.copy(playlists = pcSnapshot.playlists.filterNot { it.id == "phone-pl" })
            runBlocking { Link.sync() }
            assertEquals(listOf("pc-pl"), PlaylistStore.playlists.value.map { it.id })
        } finally {
            server.close()
            Link.unlink()
        }
    }
}
