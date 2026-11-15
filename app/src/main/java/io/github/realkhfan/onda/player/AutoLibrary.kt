package io.github.realkhfan.onda.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import io.github.realkhfan.onda.core.AutoTree
import io.github.realkhfan.onda.core.Discovery
import io.github.realkhfan.onda.core.Grouping
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.AppData
import io.github.realkhfan.onda.data.Mixes
import io.github.realkhfan.onda.data.Overrides
import io.github.realkhfan.onda.data.PlaylistStore
import io.github.realkhfan.onda.scan.MusicLibrary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What Android Auto (or any media browser) sees: Playlists, Albums, Artists, plus search and voice requests. */
@UnstableApi
class AutoLibrary(private val context: Context, private val scope: CoroutineScope) : MediaLibrarySession.Callback {

    private var scanned = false

    // Only Onda, Android Auto, car systems and trusted system apps may list your library; other phone apps can't.
    private val cars = setOf("com.google.android.projection.gearhead", "com.android.car.media", "com.google.android.carassistant")
    private fun allowed(b: MediaSession.ControllerInfo) = b.packageName == context.packageName || b.packageName in cars || b.isTrusted
    private fun <T : Any> denied(): ListenableFuture<LibraryResult<T>> =
        Futures.immediateFuture(LibraryResult.ofError<T>(SessionError.ERROR_PERMISSION_DENIED))

    private fun <T> later(block: suspend () -> T): ListenableFuture<T> = SettableFuture.create<T>().also { f ->
        scope.launch {
            try {
                f.set(block())
            } catch (e: CancellationException) {
                f.cancel(false)
            } catch (e: Exception) {
                f.setException(e)
            }
        }
    }

    /** In the car the app screen may never open, so the files are scanned on the first request. */
    private suspend fun ensureLibrary() {
        if (scanned || MusicLibrary.tracks.value.isNotEmpty() || MusicLibrary.folders.value.isEmpty()) return
        scanned = true
        if (MusicLibrary.scanning.value) MusicLibrary.scanning.first { !it } else MusicLibrary.rescan(context)
    }

    private fun library(): List<Track> = AppData.library(MusicLibrary.tracks.value, AppData.serverTracks(), Overrides.meta.value)
    private fun byId(): Map<String, Track> =
        AppData.everyTrack(MusicLibrary.tracks.value, AppData.serverTracks(), Overrides.meta.value).associateBy { it.id }

    /* ---------- Menus ---------- */

    override fun onGetLibraryRoot(
        session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        if (!allowed(browser)) denied() else Futures.immediateFuture(LibraryResult.ofItem(menu(AutoTree.ROOT, "Onda"), params))

    override fun onGetChildren(
        session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String, page: Int, pageSize: Int, params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = if (!allowed(browser)) denied() else later {
        ensureLibrary()
        val all = children(parentId) ?: return@later LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        val from = (page.toLong() * pageSize).coerceAtMost(all.size.toLong()).toInt()
        LibraryResult.ofItemList(all.subList(from, minOf(all.size, from + pageSize)).map { shareArt(it, browser.packageName) }, params)
    }

    override fun onGetItem(
        session: MediaLibrarySession, browser: MediaSession.ControllerInfo, mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> = if (!allowed(browser)) denied() else later {
        ensureLibrary()
        val parent = AutoTree.split(mediaId)?.first
        val item = if (parent != null) children(parent)?.firstOrNull { it.mediaId == mediaId } else null
        item?.let { LibraryResult.ofItem(shareArt(it, browser.packageName), null) } ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
    }

    override fun onSearch(
        session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, params: LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> = if (!allowed(browser)) denied() else later {
        ensureLibrary()
        session.notifySearchResultChanged(browser, query, AutoTree.search(library(), query).size, params)
        LibraryResult.ofVoid()
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, page: Int, pageSize: Int, params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = if (!allowed(browser)) denied() else later {
        ensureLibrary()
        val all = children("search:$query").orEmpty()
        val from = (page.toLong() * pageSize).coerceAtMost(all.size.toLong()).toInt()
        LibraryResult.ofItemList(all.subList(from, minOf(all.size, from + pageSize)).map { shareArt(it, browser.packageName) }, params)
    }

    private fun children(parent: String): List<MediaItem>? = when (parent) {
        AutoTree.ROOT -> listOf(
            menu(AutoTree.PLAYLISTS, "Playlists"),
            menu(AutoTree.ALBUMS, "Albums"),
            menu(AutoTree.ARTISTS, "Artists"),
        )
        AutoTree.PLAYLISTS -> buildList {
            add(menu(AutoTree.LIKED, "Liked Songs"))
            Discovery.Kind.entries.forEach { k ->
                Mixes.mixes.value[k]?.tracks?.takeIf { it.isNotEmpty() }?.let { add(menu("mix:${k.name}", k.label, art = it.firstNotNullOfOrNull(ArtProvider::uriFor))) }
            }
            val ids = byId()
            PlaylistStore.playlists.value.forEach { pl ->
                add(menu("pl:${pl.id}", pl.name, art = pl.trackIds.firstNotNullOfOrNull {
                    ids[it]?.let(ArtProvider::uriFor) ?: PlaylistStore.remote.value[it]?.let(ArtProvider::uriFor)
                }))
            }
        }
        AutoTree.ALBUMS -> Grouping.albums(library()).map { a ->
            menu("al:${a.key}", a.title, subtitle = a.artist, art = a.tracks.firstNotNullOfOrNull(ArtProvider::uriFor))
        }
        AutoTree.ARTISTS -> Grouping.artists(library()).map { a ->
            menu("ar:${a.name.lowercase()}", a.name, art = a.tracks.firstNotNullOfOrNull(ArtProvider::uriFor))
        }
        else -> songs(parent)?.map { song ->
            when (song) {
                is Track -> song.toMediaItem().forMenu(AutoTree.entry(parent, song.id), ArtProvider.uriFor(song))
                is RemoteTrack -> MediaItem.Builder().setMediaId(AutoTree.entry(parent, song.key)).setMediaMetadata(
                    MediaMetadata.Builder().setTitle(song.title).setArtist("Preview · ${song.artist}").setArtworkUri(ArtProvider.uriFor(song))
                        .setIsBrowsable(false).setIsPlayable(true).setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).build(),
                ).build()
                else -> error("unknown song")
            }
        }
    }

    /** The songs of a list (your files and server songs as [Track], catalog previews as [RemoteTrack]). */
    private fun songs(parent: String): List<Any>? {
        val ids = byId()
        return when {
            parent == AutoTree.LIKED -> PlaylistStore.liked.value.mapNotNull { ids[it] }
            parent == AutoTree.ALL -> library().shuffled()
            parent.startsWith("pl:") -> PlaylistStore.playlists.value.firstOrNull { it.id == parent.removePrefix("pl:") }
                ?.trackIds?.mapNotNull { ids[it] ?: PlaylistStore.remote.value[it]?.takeIf { r -> r.toPreviewItem() != null } }
            parent.startsWith("mix:") -> Discovery.Kind.entries.firstOrNull { it.name == parent.removePrefix("mix:") }
                ?.let { Mixes.mixes.value[it]?.tracks?.filter { r -> r.toPreviewItem() != null } }
            parent.startsWith("al:") -> Grouping.albums(library()).firstOrNull { it.key == parent.removePrefix("al:") }?.tracks
            parent.startsWith("ar:") -> Grouping.artists(library()).firstOrNull { it.name.lowercase() == parent.removePrefix("ar:") }?.tracks
            parent.startsWith("search:") -> AutoTree.search(library(), parent.removePrefix("search:"))
            else -> null
        }
    }

    private fun menu(id: String, title: String, subtitle: String? = null, art: Uri? = null): MediaItem =
        MediaItem.Builder().setMediaId(id).setMediaMetadata(
            MediaMetadata.Builder().setTitle(title).setSubtitle(subtitle).setArtist(subtitle).setArtworkUri(art)
                .setIsBrowsable(true).setIsPlayable(false).setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED).build(),
        ).build()

    private fun MediaItem.forMenu(id: String, art: Uri?): MediaItem = buildUpon().setMediaId(id).setUri(null as Uri?)
        .setMediaMetadata(mediaMetadata.buildUpon().setArtworkUri(art).setIsBrowsable(false).setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).build())
        .build()

    /** Only the app that browses (Android Auto) may read the cover; the provider stays closed to everyone else. */
    private fun shareArt(item: MediaItem, pkg: String): MediaItem {
        val art = item.mediaMetadata.artworkUri ?: return item
        if (art.authority == ArtProvider.AUTHORITY) {
            runCatching { context.grantUriPermission(pkg, art, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        return item
    }

    /* ---------- Playing what the car picked ---------- */

    override fun onAddMediaItems(
        mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>,
    ): ListenableFuture<MutableList<MediaItem>> {
        if (mediaItems.all { it.localConfiguration != null }) return Futures.immediateFuture(mediaItems) // from Onda's own screen
        if (!allowed(controller)) return Futures.immediateFuture(mutableListOf())
        return later {
            ensureLibrary()
            mediaItems.flatMap { item -> resolve(item).let { (list, i) -> listOfNotNull(list.getOrNull(i)) } }.toMutableList()
        }
    }

    override fun onSetMediaItems(
        mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>,
        startIndex: Int, startPositionMs: Long,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        if (mediaItems.all { it.localConfiguration != null }) {
            return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs))
        }
        if (!allowed(controller)) return Futures.immediateFailedFuture(SecurityException("Not allowed to browse Onda"))
        return later {
            ensureLibrary()
            if (mediaItems.size == 1) {
                // One song picked from a list: play the whole list from that song.
                val (list, i) = resolve(mediaItems[0])
                MediaSession.MediaItemsWithStartPosition(list, i, startPositionMs)
            } else {
                MediaSession.MediaItemsWithStartPosition(
                    mediaItems.flatMap { item -> resolve(item).let { (list, i) -> listOfNotNull(list.getOrNull(i)) } }, 0, startPositionMs,
                )
            }
        }
    }

    /** (playable list, start index) for a picked menu entry, a picked list, or a voice request ("play X on Onda"). */
    private fun resolve(item: MediaItem): Pair<List<MediaItem>, Int> {
        val voice = item.requestMetadata.searchQuery
        if (item.mediaId.isEmpty() && voice != null) {
            val parent = if (voice.isBlank()) AutoTree.ALL else "search:$voice"
            return playable(songs(parent).orEmpty()) to 0
        }
        val (parent, songId) = AutoTree.split(item.mediaId) ?: (item.mediaId to "")
        val list = songs(parent).orEmpty()
        val start = list.indexOfFirst { (it as? Track)?.id == songId || (it as? RemoteTrack)?.key == songId }.coerceAtLeast(0)
        return playable(list) to start
    }

    private fun playable(songs: List<Any>): List<MediaItem> = songs.mapNotNull { s ->
        when (s) {
            is Track -> s.toMediaItem()
            is RemoteTrack -> s.toPreviewItem()
            else -> null
        }
    }
}
