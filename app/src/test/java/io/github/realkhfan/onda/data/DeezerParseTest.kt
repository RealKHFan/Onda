package io.github.realkhfan.onda.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DeezerParseTest {

    private val sample = """
        {"data":[
          {"id":101,"readable":true,"title":"First Song","duration":181,
           "preview":"https://cdnt-preview.example/a.mp3",
           "artist":{"name":"Artist A"},
           "album":{"title":"Album A","cover_medium":"https://img.example/m.jpg","cover_big":"https://img.example/b.jpg"}},
          {"id":102,"readable":false,"title":"Blocked","preview":"https://cdnt-preview.example/x.mp3",
           "artist":{"name":"X"},"album":{"title":"Y"}},
          {"id":103,"readable":true,"title":"No Preview","preview":"","artist":{"name":"Artist B"},
           "album":{"title":"Album B","cover_big":null,"cover_medium":"https://img.example/m2.jpg"}},
          {"id":101,"readable":true,"title":"First Song","artist":{"name":"Artist A"},"album":{"title":"Album A"}}
        ],"total":4}
    """.trimIndent()

    @Test fun parsesTracksAndDropsDuplicates() {
        val list = Deezer.parseSearch(sample)
        assertEquals(listOf(101L, 102L, 103L), list.map { it.id })
        val first = list[0]
        assertEquals("First Song", first.title)
        assertEquals("Artist A", first.artist)
        assertEquals("https://img.example/b.jpg", first.cover)
        assertEquals("https://cdnt-preview.example/a.mp3", first.preview)
        assertEquals(181, first.durationSec)
    }

    @Test fun unstreamableTrackStaysListedWithoutPreview() {
        assertNull(Deezer.parseSearch(sample)[1].preview)
    }

    @Test fun blankPreviewIsNullAndCoverFallsBack() {
        val t = Deezer.parseSearch(sample)[2]
        assertNull(t.preview)
        assertEquals("https://img.example/m2.jpg", t.cover)
    }

    @Test fun apiErrorIsThrown() {
        val ex = runCatching { Deezer.parseSearch("""{"error":{"type":"Exception","message":"Quota limit exceeded","code":4}}""") }
        assertTrue(ex.exceptionOrNull() is IOException)
    }

    @Test fun missingDataIsEmpty() {
        assertTrue(Deezer.parseSearch("""{"total":0}""").isEmpty())
    }

    @Test fun artistsSkipPlaceholderPictures() {
        val artists = Deezer.parseArtists(JSONObject("""
            {"data":[
              {"id":7,"name":"Has Photo","picture_big":"https://img.example/artist/abc/500x500.jpg"},
              {"id":8,"name":"No Photo","picture_big":"https://img.example/images/artist//500x500.jpg"}
            ]}
        """.trimIndent()))
        assertEquals("https://img.example/artist/abc/500x500.jpg", artists[0].picture)
        assertNull(artists[1].picture)
    }

    @Test fun albumFillsTrackAlbumAndCoverFromParent() {
        val (album, tracks) = Deezer.parseAlbum(JSONObject("""
            {"id":55,"title":"New Album","cover_big":"https://img.example/c.jpg","release_date":"2026-11-14",
             "record_type":"album","artist":{"id":9,"name":"Band"},
             "tracks":{"data":[
               {"id":1,"title":"One","readable":true,"preview":"https://p.example/1.mp3","artist":{"name":"Band"}},
               {"id":2,"title":"Two","readable":true,"preview":"https://p.example/2.mp3","artist":{"name":"Band"}}
             ]}}
        """.trimIndent()))
        assertEquals("2026", album.year)
        assertEquals(9L, album.artistId)
        assertEquals(listOf("One", "Two"), tracks.map { it.title })
        assertEquals("New Album", tracks[0].album)
        assertEquals("https://img.example/c.jpg", tracks[0].cover)
    }

    @Test fun albumsKeepFullDateForSorting() {
        val albums = Deezer.parseAlbums(JSONObject("""
            {"data":[
              {"id":1,"title":"Older","release_date":"2026-02-02","record_type":"single"},
              {"id":2,"title":"Newer","release_date":"2026-11-14","record_type":"album"}
            ]}
        """.trimIndent()), "Band")
        val newestFirst = albums.sortedByDescending { it.date.orEmpty() }
        assertEquals(listOf("Newer", "Older"), newestFirst.map { it.title })
        assertEquals("Band", albums[0].artist)
    }
}
