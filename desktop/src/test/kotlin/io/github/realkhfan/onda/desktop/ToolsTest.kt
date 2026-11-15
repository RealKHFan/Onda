package io.github.realkhfan.onda.desktop

import com.sun.net.httpserver.HttpServer
import io.github.realkhfan.onda.core.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.Executors
import kotlin.test.Test
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.focusRequester
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ToolsTest {
    private val ui = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private fun <T> onUi(block: () -> T): T = runBlocking { withContext(ui) { block() } }
    private fun dir() = Files.createTempDirectory("onda").toFile()

    private fun track(f: File, title: String, artist: String) =
        Track(f.toURI().toString(), f.toURI().toString(), title, artist, "Album", artist, 0, 0, "Music", null, f.name)

    @Test fun renameFollowsDetailsNeverOverwritesAndUndoes() {
        val d = dir()
        val a = File(d, "track01.mp3").apply { writeText("a") }
        val b = File(d, "track02.mp3").apply { writeText("b") }
        File(d, "Lumen Trio - Meadows.mp3").writeText("someone else's") // a name already taken is never overwritten
        val plan = Renames.plan(listOf(track(a, "Paper Moon", "Velvet Hours"), track(b, "Meadows", "Lumen Trio")))
        assertEquals(setOf("Velvet Hours - Paper Moon.mp3", "Lumen Trio - Meadows.mp3"), plan.map { it.to }.toSet())
        val r = Renames.apply(plan)
        assertEquals(mapOf(a.toURI().toString() to File(d, "Velvet Hours - Paper Moon.mp3").toURI().toString()), r.moved)
        assertEquals(listOf("track02.mp3"), r.failed)
        assertEquals("b", b.readText())
        Renames.apply(r.undo)
        assertEquals("a", a.readText())
    }

    @Test fun downloadSavesAudioOnlyAndSkipsExisting() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        fun route(path: String, type: String, body: String) = server.createContext(path) { ex ->
            ex.responseHeaders.add("Content-Type", type)
            val bytes = body.toByteArray(); ex.sendResponseHeaders(200, bytes.size.toLong()); ex.responseBody.use { it.write(bytes) }
        }
        route("/song", "audio/flac", "fLaC-audio")
        route("/refused", "application/json", """{"subsonic-response":{"status":"failed"}}""")
        server.start()
        try {
            val base = "http://127.0.0.1:${server.address.port}"
            val d = dir()
            val id = Downloads.save(d, Downloads.Item("Paper Moon", "Velvet Hours", "$base/song", null))
            val f = File(d, "Velvet Hours - Paper Moon.flac")
            assertEquals(f.toURI().toString(), id)
            assertEquals("fLaC-audio", f.readText())
            assertNull(Downloads.save(d, Downloads.Item("Paper Moon", "Velvet Hours", "$base/song", null))) // already there
            assertFailsWith<IOException> { Downloads.save(d, Downloads.Item("Tides", "Velvet Hours", "$base/refused", null)) }
            assertEquals(listOf("Velvet Hours - Paper Moon.flac"), d.list()!!.toList()) // no JSON saved as a song, no leftovers
        } finally {
            server.stop(0)
        }
    }

    @Test fun movedSongsKeepTheirLikesAndPlaylists() {
        val s = onUi { AppState(CoroutineScope(ui)) }
        val old = "file:/tmp/onda-test/old.mp3"
        val new = "file:/tmp/onda-test/Artist - Song.mp3"
        val pl = onUi { s.createPlaylist("Moved test", listOf(old)) }
        try {
            onUi { if (!s.isLiked(old)) s.toggleLike(old); s.migrateIds(mapOf(old to new)) }
            assertTrue(onUi { s.isLiked(new) && !s.isLiked(old) })
            assertEquals(listOf(new), onUi { s.collection.playlists.first { it.id == pl }.trackIds })
        } finally {
            onUi { s.deletePlaylist(pl); if (s.isLiked(new)) s.toggleLike(new) }
        }
    }

    @Test fun dailyBackupKeepsTheNewestFiveOfItsOwn() {
        val s = onUi { AppState(CoroutineScope(ui)) }
        val d = dir()
        (1..6).forEach { i -> File(d, "onda-backup-2020-01-0$i.zip").apply { writeText("x"); setLastModified(1_600_000_000_000L + i * 86_400_000L) } }
        File(d, "holiday.zip").writeText("not ours")
        val before = s.settings.backupDir
        try {
            onUi { s.setBackupDir(d.path) }
            s.dailyBackup()
            val ours = d.list()!!.filter { it.startsWith("onda-backup-") }.sorted()
            assertEquals(5, ours.size)
            assertTrue(ours.last().startsWith("onda-backup-20") && "2020" !in ours.last()) // today's is there
            assertTrue("onda-backup-2020-01-01.zip" !in ours && File(d, "holiday.zip").exists())
            s.dailyBackup() // not again the same day
            assertEquals(5, d.list()!!.count { it.startsWith("onda-backup-") })
        } finally {
            onUi { s.setBackupDir(before) }
        }
    }

    private fun tone(amplitude: Int, seconds: Int = 4): String {
        val f = File.createTempFile("onda", ".wav")
        val rate = 22050; val n = rate * seconds
        val b = java.nio.ByteBuffer.allocate(44 + n * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + n * 2).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(rate).putInt(rate * 2).putShort(2).putShort(16).put("data".toByteArray()).putInt(n * 2)
        repeat(n) { b.putShort((Math.sin(it * 2 * Math.PI * 440 / rate) * amplitude).toInt().toShort()) }
        f.writeBytes(b.array()); return f.toURI().toString()
    }

    @Test fun loudnessIsMeasuredAndEvenedOut() {
        onUi { Player(CoroutineScope(ui), audioSink = "fakesink").close() } // starts GStreamer
        val quiet = Loudness.analyse(tone(1000))!!
        val loud = Loudness.analyse(tone(16000))!!
        assertEquals(20 * Math.log10(16.0), quiet.db - loud.db, 0.5) // 16 times the amplitude: about 24 dB less gain
        assertEquals(16000 / 32768.0, loud.peak, 0.02)
        assertNull(Loudness.analyse(File("/no/such/song.mp3").toURI().toString()))
        assertTrue(Loudness.factorFor(loud.db, loud.peak) < 1.0) // a loud song is turned down
        assertTrue(Loudness.factorFor(quiet.db, 1.0) <= 1.0) // a song already peaking at full scale is never pushed past it
        assertEquals(2.0, Loudness.factorFor(20.0, 0.5), 1e-9) // a quiet one goes up only as far as its peak allows
    }

    @Test fun previewsNeverShowTheFullLength() {
        val p = onUi { Player(CoroutineScope(ui), audioSink = "fakesink") }
        val wav = tone(1000, 2)
        p.resolver = { kotlinx.coroutines.delay(300); Player.Resolved(wav) }
        onUi { p.play(listOf(Player.Item("dz:1", "Song", "Artist", "Album", null, "", preview = true, deezerId = 1, durationMs = 200_000)), 0) }
        assertEquals(0L, onUi { p.durationMs }) // not 3:20 while the link is fetched
        Thread.sleep(1500)
        assertTrue(onUi { p.durationMs } in 1..30_000)
        onUi { p.close() }
    }

    @Test fun editedDetailsShowAndBlankFieldsKeepTheirValue() {
        io.github.realkhfan.onda.data.Overrides.load(dir())
        val t = track(File("/m/x.mp3"), "trk01", "Unknown artist")
        io.github.realkhfan.onda.data.Overrides.edit(t, " Paper Moon ", "Velvet Hours", "")
        val shown = io.github.realkhfan.onda.data.Overrides.apply(listOf(t), io.github.realkhfan.onda.data.Overrides.meta.value).single()
        assertEquals(Triple("Paper Moon", "Velvet Hours", "Album"), Triple(shown.title, shown.artist, shown.album))
        io.github.realkhfan.onda.data.Overrides.restore(t.id)
        assertTrue(io.github.realkhfan.onda.data.Overrides.meta.value.isEmpty())
        io.github.realkhfan.onda.data.Overrides.load(Store.data)
    }

    @Test fun spotifyCsvBecomesAPlaylistOfYourOwnSongs() {
        val s = onUi { AppState(CoroutineScope(ui)) }
        val f = File(dir(), "Paper Moon.mp3").apply { writeText("a") }
        val mine = track(f, "Paper Moon", "Velvet Hours")
        val csv = File(dir(), "night_drive.csv").apply {
            writeText("\"Track URI\",\"Track Name\",\"Album Name\",\"Artist Name(s)\"\r\n\"spotify:track:1\",\"Paper Moon\",\"Tides\",\"Velvet Hours\"\r\n")
        }
        try {
            onUi { s.local = listOf(mine) }
            runBlocking(ui) { SpotifyImports.run(csv, s) }
            assertEquals(listOf(mine.id), onUi { s.collection.playlists.first { it.name == "night drive" }.trackIds })
            assertTrue(SpotifyImports.state.result!!.contains("1 songs in your library"))
        } finally {
            onUi { s.collection.playlists.filter { it.name == "night drive" }.forEach { s.deletePlaylist(it.id) } }
        }
    }
}

class TextBoxTest {
    // A plain text field lets Space and arrows through to the window, where they pause or seek the music.
    @OptIn(androidx.compose.ui.InternalComposeUiApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Test fun spaceAndArrowsTypedInATextFieldNeverReachThePlayerShortcuts() {
        var text by androidx.compose.runtime.mutableStateOf("ab")
        val focus = androidx.compose.ui.focus.FocusRequester()
        val scene = androidx.compose.ui.ImageComposeScene(400, 200) {
            TextBox(text, { text = it }, modifier = androidx.compose.ui.Modifier.focusRequester(focus))
            androidx.compose.runtime.LaunchedEffect(Unit) { focus.requestFocus() }
        }
        repeat(3) { scene.render(it * 16_000_000L) }
        val K = androidx.compose.ui.input.key.Key
        val down = androidx.compose.ui.input.key.KeyEventType.KeyDown
        fun used(k: androidx.compose.ui.input.key.Key, ctrl: Boolean = false) =
            scene.sendKeyEvent(androidx.compose.ui.input.key.KeyEvent(k, down, isCtrlPressed = ctrl))
        assertTrue(used(K.Spacebar))
        assertTrue(used(K.DirectionUp))
        assertTrue(!used(K.F, ctrl = true)) // Ctrl+F still opens Search
        assertTrue(!used(K.Escape)) // and Esc still goes back
        scene.close()
    }
}
