package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.FileNames
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Source
import io.github.realkhfan.onda.core.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadsTest {

    private val cfg = Subsonic.Config("http://192.168.1.20:4533", "rkf", "c19b2d", "tok")
    private fun track(id: String) = Track(id, "u", "Paper Moon", "Velvet Hours", "Tides", "Velvet Hours", 1, 1000, "F", null)

    @Test fun onlyServerSongsAndAllowedFreeSongsDownload() {
        val item = Downloads.forTrack(track("srv:abc"), cfg)!!
        assertTrue(item.url.startsWith("http://192.168.1.20:4533/rest/download"))
        assertTrue("id=abc" in item.url)
        assertEquals("srv:abc", item.replaces)
        assertNull(Downloads.forTrack(track("content://x/document/y"), cfg)) // already a file
        assertNull(Downloads.forTrack(track("srv:abc"), null))

        val archive = RemoteTrack(0, "First Light", "Coastline Kids", "", null, null, 0, Source.ARCHIVE, ref = "np_042/t01.flac", canDownload = true)
        assertEquals("flac", Downloads.forRemote(archive)!!.ext)
        assertNull(Downloads.forRemote(archive.copy(canDownload = false)))
        assertNull(Downloads.forRemote(RemoteTrack(5, "Hit", "Band", "", null, "https://cdn/p.mp3", 30, canDownload = true))) // a preview
    }

    @Test fun catalogsSayWhatMayBeKept() {
        val audius = FreeCatalogs.parseAudius("""{"data":[
            {"id":"a1","title":"Free One","user":{"name":"Velvet Hours"},"is_downloadable":true,"is_download_gated":false},
            {"id":"a2","title":"Paid One","user":{"name":"Velvet Hours"},"is_downloadable":true,"is_download_gated":true},
            {"id":"a3","title":"Stream Only","user":{"name":"Velvet Hours"}}]}""")
        assertEquals(listOf(true, false, false), audius.map { it.canDownload })
        val jamendo = FreeCatalogs.parseJamendo("""{"headers":{"status":"success"},"results":[
            {"id":"1","name":"A","artist_name":"B","audiodownload_allowed":true},{"id":"2","name":"C","artist_name":"D","audiodownload_allowed":false}]}""")
        assertEquals(listOf(true, false), jamendo.map { it.canDownload })
    }

    @Test fun fileNamesAndExtensions() {
        assertEquals("Velvet Hours - Paper Moon.flac", FileNames.forSong("Paper Moon", "Velvet Hours", "FLAC"))
        assertEquals("Paper Moon.mp3", FileNames.forSong("Paper Moon", "Unknown artist", "mp3"))
        assertEquals("flac", FileNames.extFrom("attachment; filename=\"01 Paper Moon.flac\"", "application/octet-stream"))
        assertEquals("m4a", FileNames.extFrom("attachment; filename*=UTF-8''Paper%20Moon.m4a", null))
        assertEquals("ogg", FileNames.extFrom(null, "audio/ogg; codecs=vorbis"))
        assertEquals("mp3", FileNames.extFrom(null, "audio/mpeg"))
        assertEquals("mp3", FileNames.extFrom("inline", null))
    }
}
