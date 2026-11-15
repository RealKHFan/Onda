package io.github.realkhfan.onda.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.realkhfan.onda.core.Discovery
import io.github.realkhfan.onda.core.Grouping
import io.github.realkhfan.onda.core.RemoteAlbum
import io.github.realkhfan.onda.core.RemoteArtist
import io.github.realkhfan.onda.core.RemoteResults
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Source
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.Mixes
import io.github.realkhfan.onda.data.PlaylistStore
import io.github.realkhfan.onda.data.Subsonic
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Pictures of the main screens with invented data, to look at before a build goes out (no emulator here). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    private fun t(n: Int, title: String, artist: String, album: String = "Tides") =
        Track("content://x/document/$n", "u$n", title, artist, album, artist, n, 201_000, "Music", null, fileName = "$title.m4a")
    private val tracks = listOf(t(1, "Paper Moon", "Velvet Hours"), t(2, "Night Drive", "Velvet Hours"), t(3, "Open Fields", "Lumen Trio", "Meadows"))
    private fun r(id: Long, title: String, artist: String, src: Source = Source.DEEZER) =
        RemoteTrack(id, title, artist, "Low Tide", null, if (src.full) null else "https://cdn/p.mp3", 200, src, ref = if (src.full) "r$id" else "")
    private val row = RemoteRowContext({ null }, setOf("dz:2"), "dz:1") {}
    private val playlists = listOf(PlaylistStore.Playlist("p1", "Road trip", tracks.map { it.id }), PlaylistStore.Playlist("p2", "Focus", emptyList()))
    private val mix = Mixes.Mix(Discovery.Kind.DAILY, "2026-11-15", listOf(r(9, "Free Song", "Coastline Kids", Source.JAMENDO), r(1, "Big Hit", "Band One"), r(2, "Other Hit", "Band Two")), listOf("Velvet Hours"))
    private val release = RemoteAlbum(5, "Never Meant to Understand", "Velvet Hours", null, "2026-11-14", "album")

    private fun shot(name: String, content: @Composable () -> Unit) =
        captureRoboImage("screenshots/$name.png") { OndaTheme { Box(Modifier.fillMaxSize().background(Bg)) { content() } } }

    @Test fun home() = shot("home") {
        HomeScreen(playlists, { null }, scanning = false, noFolders = false, onAvatar = {}, onAddFolder = {}, onOpenLiked = {}, onOpenWant = {},
            onOpenPlaylist = {}, onShuffleAll = {}, mixes = mapOf(Discovery.Kind.DAILY to mix), building = setOf(Discovery.Kind.WEEKLY),
            onOpenMix = {}, releases = listOf(release), onOpenRelease = {})
    }

    @Test fun library() = shot("library") {
        LibraryScreen(playlists, { null }, likedCount = 12, wantCount = 3, albums = Grouping.albums(tracks), artists = Grouping.artists(tracks),
            folders = Grouping.folders(tracks), songs = tracks, currentId = tracks[0].id, onAvatar = {}, onCreate = {}, onOpenLiked = {},
            onOpenWant = {}, onOpenPlaylist = {}, onOpenAlbum = {}, onOpenArtist = {}, onOpenFolder = {},
            savedAlbums = listOf(release), followed = listOf(RemoteArtist(7, "Coastline Kids", null)),
            serverPlaylists = listOf(Subsonic.Playlist("s1", "Gym", listOf("a", "b"))), onOpenServerPlaylist = {},
            onOpenRemoteAlbum = {}, onOpenRemoteArtist = {}, onPlaySongs = { _, _ -> }, onMenu = {}, onQueue = {})
    }

    @Test fun playlist() = shot("playlist") {
        TrackListScreen("Road trip", "Playlist · 5 songs", null, tracks, "Playlist", "p1", null, onBack = {}, onMenu = {}, onQueue = {},
            onDelete = {}, previews = listOf(r(1, "Big Hit", "Band One"), r(3, "Free Song", "Coastline Kids", Source.AUDIUS)), row = row,
            onDownload = {}, onRename = {}, onMove = { _, _ -> })
    }

    @Test fun mix() = shot("mix") {
        MixScreen(Discovery.Kind.DAILY, mix, building = false, problem = null, noTaste = false, row = row, onBack = {}, onPlayList = { _, _ -> }, onRetry = {})
    }

    @Test fun search() = shot("search") {
        SearchScreen("velvet", {}, SearchState.Done,
            RemoteResults(tracks = listOf(r(1, "Paper Moon", "Velvet Hours"), r(2, "Night Drive", "Velvet Hours")),
                artists = listOf(RemoteArtist(3, "Velvet Hours", null)), albums = listOf(release),
                free = listOf(r(4, "Velvet Sky", "Coastline Kids", Source.OPENVERSE)), freeAlbums = listOf(release.copy(source = Source.ARCHIVE, ref = "np"))),
            row, null, {}, {}, {}, {})
    }

    @Test fun settings() = shot("settings") {
        SettingsScreen(listOf("content://com.android.externalstorage.documents/tree/primary%3AMusic"), scanning = false, appleOn = true,
            onAppleChange = {}, freeOn = true, onFreeChange = {}, jamendoId = "", onJamendoChange = {}, matchRunning = false,
            onMatch = {}, onViewMatch = {}, onRename = {}, onImport = {}, onBack = {})
    }
}
