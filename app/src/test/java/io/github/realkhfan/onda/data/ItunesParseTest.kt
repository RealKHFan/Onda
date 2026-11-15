package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItunesParseTest {

    @Test fun parsesSongsOnlyWithBigArtwork() {
        val list = Itunes.parseSongs("""
            {"resultCount":3,"results":[
              {"wrapperType":"track","kind":"song","trackId":42,"trackName":"Song","artistName":"Singer",
               "collectionName":"Record","artworkUrl100":"https://a.example/x/100x100bb.jpg",
               "previewUrl":"https://audio.example/p.m4a","trackTimeMillis":181500},
              {"wrapperType":"track","kind":"music-video","trackId":43,"trackName":"Video"},
              {"wrapperType":"artist","artistId":1,"artistName":"Singer"}
            ]}
        """.trimIndent())
        assertEquals(1, list.size)
        val t = list[0]
        assertEquals("it:42", t.key)
        assertEquals(Source.ITUNES, t.source)
        assertEquals("https://a.example/x/600x600bb.jpg", t.cover)
        assertEquals("https://audio.example/p.m4a", t.preview)
        assertEquals(181, t.durationSec)
    }

    @Test fun emptyResultsAreEmpty() {
        assertTrue(Itunes.parseSongs("""{"resultCount":0,"results":[]}""").isEmpty())
    }
}
