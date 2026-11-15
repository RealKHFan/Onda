package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.MetaMatch
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class FreeCatalogsTest {

    private fun au(id: String, title: String, user: String, extra: String = "\"is_stream_gated\":false,") =
        """{"id":"$id","title":"$title","duration":201,"is_streamable":true,$extra
           "artwork":{"480x480":"https://img.example/$id.jpg"},"user":{"name":"$user","is_verified":false}}"""

    @Test fun audiusKeepsStreamableOwnSongsOnly() {
        val json = """{"data":[
            ${au("aB1", "Paper Moon", "Velvet Hours")},
            ${au("aB2", "Locked Song", "Velvet Hours", "\"stream_conditions\":{\"usdc_purchase\":{}},\"is_stream_gated\":true,")},
            ${au("aB3", "Gone", "Velvet Hours", "\"is_delete\":true,")},
            ${au("aB4", "Famous Band - Big Hit (feat. Someone)", "randomuser22")},
            ${au("aB5", "Velvet Hours - Night Drive", "Velvet Hours")},
            ${au("aB7", "Night Drive (Club Remix)", "Velvet Hours")},
            {"id":"aB6","title":"No user"}
        ]}"""
        val list = FreeCatalogs.parseAudius(json)
        assertEquals(listOf("aB1", "aB5"), list.map { it.ref })
        val t = list.first()
        assertEquals("au:aB1", t.key)
        assertEquals("https://api.audius.co/v1/tracks/aB1/stream?app_name=Onda", t.preview)
        assertEquals(201, t.durationSec)
        assertEquals("https://img.example/aB1.jpg", t.cover)
    }

    @Test fun reUploadRule() {
        assertTrue(MetaMatch.namesOtherArtist("Famous Band - Big Hit", "randomuser22"))
        assertFalse(MetaMatch.namesOtherArtist("Velvet Hours - Night Drive", "Velvet Hours"))
        assertFalse(MetaMatch.namesOtherArtist("Night Drive", "anyone"))
        assertFalse(MetaMatch.namesOtherArtist("Velvet Hours - Night Drive", "Velvet Hours Official"))
    }

    @Test fun jamendoParsesAndReportsAnInvalidId() {
        val ok = """{"headers":{"status":"success","code":0,"results_count":1},"results":[
            {"id":"9001","name":"Open Fields","duration":187,"artist_name":"Lumen Trio","album_name":"Meadows",
             "image":"https://img.example/9001.jpg","audio":"https://example/x"}]}"""
        val t = FreeCatalogs.parseJamendo(ok).single()
        assertEquals("ja:9001", t.key)
        assertEquals("https://prod-1.storage.jamendo.com/?trackid=9001&format=mp32", t.preview)
        assertEquals("Meadows", t.album)
        val bad = """{"headers":{"status":"failed","code":5,"error_message":"Invalid Client Id"},"results":[]}"""
        assertEquals("Jamendo: Invalid Client Id", runCatching { FreeCatalogs.parseJamendo(bad) }.exceptionOrNull()?.message)
    }

    @Test fun archiveSearchHandlesListsAndDates() {
        val json = """{"response":{"numFound":2,"docs":[
            {"identifier":"np_042","title":"Low Tide EP","creator":["Coastline Kids","Guest"],"date":"2009-04-01T00:00:00Z"},
            {"identifier":"np_043"}]}}"""
        val a = FreeCatalogs.parseArchiveSearch(json)
        assertEquals("Coastline Kids", a[0].artist)
        assertEquals("2009-04-01", a[0].date)
        assertEquals(Source.ARCHIVE, a[0].source)
        assertEquals("np_042", a[0].ref)
        assertEquals("https://archive.org/services/img/np_042", a[0].cover)
        assertEquals("np_043", a[1].title)
    }

    @Test fun archiveItemPlaysOneCopyPerRecordingInTrackOrder() {
        val json = """{"metadata":{"title":"Low Tide EP","creator":"Coastline Kids","date":"2009"},"files":[
            {"name":"02 Second Wave.mp3","source":"original","format":"VBR MP3","title":"Second Wave","track":"2/2","length":"02:38"},
            {"name":"02 Second Wave.ogg","source":"derivative","original":"02 Second Wave.mp3","length":"157.9"},
            {"name":"t01.flac","source":"original","title":"First Light","track":"1/2","length":"158.7"},
            {"name":"t01.mp3","source":"derivative","original":"t01.flac","format":"VBR MP3"},
            {"name":"t01_64kb.mp3","source":"derivative","original":"t01.flac"},
            {"name":"t01.ogg","source":"derivative","original":"t01.mp3"},
            {"name":"t01.png","source":"derivative","original":"t01.flac"},
            {"name":"cover.jpg","source":"original"}]}"""
        val (album, tracks) = FreeCatalogs.parseArchiveItem("np_042", json)
        assertEquals("Low Tide EP", album.title)
        assertEquals(listOf("First Light", "Second Wave"), tracks.map { it.title })
        assertEquals("ia:np_042/t01.mp3", tracks[0].key)
        assertEquals(158, tracks[0].durationSec)
        assertEquals(158, tracks[1].durationSec)
        assertEquals("https://archive.org/download/np_042/02%20Second%20Wave.mp3", tracks[1].preview)
        assertEquals("Coastline Kids", tracks[1].artist)
    }

    @Test fun archiveMissingItemIsAnError() {
        assertTrue(runCatching { FreeCatalogs.parseArchiveItem("nope", "{}") }.exceptionOrNull() is IOException)
    }

    @Test fun lengthsAndSavedSongs() {
        assertEquals(3758, FreeCatalogs.seconds("1:02:38"))
        assertEquals(0, FreeCatalogs.seconds(null))
        // A saved song keeps only its id; the stream is rebuilt from it.
        val saved = RemoteTrack(0, "Paper Moon", "Velvet Hours", "", null, null, 0, Source.AUDIUS, ref = "aB1")
        assertEquals("https://api.audius.co/v1/tracks/aB1/stream?app_name=Onda", FreeCatalogs.stream(saved))
        assertNull(FreeCatalogs.stream(RemoteTrack(5, "x", "y", "", null, null, 0)))
        assertEquals(Source.AUDIUS, Source.of("au:aB1"))
        assertNull(Source.parse("au:aB1")) // text ids never pass as Deezer numbers
        assertEquals(Source.DEEZER to 5L, Source.parse("dz:5"))
    }
}
