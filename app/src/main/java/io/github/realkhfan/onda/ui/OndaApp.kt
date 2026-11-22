@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.realkhfan.onda.ui

import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material.icons.rounded.Dns
import io.github.realkhfan.onda.data.Subsonic
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.mutableLongStateOf
import io.github.realkhfan.onda.player.PlaybackCache
import io.github.realkhfan.onda.data.Backup
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Block
import io.github.realkhfan.onda.data.UserCollection
import io.github.realkhfan.onda.data.AppData
import androidx.compose.material.icons.rounded.Download
import io.github.realkhfan.onda.data.Downloads
import io.github.realkhfan.onda.data.SpotifyImporter
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.realkhfan.onda.BuildConfig
import io.github.realkhfan.onda.core.Album
import io.github.realkhfan.onda.core.ArtistGroup
import io.github.realkhfan.onda.core.FolderGroup
import io.github.realkhfan.onda.core.Grouping
import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.core.Smart
import io.github.realkhfan.onda.player.SleepTimer
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.VisibilityOff
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.Deezer
import io.github.realkhfan.onda.data.PlaylistStore
import io.github.realkhfan.onda.core.RemoteAlbum
import io.github.realkhfan.onda.core.RemoteArtist
import io.github.realkhfan.onda.core.RemoteResults
import io.github.realkhfan.onda.core.Source
import io.github.realkhfan.onda.core.Discovery
import io.github.realkhfan.onda.data.Catalog
import io.github.realkhfan.onda.data.Mixes
import io.github.realkhfan.onda.data.PlayLog
import io.github.realkhfan.onda.data.Matcher
import io.github.realkhfan.onda.data.Overrides
import io.github.realkhfan.onda.data.Prefs
import io.github.realkhfan.onda.data.ReleaseJob
import io.github.realkhfan.onda.data.Releases
import io.github.realkhfan.onda.data.Server
import io.github.realkhfan.onda.player.PlayerState
import io.github.realkhfan.onda.player.QueueItem
import io.github.realkhfan.onda.player.toMediaItem
import io.github.realkhfan.onda.player.toPreviewItem
import io.github.realkhfan.onda.scan.MusicLibrary
import io.github.realkhfan.onda.core.M3u
import io.github.realkhfan.onda.core.LinkSync
import androidx.compose.ui.draw.alpha
import io.github.realkhfan.onda.data.Suggest
import io.github.realkhfan.onda.data.Loudness
import io.github.realkhfan.onda.data.ArtistIds
import io.github.realkhfan.onda.player.OpenWith
import io.github.realkhfan.onda.player.LinkDataSource
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import io.github.realkhfan.onda.data.Link
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.mutableLongStateOf
import io.github.realkhfan.onda.player.Autoplay
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SelectAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

private sealed interface Route
private data class PlaylistRoute(val id: String) : Route
private data object LikedRoute : Route
private data object WantRoute : Route
private data class TracksRoute(
    val title: String, val subtitle: String, val art: String?, val tracks: List<Track>, val kind: String,
    val round: Boolean = false,
) : Route
/** [fromPlayer]: opened from the full player, so leaving it goes back there. */
private data class QueueRoute(val fromPlayer: Boolean) : Route
private data object SettingsRoute : Route
private data object MatchRoute : Route
private data object RenameRoute : Route
private data object StatsRoute : Route
private data object HistoryRoute : Route
private data object SmartRoute : Route
private data class MixRoute(val kind: Discovery.Kind) : Route
private data class ArtistMixRoute(val name: String) : Route
private data object RadarRoute : Route
private data object DuplicatesRoute : Route
private data class FolderRoute(val id: String) : Route
private data class RemoteArtistRoute(val artist: RemoteArtist) : Route
private data class RemoteAlbumRoute(val album: RemoteAlbum) : Route

private const val LIKED_ID = "liked"
private const val SWIPE_FRACTION = 0.4f
private val WantBg = Color(0xFF3A2F5C)

/** Set by MainActivity so the Settings screen can touch the library with the Activity context. */
object LibraryActions {
    var removeFolder: ((String) -> Unit)? = null
    var addFolder: (() -> Unit)? = null
    var rescan: (() -> Unit)? = null
}

internal class MenuAction(val label: String, val icon: ImageVector, val run: () -> Unit)

private data class MenuFor(val track: Track, val playlistId: String?, val fromPlayer: Boolean)

@Composable
fun OndaApp(player: PlayerState?) {
    val ctx = LocalContext.current
    val localTracks by MusicLibrary.tracks.collectAsState()
    val serverCfg by Server.config.collectAsState()
    val serverSongs by Server.songs.collectAsState()
    val serverKbps by Server.kbps.collectAsState()
    val meta by Overrides.meta.collectAsState()
    val artistPics by Overrides.artistPics.collectAsState()
    val matching by Matcher.state.collectAsState()
    val serverTracks = remember(serverSongs, serverCfg, serverKbps) { Server.toTracks(serverSongs, serverCfg, serverKbps) }
    // What the app shows: local files plus server songs you don't have locally, with accepted Deezer matches on top.
    val hidden by UserCollection.hidden.collectAsState()
    // Songs you hid stay out of the library (playlists that hold them still play them).
    val tracks = remember(localTracks, serverTracks, meta, hidden) { AppData.library(localTracks, serverTracks, meta).filterNot { it.id in hidden } }
    val plays by PlayLog.plays.collectAsState()
    remember { accentState.value = androidx.compose.ui.graphics.Color(Prefs.accent(ctx).toInt()); 0 }
    // Lookups also know server songs hidden by a local twin, so playlists that saved one still find it.
    val everyTrack = remember(localTracks, serverTracks, meta) { AppData.everyTrack(localTracks, serverTracks, meta) }
    val folders by MusicLibrary.folders.collectAsState()
    val scanning by MusicLibrary.scanning.collectAsState()
    val playlists by PlaylistStore.playlists.collectAsState()
    val liked by PlaylistStore.liked.collectAsState()
    val want by PlaylistStore.want.collectAsState()
    val followed by UserCollection.followed.collectAsState()
    val serverPlaylists by Server.playlists.collectAsState()
    val savedAlbums by UserCollection.albums.collectAsState()
    val aiMarked by UserCollection.aiMarked.collectAsState()
    val hiddenSongs by UserCollection.hidden.collectAsState()
    var editing by remember { mutableStateOf<PlaylistStore.Playlist?>(null) }
    var pinned by remember { mutableStateOf(Prefs.pinned(ctx)) }
    var order by remember { mutableStateOf(Prefs.playlistOrder(ctx)) }
    var searches by remember { mutableStateOf(Prefs.searches(ctx)) }
    var savingQueue by remember { mutableStateOf(false) }
    var editFor by remember { mutableStateOf<Track?>(null) }
    var movingToFolder by remember { mutableStateOf<PlaylistStore.Playlist?>(null) }
    var folderEdit by remember { mutableStateOf<PlaylistStore.Folder?>(null) }
    var newFolder by remember { mutableStateOf(false) }
    val playlistFolders by PlaylistStore.folders.collectAsState()
    val linkedPc by Link.pc.collectAsState()
    var editMany by remember { mutableStateOf<List<Track>?>(null) }
    val keptWant = remember { mutableSetOf<String>() }
    var exporting by remember { mutableStateOf<PlaylistStore.Playlist?>(null) }
    val remoteSongs by PlaylistStore.remote.collectAsState()

    val byId = remember(everyTrack) { everyTrack.associateBy { it.id } }
    val songs = remember(tracks) { Grouping.songsSorted(tracks) }
    val albums = remember(tracks) { Grouping.albums(tracks) }
    val addedAt by MusicLibrary.addedAt.collectAsState()
    // Artist photos come from Deezer (fetched by a match run), never from a song's cover.
    val artists = remember(tracks, artistPics) {
        Grouping.artists(tracks).map { a -> a.copy(artUri = artistPics[a.tracks.first().artistKey]?.takeIf { it.isNotBlank() }) }
    }
    val folderGroups = remember(tracks) { Grouping.folders(tracks) }
    val likedTracks = remember(liked, byId) { liked.mapNotNull { byId[it] } }
    // Liked Songs as listed: songs that are only on your PC stay in place, greyed out.
    val likedShown = remember(liked, byId) { liked.mapNotNull { byId[it] ?: LinkSync.missingTrack(it) } }
    val wantKeys = remember(want) { want.map { it.key }.toSet() }
    val byTitle = remember(tracks) { tracks.groupBy { Match.norm(it.title) } }
    val localFor: (RemoteTrack) -> Track? = remember(byTitle) {
        { r: RemoteTrack -> byTitle[Match.norm(r.title)]?.firstOrNull { Match.same(it.title, it.artist, r.title, r.artist) } }
    }
    val playlistArt: (PlaylistStore.Playlist) -> String? =
        { pl -> pl.cover?.let { "file://$it" } ?: pl.trackIds.firstNotNullOfOrNull { byId[it]?.artUri ?: remoteSongs[it]?.cover } }
    // Pinned first (newest pin on top), then the order you picked, like the desktop's sidebar.
    val orderedPlaylists = remember(playlists, pinned, order) {
        when (order) {
            "A–Z" -> playlists.sortedBy { it.name.lowercase() }
            "Most songs" -> playlists.sortedByDescending { it.trackIds.size }
            else -> playlists
        }.sortedBy { pinned.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }
    val movePlaylist: (List<String>, Int, Int) -> Unit = { shown, from, to ->
        val (newOrder, pins) = Grouping.dragPlaylist(PlaylistStore.playlists.value.map { it.id }, pinned, shown, from, to)
        if (pins != pinned) { pinned = pins; Prefs.setPinned(ctx, pins) }
        PlaylistStore.setOrder(newOrder)
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    // Screens opened on top of the current tab; Back pops one (album -> artist -> search results).
    val stack = remember { mutableStateListOf<Route>() }
    val detail = stack.lastOrNull()
    var fullPlayer by remember { mutableStateOf(false) }
    val roots = rememberSaveableStateHolder()
    fun push(r: Route) { stack.add(r) }
    fun pop() {
        if (stack.isEmpty()) return
        roots.removeState("page${stack.lastIndex}") // a page opened here later starts fresh
        if ((stack.removeAt(stack.lastIndex) as? QueueRoute)?.fromPlayer == true) fullPlayer = true
    }
    fun closePages() { stack.indices.forEach { roots.removeState("page$it") }; stack.clear() }
    fun openQueue() { push(QueueRoute(fromPlayer = fullPlayer)); fullPlayer = false }
    var menuFor by remember { mutableStateOf<MenuFor?>(null) }
    var remoteMenu by remember { mutableStateOf<RemoteTrack?>(null) }
    var remoteMenuPlaylist by remember { mutableStateOf<String?>(null) } // set when opened from a playlist row
    // What "Add to playlist" adds once a playlist is picked: your songs, or a catalog song.
    var adding by remember { mutableStateOf<((PlaylistStore.Playlist) -> Unit)?>(null) }
    var pendingAdd by remember { mutableStateOf<((PlaylistStore.Playlist) -> Unit)?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var showDevices by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<PlaylistStore.Playlist?>(null) }
    var infoFor by remember { mutableStateOf<Track?>(null) }
    var sleepSheet by remember { mutableStateOf(false) }

    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf(RemoteResults()) }
    var appleOn by remember { mutableStateOf(Prefs.appleSearch(ctx)) }
    // Settings can change under us (a sync with your PC): read them again.
    val prefsChanged by Prefs.changed.collectAsState()
    LaunchedEffect(prefsChanged) {
        pinned = Prefs.pinned(ctx); order = Prefs.playlistOrder(ctx); searches = Prefs.searches(ctx); appleOn = Prefs.appleSearch(ctx)
        accentState.value = androidx.compose.ui.graphics.Color(Prefs.accent(ctx).toInt())
    }
    var freeOn by remember { mutableStateOf(Prefs.freeSearch(ctx)) }
    var jamendoId by remember { mutableStateOf(Prefs.jamendoId(ctx)) }
    var searchState by remember { mutableStateOf(SearchState.Idle) }
    var searchError by remember { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val tint = rememberArtTint(player?.artUri)

    LaunchedEffect(query, appleOn, freeOn, jamendoId) {
        val q = query.trim()
        if (q.length < 2) {
            results = RemoteResults(); searchState = SearchState.Idle
            return@LaunchedEffect
        }
        delay(350) // wait until typing pauses, so not every keystroke becomes a request
        searchState = SearchState.Loading
        try {
            results = Catalog.search(q, appleOn, freeOn, jamendoId)
            searchState = SearchState.Done
            delay(2000) // still the same words two seconds later: worth keeping as a recent search
            if (!results.isEmpty) { Prefs.rememberSearch(ctx, q); searches = Prefs.searches(ctx) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Shown small under the message, so a non-permission failure (e.g. an HTTP error) is visible.
            searchError = e.javaClass.simpleName + (e.message?.let { ": $it" } ?: "")
            searchState = SearchState.Error
        }
    }

    // One message at a time: a new one replaces the old instead of queueing up behind it.
    fun toast(msg: String) {
        scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(msg) }
    }
    /** A message with Undo, like the desktop's. */
    fun undoable(msg: String, undo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(msg, actionLabel = "Undo", duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) undo()
        }
    }
    fun download(items: List<Downloads.Item>) {
        toast(Downloads.start(ctx, items) ?: "Downloading ${songCount(items.size)}…")
    }
    /** The download button's action for a list, or null when nothing in it may be downloaded (your server's songs, free songs that allow it). */
    /** "Get N songs from your PC" for a list's greyed-out songs (when linked). */
    fun getMissing(list: List<Track>): List<MenuAction> {
        val pc = linkedPc ?: return emptyList()
        val items = list.mapNotNull(Downloads::forMissing).takeIf { it.isNotEmpty() } ?: return emptyList()
        return listOf(MenuAction("Get ${songCount(items.size)} from ${pc.name}", Icons.Rounded.Download) { download(items) })
    }
    fun downloadAll(list: List<Track>, free: List<RemoteTrack> = emptyList()): (() -> Unit)? =
        (list.mapNotNull { Downloads.forTrack(it, serverCfg) } + free.mapNotNull { Downloads.forRemote(it) })
            .takeIf { it.isNotEmpty() }?.let { items -> { download(items) } }
    fun queue(t: Track) {
        player?.addToQueue(t.toMediaItem()); toast("Added to queue")
    }
    fun albumRoute(a: Album) = TracksRoute(a.title, "Album · ${a.artist}", a.artUri, a.tracks, "Album")
    fun artistRoute(a: ArtistGroup) =
        TracksRoute(a.name, "Artist · ${songCount(a.tracks.size)}", a.artUri, a.tracks, "Artist", round = true)
    /** The artist's full page (with a row to your songs); just your songs when the artist can't be found online. */
    fun openArtist(a: ArtistGroup) {
        fun go(id: Long) = push(if (id > 0) RemoteArtistRoute(RemoteArtist(id, a.name, null)) else artistRoute(a))
        ArtistIds.cached(a.name)?.let { go(it); return }
        scope.launch { go(runCatching { ArtistIds.resolve(a.name) }.getOrDefault(0L)) }
    }
    fun playRemote(r: RemoteTrack, kind: String, label: String) {
        val p = player ?: return
        localFor(r)?.let { p.play(listOf(it), 0, kind, label); return } // you own it: play the full song
        r.toPreviewItem()?.let { p.playItems(listOf(it), 0, kind, label); return }
        scope.launch { // saved songs have no preview stored (the links expire), so fetch a fresh one
            val fresh = Catalog.freshPreview(r)
            if (fresh != null) p.playItems(listOf(r.toMediaItem(fresh)), 0, kind, label) else toast("No preview available")
        }
    }
    /** Plays a catalog list (album, popular songs) as a queue: owned songs in full, the rest as previews.
     *  [index] -1 starts at the first playable song. */
    fun playRemoteList(list: List<RemoteTrack>, index: Int, kind: String, label: String) {
        val p = player ?: return
        val playable = list.mapIndexedNotNull { i, t ->
            (localFor(t)?.toMediaItem() ?: t.toPreviewItem())?.let { i to it }
        }
        val start = if (index < 0) 0 else playable.indexOfFirst { it.first == index }
        if (playable.isEmpty() || start < 0) {
            toast("No preview for this song in your country"); return
        }
        p.playItems(playable.map { it.second }, start, kind, label)
    }
    val rowCtx = RemoteRowContext(localFor, wantKeys, player?.currentId) { PlaylistStore.toggleWant(it) }

    /**
     * Radio, like the desktop's: [first] plays now and similar songs follow it. [first] null: the radio starts
     * with the similar songs themselves, or, with [keepCurrent], they follow the song that's playing.
     */
    fun startRadio(first: androidx.media3.common.MediaItem?, artist: String, skip: String, label: String, keepCurrent: Boolean = false) {
        val p = player ?: return
        if (!keepCurrent) first?.let { p.playItems(listOf(it), 0, "Radio", label) }
        toast("Starting radio…")
        scope.launch {
            val more = Autoplay.similar(listOf(artist), setOf(skip), tracks, n = 25, listenBrainz = Prefs.listenBrainz(ctx))
            when {
                more.isEmpty() -> toast("No similar songs found")
                first == null && !keepCurrent -> p.playItems(more, 0, "Radio", label)
                else -> p.append(more)
            }
        }
    }
    fun addToPlaylist(pl: PlaylistStore.Playlist, list: List<Track>) {
        PlaylistStore.addToPlaylist(pl.id, list.map { it.id })
        toast(if (list.size == 1) "Added to ${pl.name}" else "Added ${songCount(list.size)} to ${pl.name}")
    }
    fun addSongs(list: List<Track>) { adding = { pl -> addToPlaylist(pl, list) } }
    // A catalog song: your own copy if you have it, else the song itself (playable as a preview), like the desktop.
    fun addCatalogSong(r: RemoteTrack) {
        adding = { pl ->
            val own = localFor(r)
            PlaylistStore.addToPlaylist(pl.id, listOf(own?.id ?: r.key), if (own == null) listOf(r) else emptyList())
            toast("Added to ${pl.name}")
        }
    }
    fun togglePin(id: String) {
        pinned = if (id in pinned) pinned - id else listOf(id) + pinned
        Prefs.setPinned(ctx, pinned)
    }
    // Playlist files: the same .m3u format as the desktop, so a playlist moves between both.
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        val pl = exporting ?: return@rememberLauncherForActivityResult
        exporting = null
        if (uri == null) return@rememberLauncherForActivityResult
        val list = pl.trackIds.mapNotNull { id ->
            byId[id] ?: remoteSongs[id]?.let { r -> localFor(r) ?: Track(r.key, "", r.title, r.artist, r.album, r.artist, 0, r.durationSec * 1000L, "", r.cover) }
        }
        val text = M3u.write(pl.name, list) { t -> t.fileName.takeIf { it.isNotEmpty() && !t.id.startsWith(Server.PREFIX) }?.let { "${t.folderName}/$it" } }
        val ok = runCatching { ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) } }.isSuccess
        toast(if (ok) "Saved “${pl.name}” as a playlist file" else "Couldn't save the file")
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = runCatching {
            ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()?.substringBeforeLast('.') ?: "Imported playlist"
        // A playlist file is text; anything over 2 MB isn't one.
        val text = runCatching { ctx.contentResolver.openInputStream(uri)!!.use { it.readUpTo(2 * 1024 * 1024) }?.let(::String) }.getOrNull()
        if (text == null || !text.contains("#EXT") && text.lines().none { it.isNotBlank() && !it.startsWith("#") }) { toast("That isn't a playlist file"); return@rememberLauncherForActivityResult }
        val (title, entries) = M3u.parse(text, name)
        val ids = M3u.match(entries, tracks)
        if (ids.isEmpty()) { toast("None of its songs are in your library"); return@rememberLauncherForActivityResult }
        val id = PlaylistStore.createPlaylist(title)
        PlaylistStore.addToPlaylist(id, ids)
        toast("Imported “$title”: ${songCount(ids.size)}" + if (ids.size < entries.size) " of ${entries.size} found" else "")
    }

    // Daily 10 / Weekly 30: built from what you play, like, save and own; rebuilt when the day / week changes.
    val mixes by Mixes.mixes.collectAsState()
    val mixesBuilding by Mixes.building.collectAsState()
    val mixesProblem by Mixes.problem.collectAsState()
    val taste = remember(plays, likedTracks, want, tracks) {
        val played = plays.map { it.key }.toSet()
        Mixes.Taste(
            // Artists you follow come first; AI "artists" never seed anything.
            seeds = (followed.map { it.name } + Discovery.seedArtists(plays, likedTracks, want, tracks, System.currentTimeMillis()))
                .distinctBy { Match.norm(it) }.filterNot { UserCollection.isAi(it) || UserCollection.isBlocked(it) },
            known = (tracks.map { it.artist } + plays.map { it.artist } + likedTracks.map { it.artist }).map { Match.norm(it) }.toSet(),
            skip = { r -> r.key in played || localFor(r) != null || UserCollection.notWanted(r) },
            audius = freeOn, jamendoId = jamendoId, listenBrainz = Prefs.listenBrainz(ctx),
        )
    }
    LaunchedEffect(taste.seeds.isNotEmpty(), java.time.LocalDate.now()) {
        if (taste.seeds.isNotEmpty()) Mixes.refresh(taste)
    }
    // New releases follow the same artists you play most; the check itself runs at most every 6 h.
    val releases by Releases.items.collectAsState()
    LaunchedEffect(taste.seeds) {
        Releases.setArtists(taste.seeds)
        Releases.refreshIfDue(ctx)
    }
    val openRelease by Releases.open.collectAsState()
    LaunchedEffect(openRelease) {
        openRelease?.let { push(RemoteAlbumRoute(it)); Releases.open.value = null }
    }

    // New songs get measured for even loudness (when that's on), like on the desktop.
    LaunchedEffect(localTracks) { if (Prefs.normalize(ctx) && localTracks.isNotEmpty()) Loudness.measure(ctx, tracks) }

    // Songs you wanted that are in your library now (a download, a scan, a match) leave Want to listen.
    LaunchedEffect(tracks) {
        if (tracks.isEmpty()) return@LaunchedEffect
        val got = PlaylistStore.want.value.filter { it.key !in keptWant && localFor(it) != null }
        if (got.isEmpty()) return@LaunchedEffect
        PlaylistStore.removeWant(got)
        undoable(if (got.size == 1) "“${got[0].title}” is in your library now: off Want to listen" else "${got.size} songs you wanted are in your library now") {
            keptWant += got.map { it.key }; PlaylistStore.addWant(got)
        }
    }

    // A song opened from another app ("Open with Onda"): plays it, as your own song if it's in your library.
    val opened by OpenWith.uri.collectAsState()
    LaunchedEffect(opened, player) {
        val uri = opened ?: return@LaunchedEffect
        val p = player ?: return@LaunchedEffect
        OpenWith.uri.value = null
        val own = byId[uri.toString()] ?: tracks.firstOrNull { it.uri == uri.toString() }
        p.playItems(listOf(own?.toMediaItem() ?: OpenWith.item(ctx, uri)), 0, "Files", "Opened file", shuffled = false)
    }

    // Onda on your PC: sync when Onda opens (once your music is scanned) and every 15 minutes while it's open.
    // (What the PC sends to play here is picked up by the playback service.)
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(linkedPc != null, scanning) {
        if (linkedPc == null || scanning) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) { Link.syncLater(); delay(15 * 60_000L) }
        }
    }
    // A greyed-out song (only on the PC) that's here now (copied, downloaded, scanned) becomes the song itself.
    LaunchedEffect(tracks) {
        val missing = (playlists.flatMap { it.trackIds } + liked).filter(LinkSync::isMissing).toSet()
        LinkSync.heal(missing, tracks).takeIf { it.isNotEmpty() }?.let { PlaylistStore.migrateIds(it); PlayLog.migrateIds(it) }
    }

    // A song that can't play (server offline, preview gone) says why once.
    LaunchedEffect(player?.error) {
        player?.error?.let { toast(it); player.clearError() }
    }

    // System Back / edge-back pops the in-app screen instead of closing the app. One enabled at a time.
    BackHandler(enabled = fullPlayer) { fullPlayer = false }
    BackHandler(enabled = !fullPlayer && !drawerState.isOpen && stack.isNotEmpty()) { pop() }
    BackHandler(enabled = !fullPlayer && !drawerState.isOpen && stack.isEmpty() && tab != 0) { tab = 0 }

    Box(Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            // Swipe only closes it; it opens from the avatar, so right-swipes stay free for "add to queue".
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                DrawerSheet(
                    onSettings = { scope.launch { drawerState.close() }; push(SettingsRoute) },
                    onStats = { scope.launch { drawerState.close() }; push(StatsRoute) },
                    onHistory = { scope.launch { drawerState.close() }; push(HistoryRoute) },
                    onDuplicates = { scope.launch { drawerState.close() }; push(DuplicatesRoute) },
                )
            },
        ) {
            Scaffold(
                containerColor = Bg,
                bottomBar = {
                    Column {
                        if (player?.hasItem == true) MiniPlayer(player, tint) { fullPlayer = true }
                        NavBar(tab) { tab = it; closePages() }
                    }
                },
            ) { pad ->
                Box(Modifier.padding(pad).fillMaxSize()) {
                    // Each screen keeps its state (Library tab, scroll) while pages are open on top of it.
                    roots.SaveableStateProvider(if (detail == null) "tab$tab" else "page${stack.lastIndex}") {
                    when (val d = detail) {
                        SettingsRoute -> SettingsScreen(
                            folders, scanning, appleOn,
                            onAppleChange = { appleOn = it; Prefs.setAppleSearch(ctx, it) },
                            freeOn = freeOn,
                            onFreeChange = { freeOn = it; Prefs.setFreeSearch(ctx, it) },
                            jamendoId = jamendoId,
                            onJamendoChange = { jamendoId = it; Prefs.setJamendoId(ctx, it) },
                            matchRunning = matching.running,
                            onMatch = { onlyNew -> Matcher.start(tracks, onlyNew); push(MatchRoute) },
                            onViewMatch = { push(MatchRoute) },
                            onRename = { push(RenameRoute) },
                            onImport = { uris -> SpotifyImporter.start(ctx, uris, tracks) },
                            library = tracks,
                            onMessage = ::toast,
                            // Every greyed-out song in your playlists and Liked Songs, copied from your PC at once.
                            getAllFromPc = getMissing((playlists.flatMap { it.trackIds } + liked).distinct().mapNotNull(LinkSync::missingTrack)).firstOrNull(),
                        ) { pop() }
                        MatchRoute -> MatchScreen(onBack = { pop() }, onRename = { push(RenameRoute) })
                        RenameRoute -> RenameScreen(
                            localTracks = tracks.filter { it.fileName.isNotEmpty() }, folders = folders, onBack = { pop() },
                            onReAddFolder = { LibraryActions.addFolder?.invoke() },
                            onRescan = { LibraryActions.rescan?.invoke() },
                        )
                        is MixRoute -> MixScreen(
                            d.kind, mixes[d.kind], d.kind in mixesBuilding, mixesProblem, noTaste = taste.seeds.isEmpty(),
                            row = rowCtx, onBack = { pop() },
                            onPlayList = { list, i -> playRemoteList(list, i, "Playlist", d.kind.label) },
                            onRetry = { Mixes.refresh(taste) },
                            onMenu = { remoteMenu = it },
                        )
                        is QueueRoute -> QueueScreen(
                            player,
                            recent = remember(plays, byId) { plays.asReversed().mapNotNull { byId[it.key] }.distinctBy { it.id }.take(10) },
                            onSave = { savingQueue = true },
                        ) { pop() }
                        is ArtistMixRoute -> {
                            var list by remember(d.name) { mutableStateOf<List<RemoteTrack>?>(null) }
                            var round by remember(d.name) { mutableIntStateOf(0) }
                            LaunchedEffect(d.name, round) {
                                list = null
                                val own = tracks.filter { Match.norm(it.artist) == Match.norm(d.name) }.shuffled().take(10)
                                val keys = own.map { Match.norm(it.artist) + "|" + Match.norm(it.title) }.toSet()
                                list = Smart.interleave(own.map { it.asRemote() }, Suggest.artistMix(d.name, keys, Prefs.listenBrainz(ctx), refresh = round > 0))
                            }
                            RemoteListScreen("${d.name} Mix", "${d.name} and similar artists", list, "Couldn't make this mix right now.", rowCtx, onRefresh = { round++ },
                                onBack = { pop() }, onPlayList = { l, i -> playRemoteList(l, i, "Playlist", "${d.name} Mix") },
                                onMenu = { r -> localFor(r)?.let { menuFor = MenuFor(it, null, false) } ?: run { remoteMenu = r } })
                        }
                        RadarRoute -> {
                            var list by remember { mutableStateOf<List<RemoteTrack>?>(null) }
                            var round by remember { mutableIntStateOf(0) }
                            LaunchedEffect(round) { list = null; list = Suggest.releaseRadar(releases, refresh = round > 0) }
                            RemoteListScreen("Release radar", "New songs from artists you follow and play", list, "Couldn't load the new releases right now.", rowCtx, onRefresh = { round++ },
                                onBack = { pop() }, onPlayList = { l, i -> playRemoteList(l, i, "Playlist", "Release radar") }, onMenu = { remoteMenu = it })
                        }
                        is FolderRoute -> {
                            val f = playlistFolders.firstOrNull { it.id == d.id }
                            if (f == null) LaunchedEffect(Unit) { pop() }
                            else FolderScreen(f, orderedPlaylists.filter { it.folder == f.id }, playlistArt, pinned.toSet(),
                                onMove = if (order == "Custom") movePlaylist else null,
                                onBack = { pop() }, onOpen = { push(PlaylistRoute(it)) }, onRename = { folderEdit = f },
                                onDelete = {
                                    val inside = playlists.filter { it.folder == f.id }.map { it.id }
                                    PlaylistStore.deleteFolder(f.id); pop()
                                    undoable("Deleted folder “${f.name}” (its playlists stay)") { PlaylistStore.restoreFolder(f, inside) }
                                })
                        }
                        DuplicatesRoute -> {
                            val counts = remember(plays) { plays.groupingBy { it.key }.eachCount() }
                            DuplicatesScreen(remember(tracks) { Smart.duplicates(tracks) }, counts, hiddenSongs.count(UserCollection::isLibraryKey), player?.currentId,
                                onBack = { pop() }, onPlay = { player?.play(listOf(it), 0, "Library", "Duplicates") },
                                onHide = { t -> UserCollection.toggleHidden(t.id); undoable("Hidden: ${t.title}") { UserCollection.toggleHidden(t.id) } },
                                onShowHidden = { UserCollection.showHiddenSongs() })
                        }
                        StatsRoute -> StatsScreen(plays, tracks, { byId[it] }, { name -> artists.firstOrNull { Match.norm(it.name) == Match.norm(name) }?.artUri }, player,
                            onBack = { pop() }, onOpenAlbum = { list, title, artist, art -> push(TracksRoute(title, "Album · $artist", art, list, "Album")) },
                            pc = linkedPc?.name)
                        HistoryRoute -> HistoryScreen(plays, { byId[it] }, player, onMenu = { menuFor = MenuFor(it, null, false) }) { pop() }
                        SmartRoute -> {
                            val now = remember { System.currentTimeMillis() }
                            val lists = remember(tracks, plays, liked) { Smart.Kind.entries.associateWith { Smart.list(it, tracks, plays, liked, byId::get, now) } }
                            SmartScreen(lists, onBack = { pop() }) { k ->
                                val l = lists[k].orEmpty()
                                push(TracksRoute(k.title, k.about, l.firstNotNullOfOrNull { it.artUri }, l, "Playlist"))
                            }
                        }
                        LikedRoute -> TrackListScreen(
                            title = "Liked Songs", subtitle = "Playlist · ${songCount(likedShown.size)}", art = null,
                            tracks = likedShown, kind = "Playlist", sourceId = LIKED_ID, player = player,
                            tile = Icons.Rounded.Favorite,
                            emptyText = "Songs you like show up here. Tap ♥ in the player or Add to Liked Songs in a song's ⋮ menu.",
                            onBack = { pop() }, onMenu = { menuFor = MenuFor(it, LIKED_ID, false) }, onQueue = ::queue,
                            onDownload = downloadAll(likedTracks),
                            more = getMissing(likedShown),
                            onAddTo = { addSongs(it) }, onEdit = { editMany = it },
                            // Your own order (Custom): drag to move, as in a playlist; the order syncs.
                            onMove = { from, to -> PlaylistStore.setLiked(Grouping.moved(liked, likedShown.map { it.id }, from, to)) },
                            onRemove = { list ->
                                val before = liked
                                PlaylistStore.setLiked(before - list.map { it.id }.toSet())
                                undoable("Removed ${songCount(list.size)} from Liked Songs") { PlaylistStore.setLiked(before) }
                            },
                        )
                        WantRoute -> WantScreen(want, rowCtx, onBack = { pop() }, onPlay = { playRemote(it, "Playlist", "Want to listen") })
                        is RemoteArtistRoute -> {
                            val mine = remember(artists, d.artist.name) { artists.firstOrNull { Match.norm(it.name) == Match.norm(d.artist.name) } }
                            RemoteArtistScreen(
                                d.artist, rowCtx, onBack = { pop() },
                                onPlayList = { list, i, label -> playRemoteList(list, i, "Artist", label) },
                                onRadio = { startRadio(null, d.artist.name, "", "${d.artist.name} Radio") },
                                onOpenAlbum = { push(RemoteAlbumRoute(it)) },
                                yours = mine?.let { a -> Triple(a.tracks.size, a.artUri) { push(artistRoute(a)) } },
                            )
                        }
                        is RemoteAlbumRoute -> RemoteAlbumScreen(
                            d.album, rowCtx, onBack = { pop() },
                            onPlayList = { list, i, label -> playRemoteList(list, i, "Album", label) },
                            onOpenArtist = { push(RemoteArtistRoute(it)) },
                            onDownload = { list -> download(list.mapNotNull { Downloads.forRemote(it) }) },
                        )
                        is PlaylistRoute -> {
                            val pl = playlists.firstOrNull { it.id == d.id }
                            if (pl == null) {
                                LaunchedEffect(Unit) { pop() }
                            } else {
                                // Imported catalog songs you've since added play in full; the rest are previews.
                                val (ownedPairs, previews) = remember(pl, byId, remoteSongs, localFor) {
                                    val own = LinkedHashMap<String, Pair<String, Track>>() // track id -> (saved id, track)
                                    val pre = ArrayList<RemoteTrack>()
                                    pl.trackIds.forEach { id ->
                                        val t = byId[id] ?: remoteSongs[id]?.let { r -> localFor(r) ?: run { pre.add(r); null } } ?: LinkSync.missingTrack(id)
                                        if (t != null) own.putIfAbsent(t.id, id to t)
                                    }
                                    own.values.toList() to pre.toList()
                                }
                                val owned = ownedPairs.map { it.second }
                                val ownedIds = ownedPairs.map { it.first }
                                var recRound by remember(pl.id) { mutableIntStateOf(0) }
                                var recs by remember(pl.id) { mutableStateOf<List<RemoteTrack>?>(null) }
                                LaunchedEffect(pl.id, recRound, pl.trackIds.isEmpty()) {
                                    recs = null
                                    recs = Suggest.recommendFor(pl.id, owned.map { it.artist to it.title } + previews.map { it.artist to it.title },
                                        pl.trackIds.toSet(), Prefs.listenBrainz(ctx), refresh = recRound > 0)
                                }
                                val shownRecs = recs?.filter { r -> r.key !in pl.trackIds && localFor(r)?.id?.let { it in pl.trackIds } != true }
                                TrackListScreen(
                                    title = pl.name, subtitle = (if (pl.id in pinned) "Pinned · " else "") + "Playlist · ${songCount(pl.trackIds.size)}",
                                    description = pl.description, art = playlistArt(pl),
                                    tracks = owned, kind = "Playlist", sourceId = pl.id, player = player,
                                    previews = previews, row = rowCtx,
                                    onPlayPreview = { i -> playRemoteList(previews, i, "Playlist", pl.name) },
                                    onPreviewMenu = { remoteMenu = it; remoteMenuPlaylist = pl.id },
                                    emptyText = "This playlist is empty. Add songs with Add to playlist in any song's ⋮ menu.",
                                    onBack = { pop() }, onMenu = { menuFor = MenuFor(it, pl.id, false) }, onQueue = ::queue,
                                    more = getMissing(owned) + listOf(
                                        MenuAction("Edit details", Icons.Rounded.Edit) { editing = pl },
                                        MenuAction(if (pl.id in pinned) "Unpin" else "Pin to the top", Icons.Rounded.PushPin) { togglePin(pl.id) },
                                        MenuAction("Duplicate", Icons.Rounded.ContentCopy) {
                                            PlaylistStore.duplicate(pl.id)?.let { id -> toast("Made “${pl.name} (copy)”"); push(PlaylistRoute(id)) }
                                        },
                                        MenuAction("Save as playlist file", Icons.Rounded.Save) { exporting = pl; exportFile.launch("${pl.name}.m3u") },
                                        MenuAction(if (pl.folder == null) "Move to a folder" else "Move to another folder", Icons.Rounded.Folder) { movingToFolder = pl },
                                        MenuAction("Delete playlist", Icons.Rounded.Delete) { confirmDelete = pl },
                                    ),
                                    onAddTo = { addSongs(it) }, onEdit = { editMany = it },
                                    onRemove = { list ->
                                        val before = pl.trackIds
                                        // A song can be saved under its catalog key and play from your file: both ids go.
                                        val gone = list.map { it.id }.toSet() + ownedPairs.filter { (_, t) -> list.any { it.id == t.id } }.map { it.first }
                                        PlaylistStore.setTracks(pl.id, before.filterNot { it in gone })
                                        undoable("Removed ${songCount(list.size)} from the playlist") { PlaylistStore.setTracks(pl.id, before) }
                                    },
                                    below = {
                                        recommended(shownRecs, rowCtx, onRefresh = { recRound++ },
                                            onAdd = { r ->
                                                val own = localFor(r)
                                                PlaylistStore.addToPlaylist(pl.id, listOf(own?.id ?: r.key), if (own == null) listOf(r) else emptyList())
                                                toast("Added to ${pl.name}")
                                            },
                                            onPlay = { l, i -> playRemoteList(l, i, "Playlist", "Recommended for ${pl.name}") })
                                    },
                                    onMove = { from, to ->
                                        PlaylistStore.moveInPlaylist(pl.id, pl.trackIds.indexOf(ownedIds[from]), pl.trackIds.indexOf(ownedIds[to]))
                                    },
                                    onDownload = downloadAll(owned.filterNot { LinkSync.isMissing(it.id) }, previews),
                                )
                            }
                        }
                        is TracksRoute -> {
                            // One of your artists: "Fans also like" from Deezer, like the desktop's artist page.
                            var related by remember(d.title) { mutableStateOf<List<RemoteArtist>>(emptyList()) }
                            var deezerId by remember(d.title) { mutableLongStateOf(0L) }
                            if (d.kind == "Artist") LaunchedEffect(d.title) {
                                deezerId = runCatching { ArtistIds.resolve(d.title) }.getOrDefault(0L)
                                related = if (deezerId > 0) runCatching { Deezer.related(deezerId, 12) }.getOrDefault(emptyList()).filterNot { UserCollection.isAi(it.name) } else emptyList()
                            }
                            TrackListScreen(
                                title = d.title, subtitle = d.subtitle, art = d.art, tracks = d.tracks, kind = d.kind,
                                sourceId = null, player = player, round = d.round,
                                onBack = { pop() }, onMenu = { menuFor = MenuFor(it, null, false) }, onQueue = ::queue,
                                onDownload = downloadAll(d.tracks),
                                onAddTo = { addSongs(it) }, onEdit = { editMany = it },
                                below = if (d.kind != "Artist" || deezerId <= 0) null else ({
                                    fansAlsoLike(d.title, related, onMore = { push(RemoteArtistRoute(RemoteArtist(deezerId, d.title, d.art))) }) { push(RemoteArtistRoute(it)) }
                                }),
                            )
                        }
                        null -> when (tab) {
                            0 -> HomeScreen(
                                orderedPlaylists, playlistArt, scanning, folders.isEmpty(),
                                onAvatar = { scope.launch { drawerState.open() } },
                                onAddFolder = { LibraryActions.addFolder?.invoke() },
                                onOpenLiked = { push(LikedRoute) },
                                onOpenWant = { push(WantRoute) },
                                onOpenPlaylist = { push(PlaylistRoute(it)) },
                                onShuffleAll = {
                                    if (songs.isNotEmpty()) {
                                        player?.play(songs, Random.nextInt(songs.size), "Library", "All songs", shuffled = true)
                                    }
                                },
                                mixes = mixes, building = mixesBuilding,
                                onOpenMix = { push(MixRoute(it)) },
                                releases = releases,
                                onOpenRelease = { push(RemoteAlbumRoute(it)) },
                                // The same song from two places (file and preview) shows once, like on the desktop.
                                recent = remember(plays, byId) {
                                    plays.asReversed().mapNotNull { byId[it.key] }.distinctBy { Match.norm(it.title) + "|" + Match.norm(it.artist) }.take(20)
                                },
                                onPlayRecent = { list, i -> player?.play(list, i, "Playlist", "Recently played") },
                                onOpenHistory = { push(HistoryRoute) },
                                topMixes = remember(plays, artistPics) {
                                    Smart.topArtists(plays, System.currentTimeMillis(), 6) { UserCollection.isBlocked(it) }.map { name ->
                                        name to (artistPics[name.lowercase()]?.ifBlank { null } ?: tracks.firstOrNull { Match.norm(it.artist) == Match.norm(name) }?.artUri)
                                    }
                                },
                                onOpenTopMix = { push(ArtistMixRoute(it)) },
                                onOpenRadar = { push(RadarRoute) },
                                // Albums from your folders, newest files first (like the desktop).
                                recentlyAdded = remember(albums, addedAt) {
                                    albums.filter { a -> a.tracks.any { it.id in addedAt } }.sortedByDescending { a -> a.tracks.maxOf { addedAt[it.id] ?: 0L } }.take(14)
                                },
                                onOpenAlbum = { push(albumRoute(it)) },
                            )
                            1 -> SearchScreen(
                                query, { query = it }, searchState, results, rowCtx,
                                recent = searches, onForget = { Prefs.forgetSearch(ctx, it); searches = Prefs.searches(ctx) },
                                errorDetail = searchError,
                                onOpenAppInfo = { openAppInfo(ctx) },
                                onPlay = { playRemote(it, "Search", query.trim()) },
                                onOpenArtist = { push(RemoteArtistRoute(it)) },
                                onOpenAlbum = { push(RemoteAlbumRoute(it)) },
                            )
                            else -> LibraryScreen(
                                playlists = orderedPlaylists, playlistArt = playlistArt,
                                pinned = pinned.toSet(), order = order, onOrder = { order = it; Prefs.setPlaylistOrder(ctx, it) },
                                onImport = { importFile.launch(arrayOf("*/*")) },
                                playlistFolders = playlistFolders, onOpenPlaylistFolder = { push(FolderRoute(it)) }, onNewPlaylistFolder = { newFolder = true },
                                onMovePlaylist = movePlaylist,
                                likedCount = likedShown.size, wantCount = want.size,
                                albums = albums, artists = artists, folders = folderGroups, songs = songs,
                                savedAlbums = savedAlbums, followed = followed,
                                serverPlaylists = serverPlaylists,
                                onOpenSmart = { push(SmartRoute) },
                                onOpenServerPlaylist = { sp ->
                                    val list = sp.songIds.mapNotNull { byId[Server.PREFIX + it] }
                                    push(TracksRoute(sp.name, "Playlist · Music server · ${songCount(list.size)}", list.firstNotNullOfOrNull { it.artUri }, list, "Playlist"))
                                },
                                onOpenRemoteAlbum = { push(RemoteAlbumRoute(it)) },
                                onOpenRemoteArtist = { push(RemoteArtistRoute(it)) },
                                currentId = player?.currentId,
                                onAvatar = { scope.launch { drawerState.open() } },
                                onCreate = { showCreate = true },
                                onOpenLiked = { push(LikedRoute) },
                                onOpenWant = { push(WantRoute) },
                                onOpenPlaylist = { push(PlaylistRoute(it)) },
                                onOpenAlbum = { push(albumRoute(it)) },
                                onOpenArtist = { push(artistRoute(it)) },
                                onOpenFolder = { push(TracksRoute(it.name, "Folder · ${songCount(it.tracks.size)}", null, it.tracks, "Folder")) },
                                onPlaySongs = { list, i -> player?.play(list, i, "Library", "Your library") },
                                onMenu = { menuFor = MenuFor(it, null, false) },
                                onQueue = ::queue,
                                onAddTo = { addSongs(listOf(it)) },
                            )
                        }
                    }
                    }
                }
            }
        }

        val p = player
        AnimatedVisibility(
            visible = fullPlayer && p?.hasItem == true,
            enter = slideInVertically(tween(320)) { it },
            exit = slideOutVertically(tween(260)) { it },
        ) {
            if (p != null) {
                val remote = currentRemote(p)
                FullPlayer(
                    player = p,
                    tint = tint,
                    saved = if (remote != null) remote.key in wantKeys else liked.contains(p.currentId),
                    onClose = { fullPlayer = false },
                    onSave = {
                        if (remote != null) PlaylistStore.toggleWant(remote) else p.currentId?.let { PlaylistStore.toggleLike(it) }
                    },
                    onMenu = {
                        if (remote != null) remoteMenu = remote
                        else p.currentId?.let { byId[it] }?.let { menuFor = MenuFor(it, p.sourceId, true) }
                    },
                    onQueue = { openQueue() },
                    onDevices = { showDevices = true },
                    badge = rememberQuality(p.currentId?.let { byId[it] }),
                )
            }
        }

        infoFor?.let { t -> SongInfoDialog(t, plays, meta.containsKey(t.id)) { infoFor = null } }
        editFor?.let { t -> EditDetailsDialog(t, appleOn) { editFor = null } }
        editMany?.let { list -> if (list.size == 1) EditDetailsDialog(list[0], appleOn) { editMany = null } else EditManyDialog(list) { editMany = null } }
        if (sleepSheet && player != null) SleepSheet(player) { sleepSheet = false }

        // Outside the Scaffold so messages also show above the full player.
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(
                bottom = when {
                    fullPlayer -> 24.dp
                    player?.hasItem == true -> 150.dp
                    else -> 90.dp
                },
            ),
        )
    }

    menuFor?.let { m ->
        val t = m.track
        val pid = m.playlistId
        val isLiked = liked.contains(t.id)
        val playingIt = m.fromPlayer && player?.currentId == t.id // once another song plays, its ♥ and Play next are in the menu again
        val album = albums.firstOrNull { it.key == t.albumKey }
        val artist = artists.firstOrNull { a -> a.tracks.firstOrNull()?.artistKey == t.artistKey }
        val inPlaylist = when (pid) {
            null -> false
            LIKED_ID -> m.fromPlayer && isLiked // inside Liked Songs the ♥ row already covers it
            else -> playlists.firstOrNull { it.id == pid }?.trackIds?.contains(t.id) == true
        }
        val missing = LinkSync.isMissing(t.id)
        val actions = buildList {
            if (missing) {
                // Only on your PC: play it from there, copy it here, find it, or tidy the list.
                linkedPc?.let { pc ->
                    add(MenuAction("Play from ${pc.name}", Icons.Rounded.PlayArrow) { player?.playItems(listOf(LinkDataSource.item(t)), 0, "Your PC", pc.name, shuffled = false) })
                    add(MenuAction("Get it from ${pc.name}", Icons.Rounded.Download) { Downloads.forMissing(t)?.let { download(listOf(it)) } })
                }
                add(MenuAction("Search for it", Icons.Rounded.Search) { menuFor = null; closePages(); tab = 1; query = "${t.artist} ${t.title}" })
                add(MenuAction("Start radio", Icons.Rounded.Radio) { startRadio(null, t.artist, t.id, "${t.title} Radio") })
                add(MenuAction("Add to playlist", Icons.Rounded.AddCircleOutline) { addSongs(listOf(t)) })
            }
            if (!playingIt && !missing) {
                add(MenuAction(if (isLiked) "Remove from Liked Songs" else "Add to Liked Songs",
                    if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder) { PlaylistStore.toggleLike(t.id) })
            }
            if (!missing) add(MenuAction("Copy song name", Icons.Rounded.ContentCopy) { copyText(ctx, "Song name", "${t.title} — ${t.artist}") })
            if (!missing) add(MenuAction("Add to playlist", Icons.Rounded.AddCircleOutline) { addSongs(listOf(t)) })
            if (!missing) Downloads.forTrack(t, serverCfg)?.let { item -> add(MenuAction("Download", Icons.Rounded.Download) { download(listOf(item)) }) }
            if (inPlaylist && pid != null) {
                add(MenuAction("Remove from this playlist", Icons.Rounded.RemoveCircleOutline) {
                    if (pid == LIKED_ID) { PlaylistStore.toggleLike(t.id); undoable("Removed from Liked Songs") { PlaylistStore.toggleLike(t.id) } }
                    else {
                        val before = playlists.firstOrNull { it.id == pid }?.trackIds.orEmpty()
                        PlaylistStore.removeFromPlaylist(pid, t.id)
                        undoable("Removed from the playlist") { PlaylistStore.setTracks(pid, before) }
                    }
                })
                // Long playlists: a far move in one tap instead of many drags.
                playlists.firstOrNull { it.id == pid }?.let { pl ->
                    val at = pl.trackIds.indexOf(t.id)
                    if (at > 0) add(MenuAction("Move to top", Icons.Rounded.VerticalAlignTop) { PlaylistStore.moveInPlaylist(pid, at, 0) })
                    if (at >= 0 && at < pl.trackIds.size - 1) {
                        add(MenuAction("Move to bottom", Icons.Rounded.VerticalAlignBottom) { PlaylistStore.moveInPlaylist(pid, at, pl.trackIds.size - 1) })
                    }
                }
            }
            if (missing) return@buildList
            if (!playingIt) {
                add(MenuAction("Play next", Icons.Rounded.QueuePlayNext) { player?.playNext(t.toMediaItem()); toast("Playing next") })
            }
            add(MenuAction("Add to queue", Icons.AutoMirrored.Rounded.PlaylistAdd) { queue(t) })
            add(MenuAction("Start radio", Icons.Rounded.Radio) {
                startRadio(t.toMediaItem(), t.artist, t.id, "${t.title} Radio", keepCurrent = m.fromPlayer && player?.currentId == t.id)
            })
            if (m.fromPlayer) {
                add(MenuAction("Go to queue", Icons.AutoMirrored.Rounded.QueueMusic) { openQueue() })
            }
            if (album != null) add(MenuAction("Go to album", Icons.Rounded.Album) { fullPlayer = false; push(albumRoute(album)) })
            if (artist != null) add(MenuAction("Go to artist", Icons.Rounded.Person) { fullPlayer = false; openArtist(artist) })
            if (meta.containsKey(t.id)) {
                add(MenuAction("Restore original tags", Icons.Rounded.Restore) { Overrides.restore(t.id); toast("Original tags restored") })
            }
            add(MenuAction("Edit details", Icons.Rounded.Edit) { editFor = t })
            add(MenuAction("Song info", Icons.Rounded.Info) { infoFor = t })
            if (m.fromPlayer && player != null) add(MenuAction(if (SleepTimer.on) "Sleep timer (on)" else "Sleep timer", Icons.Rounded.Bedtime) { sleepSheet = true })
            if (t.id !in hidden) add(MenuAction("Hide from library", Icons.Rounded.VisibilityOff) {
                UserCollection.toggleHidden(t.id); undoable("Hidden: ${t.title}") { UserCollection.toggleHidden(t.id) }
            })
        }
        SongMenu(t.title, if (missing) "${t.artist} · not on this phone" else t.artist, t.artUri, actions) { menuFor = null }
    }

    remoteMenu?.let { r ->
        val wanted = r.key in wantKeys
        val link = if (r.source == Source.DEEZER) "\nhttps://www.deezer.com/track/${r.id}" else ""
        val inPlaylist = remoteMenuPlaylist
        val actions = listOfNotNull(
            MenuAction(if (link.isEmpty()) "Copy song name" else "Copy link", Icons.Rounded.ContentCopy) {
                copyText(ctx, if (link.isEmpty()) "Song name" else "Link", "${r.title} — ${r.artist}$link")
            },
            MenuAction(if (wanted) "Remove from Want to listen" else "Add to Want to listen",
                if (wanted) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder) { PlaylistStore.toggleWant(r) },
            MenuAction("Add to playlist", Icons.Rounded.AddCircleOutline) { addCatalogSong(r) },
            Downloads.forRemote(r)?.let { item -> MenuAction("Download", Icons.Rounded.Download) { download(listOf(item)) } },
            // "Not interested": shapes Daily/Weekly, and the artist page can undo the artist part.
            (r.key in hiddenSongs).let { h ->
                MenuAction(if (h) "Suggest this song again" else "Not interested in this song", Icons.Rounded.Block) { UserCollection.toggleHidden(r.key) }
            },
            UserCollection.isBlocked(r.artist).let { b ->
                MenuAction(if (b) "Suggest ${r.artist} again" else "Don't suggest ${r.artist}", Icons.Rounded.PersonOff) {
                    UserCollection.toggleBlocked(r.artist)
                }
            },
            inPlaylist?.let { pl ->
                MenuAction("Remove from this playlist", Icons.Rounded.RemoveCircleOutline) { PlaylistStore.removeFromPlaylist(pl, r.key) }
            },
            MenuAction("Start radio", Icons.Rounded.Radio) {
                val playing = player?.currentId == r.key
                if (!playing) playRemote(r, "Radio", "${r.title} Radio")
                startRadio(null, r.artist, r.key, "${r.title} Radio", keepCurrent = true)
            },
            MenuAction("Go to queue", Icons.AutoMirrored.Rounded.QueueMusic) { openQueue() },
        )
        SongMenu(r.title, r.artist, r.cover, actions) { remoteMenu = null; remoteMenuPlaylist = null }
    }

    adding?.let { add ->
        AddToPlaylistSheet(
            orderedPlaylists,
            onDismiss = { adding = null },
            onPick = { pl -> add(pl); adding = null },
            onCreate = { pendingAdd = add; adding = null; showCreate = true },
        )
    }

    if (showCreate) {
        CreatePlaylistDialog(
            onDismiss = { showCreate = false; pendingAdd = null },
            onCreate = { name ->
                val id = PlaylistStore.createPlaylist(name)
                pendingAdd?.let { add -> PlaylistStore.playlists.value.firstOrNull { it.id == id }?.let(add) }
                pendingAdd = null; showCreate = false
            },
        )
    }

    confirmDelete?.let { pl ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    val at = playlists.indexOfFirst { it.id == pl.id }
                    PlaylistStore.deletePlaylist(pl.id); confirmDelete = null; pop()
                    undoable("Deleted “${pl.name}”") { PlaylistStore.restore(pl, at) } // a pin stays, so it comes back pinned
                }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel", color = TextDim) } },
            title = { Text("Delete “${pl.name}”?") },
            text = { Text("Only the playlist is removed. Your music files stay untouched.", color = TextDim) },
            containerColor = CardBg,
        )
    }

    editing?.let { pl ->
        PlaylistDetailsDialog(pl, playlistArt(pl), onDismiss = { editing = null }) { name, about, picked, removed ->
            val cover = when {
                picked != null -> saveCover(ctx, pl.id, picked) ?: pl.cover.also { toast("Couldn't use that photo") }
                removed -> null
                else -> pl.cover
            }
            if (cover != pl.cover) pl.cover?.let { old -> runCatching { java.io.File(old).delete() } }
            PlaylistStore.setDetails(pl.id, name, about, cover)
            editing = null
        }
    }

    movingToFolder?.let { pl ->
        MoveToFolderDialog(pl, playlistFolders, onDismiss = { movingToFolder = null }, onNew = { newFolder = true }) { fid ->
            PlaylistStore.moveToFolder(pl.id, fid); movingToFolder = null
            toast(if (fid == null) "Moved out of its folder" else "Moved to “${playlistFolders.firstOrNull { it.id == fid }?.name}”")
        }
    }
    if (newFolder) CreatePlaylistDialog(
        onDismiss = { newFolder = false },
        onCreate = { name ->
            val id = PlaylistStore.createFolder(name)
            movingToFolder?.let { PlaylistStore.moveToFolder(it.id, id); movingToFolder = null }
            newFolder = false
        },
        heading = "New folder", initial = "", confirm = "Create",
    )
    folderEdit?.let { f ->
        CreatePlaylistDialog(onDismiss = { folderEdit = null }, onCreate = { PlaylistStore.renameFolder(f.id, it); folderEdit = null },
            heading = "Rename folder", initial = f.name, confirm = "Rename")
    }

    if (savingQueue) {
        val day = remember { java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("d MMM")) }
        CreatePlaylistDialog(
            onDismiss = { savingQueue = false },
            onCreate = { name ->
                val items = player?.queue().orEmpty()
                val songs = items.mapNotNull { q -> remoteOf(q.id, q.title, q.artist, "", q.artUri) }
                val id = PlaylistStore.createPlaylist(name.ifBlank { "Queue $day" })
                PlaylistStore.addToPlaylist(id, items.map { it.id }.distinct(), songs)
                savingQueue = false
                toast("Saved ${songCount(items.map { it.id }.distinct().size)} as a playlist")
            },
            heading = "Save queue as playlist", initial = "Queue $day", confirm = "Save",
        )
    }

    if (showDevices) PlayOnSheet(player, tracks, byId, ::toast) { showDevices = false }
}

private fun currentRemote(p: PlayerState): RemoteTrack? = p.currentId?.let { remoteOf(it, p.title, p.artist, p.album, p.artUri) }

private fun remoteOf(key: String, title: String, artist: String, album: String, art: String?): RemoteTrack? =
    io.github.realkhfan.onda.core.LinkSync.remoteOf(key, title, artist, album, art)

/* ---------------- Home ---------------- */

@Composable
internal fun HomeScreen(
    playlists: List<PlaylistStore.Playlist>,
    playlistArt: (PlaylistStore.Playlist) -> String?,
    scanning: Boolean,
    noFolders: Boolean,
    onAvatar: () -> Unit,
    onAddFolder: () -> Unit,
    onOpenLiked: () -> Unit,
    onOpenWant: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onShuffleAll: () -> Unit,
    mixes: Map<Discovery.Kind, Mixes.Mix>,
    building: Set<Discovery.Kind>,
    onOpenMix: (Discovery.Kind) -> Unit,
    releases: List<RemoteAlbum>,
    onOpenRelease: (RemoteAlbum) -> Unit,
    recent: List<Track> = emptyList(),
    onPlayRecent: (List<Track>, Int) -> Unit = { _, _ -> },
    onOpenHistory: () -> Unit = {},
    topMixes: List<Pair<String, String?>> = emptyList(),
    onOpenTopMix: (String) -> Unit = {},
    onOpenRadar: () -> Unit = {},
    recentlyAdded: List<Album> = emptyList(),
    onOpenAlbum: (Album) -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        // Greets you like the desktop does.
        val hour = remember { java.time.LocalTime.now().hour }
        TopBar(when (hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }, onAvatar)
        if (noFolders) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add the folders your music lives in to get started.", color = TextDim)
                Button(onClick = onAddFolder) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Add music folder") }
            }
            return
        }
        if (scanning) Text("Scanning…", color = TextDim, modifier = Modifier.padding(16.dp, 0.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { ShortcutTile("Liked Songs", onOpenLiked) { IconTile(Icons.Rounded.Favorite, 56.dp) } }
            item { ShortcutTile("Want to listen", onOpenWant) { IconTile(Icons.Rounded.Bookmark, 56.dp, WantBg, Accent) } }
            item { ShortcutTile("Shuffle all", onShuffleAll) { IconTile(Icons.Rounded.Shuffle, 56.dp, CardBg, Accent) } }
            // A few playlists as shortcuts; the rest are in Your Library, so the mixes stay near the top.
            items(playlists.take(3)) { pl -> ShortcutTile(pl.name, { onOpenPlaylist(pl.id) }) { Art(playlistArt(pl), 56.dp) } }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Made for you", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Discovery.Kind.entries.forEach { k ->
                        MixCard(k, mixes[k], k in building, Modifier.weight(1f)) { onOpenMix(k) }
                    }
                }
            }
            if (topMixes.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Your top mixes", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(topMixes, key = { it.first }) { (name, art) -> TopMixCard(name, art) { onOpenTopMix(name) } }
                    }
                }
            }
            if (releases.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("New releases", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = onOpenRadar) { Text("Play as a playlist", color = Accent) }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val today = remember { java.time.LocalDate.now() }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(releases.take(20), key = { it.id }) { a -> ReleaseCard(a, today) { onOpenRelease(a) } }
                    }
                }
            }
            if (recent.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recently played", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = onOpenHistory) { Text("Show all", color = TextDim) }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(recent) { i, t ->
                            Column(Modifier.width(132.dp).clickable { onPlayRecent(recent, i) }) {
                                Art(t.artUri, 132.dp)
                                Text(t.title, color = TextMain, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                                Text(t.artist, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            if (recentlyAdded.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Recently added", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recentlyAdded, key = { it.key }) { a ->
                            Column(Modifier.width(132.dp).clickable { onOpenAlbum(a) }) {
                                Art(a.artUri, 132.dp)
                                Text(a.title, color = TextMain, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                                Text(a.artist, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutTile(title: String, onClick: () -> Unit, leading: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(6.dp)).background(Raised).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(10.dp))
        Text(title, color = TextMain, fontWeight = FontWeight.SemiBold, maxLines = 2, fontSize = 13.sp,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
    }
}

/* ---------------- Library ---------------- */

@Composable
internal fun LibraryScreen(
    playlists: List<PlaylistStore.Playlist>,
    playlistArt: (PlaylistStore.Playlist) -> String?,
    likedCount: Int,
    wantCount: Int,
    albums: List<Album>,
    artists: List<ArtistGroup>,
    folders: List<FolderGroup>,
    songs: List<Track>,
    currentId: String?,
    onAvatar: () -> Unit,
    onCreate: () -> Unit,
    onOpenLiked: () -> Unit,
    onOpenWant: () -> Unit,
    onOpenSmart: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (ArtistGroup) -> Unit,
    onOpenFolder: (FolderGroup) -> Unit,
    savedAlbums: List<RemoteAlbum>,
    followed: List<RemoteArtist>,
    serverPlaylists: List<Subsonic.Playlist>,
    onOpenServerPlaylist: (Subsonic.Playlist) -> Unit,
    onOpenRemoteAlbum: (RemoteAlbum) -> Unit,
    onOpenRemoteArtist: (RemoteArtist) -> Unit,
    onPlaySongs: (List<Track>, Int) -> Unit,
    onMenu: (Track) -> Unit,
    onQueue: (Track) -> Unit,
    onAddTo: (Track) -> Unit = {},
    pinned: Set<String> = emptySet(),
    order: String = "Custom",
    onOrder: (String) -> Unit = {},
    onImport: () -> Unit = {},
    playlistFolders: List<PlaylistStore.Folder> = emptyList(),
    onOpenPlaylistFolder: (String) -> Unit = {},
    onNewPlaylistFolder: () -> Unit = {},
    /** A drag in the custom order ([shown]: the playlist ids as listed). */
    onMovePlaylist: (shown: List<String>, from: Int, to: Int) -> Unit = { _, _, _ -> },
) {
    var chip by rememberSaveable { mutableIntStateOf(0) }
    val reorder = rememberReorder()
    var ordering by remember { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var q by rememberSaveable { mutableStateOf("") }
    BackHandler(enabled = searching) { searching = false; q = "" }

    val ql = q.trim().lowercase()
    val fPl = remember(ql, playlists) { if (ql.isEmpty()) emptyList() else playlists.filter { it.name.lowercase().contains(ql) } }
    val fAr = remember(ql, artists) { if (ql.isEmpty()) emptyList() else artists.filter { it.name.lowercase().contains(ql) }.take(20) }
    val fAl = remember(ql, albums) {
        if (ql.isEmpty()) emptyList() else albums.filter { it.title.lowercase().contains(ql) || it.artist.lowercase().contains(ql) }.take(20)
    }
    val fSo = remember(ql, songs) {
        if (ql.isEmpty()) emptyList()
        else songs.filter {
            it.title.lowercase().contains(ql) || it.artist.lowercase().contains(ql) || it.album.lowercase().contains(ql)
        }.take(100)
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp, 16.dp, 8.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(onAvatar)
            Spacer(Modifier.width(12.dp))
            Text("Your Library", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { searching = !searching; if (!searching) q = "" }) {
                Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search,
                    if (searching) "Close library search" else "Search your library", tint = TextMain)
            }
            IconButton(onClick = onCreate) { Icon(Icons.Rounded.Add, "Create playlist", tint = TextMain) }
        }
        if (searching) {
            SearchField(q, { q = it }, "Search your library", Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp), autoFocus = true)
        } else {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp, 0.dp, 16.dp, 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Playlists", "Albums", "Artists", "Folders").forEachIndexed { i, l -> Chip(l, chip == i) { chip = i } }
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (searching) {
                when {
                    ql.isEmpty() -> item {
                        Text("Search your songs, albums, artists and playlists.", color = TextDim, modifier = Modifier.padding(24.dp))
                    }
                    fPl.isEmpty() && fAr.isEmpty() && fAl.isEmpty() && fSo.isEmpty() -> item {
                        Text("Nothing in your library matches “${q.trim()}”.", color = TextDim, modifier = Modifier.padding(24.dp))
                    }
                }
                if (fPl.isNotEmpty()) item { SectionLabel("Playlists") }
                items(fPl) { pl ->
                    GroupRow(pl.name, "Playlist · ${songCount(pl.trackIds.size)}", { onOpenPlaylist(pl.id) }) { Art(playlistArt(pl), 52.dp) }
                }
                if (fAr.isNotEmpty()) item { SectionLabel("Artists") }
                items(fAr) { a -> GroupRow(a.name, "Artist", { onOpenArtist(a) }) { Art(a.artUri, 52.dp, round = true) } }
                if (fAl.isNotEmpty()) item { SectionLabel("Albums") }
                items(fAl) { a -> GroupRow(a.title, "Album · ${a.artist}", { onOpenAlbum(a) }) { Art(a.artUri, 52.dp) } }
                if (fSo.isNotEmpty()) item { SectionLabel("Songs") }
                itemsIndexed(fSo) { i, t ->
                    SwipeTrackRow(t, t.id == currentId, onQueue = { onQueue(t) }, onMenu = { onMenu(t) }, onAddTo = { onAddTo(t) }) { onPlaySongs(fSo, i) }
                }
            } else {
                when (chip) {
                    0 -> {
                        item {
                            GroupRow("Liked Songs", "Playlist · ${songCount(likedCount)}", onOpenLiked) { IconTile(Icons.Rounded.Favorite, 52.dp) }
                        }
                        item {
                            GroupRow("Want to listen", "Previews · ${songCount(wantCount)}", onOpenWant) {
                                IconTile(Icons.Rounded.Bookmark, 52.dp, WantBg, Accent)
                            }
                        }
                        item { GroupRow("Smart playlists", "Most played, On repeat, Never played, Forgotten favourites", onOpenSmart) { IconTile(Icons.Rounded.AutoAwesome, 52.dp, CardBg, Accent) } }
                        if (playlists.size > 1) item {
                            Box(Modifier.padding(start = 4.dp)) {
                                TextButton(onClick = { ordering = true }) {
                                    Icon(Icons.AutoMirrored.Rounded.Sort, null, tint = TextDim, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Playlists: $order", color = TextDim, fontSize = 13.sp)
                                }
                                DropdownMenu(ordering, { ordering = false }) {
                                    listOf("Custom", "A–Z", "Most songs").forEach { o ->
                                        DropdownMenuItem({ Text(o, color = if (o == order) Accent else TextMain) }, { onOrder(o); ordering = false })
                                    }
                                }
                            }
                        }
                        // Folders first (like Spotify), then the playlists that aren't in one.
                        items(playlistFolders, key = { "f" + it.id }) { f ->
                            val n = playlists.count { it.folder == f.id }
                            GroupRow(f.name, "Folder · $n playlist${if (n == 1) "" else "s"}", { onOpenPlaylistFolder(f.id) }) { IconTile(Icons.Rounded.Folder, 52.dp, CardBg, TextDim) }
                        }
                        val folderIds = playlistFolders.map { it.id }.toSet()
                        val loose = playlists.filter { it.folder == null || it.folder !in folderIds }
                        val movable = order == "Custom" && loose.size > 1 // in your own order, drag the handle to move one
                        itemsIndexed(loose, key = { _, pl -> pl.id }) { i, pl ->
                            GroupRow(pl.name, (if (pl.id in pinned) "Pinned · " else "") + "Playlist · ${songCount(pl.trackIds.size)}", { onOpenPlaylist(pl.id) },
                                modifier = if (movable) Modifier.reorderRow(reorder, i, loose.size) else Modifier,
                                trailing = if (!movable) null else { { DragHandle(reorder, i, loose.size, pl.name) { from, to -> onMovePlaylist(loose.map { it.id }, from, to) } } },
                                gestures = if (movable) Modifier.holdToMove(reorder, i, loose.size) { from, to -> onMovePlaylist(loose.map { it.id }, from, to) } else Modifier,
                            ) {
                                Art(playlistArt(pl), 52.dp)
                            }
                        }
                        items(serverPlaylists) { sp ->
                            GroupRow(sp.name, "Playlist · Music server · ${songCount(sp.songIds.size)}", { onOpenServerPlaylist(sp) }) {
                                IconTile(Icons.Rounded.Dns, 52.dp)
                            }
                        }
                        if (playlists.isEmpty()) item {
                            Text("Tap + to create your first playlist.", color = TextDim, modifier = Modifier.padding(24.dp))
                        }
                        item { GroupRow("New folder", "Group playlists, e.g. by mood or year", onNewPlaylistFolder) { IconTile(Icons.Rounded.CreateNewFolder, 52.dp, CardBg, TextDim) } }
                        item { GroupRow("Import a playlist file", ".m3u or .m3u8, e.g. saved by Onda on your PC", onImport) { IconTile(Icons.Rounded.FileOpen, 52.dp, CardBg, TextDim) } }
                    }
                    1 -> {
                        items(savedAlbums) { a -> GroupRow(a.title, "Saved · ${a.artist}", { onOpenRemoteAlbum(a) }) { Art(a.cover, 52.dp) } }
                        items(albums) { a -> GroupRow(a.title, "Album · ${a.artist}", { onOpenAlbum(a) }) { Art(a.artUri, 52.dp) } }
                    }
                    2 -> {
                        items(followed) { a -> GroupRow(a.name, "Following", { onOpenRemoteArtist(a) }) { Art(a.picture, 52.dp, round = true) } }
                        items(artists) { a -> GroupRow(a.name, "Artist", { onOpenArtist(a) }) { Art(a.artUri, 52.dp, round = true) } }
                    }
                    else -> items(folders) { f ->
                        GroupRow(f.name, "Folder · ${songCount(f.tracks.size)}", { onOpenFolder(f) }) {
                            Art(f.tracks.firstNotNullOfOrNull { it.artUri }, 52.dp)
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- Track list (playlist / album / artist / folder) ---------------- */

@Composable
internal fun TrackListScreen(
    title: String,
    subtitle: String,
    art: String?,
    tracks: List<Track>,
    kind: String,
    sourceId: String?,
    player: PlayerState?,
    onBack: () -> Unit,
    onMenu: (Track) -> Unit,
    onQueue: (Track) -> Unit,
    tile: ImageVector? = null,
    round: Boolean = false,
    emptyText: String? = null,
    description: String = "",
    previews: List<RemoteTrack> = emptyList(),
    row: RemoteRowContext? = null,
    onPlayPreview: (index: Int) -> Unit = {},
    onPreviewMenu: (RemoteTrack) -> Unit = {},
    onDownload: (() -> Unit)? = null,
    onMove: ((from: Int, to: Int) -> Unit)? = null,
    /** The playlist's own ⋮ menu (edit, pin, duplicate, save as file, delete). */
    more: List<MenuAction> = emptyList(),
    /** Long-press selects songs; these act on the selection (null: not offered here). */
    onAddTo: ((List<Track>) -> Unit)? = null,
    onRemove: ((List<Track>) -> Unit)? = null,
    below: (LazyListScope.() -> Unit)? = null,
    onEdit: ((List<Track>) -> Unit)? = null,
) {
    val reorder = rememberReorder()
    // Sort and find, like the desktop's list header.
    var sort by remember(title) { mutableStateOf(Smart.Sort.CUSTOM) }
    var filter by remember(title) { mutableStateOf<String?>(null) }
    var sorting by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    var selected by remember(title) { mutableStateOf<Set<String>>(emptySet()) }
    BackHandler(enabled = selected.isNotEmpty()) { selected = emptySet() }
    val plays by PlayLog.plays.collectAsState()
    val counts = remember(plays) { plays.groupingBy { it.key }.eachCount() }
    val shown = remember(tracks, sort, counts, filter) {
        val f = filter?.let(Match::norm).orEmpty()
        Smart.sorted(tracks, sort, counts, MusicLibrary.addedAt.value).filter { f.isEmpty() || Match.norm("${it.title} ${it.artist} ${it.album}").contains(f) }
    }
    val picked = shown.filter { it.id in selected }
    // Songs only on your PC (after a sync) are listed greyed out but never played or queued.
    val playable = remember(shown) { shown.filterNot { LinkSync.isMissing(it.id) } }
    val pickedHere = picked.filterNot { LinkSync.isMissing(it.id) }
    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) {
            SelectionBar(
                count = picked.size,
                onClose = { selected = emptySet() },
                onPlay = { if (pickedHere.isNotEmpty()) player?.play(pickedHere, 0, kind, title, sourceId, shuffled = false); selected = emptySet() },
                onQueue = { pickedHere.forEach(onQueue); selected = emptySet() },
                onAddTo = onAddTo?.let { add -> { add(picked); selected = emptySet() } },
                actions = buildList {
                    add(MenuAction("Select all", Icons.Rounded.SelectAll) { selected = shown.map { it.id }.toSet() })
                    add(MenuAction("Add to Liked Songs", Icons.Rounded.FavoriteBorder) { PlaylistStore.addLikes(picked.map { it.id }); selected = emptySet() })
                    onEdit?.let { ed -> add(MenuAction("Edit details", Icons.Rounded.Edit) { if (pickedHere.isNotEmpty()) ed(pickedHere); selected = emptySet() }) }
                    onRemove?.let { rm -> add(MenuAction("Remove from this list", Icons.Rounded.RemoveCircleOutline) { rm(picked); selected = emptySet() }) }
                    add(MenuAction("Hide from library", Icons.Rounded.VisibilityOff) {
                        pickedHere.filterNot { it.id in UserCollection.hidden.value }.forEach { UserCollection.toggleHidden(it.id) }; selected = emptySet()
                    })
                },
            )
        } else BackTopBar(title, onBack) {
            if (tracks.size > 1) IconButton(onClick = { filter = if (filter == null) "" else null }) {
                Icon(Icons.Rounded.Search, "Find in this list", tint = if (filter != null) Accent else TextDim)
            }
            if (tracks.size > 1) Box {
                IconButton(onClick = { sorting = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, "Sort: ${sort.label}", tint = if (sort != Smart.Sort.CUSTOM) Accent else TextDim) }
                DropdownMenu(sorting, { sorting = false }) {
                    Smart.Sort.entries.forEach { o -> DropdownMenuItem({ Text(o.label, color = if (o == sort) Accent else TextMain) }, { sort = o; sorting = false }) }
                }
            }
            if (playable.isNotEmpty()) IconButton(onClick = { playable.forEach(onQueue) }) { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add all to the queue", tint = TextDim) }
            if (onDownload != null) IconButton(onClick = onDownload) { Icon(Icons.Rounded.Download, "Download $title", tint = TextDim) }
            if (more.isNotEmpty()) Box {
                IconButton(onClick = { moreOpen = true }) { Icon(Icons.Rounded.MoreVert, "More for $title", tint = TextDim) }
                DropdownMenu(moreOpen, { moreOpen = false }) {
                    more.forEach { a -> DropdownMenuItem({ Text(a.label) }, { moreOpen = false; a.run() }, leadingIcon = { Icon(a.icon, null) }) }
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (tile != null) IconTile(tile, 96.dp) else Art(art, 96.dp, round = round)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        if (description.isNotBlank()) Text(description, color = TextMain, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(subtitle + if (tracks.any { it.durationMs > 0 }) " · ${Smart.length(tracks)}" else "", color = TextDim, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (playable.isNotEmpty()) player?.play(playable, 0, kind, title, sourceId, shuffled = false) else onPlayPreview(-1)
                                },
                                enabled = playable.isNotEmpty() || previews.isNotEmpty(),
                            ) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
                            OutlinedButton(
                                onClick = { player?.play(playable, Random.nextInt(playable.size), kind, title, sourceId, shuffled = true) },
                                enabled = playable.isNotEmpty(),
                            ) { Icon(Icons.Rounded.Shuffle, null); Text("Shuffle") }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                filter?.let { f ->
                    OutlinedTextField(f, { filter = it }, placeholder = { Text("Find in $title") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(16.dp, 0.dp, 16.dp, 8.dp))
                }
                if (tracks.isEmpty() && previews.isEmpty() && emptyText != null) Text(emptyText, color = TextDim, modifier = Modifier.padding(24.dp))
            }
            itemsIndexed(shown) { i, t ->
                val selecting = selected.isNotEmpty()
                val toggle = { selected = if (t.id in selected) selected - t.id else selected + t.id }
                val row = @Composable {
                    val missing = LinkSync.isMissing(t.id)
                    SwipeTrackRow(
                        t, player?.currentId == t.id, onQueue = { onQueue(t) }, onMenu = { onMenu(t) },
                        selected = t.id in selected, onLongClick = toggle, missing = missing, onAddTo = onAddTo?.let { add -> { add(listOf(t)) } },
                    ) { if (selecting) toggle() else if (!missing) player?.play(playable, playable.indexOf(t), kind, title, sourceId) }
                }
                // Dragging moves songs only in the list's own order.
                if (onMove == null || sort != Smart.Sort.CUSTOM || filter != null || selecting) {
                    row()
                } else {
                    Row(Modifier.reorderRow(reorder, i, shown.size), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { row() }
                        DragHandle(reorder, i, shown.size, t.title, onMove)
                    }
                }
            }
            if (previews.isNotEmpty() && row != null) {
                item { SectionLabel("Previews · not in your library") }
                itemsIndexed(previews) { i, r -> RemoteRow(r, row, onPlay = { onPlayPreview(i) }, onMenu = { onPreviewMenu(r) }) }
            }
            below?.invoke(this)
        }
    }
}

/** Shown instead of the title bar while songs are selected (long-press a song to start). */
@Composable
private fun SelectionBar(count: Int, onClose: () -> Unit, onPlay: () -> Unit, onQueue: () -> Unit, onAddTo: (() -> Unit)?, actions: List<MenuAction>) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().background(Raised).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Stop selecting", tint = TextMain) }
        Text("$count selected", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onPlay, enabled = count > 0) { Icon(Icons.Rounded.PlayArrow, "Play selected", tint = TextMain) }
        IconButton(onClick = onQueue, enabled = count > 0) { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add selected to the queue", tint = TextMain) }
        if (onAddTo != null) IconButton(onClick = onAddTo, enabled = count > 0) { Icon(Icons.Rounded.AddCircleOutline, "Add selected to a playlist", tint = TextMain) }
        Box {
            IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, "More for the selected songs", tint = TextMain) }
            DropdownMenu(open, { open = false }) {
                actions.forEach { a -> DropdownMenuItem({ Text(a.label) }, { open = false; a.run() }, leadingIcon = { Icon(a.icon, null) }) }
            }
        }
    }
}

/* ---------------- Queue ---------------- */

@Composable
private fun QueueScreen(player: PlayerState?, recent: List<Track>, onSave: () -> Unit, onBack: () -> Unit) {
    val items = remember(player?.queueVersion, player?.currentId) { player?.queue() ?: emptyList() }
    val now = items.firstOrNull { it.isCurrent }
    val queued = items.filter { it.queued && !it.isCurrent }
    val rest = items.filter { !it.queued && !it.isCurrent }
    val reorderQueued = rememberReorder()
    val reorderRest = rememberReorder()
    Column(Modifier.fillMaxSize()) {
        BackTopBar("Queue", onBack) {
            if (now != null) IconButton(onClick = onSave) { Icon(Icons.Rounded.Save, "Save the queue as a playlist", tint = TextDim) }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (now == null) item { Text("Nothing is playing.", color = TextDim, modifier = Modifier.padding(24.dp)) }
            if (now != null) {
                item { SectionLabel("Now playing") }
                item { QueueRow(now, onClick = null, onRemove = null) }
            }
            if (queued.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Next in queue", Modifier.weight(1f))
                        TextButton(onClick = { player?.clearQueue() }) { Text("Clear queue", color = Accent) }
                    }
                }
                itemsIndexed(queued, key = { _, q -> "q${q.index}" }) { i, q ->
                    QueueRow(q, onClick = { player?.jumpTo(q.index) }, onRemove = { player?.removeAt(q.index) },
                        modifier = Modifier.reorderRow(reorderQueued, i, queued.size)) {
                        DragHandle(reorderQueued, i, queued.size, q.title) { from, to -> player?.move(queued[from].index, queued[to].index) }
                    }
                }
            }
            if (rest.isNotEmpty()) {
                item { SectionLabel("Next from: ${player?.sourceLabel.orEmpty()}") }
                itemsIndexed(rest, key = { _, q -> "r${q.index}" }) { i, q ->
                    QueueRow(q, onClick = { player?.jumpTo(q.index) }, onRemove = { player?.removeAt(q.index) },
                        modifier = Modifier.reorderRow(reorderRest, i, rest.size)) {
                        DragHandle(reorderRest, i, rest.size, q.title) { from, to -> player?.move(rest[from].index, rest[to].index) }
                    }
                }
            }
            // Tapping one plays it now and keeps the rest of the queue, like on the desktop.
            val earlier = recent.filter { it.id != now?.id }
            if (earlier.isNotEmpty()) {
                item { SectionLabel("Recently played") }
                items(earlier, key = { "h" + it.id }) { t ->
                    QueueRow(QueueItem(-1, t.id, t.title, t.artist, t.artUri, queued = false, isCurrent = false),
                        onClick = { player?.playNext(t.toMediaItem()); player?.next() }, onRemove = null)
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    q: QueueItem, onClick: (() -> Unit)?, onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier, handle: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Art(q.artUri, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(q.title, color = if (q.isCurrent) Accent else TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(q.artist, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onRemove != null) IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, "Remove ${q.title} from queue", tint = TextDim) }
        handle?.invoke()
    }
}

/* ---------------- Settings (in the drawer) ---------------- */

@Composable
internal fun SettingsScreen(
    folders: List<String>,
    scanning: Boolean,
    appleOn: Boolean,
    onAppleChange: (Boolean) -> Unit,
    freeOn: Boolean,
    onFreeChange: (Boolean) -> Unit,
    jamendoId: String,
    onJamendoChange: (String) -> Unit,
    matchRunning: Boolean,
    onMatch: (onlyNew: Boolean) -> Unit,
    onViewMatch: () -> Unit,
    onRename: () -> Unit,
    onImport: (List<Uri>) -> Unit,
    library: List<Track> = emptyList(),
    onMessage: (String) -> Unit = {},
    getAllFromPc: MenuAction? = null,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val importing by SpotifyImporter.state.collectAsState()
    val pickExport = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { onImport(it) }
    var showLicenses by remember { mutableStateOf(false) }
    var keepPlaying by remember { mutableStateOf(Prefs.keepPlaying(ctx)) }
    var mobileDl by remember { mutableStateOf(Prefs.mobileDownloads(ctx)) }
    var autoplay by remember { mutableStateOf(Prefs.autoplay(ctx)) }
    var cacheMb by remember { mutableLongStateOf(PlaybackCache.get(ctx).cacheSpace shr 20) }
    var backupTree by remember { mutableStateOf(Prefs.backupTree(ctx)) }
    var backupNote by remember { mutableStateOf<String?>(null) }
    var restoreFrom by remember { mutableStateOf<Uri?>(null) }
    val bgScope = rememberCoroutineScope()
    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) bgScope.launch {
            backupNote = runCatching {
                withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri, "wt")!!.use { Backup.write(ctx, it) } }
                "Backup saved."
            }.getOrElse { "Couldn't save the backup." }
        }
    }
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { restoreFrom = it }
    val pickBackupFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            Prefs.setBackupTree(ctx, uri.toString()); Prefs.setLastBackup(ctx, 0); backupTree = uri.toString()
            bgScope.launch { withContext(Dispatchers.IO) { Backup.autoIfDue(ctx) }; backupNote = "Daily backups go to ${prettyFolder(uri.toString())}." }
        }
    }
    var releaseAlerts by remember { mutableStateOf(Prefs.releaseAlerts(ctx)) }
    fun setReleaseAlerts(on: Boolean) {
        releaseAlerts = on
        Prefs.setReleaseAlerts(ctx, on)
        if (on) ReleaseJob.schedule(ctx) else ReleaseJob.cancel(ctx)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackTopBar("Settings", onBack)
        Column(Modifier.padding(20.dp, 0.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Profile picture", Modifier.padding(0.dp))
            ProfileSettings()
            Spacer(Modifier.height(4.dp))
            SectionLabel("Music folders", Modifier.padding(0.dp))
            folders.forEach { uri ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FolderOpen, null, tint = Accent)
                    Spacer(Modifier.width(8.dp))
                    Text(prettyFolder(uri), color = TextMain, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { LibraryActions.removeFolder?.invoke(uri) }) {
                        Icon(Icons.Rounded.Delete, "Remove folder ${prettyFolder(uri)}", tint = TextDim)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { LibraryActions.addFolder?.invoke() }) { Icon(Icons.Rounded.Add, null); Text("Add folder") }
                OutlinedButton(onClick = { LibraryActions.rescan?.invoke() }, enabled = !scanning) { Text(if (scanning) "Scanning…" else "Rescan") }
            }
            Spacer(Modifier.height(4.dp))
            ServerSection()
            Spacer(Modifier.height(4.dp))
            SectionLabel("Match metadata online", Modifier.padding(0.dp))
            Text(
                "Looks your songs up on Deezer. You review each change (Keep or Replace); songs without a match keep " +
                    "their own details, and artist photos are fetched too. Your audio files are never changed.",
                color = TextDim, fontSize = 13.sp,
            )
            if (matchRunning) {
                Button(onClick = onViewMatch) { Text("Matching… view progress") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onMatch(true) }) { Text("Match new songs") }
                    OutlinedButton(onClick = { onMatch(false) }) { Text("Match all songs") }
                }
            }
            OutlinedButton(onClick = onRename) { Text("Rename files from tags") }
            Text("Names your files “Artist - Title”. You see every change first, and can undo it.",
                color = TextDim, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            SectionLabel("Import from Spotify", Modifier.padding(0.dp))
            Text("In Spotify: Account → Privacy settings → Download your data → Account data. Spotify emails you a " +
                "ZIP a few days later; pick it here. Exportify CSV files work too. Nothing is sent to Spotify.",
                color = TextDim, fontSize = 13.sp)
            when {
                importing.running -> {
                    Text(if (importing.total == 0) "Reading the export…" else "Finding songs: ${importing.done} of ${importing.total}",
                        color = TextMain, fontSize = 13.sp)
                    LinearProgressIndicator(
                        progress = { if (importing.total == 0) 0f else importing.done.toFloat() / importing.total },
                        modifier = Modifier.fillMaxWidth(), color = Accent, trackColor = Raised,
                    )
                }
                else -> {
                    importing.result?.let { Text(it, color = TextMain, fontSize = 13.sp) }
                    importing.error?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
                    OutlinedButton(onClick = { pickExport.launch(arrayOf("*/*")) }) { Text("Choose Spotify export") }
                }
            }
            SectionLabel("Downloads", Modifier.padding(0.dp))
            val dlFolder = Downloads.targetFolder(ctx)
            Text(
                if (dlFolder != null) "Saved as music files in “${Downloads.FOLDER}” inside ${prettyFolder(dlFolder)}, so they're part of " +
                    "your library and play offline. Songs from your music server, and free songs whose artist allows it."
                else "Add a music folder first: downloads are saved as files there.",
                color = TextDim, fontSize = 13.sp,
            )
            val dl by Downloads.state.collectAsState()
            if (dl.running) {
                Text("Downloading ${minOf(dl.done + 1, dl.total)} of ${dl.total}: ${dl.current.orEmpty()}", color = TextMain, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                LinearProgressIndicator(progress = { dl.done.toFloat() / dl.total.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth(), color = Accent, trackColor = Raised)
            }
            Row(Modifier.fillMaxWidth().clickable { mobileDl = !mobileDl; Prefs.setMobileDownloads(ctx, mobileDl) },
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Download on mobile data", color = TextMain)
                    Text("Off: downloads only start on Wi-Fi.", color = TextDim, fontSize = 12.sp)
                }
                Switch(checked = mobileDl, onCheckedChange = { mobileDl = it; Prefs.setMobileDownloads(ctx, it) })
            }
            SectionLabel("Equalizer", Modifier.padding(0.dp))
            CrossfadeSetting(ctx)
            EqualizerSettings(ctx)
            SectionLabel("Your PC", Modifier.padding(0.dp))
            PcSettings(onMessage, getAllFromPc)
            SectionLabel("Loudness", Modifier.padding(0.dp))
            LoudnessSettings(ctx, library)
            SectionLabel("Display", Modifier.padding(0.dp))
            Text("Colour", color = TextMain)
            AccentPicker(ctx)
            val hiddenNow by UserCollection.hidden.collectAsState()
            val hiddenCount = hiddenNow.count(UserCollection::isLibraryKey)
            if (hiddenCount > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${songCount(hiddenCount)} hidden from your library", color = TextMain, modifier = Modifier.weight(1f))
                TextButton(onClick = { UserCollection.showHiddenSongs() }) { Text("Show them again") }
            }
            SectionLabel("New releases", Modifier.padding(0.dp))
            Row(Modifier.fillMaxWidth().clickable { setReleaseAlerts(!releaseAlerts) }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Notify me about new releases", color = TextMain)
                    Text("From the artists you listen to most. Checked twice a day; only artist ids go to Deezer.",
                        color = TextDim, fontSize = 12.sp)
                }
                Switch(checked = releaseAlerts, onCheckedChange = { setReleaseAlerts(it) })
            }
            Spacer(Modifier.height(4.dp))
            SectionLabel("Playback", Modifier.padding(0.dp))
            Row(
                Modifier.fillMaxWidth().clickable { keepPlaying = !keepPlaying; Prefs.setKeepPlaying(ctx, keepPlaying) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Keep playing after closing Onda", color = TextMain)
                    Text("Off: swiping Onda away from Recents pauses it and ends background playback.",
                        color = TextDim, fontSize = 12.sp)
                }
                Switch(checked = keepPlaying, onCheckedChange = { keepPlaying = it; Prefs.setKeepPlaying(ctx, it) })
            }
            Row(Modifier.fillMaxWidth().clickable { autoplay = !autoplay; Prefs.setAutoplay(ctx, autoplay) }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Similar songs when the queue ends", color = TextMain)
                    Text("Keeps the music going with songs like the last ones (yours in full, others as previews or free full copies).",
                        color = TextDim, fontSize = 12.sp)
                }
                Switch(checked = autoplay, onCheckedChange = { autoplay = it; Prefs.setAutoplay(ctx, it) })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Playback cache: $cacheMb MB of 1,024 MB", color = TextMain)
                    Text("Streamed songs are kept so replays and seeking use no data.", color = TextDim, fontSize = 12.sp)
                }
                TextButton(onClick = { PlaybackCache.clear(ctx); cacheMb = PlaybackCache.get(ctx).cacheSpace shr 20 }) { Text("Clear", color = Accent) }
            }
            Spacer(Modifier.height(4.dp))
            SectionLabel("Backup", Modifier.padding(0.dp))
            Text("Playlists, likes, history, matches, followed artists, saved albums and settings in one file. " +
                "Not your server login or music folders.", color = TextDim, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // With a daily-backup folder: straight there, no picker ("Save as…" picks another place).
                OutlinedButton(onClick = {
                    val tree = backupTree
                    if (tree == null) saveBackup.launch(Backup.name())
                    else bgScope.launch {
                        backupNote = if (withContext(Dispatchers.IO) { Backup.toFolder(ctx) }) "Saved to ${prettyFolder(tree)}." else "Couldn't save the backup there."
                    }
                }) { Text("Back up now") }
                if (backupTree != null) TextButton(onClick = { saveBackup.launch(Backup.name()) }) { Text("Save as…", color = Accent) }
                OutlinedButton(onClick = { pickBackup.launch(arrayOf("application/zip", "application/octet-stream")) }) { Text("Restore") }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(backupTree?.let { "Daily backup: ${prettyFolder(it)} (newest 5 kept)" } ?: "Daily backup: off",
                    color = TextMain, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { pickBackupFolder.launch(null) }) { Text(if (backupTree == null) "Choose folder" else "Change", color = Accent) }
                if (backupTree != null) TextButton(onClick = { Prefs.setBackupTree(ctx, null); backupTree = null }) { Text("Off", color = TextDim) }
            }
            backupNote?.let { Text(it, color = TextMain, fontSize = 13.sp) }
            CopySongsSetting()
            SectionLabel("Privacy", Modifier.padding(0.dp))
            Text(
                "Search, previews and cover art come from Deezer's public API without an account. " +
                    "Deezer sees your search words and IP address, nothing else. Free songs stream straight " +
                    "from the catalog they come from. Revoke Onda's Network " +
                    "permission and it keeps working offline with your own files.",
                color = TextDim, fontSize = 13.sp,
            )
            Row(Modifier.fillMaxWidth().clickable { onAppleChange(!appleOn) }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Also search Apple's catalog", color = TextMain)
                    Text("Finds songs Deezer is missing. Apple then also sees your search words and IP address.",
                        color = TextDim, fontSize = 12.sp)
                }
                Switch(checked = appleOn, onCheckedChange = onAppleChange)
            }
            Row(Modifier.fillMaxWidth().clickable { onFreeChange(!freeOn) }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Free full songs in search", color = TextMain)
                    Text("Adds whole songs from Audius and free albums from the Internet Archive, and plays a free full " +
                        "copy instead of a Deezer preview when the artist put one there. They then also see your search " +
                        "words and IP address.", color = TextDim, fontSize = 12.sp)
                }
                Switch(checked = freeOn, onCheckedChange = onFreeChange)
            }
            var lbOn by remember { mutableStateOf(Prefs.listenBrainz(ctx)) }
            Row(Modifier.fillMaxWidth().clickable { lbOn = !lbOn; Prefs.setListenBrainz(ctx, lbOn) }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Similar artists from ListenBrainz", color = TextMain)
                    Text("Better mixes and autoplay. ListenBrainz and MusicBrainz then see artist names (no songs, no history) and your IP address.",
                        color = TextDim, fontSize = 12.sp)
                }
                Switch(checked = lbOn, onCheckedChange = { lbOn = it; Prefs.setListenBrainz(ctx, it) })
            }
            OutlinedTextField(
                value = jamendoId,
                onValueChange = onJamendoChange,
                label = { Text("Jamendo client ID (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Adds Jamendo's free Creative Commons songs. Get a free client ID at devportal.jamendo.com " +
                "(needs a Jamendo account). Leave empty to keep Jamendo off.", color = TextDim, fontSize = 12.sp)
            OutlinedButton(onClick = { openAppInfo(ctx) }) { Text("Network permission (Android settings)") }
            Text("Opens Onda's app info. Go to Permissions → Network.", color = TextDim, fontSize = 12.sp)
            TextButton(onClick = { showLicenses = true }) { Text("Open-source licenses", color = Accent) }
            Text("Onda ${BuildConfig.VERSION_NAME}. No account, no tracking, no Google code.", color = TextDim, fontSize = 12.sp)
        }
    }
    restoreFrom?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreFrom = null },
            confirmButton = {
                TextButton(onClick = {
                    restoreFrom = null
                    bgScope.launch {
                        val n = runCatching { withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)!!.use { Backup.restore(ctx, it) } } }
                        if (n.isSuccess) (ctx as? android.app.Activity)?.recreate()
                        else backupNote = "That file isn't an Onda backup."
                    }
                }) { Text("Restore", color = ErrorRed) }
            },
            dismissButton = { TextButton(onClick = { restoreFrom = null }) { Text("Cancel", color = TextDim) } },
            title = { Text("Restore this backup?") },
            text = { Text("Your playlists, likes, history and settings are replaced by the backup's. A copy of the current ones is kept.", color = TextDim) },
            containerColor = CardBg,
        )
    }
    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("Close") } },
            title = { Text("Licenses") },
            text = {
                Text(
                    "Onda is licensed under GPL-3.0.\n\n" +
                        "Uses: AndroidX / Jetpack Compose & Media3 (Apache-2.0), Coil and OkHttp (Apache-2.0), " +
                        "Kotlin & coroutines (Apache-2.0). Full license texts ship with each library. AI artist list: " +
                        "CennoxX/spotify-ai-blocker (MIT).",
                    color = TextDim,
                )
            },
            containerColor = CardBg,
        )
    }
}

@Composable
private fun DrawerSheet(onSettings: () -> Unit, onStats: () -> Unit, onHistory: () -> Unit, onDuplicates: () -> Unit) {
    ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.55f), drawerContainerColor = CardBg) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                ProfilePic(40.dp)
                Spacer(Modifier.width(12.dp))
                Text("Onda", color = Accent, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            listOf(Triple("Settings", Icons.Rounded.Settings, onSettings), Triple("Your stats", Icons.Rounded.BarChart, onStats),
                Triple("History", Icons.Rounded.History, onHistory), Triple("Duplicates", Icons.Rounded.ContentCopy, onDuplicates)).forEach { (label, icon, go) ->
                Row(Modifier.fillMaxWidth().clickable(onClick = go).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = TextMain)
                    Spacer(Modifier.width(12.dp))
                    Text(label, color = TextMain, fontSize = 16.sp)
                }
            }
        }
    }
}

/* ---------------- shared pieces ---------------- */

@Composable
private fun TopBar(title: String, onAvatar: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(onAvatar)
        Spacer(Modifier.width(12.dp))
        Text(title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

@Composable
internal fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TextMain) }
        Text(title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
private fun Avatar(onClick: () -> Unit) {
    ProfilePic(34.dp, Modifier.clip(CircleShape).clickable(onClickLabel = "Open menu", onClick = onClick))
}

@Composable
private fun NavBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = CardBg) {
        listOf(
            Triple("Home", Icons.Rounded.Home, 0),
            Triple("Search", Icons.Rounded.Search, 1),
            Triple("Your Library", Icons.Rounded.LibraryMusic, 2),
        ).forEach { (label, icon, i) ->
            NavigationBarItem(
                selected = selected == i, onClick = { onSelect(i) },
                icon = { Icon(icon, null) }, label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Accent, selectedTextColor = Accent, indicatorColor = Raised,
                    unselectedIconColor = TextDim, unselectedTextColor = TextDim,
                ),
            )
        }
    }
}

@Composable
internal fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(if (selected) Accent else Raised).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) { Text(label, color = if (selected) Color(0xFF141018) else TextDim, fontSize = 13.sp) }
}

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)) {
    Text(text, color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

/**
 * A song row. Dragging it right past 40% and letting go adds it to the queue; left, to a playlist ([onAddTo]). Exactly once.
 * A song that's only on your PC can't be queued, but can go into a playlist.
 */
@Composable
internal fun SwipeTrackRow(
    t: Track, playing: Boolean, onQueue: () -> Unit, onMenu: () -> Unit,
    selected: Boolean = false, onLongClick: (() -> Unit)? = null, missing: Boolean = false,
    onAddTo: (() -> Unit)? = null, onClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val queue by rememberUpdatedState(onQueue)
    val add by rememberUpdatedState(onAddTo)
    var dx by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableFloatStateOf(1f) }
    val armed = abs(dx) > width * SWIPE_FRACTION
    Box(Modifier.fillMaxWidth().onSizeChanged { width = it.width.toFloat() }) {
        if (dx != 0f) {
            val toQueue = dx > 0f
            Row(Modifier.matchParentSize().background(Raised).padding(horizontal = 20.dp),
                horizontalArrangement = if (toQueue) Arrangement.Start else Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                val tint = if (armed) Accent else TextDim
                Icon(if (toQueue) Icons.AutoMirrored.Rounded.PlaylistAdd else Icons.Rounded.AddCircleOutline, null, tint = tint)
                Spacer(Modifier.width(8.dp))
                Text(if (toQueue) "Add to queue" else "Add to playlist", color = tint, fontSize = 13.sp)
            }
        }
        Row(
            Modifier.fillMaxWidth()
                .offset { IntOffset(dx.roundToInt(), 0) }
                .background(Bg)
                .pointerInput(missing, onAddTo != null) {
                    if (missing && onAddTo == null) return@pointerInput // nothing to swipe to
                    val most = { size.width.toFloat() }
                    var wasArmed = false
                    val back: () -> Unit = { wasArmed = false; scope.launch { animate(dx, 0f) { v, _ -> dx = v } } }
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Decided once, on release.
                            if (dx > most() * SWIPE_FRACTION) queue() else if (dx < -most() * SWIPE_FRACTION) add?.invoke()
                            back()
                        },
                        onDragCancel = back,
                    ) { change, amount ->
                        change.consume()
                        dx = (dx + amount).coerceIn(if (onAddTo != null) -most() else 0f, if (missing) 0f else most())
                        val nowArmed = abs(dx) > most() * SWIPE_FRACTION
                        if (nowArmed && !wasArmed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        wasArmed = nowArmed
                    }
                }
                .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = onLongClick?.let { "Select" },
                    onClickLabel = if (missing) "Not on this phone" else null)
                .then(if (selected) Modifier.background(Accent.copy(alpha = 0.18f)) else Modifier)
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A song only on your PC: greyed out; its ⋮ menu still works.
            Row(Modifier.weight(1f).alpha(if (missing) 0.4f else 1f), verticalAlignment = Alignment.CenterVertically) {
                if (selected) IconTile(Icons.Rounded.Check, 48.dp) else Art(t.artUri, 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, color = if (playing) Accent else TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (missing) "${t.artist} · not on this phone" else t.artist, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onMenu) { Icon(Icons.Rounded.MoreVert, "More options for ${t.title}", tint = TextDim) }
        }
    }
}

@Composable
private fun GroupRow(
    title: String, subtitle: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null,
    gestures: Modifier = Modifier, // inside the click, e.g. holdToMove
    leading: @Composable () -> Unit,
) {
    Row(modifier.fillMaxWidth().clickable(onClick = onClick).then(gestures).padding(16.dp, 8.dp, if (trailing != null) 4.dp else 16.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        leading()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

@Composable
internal fun Art(uri: String?, side: Dp, modifier: Modifier = Modifier, round: Boolean = false) {
    val shape = if (round) CircleShape else RoundedCornerShape(if (side in 1.dp..56.dp) 4.dp else 8.dp)
    val sized = if (side > 0.dp) modifier.size(side) else modifier
    Box(sized.clip(shape).background(Raised), contentAlignment = Alignment.Center) {
        if (uri != null) {
            AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(if (round) Icons.Rounded.Person else Icons.Rounded.MusicNote, null, tint = Accent) // round = an artist
        }
    }
}

@Composable
internal fun IconTile(icon: ImageVector, side: Dp, bg: Color = Accent, fg: Color = Color(0xFF141018)) {
    Box(Modifier.size(side).clip(RoundedCornerShape(4.dp)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(side * 0.42f))
    }
}

/* ---------------- sheets / dialogs ---------------- */

@Composable
internal fun SongMenu(title: String, artist: String, art: String?, actions: List<MenuAction>, onDismiss: () -> Unit) = MenuSheet(onDismiss) {
    Row(Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Art(art, 48.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    HorizontalDivider(color = Raised)
    actions.forEach { a -> MenuRow(a.label, a.icon) { onDismiss(); a.run() } }
}

/**
 * A menu sheet. Material's sheet springs past its top and back when a swipe up is let go on it (the whole menu jumps), so
 * its handle is drawn in the list here, and a swipe up that's left over at the end of the list stops there. Swipes down
 * still reach the sheet, to close it.
 */
@Composable
private fun MenuSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardBg, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), dragHandle = null) {
        Column(Modifier.fillMaxWidth().nestedScroll(KeepUpFlings).verticalScroll(rememberScrollState(), overscrollEffect = null).padding(bottom = 24.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(vertical = 22.dp).size(32.dp, 4.dp).clip(RoundedCornerShape(2.dp))
                .background(TextDim.copy(alpha = 0.4f)).testTag("sheet-handle"))
            content()
        }
    }
}

/**
 * A swipe up that's left over at the end of a sheet's list stops here. Given to Material's sheet, it springs to where it
 * already is, overshoots upward and settles back: the whole menu jumps. Swipes down still reach it (to close it).
 */
private val KeepUpFlings = object : NestedScrollConnection {
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = if (available.y < 0) available else Velocity.Zero
}

@Composable
private fun MenuRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp, 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextMain)
        Spacer(Modifier.width(16.dp))
        Text(label, color = TextMain)
    }
}

@Composable
private fun AddToPlaylistSheet(
    playlists: List<PlaylistStore.Playlist>,
    onDismiss: () -> Unit,
    onPick: (PlaylistStore.Playlist) -> Unit,
    onCreate: () -> Unit,
) {
    MenuSheet(onDismiss) {
        Text("Add to playlist", color = TextMain, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp))
        MenuRow("New playlist", Icons.Rounded.Add, onCreate)
        HorizontalDivider(color = Raised)
        playlists.forEach { pl -> MenuRow(pl.name, Icons.Rounded.MusicNote) { onPick(pl) } }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit, onCreate: (String) -> Unit,
    heading: String = "New playlist", initial: String = "", confirm: String = "Create",
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onCreate(name.trim()) }) { Text(confirm, color = Accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextDim) } },
        title = { Text(heading) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, placeholder = { Text("Name") }) },
        containerColor = CardBg,
    )
}

internal fun songCount(n: Int): String = "$n song${if (n == 1) "" else "s"}"

internal fun prettyFolder(treeUri: String): String =
    treeUri.substringAfterLast("%3A").substringAfterLast("%2F").ifBlank { treeUri }
        .let { java.net.URLDecoder.decode(it, "UTF-8") }

/** Android's own "App info" page for Onda, where GrapheneOS keeps the Network toggle. */
private fun openAppInfo(ctx: Context) {
    ctx.startActivity(
        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", ctx.packageName, null)),
    )
}

/** Android 13 and newer show their own "Copied" note; older ones get Onda's. */
private fun copyText(ctx: Context, label: String, text: String) {
    ctx.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
    if (android.os.Build.VERSION.SDK_INT < 33) android.widget.Toast.makeText(ctx, "$label copied", android.widget.Toast.LENGTH_SHORT).show()
}
