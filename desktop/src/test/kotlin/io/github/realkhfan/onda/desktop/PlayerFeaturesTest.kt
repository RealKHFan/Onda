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

    @Test fun aShuffledQueueComesBackInTheSameOrder() {
        val p = player()
        val u = wav(1.0)
        val items = (1..6).map { item(it, u) }
        val order = listOf(3, 0, 5, 1, 4, 2)
        onUi { p.restore(items, 5, 0, shuffle = true, repeat = 0, playOrder = order) }
        assertEquals(order, onUi { p.playOrder })
        assertEquals(listOf(1, 4, 2), onUi { p.upcoming().map { it.first } }) // after index 5 in that order
        onUi { p.restore(items, 0, 0, shuffle = true, repeat = 0, playOrder = listOf(0, 1)) } // doesn't fit: a new order
        assertEquals((0..5).toList(), onUi { p.playOrder.sorted() })
        onUi { p.close() }
        // The session file keeps the order and when it last played.
        val file = File(Store.data, "session.json")
        val before = file.takeIf { it.exists() }?.readBytes()
        try {
            Store.saveSession(Store.Session(items, 5, 700, true, 2, order, 1234))
            val back = Store.session()!!
            assertEquals(order, back.order); assertEquals(1234L, back.at); assertEquals(5, back.index); assertEquals(700L, back.positionMs)
        } finally {
            if (before != null) file.writeBytes(before) else file.delete()
        }
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
    @Test fun crossfadesBetweenSongsButNotWithinAnAlbum() {
        val p = player()
        val a = wav(3.0); val b = wav(3.0)
        onUi { p.crossfade = 1 }
        // Two albums: the second song fades in on the spare player before the first one ends.
        onUi { p.play(listOf(item(1, a).copy(album = "One"), item(2, b).copy(album = "Two")), 0) }
        waitFor(6000) { p.crossfading }
        assertTrue("fading", onUi { p.crossfading })
        assertEquals(0, onUi { p.index })
        waitFor(4000) { p.index == 1 && !p.crossfading }
        assertEquals(1, onUi { p.index })
        assertTrue(onUi { p.playing })
        // Same album: gapless, no fade.
        onUi { p.play(listOf(item(3, a), item(4, b)), 0) }
        var faded = false
        val end = System.currentTimeMillis() + 4500
        while (System.currentTimeMillis() < end) { if (onUi { p.crossfading }) faded = true; Thread.sleep(50) }
        assertFalse("no crossfade within an album", faded)
        assertEquals(1, onUi { p.index })
        onUi { p.close() }
    }
}

class QueueEditingTest {
    private val ui = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private fun <T> onUi(block: () -> T): T = runBlocking { withContext(ui) { block() } }
    // A real (silent) file: a missing one makes the player skip ahead on its own while the test edits the queue.
    private val silence: String by lazy {
        val f = File.createTempFile("onda", ".wav")
        val rate = 8000; val n = rate * 20
        val b = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + n * 2).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(rate).putInt(rate * 2).putShort(2).putShort(16).put("data".toByteArray()).putInt(n * 2)
        f.writeBytes(b.array()); f.toURI().toString()
    }
    private fun item(n: Int) = Player.Item("id$n", "Song $n", "Artist", "Album", null, silence)

    @Test fun previousGoesBackToWhatActuallyPlayed() {
        val p = onUi { Player(CoroutineScope(ui), audioSink = "fakesink") }
        fun cur() = onUi { p.current?.id }
        // Paused, so no song passes 3 s (Previous would start it over) on a slow machine.
        onUi { p.play((1..6).map(::item), 0); p.pause() }
        onUi { p.jumpTo(4); p.pause() } // skips 2-4
        onUi { p.previous() }
        assertEquals("id1", cur()) // what played before, not id4
        onUi { p.next() }
        assertEquals("id5", cur()) // and forward to where it was
        onUi { p.toggleShuffle(); p.next() }
        onUi { p.previous() }
        assertEquals("id5", cur())
        onUi { p.toggleShuffle(); p.previous() } // shuffle off: Previous still follows what played
        assertEquals("id1", cur())
        // A queue from the phone keeps its order, shuffle and repeat.
        onUi { p.play(listOf(item(9), item(8), item(7)), 0, here = true); p.adopt(shuffle = true, repeat = 1) }
        assertEquals(listOf("id8", "id7"), onUi { p.upcoming().map { it.second.id } })
        assertEquals(true to 1, onUi { p.shuffle to p.repeat })
        onUi { p.close() }
    }

    @Test fun skippingFastNeverSkipsAnExtraSong() {
        val p = onUi { Player(CoroutineScope(ui), audioSink = "fakesink") }
        repeat(20) {
            onUi { p.play((1..6).map(::item), 0); p.pause() }
            repeat(4) { onUi { p.next() } } // GStreamer reports the stopped songs late: that must not count against the next one
            Thread.sleep(150)
            assertEquals("id5", onUi { p.current?.id })
            assertEquals(null, onUi { p.error })
        }
        onUi { p.close() }
    }

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

    @Test fun playingTheSameSongAgainCountsAgain() = runBlocking {
        val s = AppState(CoroutineScope(Dispatchers.Default))
        val i = Player.Item("test:loop", "Loop Song", "Lumen Trio", "", null, "file:///nowhere/loop.mp3", durationMs = 180_000)
        val before = s.playCounts[i.id] ?: 0
        s.played(i); s.played(i) // a song on repeat-one: two loops
        assertEquals(before + 2, s.playCounts[i.id] ?: 0)
    }

    @Test fun settingsPinsAndSearchesFromThePhoneAreTakenIn() = runBlocking {
        val s = AppState(CoroutineScope(Dispatchers.Default))
        val before = s.linkSnapshot(emptyMap(), HashMap())
        val shared = s.sharedSettings()
        s.applyLink(before.copy(settings = shared + ("crossfade" to "5") + ("apple" to (!s.settings.apple).toString()),
            pins = listOf("pin-test"), searches = listOf("tides", "velvet")), HashMap())
        assertEquals(5, s.settings.crossfade)
        assertEquals(shared["apple"] != "true", s.settings.apple)
        assertEquals(listOf("pin-test"), s.settings.pinned)
        assertEquals(listOf("tides", "velvet"), s.settings.searches)
        assertEquals("5", s.linkSnapshot(emptyMap(), HashMap()).settings!!["crossfade"]) // and it goes back out as it is now
        s.applyLink(before.copy(settings = shared, pins = before.pins, searches = before.searches), HashMap())
        assertEquals(shared, s.sharedSettings())
    }

    @Test fun draggingAPlaylistInTheSidebarMovesIt() = runBlocking {
        val s = AppState(CoroutineScope(Dispatchers.Default))
        val order = s.settings.playlistOrder
        s.setPlaylistOrder("Custom")
        val a = s.createPlaylist("Drag A"); val b = s.createPlaylist("Drag B")
        val shown = { s.sidebarPlaylists.filter { it.folder == null }.map { it.id } }
        val before = shown()
        s.dragPlaylist(before, before.indexOf(b), before.indexOf(a)) // B up onto A: B takes A's place, A moves aside
        assertEquals(listOf(b, a), shown().filter { it == a || it == b })
        assertEquals(before.indexOf(a), shown().indexOf(b))
        s.deletePlaylist(a); s.deletePlaylist(b); s.setPlaylistOrder(order)
    }

}
