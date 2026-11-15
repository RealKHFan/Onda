package io.github.realkhfan.onda.core

import io.github.realkhfan.onda.core.SpotifyImport.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SpotifyImportTest {

    @Test fun playlistsKeepSongsAndLocalFilesButSkipEpisodes() {
        val json = """{"playlists":[{"name":"Night drive","items":[
            {"track":{"trackName":"Paper Moon","artistName":"Velvet Hours","albumName":"Tides","trackUri":"spotify:track:x1"},"episode":null,"localTrack":null},
            {"track":null,"episode":{"episodeName":"Talk"},"localTrack":null},
            {"track":null,"episode":null,"localTrack":{"uri":"spotify:local:Lumen+Trio:Fields%21:Open+Fields:187"}}]},
            {"name":"","items":[]}]}"""
        val pls = SpotifyImport.playlists(json)
        assertEquals(listOf("Night drive", "Spotify playlist"), pls.map { it.first })
        assertEquals(listOf(Song("Paper Moon", "Velvet Hours", "Tides"), Song("Open Fields", "Lumen Trio", "Fields!", local = true)), pls[0].second)
    }

    @Test fun likedSongs() {
        val json = """{"tracks":[{"artist":"Velvet Hours","album":"Tides","track":"Paper Moon","uri":"spotify:track:x1"},{"artist":"x","track":""}]}"""
        assertEquals(listOf(Song("Paper Moon", "Velvet Hours", "Tides")), SpotifyImport.liked(json))
    }

    @Test fun bothHistoryFormatsKeepRealRecentPlays() {
        val since = Instant.parse("2026-10-01T00:00:00Z").toEpochMilli()
        val basic = """[
            {"endTime":"2026-11-10 21:05","artistName":"Velvet Hours","trackName":"Paper Moon","msPlayed":201000},
            {"endTime":"2026-11-10 21:06","artistName":"Velvet Hours","trackName":"Skipped","msPlayed":4000},
            {"endTime":"2025-01-02 10:00","artistName":"Velvet Hours","trackName":"Old","msPlayed":201000}]"""
        val p = SpotifyImport.history(basic, since).single()
        assertEquals("Paper Moon", p.title)
        assertEquals(Instant.parse("2026-11-10T21:05:00Z").toEpochMilli(), p.atMs)
        val extended = """[
            {"ts":"2026-11-12T08:00:00Z","ms_played":180000,"master_metadata_track_name":"Open Fields",
             "master_metadata_album_artist_name":"Lumen Trio","spotify_track_uri":"spotify:track:abc"},
            {"ts":"2026-11-12T09:00:00Z","ms_played":900000,"master_metadata_track_name":null,
             "master_metadata_album_artist_name":null,"episode_name":"A podcast"}]"""
        val e = SpotifyImport.history(extended, since).single()
        assertEquals("Lumen Trio", e.artist)
        assertEquals("sp:abc", e.key)
    }

    @Test fun exportifyCsvWithQuotesCommasAndLineBreaks() {
        val csv = "\"Track URI\",\"Track Name\",\"Album Name\",\"Artist Name(s)\"\r\n" +
            "\"spotify:track:1\",\"Paper Moon\",\"Tides, Vol. 1\",\"Velvet Hours\"\r\n" +
            "\"spotify:track:2\",\"Say \"\"Hi\"\"\",\"Two\nLines\",\"Lumen Trio,Guest\"\r\n"
        val (name, songs) = SpotifyImport.csv("night_drive.csv", csv)
        assertEquals("night drive", name)
        assertEquals(listOf(Song("Paper Moon", "Velvet Hours", "Tides, Vol. 1"), Song("Say \"Hi\"", "Lumen Trio,Guest", "Two\nLines")), songs)
        assertTrue(SpotifyImport.csv("x.csv", "a,b\n1,2").second.isEmpty()) // not an Exportify file
    }

    @Test fun matchingAcceptsSpotifyVersionSuffixes() {
        val s = Song("Paper Moon - Remastered 2011", "Velvet Hours", "")
        assertTrue(SpotifyImport.matches(s, "Paper Moon (Remastered 2011)", "Velvet Hours"))
        assertTrue(SpotifyImport.matches(s, "Paper Moon", "Velvet Hours"))
        assertFalse(SpotifyImport.matches(s, "Paper Moon", "Someone Else"))
        assertFalse(SpotifyImport.matches(Song("Paper Moon", "Velvet Hours", ""), "Paper Moon (Club Remix)", "Velvet Hours"))
        assertTrue(SpotifyImport.matches(Song("Paper Moon - Live", "Velvet Hours", ""), "Paper Moon (Live)", "Velvet Hours"))
        assertTrue(SpotifyImport.matches(Song("Open Fields", "Lumen Trio, Guest", ""), "Open Fields", "Lumen Trio"))
    }
}
