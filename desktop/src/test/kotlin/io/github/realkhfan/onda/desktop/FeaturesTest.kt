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

    @Test fun gaplessHandsOverTheNextFile() {
        val p = onUi { Player(CoroutineScope(ui), audioSink = "fakesink") }
        var ends = 0
        val items = listOf(Player.Item("1", "One", "A", "B", null, wav(1.0)), Player.Item("2", "Two", "A", "B", null, wav(3.0)))
        onUi { p.onLastSong = { ends++ }; p.play(items, 0) }
        Thread.sleep(1700)
        assertEquals(1, onUi { p.index })
        assertTrue(onUi { p.playing })
        assertTrue(onUi { p.positionMs } < 1500) // the second song, from its start
        assertEquals(1, ends) // "similar songs when the queue ends" still hears about the last song
        onUi { p.close() }
    }

    @Test fun theSameFileTwiceInARowMovesOnOnlyWhenTheFirstEnds() {
        val p = onUi { Player(CoroutineScope(ui), audioSink = "fakesink") }
        val u = wav(1.5)
        onUi { p.play(listOf(Player.Item("a", "One", "A", "B", null, u), Player.Item("b", "One again", "A", "B", null, u)), 0) }
        Thread.sleep(600)
        assertEquals(0, onUi { p.index }) // the next song was lined up early, but this one still plays
        Thread.sleep(1700)
        assertEquals(1, onUi { p.index })
        onUi { p.close() }
    }

    @Test fun wantedSongsLeaveTheListOnceInTheLibraryAndPicksAreKept() {
        val s = onUi { AppState(CoroutineScope(ui)) }
        val mine = t("file:/tmp/onda-test/paper.mp3", "Paper Moon", "Velvet Hours")
        val wanted = io.github.realkhfan.onda.core.RemoteTrack(424242, "Paper Moon", "Velvet Hours", "Tides", null, null, 200)
        val other = io.github.realkhfan.onda.core.RemoteTrack(434343, "Open Fields", "Lumen Trio", "Meadows", null, null, 200)
        try {
            onUi { s.local = listOf(mine); if (!s.isWanted(wanted)) s.toggleWant(wanted); if (!s.isWanted(other)) s.toggleWant(other) }
            assertEquals(listOf(wanted.key), onUi { s.cleanWant().map { it.key } })
            assertTrue(onUi { !s.isWanted(wanted) && s.isWanted(other) })
            onUi { s.setLyricsPick(mine.id, 123, 500) }
            assertEquals(123L to 500L, Store.settings().picks[mine.id])
        } finally {
            onUi { if (s.isWanted(other)) s.toggleWant(other); s.setLyricsPick(mine.id, 0, 0) }
        }
    }

    @Test fun autostartEntryStartsInTheBackground() {
        val s = onUi { AppState(CoroutineScope(ui)) }
        val before = s.autostart
        try {
            onUi { s.setAutostart(true) }
            val f = File(System.getenv("XDG_CONFIG_HOME") ?: (System.getProperty("user.home") + "/.config"), "autostart/io.github.realkhfan.Onda.desktop")
            assertTrue(f.readText().contains("--background") && f.readText().contains("Exec=\""))
            onUi { s.setAutostart(false) }
            assertFalse(f.exists())
        } finally {
            onUi { s.setAutostart(before) }
        }
    }

    @Test fun qualityBadgeAndSleepFadeAndRecentSearches() {
        val f = File(java.net.URI(wav(1.0)))
        val info = Scanner.audioInfo(f)!!
        assertEquals("WAV · 16-bit · 8 kHz", info.badge)
        assertEquals("MP3 · 320 kbps", Scanner.AudioInfo("MP3", 320, 44100, 0, "Stereo", 1, 0).badge)
        assertEquals("FLAC · 24-bit · 96 kHz", Scanner.AudioInfo("FLAC", 2400, 96000, 24, "Stereo", 1, 0).badge)
        assertEquals("FLAC · 16-bit · 44.1 kHz", Scanner.AudioInfo("FLAC", 900, 44100, 16, "Stereo", 1, 0).badge)

        val p = onUi { Player(CoroutineScope(ui), audioSink = "fakesink") }
        onUi { p.changeVolume(1.0); p.play(listOf(Player.Item("1", "One", "A", "B", null, wav(5.0))), 0); p.sleepAt = System.currentTimeMillis() + 15_000; p.refreshVolume() }
        Thread.sleep(500)
        assertEquals(0.5, onUi { p.outputVolume }, 0.05) // halfway through the 30-second fade
        onUi { p.sleepAt = 0; p.refreshVolume() }
        assertEquals(1.0, onUi { p.outputVolume }, 0.001)
        onUi { p.close() }

        val s = onUi { AppState(CoroutineScope(ui)) }
        val before = s.settings.searches
        try {
            onUi { s.rememberSearch("Velvet Hours"); s.rememberSearch("lumen"); s.rememberSearch("velvet hours"); s.rememberSearch("x") }
            assertEquals(listOf("velvet hours", "lumen"), s.settings.searches.take(2)) // newest first, no duplicates, no one-letter searches
        } finally {
            onUi { s.forgetSearch(null); before.reversed().forEach(s::rememberSearch) }
        }
    }

    @Test fun playlistCoverIsCopiedAndRemovable() {
        val s = onUi { AppState(CoroutineScope(ui)) }
        val img = File.createTempFile("cover", ".png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val id = onUi { s.createPlaylist("Cover test") }
        try {
            onUi { s.editPlaylist(id, "Night drive", "  For late roads  ", img, removeCover = false) }
            val pl = s.collection.playlists.first { it.id == id }
            assertEquals("Night drive" to "For late roads", pl.name to pl.description)
            val copy = File(pl.cover!!)
            assertTrue(copy.exists() && copy.path != img.path)
            onUi { s.editPlaylist(id, "", "", null, removeCover = true) }
            assertEquals("Night drive", s.collection.playlists.first { it.id == id }.name) // a blank name keeps the old one
            assertFalse(copy.exists())
        } finally {
            onUi { s.deletePlaylist(id) }
        }
    }

    @Test fun albumLoudnessAndOutputsAndRecentlyAdded() {
        assertEquals(null, Loudness.albumGain(listOf("unknown-1", "unknown-2")))
        val list = listOf(t("a", "x", "A"), t("b", "y", "A"), t("c", "z", "A"))
        assertEquals(listOf("b", "c", "a"), sorted(list, Sort.ADDED, emptyMap(), mapOf("a" to 1L, "b" to 3L, "c" to 2L)).map { it.id })
        val sinks = Outputs.parse("""[{"index":1,"name":"alsa_output.usb-headset","description":"USB Headset"},{"index":2,"name":"alsa_output.hdmi","description":""}]""")
        assertEquals(listOf("USB Headset", "alsa_output.hdmi"), sinks.map { it.label })
        assertEquals("alsa_output.usb-headset", sinks[0].name)
    }

    @Test fun longSongsResumeWhereYouLeftThem() {
        val s = onUi { AppState(CoroutineScope(ui)) }
        onUi { s.leftAt("mix", 30 * 60_000L, 60 * 60_000L); s.leftAt("song", 60_000L, 4 * 60_000L) }
        assertEquals(30 * 60_000L, s.resumeAt("mix"))
        assertEquals(0L, s.resumeAt("song")) // short songs always start at the beginning
        onUi { s.leftAt("mix", 59 * 60_000L + 30_000L, 60 * 60_000L) }
        assertEquals(0L, s.resumeAt("mix")) // left near the end: starts over next time

        val p = onUi { Player(CoroutineScope(ui), audioSink = "fakesink") }
        var left: Pair<String, Long>? = null
        onUi { p.resumeAt = { if (it.id == "1") 2000 else 0 }; p.onLeave = { i, pos, _ -> left = i.id to pos } }
        onUi { p.play(listOf(Player.Item("1", "One", "A", "B", null, wav(4.0)), Player.Item("2", "Two", "A", "B", null, wav(4.0))), 0) }
        Thread.sleep(1200)
        assertTrue(onUi { p.positionMs } >= 1900) // started at the saved spot
        onUi { p.next() }
        assertEquals("1", left?.first)
        onUi { p.close() }
    }

    @Test fun smartPlaylistsAndUndoingADelete() {
        val s = onUi { AppState(CoroutineScope(ui)) }
        val mine = t("file:/tmp/onda-test/smart.mp3", "Smart Song", "Velvet Hours")
        val preview = io.github.realkhfan.onda.core.RemoteTrack(515151, "Tides", "Lumen Trio", "Meadows", null, null, 200)
        try {
            onUi { s.local = listOf(mine); if (!s.isLiked(mine.id)) s.toggleLike(mine.id) }
            assertTrue(onUi { s.smart(AppState.Smart.NEVER).any { it.id == mine.id } })
            assertTrue(onUi { s.smart(AppState.Smart.FORGOTTEN).any { it.id == mine.id } })
            val id = onUi { s.remember(preview); s.createPlaylist("Undo test", listOf(mine.id, preview.key)) }
            val pl = s.collection.playlists.first { it.id == id }
            val remotes = s.collection.remote.filter { it.key in pl.trackIds }
            onUi { s.deletePlaylist(id) }
            onUi { s.restorePlaylist(pl, remotes) }
            assertEquals(2, onUi { s.playlistTracks(id).size }) // the preview comes back too
            onUi { s.deletePlaylist(id) }
        } finally {
            onUi { if (s.isLiked(mine.id)) s.toggleLike(mine.id) }
        }
    }

    @Test fun mixPreviewsThatWereNeverSavedStillPlay() {
        val s = onUi { AppState(CoroutineScope(ui)) }
        val shown = io.github.realkhfan.onda.core.RemoteTrack(626262, "Sombrero", "Night Kites", "Dunes", null, null, 180)
        val item = onUi { s.remember(shown); s.item(s.asTrack(shown)) } // a mix row: seen on the page, not in your collection
        assertTrue(item.preview)
        assertEquals(626262L, item.deezerId) // the player fetches the link instead of trying an empty one
    }

    @Test fun openWithOndaHandsFilesToTheRunningOne() {
        val p = onUi { Player(CoroutineScope(ui), audioSink = "fakesink") }
        val opened = ArrayList<String>()
        var raised = false
        val m = Mpris(p, CoroutineScope(ui), ui, onRaise = { raised = true }, onQuit = {}, onOpen = { opened += it })
        m.start()
        try {
            val song = File(java.net.URI(wav(1.0))).toURI().toString()
            assertTrue(Instance.raiseExisting(listOf(song)))
            Thread.sleep(500)
            assertEquals(listOf(song), opened)
            assertTrue(raised)
            val s = onUi { AppState(CoroutineScope(ui)) }
            val items = onUi { s.itemsForFiles(listOf(song, "file:/no/such/file.mp3")) }
            assertEquals(1, items.size) // a missing file is skipped
            assertEquals(song, items[0].uri)
            onUi { s.setWindow(listOf(10, 20, 900, 700), maximized = true) }
            assertEquals(listOf(10, 20, 900, 700) to true, Store.settings().let { it.window to it.maximized })
            onUi { s.setWindow(emptyList(), maximized = false) }
        } finally {
            m.close(); onUi { p.close() }
        }
    }

    @Test fun playlistCopiesOrderHidingAndLooks() {
        val s = onUi { AppState(CoroutineScope(ui)) }
        val mine = t("file:/tmp/onda-test/hide.mp3", "Hide Me", "Velvet Hours")
        val before = s.settings
        val b = onUi { s.createPlaylist("Bravo", listOf(mine.id)) }
        val a = onUi { s.createPlaylist("alpha", emptyList()) }
        try {
            val copy = onUi { s.duplicatePlaylist(b)!! }
            assertEquals("Bravo (copy)" to listOf(mine.id), s.collection.playlists.first { it.id == copy }.let { it.name to it.trackIds })
            onUi { s.setPlaylistOrder("A–Z") }
            val names = s.sidebarPlaylists.map { it.name }
            assertTrue(names.indexOf("alpha") < names.indexOf("Bravo") && names.indexOf("Bravo") < names.indexOf("Bravo (copy)"))
            onUi { s.togglePin(copy) }
            assertEquals(copy, s.sidebarPlaylists.first().id) // pinned stays on top whatever the order
            onUi { s.local = listOf(mine); s.toggleHidden(mine.id) }
            assertTrue(onUi { s.library.none { it.id == mine.id } && s.hiddenSongs >= 1 })
            onUi { s.showHiddenSongs() }
            assertTrue(onUi { s.library.any { it.id == mine.id } })
            onUi { s.setAccent(0xFF2EC4B6); s.setLyricsScale(5f) }
            assertEquals(0xFF2EC4B6, Store.settings().accent)
            assertEquals(androidx.compose.ui.graphics.Color(0xFF2EC4B6.toInt()), Accent)
            assertEquals(1.8f, s.settings.lyricsScale)
            onUi { s.deletePlaylist(copy) }
        } finally {
            onUi {
                s.deletePlaylist(a); s.deletePlaylist(b)
                s.setAccent(before.accent); s.setLyricsScale(before.lyricsScale); s.setPlaylistOrder(before.playlistOrder)
                s.settings.pinned.filterNot { it in before.pinned }.forEach(s::togglePin)
            }
        }
    }

    @Test fun serverSongsAreKeptForReplays() {
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        var hits = 0
        server.createContext("/song") { ex -> hits++; val b = "audio-bytes".toByteArray(); ex.responseHeaders.add("Content-Type", "audio/mpeg"); ex.sendResponseHeaders(200, b.size.toLong()); ex.responseBody.use { it.write(b) } }
        server.start()
        try {
            val id = "nav:cache-test-${System.nanoTime()}"
            SongCache.keep(CoroutineScope(ui), id, "http://127.0.0.1:${server.address.port}/song")
            Thread.sleep(800)
            assertEquals("audio-bytes", SongCache.get(id)?.readText())
            SongCache.keep(CoroutineScope(ui), id, "http://127.0.0.1:${server.address.port}/song")
            Thread.sleep(300)
            assertEquals(1, hits) // kept: not fetched again
        } finally { server.stop(0) }
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

    @Test fun newSongsAreCopiedOnceIntoTheChosenFolder() {
        val s = onUi { AppState(CoroutineScope(ui)) }
        val music = Files.createTempDirectory("onda-music").toFile()
        val backup = Files.createTempDirectory("onda-copy").toFile()
        val backup2 = Files.createTempDirectory("onda-copy2").toFile()
        fun song(path: String) = File(music, path).apply { parentFile.mkdirs(); File(java.net.URI(wav(0.2))).copyTo(this) }
        fun later(block: suspend () -> Unit) = runBlocking { withContext(ui) { block() } }
        fun waitForCheck() {
            val end = System.currentTimeMillis() + 15_000
            while (onUi { s.copyNote?.startsWith("Checked") != true }) { check(System.currentTimeMillis() < end) { "no copy: ${onUi { s.copyNote }}" }; Thread.sleep(50) }
        }
        val before = onUi { s.settings.folders }
        try {
            val tides = song("Lumen Trio/Sea/01 Tides.wav")
            song("Onda from phone/Phone Artist - Sent.wav")
            onUi { before.forEach(s::removeFolder); s.addFolder(music.path) }
            later { s.rescanNow() }
            // All songs: copied into the same folders; the one from the phone isn't sent back.
            onUi { s.setCopyDir(backup.path, everything = true) }
            waitForCheck()
            assertTrue(File(backup, "Lumen Trio/Sea/01 Tides.wav").readBytes().contentEquals(tides.readBytes()))
            assertFalse(File(backup, "Onda from phone").exists())
            assertTrue(backup.walk().none { it.name.endsWith(".part") })
            // Later: only the new song is copied; one already copied isn't copied again (even if deleted there).
            File(backup, "Lumen Trio/Sea/01 Tides.wav").delete()
            song("Lumen Trio/Sea/02 Waves.wav")
            later { s.rescanNow(); s.copyNewSongs() }
            assertTrue(File(backup, "Lumen Trio/Sea/02 Waves.wav").isFile)
            assertFalse(File(backup, "Lumen Trio/Sea/01 Tides.wav").exists())
            assertEquals("Checked", onUi { s.copyNote }?.substringBefore(" at"))
            // Only new ones: what's there now stays out; a song added afterwards goes in.
            onUi { s.setCopyDir(backup2.path) }
            waitForCheck()
            assertEquals(emptyList(), backup2.listFiles()!!.toList())
            song("Night Owls/Late/03 Owl.wav")
            later { s.rescanNow(); s.copyNewSongs() }
            assertEquals(listOf("Night Owls/Late/03 Owl.wav"), backup2.walk().filter { it.isFile }.map { it.relativeTo(backup2).path }.toList())
            // The folder isn't there (a drive not connected): nothing happens, and it says so.
            backup2.deleteRecursively()
            later { s.copyNewSongs() }
            assertTrue(onUi { s.copyNote }!!.contains("isn't there"))
        } finally {
            onUi { s.setCopyDir(null); s.removeFolder(music.path); before.forEach(s::addFolder) }
            later { s.rescanNow() }
            listOf(music, backup, backup2).forEach { it.deleteRecursively() }
        }
    }
}
