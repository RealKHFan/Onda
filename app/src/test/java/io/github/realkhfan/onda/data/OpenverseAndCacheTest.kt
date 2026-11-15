package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.Source
import io.github.realkhfan.onda.player.PlaybackCache
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenverseAndCacheTest {
    @Test fun openverseJamendoBecomesJamendoAndOthersKeepTheirFile() {
        val json = """{"results":[
            {"source":"jamendo","title":"Open Fields","creator":"Lumen Trio","duration":187000,
             "url":"https://prod-1.storage.jamendo.com/?trackid=9001&format=mp32","thumbnail":"https://api.openverse.org/v1/audio/x/thumb/"},
            {"source":"wikimedia_audio","title":"Air on the G String","creator":"Old Orchestra","duration":300000,
             "url":"https://upload.wikimedia.org/wikipedia/commons/a/ab/Air.ogg"},
            {"source":"ccmixter","title":"Night Drive (Club Remix)","creator":"Someone","url":"https://ccmixter.org/x.mp3"},
            {"source":"ccmixter","title":"Plain","creator":"Someone","url":"http://ccmixter.org/insecure.mp3"}]}"""
        val list = FreeCatalogs.parseOpenverse(json)
        assertEquals(listOf("ja:9001", "ov:https://upload.wikimedia.org/wikipedia/commons/a/ab/Air.ogg"), list.map { it.key })
        assertEquals(187, list[0].durationSec)
        assertEquals(Source.OPENVERSE, list[1].source)
        assertEquals("https://upload.wikimedia.org/wikipedia/commons/a/ab/Air.ogg", FreeCatalogs.stream(list[1]))
    }

    @Test fun deezerPreviewsCacheUnderOneKey() {
        assertEquals("https://cdnt-preview.dzcdn.net/api/1/1/a/b/c/0/abc.mp3",
            PlaybackCache.key("https://cdnt-preview.dzcdn.net/api/1/1/a/b/c/0/abc.mp3?hdnea=exp=1~hmac=2"))
        val server = "http://192.168.1.20:4533/rest/stream?u=rkf&t=tok&s=salt&id=abc"
        assertEquals(server, PlaybackCache.key(server))
    }
}
