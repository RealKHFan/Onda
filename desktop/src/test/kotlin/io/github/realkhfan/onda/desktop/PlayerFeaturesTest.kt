package io.github.realkhfan.onda.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

class PlayerFeaturesTest {
    private val ui = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private fun <T> onUi(block: () -> T): T = runBlocking { withContext(ui) { block() } }

    private fun wav(seconds: Double): String {
        val f = File.createTempFile("onda", ".wav")
        val rate = 22050; val n = (rate * seconds).toInt()
        val b = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + n * 2).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(rate).putInt(rate * 2).putShort(2).putShort(16).put("data".toByteArray()).putInt(n * 2)
        repeat(n) { b.putShort((Math.sin(it * 2 * Math.PI * 440 / rate) * 2000).toInt().toShort()) }
        f.writeBytes(b.array()); return f.toURI().toString()
    }

    private fun item(n: Int, uri: String, dz: Long = 0) = Player.Item("id$n", "Song $n", "Artist", "Album", null, uri, deezerId = dz)
    private fun player() = onUi { Player(CoroutineScope(ui), audioSink = "fakesink") }
    private fun waitFor(ms: Long = 5000, cond: () -> Boolean) { val end = System.currentTimeMillis() + ms; while (!onUi(cond) && System.currentTimeMillis() < end) Thread.sleep(50) }

    @Test fun volumeFollowsTheEarNotTheSignal() {
        val p = player()
        onUi { p.changeVolume(0.1) }
        assertEquals(0.001, onUi { p.outputVolume }, 1e-6) // 10% on the slider is quiet, not 60%
        onUi { p.changeVolume(0.5) }
        assertEquals(0.125, onUi { p.outputVolume }, 1e-6)
        onUi { p.close() }
    }

    @Test fun previewLinksAreFetchedRightBeforePlaying() {
        val p = player()
        val real = wav(0.5)
        var asked = 0L
        onUi { p.resolver = { i -> asked = i.deezerId; Player.Resolved(real, fullFrom = "Openverse") } }
        onUi { p.play(listOf(item(1, "", dz = 42)), 0) }
        waitFor { p.fullFrom != null }
        assertEquals(42L, asked)
        assertEquals("Openverse", onUi { p.fullFrom })
        waitFor { !p.playing }
        assertFalse("ended", onUi { p.playing })
        onUi { p.close() }
    }

    @Test fun resumesPausedWhereItWas() {
        val p = player()
        val u = wav(3.0)
        onUi { p.restore(listOf(item(1, u), item(2, u)), 1, 1200, shuffle = false, repeat = 1) }
        waitFor { p.positionMs in 1100..1400 && p.durationMs > 0 }
        assertFalse(onUi { p.playing })
        assertEquals(1, onUi { p.index })
        assertEquals(1, onUi { p.repeat })
        assertTrue(onUi { p.positionMs } in 1100..1400)
        onUi { p.close() }
    }

    @Test fun pauseAndResumeCarryOn() {
        val p = player()
        onUi { p.restore(listOf(item(1, wav(5.0))), 0, 1200, shuffle = false, repeat = 0) }
        Thread.sleep(1200)
        onUi { p.toggle() } // play after a resumed session
        Thread.sleep(600)
        assertTrue(onUi { p.playing })
        assertTrue("continues from 1.2 s", onUi { p.positionMs } in 1500..2400)
        onUi { p.toggle() }; Thread.sleep(800); onUi { p.toggle() }; Thread.sleep(300)
        assertTrue("still playing after pause and resume", onUi { p.playing })
        onUi { p.close() }
    }

    @Test fun playNextComesBeforeTheQueue() {
        val p = player()
        val u = wav(3.0)
        onUi { p.play(listOf(item(1, u), item(2, u)), 0) }
        onUi { p.enqueue(item(3, u)); p.playNext(item(4, u)) }
        assertEquals(listOf("id4", "id2", "id3"), onUi { p.upcoming().map { it.second.id } })
        onUi { p.close() }
    }

    @Test fun desktopMediaControlsSeeAndControlOnda() {
        assumeTrue("needs a session bus (run under dbus-run-session)", System.getenv("DBUS_SESSION_BUS_ADDRESS") != null)
        val p = player()
        val u = wav(5.0)
        onUi { p.restore(listOf(Player.Item("a", "Paper Moon", "Velvet Hours", "Tides", null, u)), 0, 0, false, 0) }
        val m = Mpris(p, CoroutineScope(ui), ui, onRaise = {}, onQuit = {})
        m.start()
        fun send(vararg args: String) = ProcessBuilder(listOf("dbus-send", "--session", "--print-reply", "--dest=org.mpris.MediaPlayer2.onda",
            "/org/mpris/MediaPlayer2") + args).redirectErrorStream(true).start().let { it.inputStream.bufferedReader().readText().also { _ -> it.waitFor() } }
        fun prop(name: String) = send("org.freedesktop.DBus.Properties.Get", "string:org.mpris.MediaPlayer2.Player", "string:$name")
        assertTrue(prop("PlaybackStatus").contains("\"Paused\""))
        assertTrue(prop("Metadata").let { it.contains("Paper Moon") && it.contains("Velvet Hours") })
        send("org.mpris.MediaPlayer2.Player.PlayPause") // what a media key does
        waitFor { p.playing }
        assertTrue(prop("PlaybackStatus").contains("\"Playing\""))
        assertTrue(send("org.freedesktop.DBus.Properties.Get", "string:org.mpris.MediaPlayer2", "string:Identity").contains("\"Onda\""))
        m.close(); onUi { p.close() }
    }
}

class QueueEditingTest {
    private val ui = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private fun <T> onUi(block: () -> T): T = runBlocking { withContext(ui) { block() } }
    private fun item(n: Int) = Player.Item("id$n", "Song $n", "Artist", "Album", null, "file:///nowhere/$n.mp3")

    @Test fun removeMoveAppendAndClear() {
        val p = onUi { Player(CoroutineScope(ui), audioSink = "fakesink") }
        var lastSongCalls = 0
        onUi { p.onLastSong = { lastSongCalls++ }; p.play((1..4).map(::item), 0) }
        fun next() = onUi { p.upcoming().map { it.second.id } }
        assertEquals(listOf("id2", "id3", "id4"), next())
        onUi { p.moveUpcoming(2, 0) }
        assertEquals(listOf("id4", "id2", "id3"), next())
        onUi { p.removeAt(1) } // id2
        assertEquals(listOf("id4", "id3"), next())
        onUi { p.append(listOf(item(5))) }
        assertEquals(listOf("id4", "id3", "id5"), next())
        onUi { p.clearUpcoming() }
        assertEquals(emptyList<String>(), next())
        assertEquals("id1", onUi { p.current?.id })
        onUi { p.close() }
    }
}

class CollectionRulesTest {
    @Test fun notInterestedAndPlaylistMoves() = runBlocking {
        val s = AppState(CoroutineScope(Dispatchers.Default))
        val r = io.github.realkhfan.onda.core.RemoteTrack(77, "Night Drive", "Lumen Trio", "", null, "x", 180)
        if (s.isBlocked("Lumen Trio")) s.toggleBlocked("Lumen Trio")
        if (s.isHidden(r.key)) s.toggleHidden(r.key)
        assertFalse(s.notWanted(r))
        s.toggleHidden(r.key); assertTrue(s.notWanted(r)); s.toggleHidden(r.key)
        s.toggleBlocked("LUMEN TRIO"); assertTrue(s.notWanted(r)); s.toggleBlocked("Lumen Trio")
        // A playlist holding a song that can't be shown: moves follow what you see.
        s.remember(r)
        val pl = s.createPlaylist("Test moves", listOf("gone-1", r.key, "gone-2"))
        s.remember(r.copy(id = 78, title = "Paper Moon")); s.addToPlaylist(pl, "dz:78")
        assertEquals(listOf("Night Drive", "Paper Moon"), s.playlistTracks(pl).map { it.title })
        s.movePlaylistTrack(pl, 1, 0)
        assertEquals(listOf("Paper Moon", "Night Drive"), s.playlistTracks(pl).map { it.title })
        s.deletePlaylist(pl)
    }
}
