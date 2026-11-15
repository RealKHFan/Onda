package io.github.realkhfan.onda.data

import android.content.Context
import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.scan.MusicLibrary

/** Onda's saved data, loaded once per run: from the app screen, or from the player when a car starts Onda. */
object AppData {
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val c = context.applicationContext
        MusicLibrary.load(c)
        PlaylistStore.load(c)
        Overrides.load(c)
        Lrclib.init(c)
        Server.load(c)
        PlayLog.load(c)
        Mixes.load(c)
        ArtistIds.load(c)
        Releases.load(c)
        UserCollection.load(c)
        ListenBrainz.load(c)
    }

    /** Your library as shown: files and server songs (a local copy wins), with accepted matches applied. */
    fun library(local: List<Track>, server: List<Track>, meta: Map<String, Overrides.Meta>): List<Track> =
        Overrides.apply(Match.mergeLibrary(local, server), meta)

    /** Every song including server copies of files you have, so saved ids always resolve. */
    fun everyTrack(local: List<Track>, server: List<Track>, meta: Map<String, Overrides.Meta>): List<Track> =
        Overrides.apply(server + local, meta)

    fun serverTracks(): List<Track> = Server.toTracks(Server.songs.value, Server.config.value, Server.kbps.value)
}
