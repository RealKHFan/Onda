package io.github.realkhfan.onda.desktop

import io.github.realkhfan.onda.core.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeaturesTest {
    private val ui = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private fun <T> onUi(block: () -> T): T = runBlocking { withContext(ui) { block() } }

    private fun t(id: String, title: String, artist: String, album: String = "A", no: Int = 0, ms: Long = 0) =
        Track(id, id, title, artist, album, artist, no, ms, "Music", null, "$title.mp3")

    private fun wav(seconds: Double): String {
        val f = File.createTempFile("onda", ".wav")
        val rate = 8000; val n = (rate * seconds).toInt()
        val b = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + n * 2).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(rate).putInt(rate * 2).putShort(2).putShort(16).put("data".toByteArray()).putInt(n * 2)
        f.writeBytes(b.array()); return f.toURI().toString()
    }

    @Test fun sortsAndSelectsSongs() {
        val list = listOf(t("1", "b", "Zed", "Y", 2, 300), t("2", "a", "Amy", "X", 1, 100), t("3", "c", "Amy", "X", 2, 200))
        assertEquals(listOf("2", "1", "3"), sorted(list, Sort.TITLE, emptyMap()).map { it.id })
        assertEquals(listOf("2", "3", "1"), sorted(list, Sort.ARTIST, emptyMap()).map { it.id })
        assertEquals(listOf("3", "1", "2"), sorted(list, Sort.PLAYS, mapOf("3" to 5, "1" to 2)).map { it.id })
        assertEquals(list, sorted(list, Sort.CUSTOM, emptyMap()))
        var sel = select(emptySet(), list, -1, 0, ctrl = true, shift = false)
        sel = select(sel, list, 0, 2, ctrl = false, shift = true)
        assertEquals(setOf("1", "2", "3"), sel)
        assertEquals(setOf("1", "3"), select(sel, list, 2, 1, ctrl = true, shift = false))
        assertTrue(select(sel, list, 2, 1, ctrl = false, shift = false).isEmpty()) // a plain click clears it
    }

    @Test fun playlistFilesRoundTripAndMatchByName() {
        val dir = Files.createTempDirectory("onda").toFile()
        val song = File(dir, "Paper Moon.mp3").apply { writeText("x") }
        val a = t(song.toURI().toString(), "Paper Moon", "Velvet Hours", ms = 200_000)
        val server = t("nav:9", "Tides", "Lumen Trio")
        val m3u = File(dir, "mix.m3u8")
        assertEquals(1 to 1, M3u.write("Night mix", listOf(a, server), m3u))
        val text = m3u.readText()
        assertTrue(song.path in text && "nav:9" !in text) // no server links (they hold the login)
        assertEquals("Night mix" to listOf(a.id, server.id), M3u.read(m3u, listOf(a, server)))
        // Another player's file: a relative path, and a name for a song that's elsewhere in your library.
        File(dir, "other.m3u").writeText("#EXTM3U\n#EXTINF:200,Velvet Hours - Paper Moon\nPaper Moon.mp3\n#EXTINF:180,Lumen Trio - Tides\n/gone/tides.flac\n")
        assertEquals("other" to listOf(a.id, server.id), M3u.read(File(dir, "other.m3u"), listOf(a, server)))
    }

    @Test fun sleepTimerStopsAtTheEndOfTheSongAndAfterTime() {
        val p = onUi { Player(CoroutineScope(ui), audioSink = "fakesink") }
        val items = listOf(Player.Item("1", "One", "A", "B", null, wav(1.0)), Player.Item("2", "Two", "A", "B", null, wav(5.0)))
        onUi { p.play(items, 0); p.sleepAfterSong = true }
        Thread.sleep(2200)
        assertEquals(1, onUi { p.index }) // the next song is ready…
        assertFalse(onUi { p.playing }) // …but paused
        onUi { p.resume(); p.sleepAt = System.currentTimeMillis() - 1 }
        Thread.sleep(800)
        assertFalse(onUi { p.playing })
        assertEquals(0L, onUi { p.sleepAt })
        onUi { p.close() }
    }

    @Test fun equalizerIsInThePlaybackChain() {
        val p = onUi { Player(CoroutineScope(ui), audioSink = "fakesink") }
        assertTrue(p.hasEqualizer)
        onUi { p.equalize(listOf(6.0, 5.0, 4.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 30.0)); p.play(listOf(Player.Item("1", "One", "A", "B", null, wav(0.6))), 0) }
        Thread.sleep(1500)
        assertTrue(onUi { p.error } == null) // plays through the equalizer without an error
        assertFalse(onUi { p.playing } && onUi { p.positionMs } == 0L)
        onUi { p.close() }
    }
}
