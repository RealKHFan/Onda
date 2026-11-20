package io.github.realkhfan.onda.desktop

import io.github.realkhfan.onda.core.LinkSync

import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Computer

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import io.github.realkhfan.onda.core.NewReleases
import io.github.realkhfan.onda.core.Discovery
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.DefaultContextMenuRepresentation
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.realkhfan.onda.core.Album
import io.github.realkhfan.onda.core.ArtistGroup
import io.github.realkhfan.onda.core.AutoTree
import io.github.realkhfan.onda.core.Grouping
import io.github.realkhfan.onda.core.Lrc
import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.core.RemoteAlbum
import io.github.realkhfan.onda.core.RemoteArtist
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.Deezer
import io.github.realkhfan.onda.data.FreeCatalogs
import io.github.realkhfan.onda.data.Lrclib
import io.github.realkhfan.onda.data.Itunes
import io.github.realkhfan.onda.data.Matcher
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Slider
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.onClick
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import io.github.realkhfan.onda.data.Overrides
import androidx.compose.material.icons.rounded.Download
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import io.github.realkhfan.onda.core.Confidence
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import io.github.realkhfan.onda.data.Subsonic
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

val Bg = Color(0xFF0F0F12)
val Side = Color(0xFF09090B)
val Raised = Color(0xFF1C1C22)
/** The highlight colour (violet unless you pick another in Settings). */
internal val accentState = androidx.compose.runtime.mutableStateOf(Color(0xFF9D7BFF))
val Accent: Color get() = accentState.value

/** The highlight colours to choose from. */
internal val accents = listOf("Violet" to 0xFF9D7BFF, "Blue" to 0xFF5B9DFF, "Teal" to 0xFF2EC4B6, "Green" to 0xFF4ADE80, "Yellow" to 0xFFFACC15,
    "Orange" to 0xFFFF9248, "Pink" to 0xFFF472B6, "Red" to 0xFFF87171)
val TextMain = Color(0xFFF2F2F5)
val TextDim = Color(0xFFA7A7B0)
private val ErrorRed = Color(0xFFFF6B6B)

/** What the main area shows; detail views stack up so Back returns. */
private sealed interface View {
    data object Home : View
    data object Songs : View
    data object Albums : View
    data object Artists : View
    data object Search : View
    data object Settings : View
    data object Liked : View
    data object Want : View
    data class Playlist(val id: String) : View
    data class ServerPlaylist(val p: Subsonic.Playlist) : View
    data class LocalList(val title: String, val subtitle: String, val art: String?, val tracks: List<Track>) : View
    data class CatalogArtist(val a: RemoteArtist) : View
    data class CatalogAlbum(val a: RemoteAlbum) : View
    data class Mix(val kind: Discovery.Kind) : View
    data object Match : View
    data object Rename : View
    data object Stats : View
    data object ReleaseRadar : View
    data class ArtistMix(val name: String) : View
    data object Smart : View
    data class SmartList(val kind: AppState.Smart) : View
    data object NowPlaying : View
    data object History : View
    data object Duplicates : View
    /** null: your music folders. */
    data class Folder(val path: String?) : View
    data class LibraryArtist(val name: String) : View
}

private enum class Panel { NONE, QUEUE, LYRICS }

@Composable
fun OndaDesktop(state: AppState, player: Player?) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Raised, onPrimary = Color.Black)) {
      CompositionLocalProvider(LocalContextMenuRepresentation provides DefaultContextMenuRepresentation(Raised, TextMain, Color(0xFF2E2E38))) {
        val stack = remember { mutableStateListOf<View>(View.Home) }
        // Each page keeps its scroll while others are open on top of it; a page that's closed starts fresh next time.
        val pages = rememberSaveableStateHolder()
        fun go(v: View) { stack.indices.forEach { pages.removeState("page$it") }; stack.clear(); stack.add(v) }
        fun push(v: View) { stack.add(v) }
        fun pop() { if (stack.size > 1) { pages.removeState("page${stack.lastIndex}"); stack.removeAt(stack.lastIndex) } }
        val back: (() -> Unit)? = if (stack.size > 1) ({ pop() }) else null
        var quick by remember { mutableStateOf(false) } // Ctrl+K
        var shortcuts by remember { mutableStateOf(false) } // F1
        LaunchedEffect(state.navRequest) {
            when (state.navRequest) {
                "search" -> go(View.Search)
                "settings" -> go(View.Settings)
                "quick" -> quick = true
                "keys" -> shortcuts = true
                "back" -> pop()
            }
            state.navRequest = null
        }
        var panel by remember { mutableStateOf(Panel.NONE) }
        var lyricsBig by remember { mutableStateOf(false) }
        var naming by remember { mutableStateOf<Naming?>(null) }
        val scope = rememberCoroutineScope()
        var toast by remember { mutableStateOf<String?>(null) }
        var undo by remember { mutableStateOf<(() -> Unit)?>(null) }
        LaunchedEffect(toast) { if (toast != null) { delay(if (undo != null) 6000 else 3500); toast = null; undo = null } }
        val ctx = Ctx(state, player, ::push, { naming = it }, { toast = it; undo = null }, { m, u -> toast = m; undo = u }) { name ->
            // The artist's full page; just your songs when the artist can't be found online.
            scope.launch {
                val found = runCatching { Deezer.searchArtists(name).firstOrNull { Match.norm(it.name) == Match.norm(name) } }.getOrNull()
                when {
                    found != null -> push(View.CatalogArtist(found))
                    Grouping.artists(state.library).any { Match.norm(it.name) == Match.norm(name) } -> push(View.LibraryArtist(name))
                    else -> { toast = "Couldn't find $name online"; undo = null }
                }
            }
        }
        // Folders dropped on the window (from Dolphin, say) become music folders.
        val drop = remember(state) {
            object : DragAndDropTarget {
                @OptIn(ExperimentalComposeUiApi::class)
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val files = (event.dragData() as? DragData.FilesList)?.readFiles().orEmpty()
                    val all = files.mapNotNull { runCatching { File(java.net.URI(it)) }.getOrNull() }
                    val dirs = all.filter { it.isDirectory }
                    dirs.forEach { state.addFolder(it.absolutePath) }
                    // Songs dropped on the window play right away (folders become music folders).
                    val songs = state.itemsForFiles(all.filter(Scanner::isAudio).map { it.toURI().toString() })
                    if (songs.isNotEmpty()) player?.play(songs, 0)
                    toast = when {
                        songs.isNotEmpty() && dirs.isNotEmpty() -> "Playing ${songs.size} songs · added ${dirs.joinToString { it.name }} to your music folders"
                        songs.isNotEmpty() -> "Playing ${songs.size} dropped " + if (songs.size == 1) "song" else "songs"
                        dirs.isNotEmpty() -> "Added ${dirs.joinToString { it.name }} to your music folders"
                        else -> "Drop songs to play them, or a folder to add it to your music folders."
                    }
                    return songs.isNotEmpty() || dirs.isNotEmpty()
                }
            }
        }
        Column(Modifier.fillMaxSize().background(Bg).dragAndDropTarget(shouldStartDragAndDrop = { true }, target = drop)) {
            Row(Modifier.weight(1f)) {
                Sidebar(state, stack.first(), ::go, ctx.dialog, ctx.undoable) { naming = Naming.New() }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    if (player == null) Note("Onda needs GStreamer to play music. On Bazzite it's already there; on other systems install gstreamer1 and its plugins.")
                    if (player != null && panel == Panel.LYRICS && lyricsBig) {
                        LyricsFull(state, player, onCollapse = { lyricsBig = false }) { panel = Panel.NONE; lyricsBig = false }
                    } else pages.SaveableStateProvider("page${stack.lastIndex}") { when (val v = stack.last()) {
                        View.Home -> HomePage(ctx, ::push)
                        View.Songs -> Songs(ctx)
                        View.Albums -> Albums(state, { a -> push(View.LocalList(a.title, "Album · ${a.artist}", a.artUri, a.tracks)) }) { push(View.CatalogAlbum(it)) }
                        View.Artists -> Artists(state, { a -> push(View.LibraryArtist(a.name)) }) { push(View.CatalogArtist(it)) }
                        is View.Mix -> MixPage(ctx, v.kind, back)
                        View.Stats -> StatsPage(ctx)
                        View.ReleaseRadar -> RadarPage(ctx, back)
                        is View.ArtistMix -> ArtistMixPage(ctx, v.name, back)
                        View.Smart -> SmartPage(ctx)
                        is View.SmartList -> SongList(ctx, v.kind.title, v.kind.about, state.smart(v.kind), back = back, mosaic = true)
                        View.NowPlaying -> NowPlayingPage(ctx, back)
                        View.History -> HistoryPage(ctx)
                        View.Duplicates -> DuplicatesPage(ctx, back)
                        is View.Folder -> FolderPage(ctx, v.path, back)
                        is View.LibraryArtist -> LibraryArtistPage(ctx, v.name, back)
                        View.Search -> Search(ctx)
                        View.Settings -> SettingsPage(ctx)
                        View.Match -> MatchPage(ctx, back)
                        View.Rename -> RenamePage(ctx, back)
                        View.Liked -> SongList(ctx, "Liked Songs", "Playlist", state.likedTracks, back = back) { GetFromPhone(ctx, state.likedTracks) }
                        View.Want -> WantPage(ctx, back)
                        is View.Playlist -> {
                            val pl = state.collection.playlists.firstOrNull { it.id == v.id }
                            if (pl == null) LaunchedEffect(Unit) { go(View.Songs) } else {
                                SongList(ctx, pl.name, pl.description.ifBlank { "Playlist" }, state.playlistTracks(pl.id), art = pl.cover, back = back, inPlaylist = pl.id,
                                    mosaic = pl.cover == null, onMove = { from, to -> state.movePlaylistTrack(pl.id, from, to) },
                                    below = { recommendations(ctx, pl.id) }) {
                                    IconButton(onClick = { state.togglePin(pl.id) }) { Icon(Icons.Rounded.PushPin, if (pl.id in state.settings.pinned) "Unpin" else "Pin to the top", tint = if (pl.id in state.settings.pinned) Accent else TextDim) }
                                    Tip("Duplicate playlist") { IconButton(onClick = { state.duplicatePlaylist(pl.id)?.let { push(View.Playlist(it)); toast = "Made “${pl.name} (copy)”"; undo = null } }) { Icon(Icons.Rounded.ContentCopy, "Duplicate playlist", tint = TextDim) } }
                                    Tip("Edit playlist") { IconButton(onClick = { naming = Naming.EditPlaylist(pl.id) }) { Icon(Icons.Rounded.Edit, "Edit playlist", tint = TextDim) } }
                                    Tip("Delete playlist") { IconButton(onClick = { naming = Naming.Delete(pl.id, pl.name) }) { Icon(Icons.Rounded.Delete, "Delete playlist", tint = TextDim) } }
                                    DownloadAll(ctx, state.playlistTracks(pl.id))
                                    GetFromPhone(ctx, state.playlistTracks(pl.id))
                                    Tip("Save as playlist file (M3U)") { IconButton(onClick = { ctx.exportM3u(pl.name, state.playlistTracks(pl.id)) }) { Icon(Icons.Rounded.SaveAlt, "Save as playlist file (M3U)", tint = TextDim) } }
                                }
                            }
                        }
                        is View.ServerPlaylist -> SongList(ctx, v.p.name, "Playlist · Music server", state.serverPlaylistTracks(v.p), back = back) {
                            DownloadAll(ctx, state.serverPlaylistTracks(v.p))
                        }
                        is View.LocalList -> {
                            val tracks = v.tracks.map { state.byId[it.id] ?: it } // edited details show at once
                            SongList(ctx, v.title, v.subtitle, tracks, art = v.art, back = back) {
                                Tip("Edit details of all songs") { IconButton(onClick = { naming = Naming.EditMany(tracks.filterNot(state::isPreviewRow)) }) { Icon(Icons.Rounded.Edit, "Edit details of all songs", tint = TextDim) } }
                            }
                        }
                        is View.CatalogArtist -> CatalogArtistPage(ctx, v.a, back)
                        is View.CatalogAlbum -> CatalogAlbumPage(ctx, v.a, back)
                    } }
                }
                val phone = Link.activePhone()
                if (phone != null && panel == Panel.QUEUE) PhoneQueuePanel(phone) { panel = Panel.NONE }
                else if (player != null && panel == Panel.QUEUE) QueuePanel(player, recent = state.recent.filter { r -> r.id != player.current?.id }.take(5), onClear = {
                    val gone = player.upcoming().map { it.second }
                    player.clearUpcoming()
                    ctx.undoable("Queue cleared") { player.append(gone) }
                }, onSave = { naming = Naming.New(null, player.queue.map { it.id }.filter(state::canSave).distinct()) }) { panel = Panel.NONE }
                if (player != null && panel == Panel.LYRICS && !lyricsBig) LyricsPanel(state, player, onExpand = { lyricsBig = true }) { panel = Panel.NONE }
            }
            if (quick) QuickSearch(ctx) { quick = false }
            if (shortcuts) ShortcutsDialog { shortcuts = false }
            toast?.let { m ->
                Row(Modifier.fillMaxWidth().background(Color(0xFF2E2E38)).padding(24.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(m, color = TextMain, modifier = Modifier.weight(1f).padding(vertical = 6.dp))
                    undo?.let { u -> TextButton(onClick = { u(); toast = null; undo = null }) { Text("Undo", color = Accent, fontWeight = FontWeight.Bold) } }
                }
            }
            PlayerBar(ctx, panel) { panel = if (panel == it) Panel.NONE else it }
        }
        naming?.let { n -> NamingDialog(ctx, n) { naming = null } }
      }
    }
}

/* ---------- Shared bits ---------- */

/** What rows and pages need: state, player, navigation and dialogs. */
private class Ctx(
    val state: AppState,
    val player: Player?,
    val push: (View) -> Unit,
    val dialog: (Naming) -> Unit,
    /** A short message above the player bar. */
    val toast: (String) -> Unit,
    /** The same, with an Undo button for a few seconds. */
    val undoable: (String, () -> Unit) -> Unit,
    val openArtistByName: (String) -> Unit,
) {
    fun removeFromPlaylist(id: String, ids: List<String>) {
        val before = state.collection.playlists.firstOrNull { it.id == id }?.trackIds ?: return
        ids.forEach { state.removeFromPlaylist(id, it) }
        undoable(if (ids.size == 1) "Removed from the playlist" else "${ids.size} songs removed") { state.setPlaylistTracks(id, before) }
    }
    /** Songs like an artist's (for a song that isn't on this PC). */
    fun artistRadio(artist: String) {
        val p = player ?: return
        toast("Radio: finding songs like $artist…")
        state.launch {
            val more = state.radio(artist, "")
            if (more.isEmpty()) toast("Couldn't find similar songs right now.") else p.play(more, 0)
        }
    }
    /** Plays the song now and fills the queue with similar songs as soon as they're found. */
    fun radio(first: Player.Item, artist: String) {
        val p = player ?: return
        p.play(listOf(first), 0)
        toast("Song radio: finding similar songs…")
        state.launch {
            val more = state.radio(artist, first.id)
            if (more.isEmpty()) toast("Couldn't find similar songs right now.")
            else if (p.queue.firstOrNull()?.id == first.id && p.queue.size == 1) { p.append(more); toast("Song radio: ${more.size} songs added") }
        }
    }

    fun download(items: List<Downloads.Item>) = toast(Downloads.start(state, items) ?: "Downloading to “${Downloads.FOLDER}” in your music folder")
    /** Saves a playlist file other players open too. */
    fun exportM3u(name: String, tracks: List<Track>) {
        state.launch {
            val safe = name.replace(Regex("""[/\\:*?"<>|]"""), "_").ifBlank { "playlist" }
            when (val r = FolderPicker.file(true, System.getProperty("user.home") + "/Music/$safe.m3u8", "Save playlist file")) {
                is FolderPicker.Result.Picked -> {
                    val f = java.io.File(r.path).let { if (it.extension.lowercase() in setOf("m3u", "m3u8")) it else java.io.File(it.path + ".m3u8") }
                    val (files, other) = withContext(kotlinx.coroutines.Dispatchers.IO) { M3u.write(name, tracks, f) }
                    toast("Saved ${f.name}: $files songs" + if (other > 0) " ($other aren't files on this PC and are listed by name only)" else "")
                }
                FolderPicker.Result.NoPicker -> toast("No file picker found on this system.")
                FolderPicker.Result.Cancelled -> {}
                is FolderPicker.Result.Remote -> toast(r.message)
            }
        }
    }

    fun copy(text: String, what: String) {
        java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(text), null)
        toast("$what copied")
    }
    /** Songs that aren't on this PC (only on your phone) are skipped. */
    fun play(list: List<Track>, i: Int) {
        val here = list.filterNot { LinkSync.isMissing(it.id) }
        if (here.isNotEmpty()) player?.play(here.map(state::item), here.indexOf(list.getOrNull(i)).coerceAtLeast(0))
    }
    fun playRemote(list: List<RemoteTrack>, i: Int) {
        val items = list.mapNotNull(state::item)
        if (items.isEmpty()) return
        player?.play(items, items.indexOfFirst { it.title == list[i].title && it.artist == list[i].artist }.coerceAtLeast(0))
    }
}

private sealed interface Naming {
    data class New(val first: String? = null, val more: List<String> = emptyList()) : Naming
    data class Rename(val id: String, val name: String) : Naming
    /** A new playlist folder ([move]: a playlist that goes into it). */
    data class NewFolder(val move: String? = null) : Naming
    data class RenameFolder(val id: String, val name: String) : Naming
    data class Delete(val id: String, val name: String) : Naming
    data class Edit(val t: Track) : Naming
    data class Info(val t: Track) : Naming
    data class EditPlaylist(val id: String) : Naming
    /** The same artist or album for every song of a list (an album, an artist). */
    data class EditMany(val tracks: List<Track>) : Naming
}

@Composable
private fun NamingDialog(ctx: Ctx, n: Naming, close: () -> Unit) {
    val state = ctx.state
    if (n is Naming.Delete) {
        AlertDialog(onDismissRequest = close, title = { Text("Delete “${n.name}”?") },
            text = { Text("Only the playlist goes; your songs stay.", color = TextDim) },
            confirmButton = {
                TextButton(onClick = {
                    val pl = state.collection.playlists.firstOrNull { it.id == n.id }
                    val remotes = state.collection.remote.filter { r -> pl?.trackIds?.contains(r.key) == true }
                    state.deletePlaylist(n.id); close()
                    if (pl != null) ctx.undoable("Deleted “${pl.name}”") { state.restorePlaylist(pl, remotes) }
                }) { Text("Delete", color = ErrorRed) }
            },
            dismissButton = { TextButton(onClick = close) { Text("Cancel", color = TextDim) } })
        return
    }
    if (n is Naming.Edit) return EditDialog(state, n.t, close)
    if (n is Naming.Info) return InfoDialog(state, n.t, close)
    if (n is Naming.EditPlaylist) return PlaylistDialog(state, n.id, close)
    if (n is Naming.EditMany) {
        var artist by remember { mutableStateOf("") }
        var album by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = close, title = { Text("Edit ${n.tracks.size} songs") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextBox(artist, { artist = it }, label = { Text("Artist for all") }, placeholder = { Text("Leave empty to keep each song's") })
                    TextBox(album, { album = it }, label = { Text("Album for all") }, placeholder = { Text("Leave empty to keep each song's") })
                    Text("Titles stay. Shown in Onda; the files aren't changed.", color = TextDim, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { n.tracks.forEach { Overrides.edit(it, it.title, artist, album) }; close() }) { Text("Save", color = Accent) } },
            dismissButton = { TextButton(onClick = close) { Text("Cancel", color = TextDim) } })
        return
    }
    var name by remember { mutableStateOf(when (n) { is Naming.Rename -> n.name; is Naming.RenameFolder -> n.name; else -> "" }) }
    AlertDialog(onDismissRequest = close, title = {
        Text(when (n) { is Naming.Rename -> "Rename playlist"; is Naming.NewFolder -> "New folder"; is Naming.RenameFolder -> "Rename folder"; else -> "New playlist" })
    },
        text = { TextBox(name, { name = it }, singleLine = true, placeholder = { Text("Name") }) },
        confirmButton = {
            TextButton(onClick = {
                when (n) {
                    is Naming.New -> state.createPlaylist(name.trim(), listOfNotNull(n.first) + n.more)
                    is Naming.Rename -> state.renamePlaylist(n.id, name.trim())
                    is Naming.NewFolder -> state.createFolder(name).let { id -> n.move?.let { state.moveToFolder(it, id) } }
                    is Naming.RenameFolder -> state.renameFolder(n.id, name)
                    else -> Unit
                }
                close()
            }) { Text(if (n is Naming.Rename || n is Naming.RenameFolder) "Rename" else "Create", color = Accent) }
        },
        dismissButton = { TextButton(onClick = close) { Text("Cancel", color = TextDim) } })
}

@Composable
private fun Note(text: String) = Text(text, color = TextDim, modifier = Modifier.padding(32.dp))

@Composable
private fun Art(model: String?, size: Int, round: Boolean = false, corner: Int = 6) {
    Box(Modifier.size(size.dp).clip(if (round) CircleShape else RoundedCornerShape(corner.dp)).background(Raised), contentAlignment = Alignment.Center) {
        if (model == null) Icon(Icons.Rounded.MusicNote, null, tint = Accent)
        else AsyncImage(if (model.startsWith("/")) File(model) else model, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

private fun time(ms: Long): String = (ms / 1000).let { "%d:%02d".format(it / 60, it % 60) }

@Composable
private fun Header(title: String, subtitle: String, art: String? = null, round: Boolean = false, back: (() -> Unit)? = null,
                   mosaic: List<String?>? = null, actions: @Composable () -> Unit = {}) {
    Row(Modifier.padding(16.dp, 20.dp, 24.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) IconButton(onClick = back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TextMain) } else Spacer(Modifier.width(8.dp))
        if (mosaic != null) { Mosaic(mosaic, 120); Spacer(Modifier.width(20.dp)) }
        else if (art != null || round) { Art(art, 120, round); Spacer(Modifier.width(20.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = TextDim)
        }
        actions()
    }
}

@Composable
private fun Section(text: String) = Text(text, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(24.dp, 16.dp, 24.dp, 6.dp))

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SongRow(
    n: Int?, title: String, artist: String, album: String, durMs: Long, art: String?, playing: Boolean,
    note: String? = null, trailing: (@Composable () -> Unit)? = null, selected: Boolean = false, dim: Boolean = false,
    onCtrlClick: (() -> Unit)? = null, onShiftClick: (() -> Unit)? = null, onClick: () -> Unit,
) {
    val hover = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val hovered by hover.collectIsHoveredAsState()
    // Ctrl+click and Shift+click select songs (several at once for the right-click menu); a plain click plays.
    Row(Modifier.fillMaxWidth().background(if (selected) Accent.copy(alpha = 0.22f) else if (hovered) Raised.copy(alpha = 0.6f) else Color.Transparent)
        .hoverable(hover)
        .onClick(keyboardModifiers = { isCtrlPressed }) { (onCtrlClick ?: onClick)() }
        .onClick(keyboardModifiers = { isShiftPressed && !isCtrlPressed }) { (onShiftClick ?: onClick)() }
        .onClick(keyboardModifiers = { !isCtrlPressed && !isShiftPressed }, onClick = onClick)
        .alpha(if (dim) 0.4f else 1f)
        .padding(start = 24.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (n != null) Text("$n", color = TextDim, modifier = Modifier.width(36.dp))
        Art(art, 40)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1.4f)) {
            Text(title, color = if (playing) Accent else TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(note, artist).joinToString(" · "), color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(album, color = TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
        Text(if (durMs > 0) time(durMs) else "", color = TextDim, fontSize = 13.sp, modifier = Modifier.width(48.dp))
        Box(Modifier.width(48.dp)) { trailing?.invoke() }
    }
}

/** The right-click menu of a library song, or of all selected songs when several are selected. */
private fun trackMenu(ctx: Ctx, list: List<Track>, t: Track, inPlaylist: String?, selected: Set<String>): List<ContextMenuItem> {
    val s = ctx.state
    val many = list.filter { it.id in selected }.takeIf { t.id in selected && it.size > 1 }
    if (many == null && LinkSync.isMissing(t.id)) return missingMenu(ctx, t, inPlaylist)
    return if (many != null) buildList {
        val n = many.size
        val here = many.filterNot { LinkSync.isMissing(it.id) }
        ctx.player?.let { p ->
            if (here.isNotEmpty()) {
                add(ContextMenuItem("Play ${here.size} songs") { ctx.play(here, 0) })
                add(ContextMenuItem("Play next") { here.reversed().forEach { p.playNext(s.item(it)) } })
                add(ContextMenuItem("Add to queue") { here.forEach { p.enqueue(s.item(it)) } })
            }
        }
        val canLike = many.filter { s.canSave(it.id) }
        if (canLike.any { !s.isLiked(it.id) }) add(ContextMenuItem("Add $n to Liked Songs") { canLike.filterNot { s.isLiked(it.id) }.forEach { s.toggleLike(it.id) } })
        else if (canLike.isNotEmpty()) add(ContextMenuItem("Remove $n from Liked Songs") { canLike.forEach { s.toggleLike(it.id) } })
        s.collection.playlists.filter { it.id != inPlaylist }.forEach { pl -> add(ContextMenuItem("Add $n to “${pl.name}”") { many.forEach { s.addToPlaylist(pl.id, it.id) } }) }
        add(ContextMenuItem("Add $n to new playlist…") { ctx.dialog(Naming.New(null, many.map { it.id })) })
        if (inPlaylist != null) add(ContextMenuItem("Remove $n from this playlist") { ctx.removeFromPlaylist(inPlaylist, many.map { it.id }) })
        many.mapNotNull { Downloads.forTrack(it, s.settings.server) }.takeIf { it.isNotEmpty() }?.let { d -> add(ContextMenuItem("Download ${d.size}") { ctx.download(d) }) }
        val editable = here.filterNot(s::isPreviewRow)
        if (editable.any { s.meta.containsKey(it.id) }) add(ContextMenuItem("Restore original tags") { editable.forEach { Overrides.restore(it.id) }; ctx.toast("Original tags restored") })
        if (editable.isNotEmpty()) add(ContextMenuItem("Edit details of ${editable.size} songs…") { ctx.dialog(Naming.EditMany(editable)) })
        if (editable.isNotEmpty()) add(ContextMenuItem("Hide ${editable.size} from library") {
            editable.forEach { s.toggleHidden(it.id) }; ctx.undoable("${editable.size} songs hidden") { editable.forEach { s.toggleHidden(it.id) } }
        })
    } else buildList {
        ctx.player?.let { p ->
            add(ContextMenuItem("Play next") { p.playNext(s.item(t)) })
            add(ContextMenuItem("Add to queue") { p.enqueue(s.item(t)) })
            add(ContextMenuItem("Start radio") { ctx.radio(s.item(t), t.artist) })
        }
        add(ContextMenuItem(if (s.isLiked(t.id)) "Remove from Liked Songs" else "Add to Liked Songs") {
            val was = s.isLiked(t.id)
            s.toggleLike(t.id)
            if (was) ctx.undoable("Removed from Liked Songs") { s.toggleLike(t.id) }
        })
        s.collection.playlists.filter { it.id != inPlaylist }.forEach { pl -> add(ContextMenuItem("Add to “${pl.name}”") { s.addToPlaylist(pl.id, t.id) }) }
        add(ContextMenuItem("Add to new playlist…") { ctx.dialog(Naming.New(t.id)) })
        if (inPlaylist != null) add(ContextMenuItem("Remove from this playlist") { ctx.removeFromPlaylist(inPlaylist, listOf(t.id)) })
        add(ContextMenuItem("Go to album") {
            Grouping.albums(s.library).firstOrNull { it.key == t.albumKey }?.let { a -> ctx.push(View.LocalList(a.title, "Album · ${a.artist}", a.artUri, a.tracks)) }
        })
        add(ContextMenuItem("Go to artist") { ctx.openArtistByName(t.artist) })
        Downloads.forTrack(t, s.settings.server)?.let { d -> add(ContextMenuItem("Download") { ctx.download(listOf(d)) }) }
        val r = s.remoteFor(t)?.takeIf { it.source == io.github.realkhfan.onda.core.Source.DEEZER || it.source == io.github.realkhfan.onda.core.Source.ITUNES }
        add(ContextMenuItem(if (r != null) "Copy link" else "Copy song name") { ctx.copy(shareText(t.title, t.artist, r), if (r != null) "Link" else "Song name") })
        if (s.meta.containsKey(t.id)) add(ContextMenuItem("Restore original tags") { Overrides.restore(t.id); ctx.toast("Original tags restored") })
        if (!s.isPreviewRow(t)) add(ContextMenuItem("Edit details…") { ctx.dialog(Naming.Edit(t)) })
        if (!s.isPreviewRow(t)) add(ContextMenuItem("Song info") { ctx.dialog(Naming.Info(t)) })
        if (!s.isPreviewRow(t) && !s.isHidden(t.id)) add(ContextMenuItem("Hide from library") { s.toggleHidden(t.id); ctx.undoable("Hidden: ${t.title}") { s.toggleHidden(t.id) } })
    }
}

/** The right-click menu of a song that's only on your phone: find it here, or tidy the list. */
private fun missingMenu(ctx: Ctx, t: Track, inPlaylist: String?): List<ContextMenuItem> = buildList {
    val s = ctx.state
    Link.phones.firstOrNull { Link.on && Link.connected(it) }?.let { ph ->
        add(ContextMenuItem("Get it from ${ph.name}") {
            ctx.toast(if (Link.getFromPhone(listOf(LinkSync.Song(null, t.title, t.artist)))) "Asked ${ph.name} for it; it arrives in “Onda from phone”" else "Your phone isn't connected")
        })
    }
    add(ContextMenuItem("Search for it") { s.searchFor = "${t.artist} ${t.title}"; ctx.push(View.Search) })
    ctx.player?.let { add(ContextMenuItem("Start radio") { ctx.artistRadio(t.artist) }) }
    if (s.isLiked(t.id)) add(ContextMenuItem("Remove from Liked Songs") { s.toggleLike(t.id); ctx.undoable("Removed from Liked Songs") { s.toggleLike(t.id) } })
    s.collection.playlists.filter { it.id != inPlaylist }.forEach { pl -> add(ContextMenuItem("Add to “${pl.name}”") { s.addToPlaylist(pl.id, t.id) }) }
    if (inPlaylist != null) add(ContextMenuItem("Remove from this playlist") { ctx.removeFromPlaylist(inPlaylist, listOf(t.id)) })
    add(ContextMenuItem("Copy song name") { ctx.copy("${t.title} — ${t.artist}", "Song name") })
}

/** A song from your library, with its right-click menu (for all selected songs when several are selected). */
@Composable
private fun TrackRow(
    ctx: Ctx, list: List<Track>, i: Int, numbered: Boolean, inPlaylist: String? = null,
    selected: Set<String> = emptySet(), onSelect: ((Int, Boolean, Boolean) -> Unit)? = null,
) {
    val t = list[i]
    val s = ctx.state
    val missing = LinkSync.isMissing(t.id)
    ContextMenuArea(items = { trackMenu(ctx, list, t, inPlaylist, selected) }) {
        // A song only on your phone: greyed out and not played, but its right-click menu works.
        SongRow(if (numbered) i + 1 else null, t.title, t.artist, t.album, t.durationMs, t.artUri, ctx.player?.current?.id == t.id,
            note = if (missing) "Not on this PC" else if (s.isPreviewRow(t)) "Preview" else null, selected = t.id in selected, dim = missing,
            onCtrlClick = onSelect?.let { f -> { f(i, true, false) } }, onShiftClick = onSelect?.let { f -> { f(i, false, true) } },
            trailing = {
                // The heart of a liked song takes the like away (with Undo), like Spotify's.
                if (s.isLiked(t.id)) Tip("Remove from Liked Songs") {
                    IconButton(onClick = { s.toggleLike(t.id); ctx.undoable("Removed from Liked Songs") { s.toggleLike(t.id) } }) {
                        Icon(Icons.Rounded.Favorite, "Remove from Liked Songs", tint = Accent)
                    }
                }
            }) {
            onSelect?.invoke(i, false, false)
            if (!missing) ctx.play(list, i)
        }
    }
}

/** Bars per hour of the day and per weekday, from your plays. */
@Composable
private fun ListeningClock(plays: List<io.github.realkhfan.onda.core.Discovery.Play>) {
    val zone = java.time.ZoneId.systemDefault()
    val times = plays.map { java.time.Instant.ofEpochMilli(it.atMs).atZone(zone) }
    val hours = IntArray(24).also { a -> times.forEach { a[it.hour]++ } }
    val days = IntArray(7).also { a -> times.forEach { a[it.dayOfWeek.value - 1]++ } }
    @Composable
    fun Bars(values: IntArray, labels: List<String>, barWidth: Int) {
        val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            values.forEachIndexed { i, v ->
                Tip("${labels[i]}: $v plays") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.width(barWidth.dp).height((4 + 96f * v / max).dp).clip(RoundedCornerShape(3.dp)).background(if (v == max) Accent else Accent.copy(alpha = 0.45f)))
                        Text(labels[i].take(if (barWidth > 20) 3 else 2), color = TextDim, fontSize = 10.sp)
                    }
                }
            }
        }
    }
    Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
        Bars(hours, (0..23).map { "%02d".format(it) }, 14)
        Bars(days, listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"), 28)
    }
}

/** "Recommended songs" under a playlist, like Spotify's: + adds one; Refresh finds others. */
private fun androidx.compose.foundation.lazy.LazyListScope.recommendations(ctx: Ctx, playlistId: String) {
    item {
        val state = ctx.state
        var recs by remember(playlistId) { mutableStateOf<List<RemoteTrack>?>(null) }
        var round by remember(playlistId) { mutableIntStateOf(0) }
        LaunchedEffect(playlistId, round) { recs = null; recs = state.recommendFor(playlistId, refresh = round > 0) }
        val inList = state.collection.playlists.firstOrNull { it.id == playlistId }?.trackIds.orEmpty().toSet()
        val shown = recs.orEmpty().filter { it.key !in inList }
        Column(Modifier.padding(top = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Section("Recommended songs")
                TextButton(onClick = { round++ }) { Text("Refresh", color = TextDim) }
            }
            when {
                recs == null -> LinearProgressIndicator(Modifier.padding(24.dp, 8.dp).width(240.dp), color = Accent, trackColor = Raised)
                shown.isEmpty() -> Note("Add a few songs and similar ones show up here.")
                else -> shown.forEachIndexed { i, r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { RemoteRow(ctx, shown, i) }
                        TextButton(onClick = { state.remember(r); state.addToPlaylist(playlistId, r.key) }, modifier = Modifier.padding(end = 12.dp)) { Text("Add", color = Accent) }
                    }
                }
            }
        }
    }
}

/** "Title — Artist" and, for a Deezer or Apple song, its public page. */
private fun shareText(title: String, artist: String, r: RemoteTrack?): String {
    val link = when (r?.source) {
        null -> null
        io.github.realkhfan.onda.core.Source.DEEZER -> "https://www.deezer.com/track/${r.id}"
        io.github.realkhfan.onda.core.Source.ITUNES -> "https://music.apple.com/song/${r.id}"
        else -> null
    }
    return "$title — $artist" + (link?.let { "\n$it" } ?: "")
}

/** A catalog song (preview or free full song): + saves it to Want to listen. */
@Composable
private fun RemoteRow(ctx: Ctx, list: List<RemoteTrack>, i: Int, numbered: Boolean = false) {
    val r = list[i]
    val s = ctx.state
    val wanted = s.isWanted(r)
    ContextMenuArea(items = {
        buildList {
            ctx.player?.let { p ->
                s.item(r)?.let { item ->
                    add(ContextMenuItem("Play next") { p.playNext(item) })
                    add(ContextMenuItem("Add to queue") { p.enqueue(item) })
                }
            }
            // Saving a catalog song is Want to listen (as on the phone); one liked before can still be taken out.
            if (s.isLiked(r.key)) add(ContextMenuItem("Remove from Liked Songs") { s.toggleLike(r.key) })
            s.collection.playlists.forEach { pl -> add(ContextMenuItem("Add to “${pl.name}”") { s.remember(r); s.addToPlaylist(pl.id, r.key) }) }
            add(ContextMenuItem("Add to new playlist…") { s.remember(r); ctx.dialog(Naming.New(r.key)) })
            add(ContextMenuItem(if (wanted) "Remove from Want to listen" else "Add to Want to listen") { s.toggleWant(r) })
            add(ContextMenuItem(if (s.isHidden(r.key)) "Suggest this song again" else "Not interested in this song") { s.toggleHidden(r.key) })
            add(ContextMenuItem(if (s.isBlocked(r.artist)) "Suggest ${r.artist} again" else "Don't suggest ${r.artist}") { s.toggleBlocked(r.artist) })
            add(ContextMenuItem("Go to artist") { ctx.openArtistByName(r.artist) })
            ctx.state.item(r)?.let { item -> if (ctx.player != null) add(ContextMenuItem("Start radio") { ctx.radio(item, r.artist) }) }
            Downloads.forRemote(r)?.let { d -> add(ContextMenuItem("Download") { ctx.download(listOf(d)) }) }
            val linked = r.source == io.github.realkhfan.onda.core.Source.DEEZER || r.source == io.github.realkhfan.onda.core.Source.ITUNES
            add(ContextMenuItem(if (linked) "Copy link" else "Copy song name") { ctx.copy(shareText(r.title, r.artist, r.takeIf { linked }), if (linked) "Link" else "Song name") })
        }
    }) {
        SongRow(if (numbered) i + 1 else null, r.title, r.artist, r.album, r.durationSec * 1000L, r.cover, ctx.player?.current?.id == r.key,
            note = if (r.source.full) "Full · ${r.source.label}" else "Preview",
            trailing = {
                IconButton(onClick = { s.toggleWant(r) }) {
                    Icon(if (wanted) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline,
                        if (wanted) "Remove from Want to listen" else "Add to Want to listen", tint = if (wanted) Accent else TextDim)
                }
            }) { ctx.playRemote(list, i) }
    }
}

/* ---------- Sidebar ---------- */

@Composable
private fun Sidebar(state: AppState, top: View, go: (View) -> Unit, dialog: (Naming) -> Unit, undoable: (String, () -> Unit) -> Unit, newPlaylist: () -> Unit) {
    val compact = state.settings.compact
    @Composable
    fun Entry(v: View, label: String, icon: ImageVector, big: Boolean = false) {
        val on = top == v
        val row = @Composable {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (on) Raised else Color.Transparent)
                .clickable { go(v) }.padding(horizontal = 10.dp, vertical = if (big) 10.dp else 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = if (on) Accent else TextDim, modifier = Modifier.size(if (big) 24.dp else 20.dp))
                if (!compact) {
                    Spacer(Modifier.width(12.dp))
                    Text(label, color = if (on) TextMain else TextDim, fontWeight = FontWeight.SemiBold, fontSize = if (big) 16.sp else 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (compact) Tip(label, row) else row()
    }
    Column(Modifier.width(if (compact) 68.dp else 240.dp).fillMaxHeight().background(Side).padding(12.dp)) {
        if (!compact) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)) {
            // Settings live behind your avatar, like on the phone.
            Tip("Settings") { IconButton(onClick = { go(View.Settings) }, Modifier.size(36.dp).clip(CircleShape)) {
                ProfilePic(state.settings.picture, 36.dp, highlighted = top == View.Settings)
            } }
            Spacer(Modifier.width(10.dp))
            Text("Onda", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.weight(1f))
            Tip("Smaller sidebar (Ctrl+B)") { IconButton(onClick = { state.setCompact(true) }, Modifier.size(32.dp)) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Smaller sidebar", tint = TextDim) } }
        } else Column(Modifier.padding(bottom = 12.dp)) {
            Tip("Settings") { IconButton(onClick = { go(View.Settings) }, Modifier.size(36.dp).clip(CircleShape)) {
                ProfilePic(state.settings.picture, 36.dp, highlighted = top == View.Settings) } }
            Tip("Wider sidebar (Ctrl+B)") { IconButton(onClick = { state.setCompact(false) }, Modifier.size(36.dp)) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Wider sidebar", tint = TextDim) } }
        }
        Entry(View.Home, "Home", Icons.Rounded.Home, big = true)
        Entry(View.Search, "Search", Icons.Rounded.Search, big = true)
        Entry(View.Stats, "Your stats", Icons.Rounded.BarChart)
        Entry(View.History, "History", Icons.Rounded.History)
        Row(Modifier.padding(top = 18.dp, start = if (compact) 0.dp else 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!compact) {
                Text("Your library", color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                var open by remember { mutableStateOf(false) }
                Box {
                    Tip("Order of your playlists") { IconButton(onClick = { open = true }, Modifier.size(32.dp)) { Icon(Icons.AutoMirrored.Rounded.Sort, "Order of your playlists", tint = TextDim) } }
                    DropdownMenu(open, { open = false }, modifier = Modifier.background(Color(0xFF2E2E38))) {
                        listOf("Custom", "A–Z", "Most songs").forEach { o ->
                            DropdownMenuItem({ Text(if (o == "Custom") "Custom (drag to move)" else o, color = if (state.settings.playlistOrder == o) Accent else TextMain) }, { state.setPlaylistOrder(o); open = false })
                        }
                    }
                }
            }
            Tip("New playlist") { IconButton(onClick = newPlaylist, Modifier.size(32.dp)) { Icon(Icons.Rounded.Add, "New playlist", tint = TextDim) } }
            Tip("New folder") { IconButton(onClick = { dialog(Naming.NewFolder()) }, Modifier.size(32.dp)) { Icon(Icons.Rounded.CreateNewFolder, "New folder", tint = TextDim) } }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Entry(View.Songs, "Songs", Icons.Rounded.MusicNote)
            Entry(View.Albums, "Albums", Icons.Rounded.Album)
            Entry(View.Artists, "Artists", Icons.Rounded.Person)
            if (state.settings.folders.isNotEmpty()) Entry(View.Folder(null), "Folders", Icons.Rounded.Folder)
            Spacer(Modifier.height(8.dp))
            Entry(View.Liked, "Liked Songs", Icons.Rounded.Favorite)
            Entry(View.Smart, "Smart playlists", Icons.Rounded.AutoAwesome)
            Entry(View.Want, "Want to listen", Icons.Rounded.Bookmark)
            // Pinned playlists first (newest pin on top), then the rest; right-click to pin or unpin.
            val pins = state.settings.pinned
            val folders = state.collection.folders
            // In your own order, drag a playlist to move it (within its folder, or among those outside one).
            val movable = state.settings.playlistOrder == "Custom"
            @Composable fun PlaylistEntry(pl: Store.Playlist, group: List<Store.Playlist>, reorder: ReorderState) {
                val i = group.indexOf(pl)
                val drag = if (!movable || group.size < 2) Modifier
                    else Modifier.reorderRow(reorder, i, group.size).dragToMove(reorder, i, group.size) { from, to -> state.dragPlaylist(group.map { it.id }, from, to) }
                Box(drag) { ContextMenuArea(items = {
                    buildList {
                        add(ContextMenuItem(if (pl.id in pins) "Unpin" else "Pin to the top") { state.togglePin(pl.id) })
                        folders.filter { it.id != pl.folder }.forEach { f -> add(ContextMenuItem("Move to “${f.name}”") { state.moveToFolder(pl.id, f.id) }) }
                        add(ContextMenuItem("Move to a new folder…") { dialog(Naming.NewFolder(pl.id)) })
                        if (pl.folder != null) add(ContextMenuItem("Take out of the folder") { state.moveToFolder(pl.id, null) })
                    }
                }) {
                    Entry(View.Playlist(pl.id), pl.name, if (pl.id in pins) Icons.Rounded.PushPin else Icons.AutoMirrored.Rounded.QueueMusic)
                } }
            }
            // Folders first, like Spotify: click to open or close; right-click to rename or delete.
            folders.forEach { f ->
                var open by remember(f.id) { mutableStateOf(false) }
                ContextMenuArea(items = {
                    listOf(ContextMenuItem("Rename folder…") { dialog(Naming.RenameFolder(f.id, f.name)) },
                        ContextMenuItem("Delete folder (its playlists stay)") {
                            state.deleteFolder(f.id)?.let { (folder, ids) -> undoable("Deleted folder “${folder.name}”") { state.restoreFolder(folder, ids) } }
                        })
                }) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { open = !open }.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (open) Icons.Rounded.FolderOpen else Icons.Rounded.Folder, null, tint = TextDim, modifier = Modifier.size(20.dp))
                        if (!compact) {
                            Spacer(Modifier.width(12.dp))
                            Text(f.name, color = TextDim, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (open) Column(Modifier.padding(start = if (compact) 0.dp else 14.dp)) {
                    val inside = state.sidebarPlaylists.filter { it.folder == f.id }
                    val reorder = remember(f.id) { ReorderState() }
                    inside.forEach { PlaylistEntry(it, inside, reorder) }
                }
            }
            val folderIds = folders.map { it.id }.toSet()
            val loose = state.sidebarPlaylists.filter { it.folder == null || it.folder !in folderIds }
            val reorder = remember { ReorderState() }
            loose.forEach { PlaylistEntry(it, loose, reorder) }
            state.serverPlaylists.forEach { Entry(View.ServerPlaylist(it), it.name, Icons.Rounded.Dns) }
        }
        if (state.scanning || state.syncing) {
            if (!compact) Text(if (state.scanning) "Scanning your folders…" else "Syncing your server…", color = TextDim, fontSize = 12.sp)
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp), color = Accent, trackColor = Raised)
        }
    }
}

/* ---------- Home ---------- */

@Composable
private fun HomePage(ctx: Ctx, open: (View) -> Unit) {
    val s = ctx.state
    val hour = java.time.LocalTime.now().hour
    val hello = when (hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
    data class Tile(val label: String, val art: String?, val icon: ImageVector?, val onClick: () -> Unit)
    val tiles = buildList {
        add(Tile("Liked Songs", null, Icons.Rounded.Favorite) { open(View.Liked) })
        add(Tile("Want to listen", null, Icons.Rounded.Bookmark) { open(View.Want) })
        add(Tile("Shuffle all", null, Icons.Rounded.Shuffle) { s.library.shuffled().takeIf { it.isNotEmpty() }?.let { ctx.play(it, 0) } })
        s.collection.playlists.forEach { pl -> add(Tile(pl.name, s.playlistTracks(pl.id).firstNotNullOfOrNull { it.artUri }, null) { open(View.Playlist(pl.id)) }) }
        s.serverPlaylists.forEach { sp -> add(Tile(sp.name, s.serverPlaylistTracks(sp).firstNotNullOfOrNull { it.artUri }, null) { open(View.ServerPlaylist(sp)) }) }
    }.take(8)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Text(hello, color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 16.dp)) }
        items(tiles.chunked(4)) { row ->
            Row(Modifier.padding(horizontal = 24.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { t ->
                    Row(Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(6.dp)).background(Raised).clickable(onClick = t.onClick),
                        verticalAlignment = Alignment.CenterVertically) {
                        if (t.icon != null) Box(Modifier.size(60.dp).background(Color(0xFF2B2140)), contentAlignment = Alignment.Center) { Icon(t.icon, null, tint = Accent) }
                        else Art(t.art, 60)
                        Text(t.label, color = TextMain, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        item { Section("Made for you") }
        item {
            ScrollRow {
                items(Discovery.Kind.entries) { k ->
                    Column(Modifier.width(180.dp).clip(RoundedCornerShape(8.dp)).clickable { open(View.Mix(k)) }.padding(8.dp)) {
                        Box(Modifier.size(164.dp).clip(RoundedCornerShape(8.dp))
                            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF2B2140), Accent))), contentAlignment = Alignment.BottomStart) {
                            Text(k.label, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(12.dp))
                        }
                        val m = s.mixes[k]
                        Text(when { k in s.building -> "Making your mix…"; m == null -> "Play some music first"
                            else -> m.tracks.take(3).joinToString(", ") { it.artist } }, color = TextDim, fontSize = 12.sp, maxLines = 2,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
        val tops = s.topArtists(6)
        if (tops.isNotEmpty()) {
            item { Section("Your top mixes") }
            item {
                ScrollRow {
                    items(tops) { name ->
                        Column(Modifier.width(180.dp).clip(RoundedCornerShape(8.dp)).clickable { open(View.ArtistMix(name)) }.padding(8.dp)) {
                            Box(contentAlignment = Alignment.BottomStart) {
                                Art(s.artistPics[name.lowercase()]?.ifBlank { null } ?: s.library.firstOrNull { Match.norm(it.artist) == Match.norm(name) }?.artUri, 164)
                                Text("$name Mix", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(164.dp).background(Color(0xAA000000)).padding(8.dp))
                            }
                            Text("$name and similar artists", color = TextDim, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
        if (s.releases.items.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Section("New releases")
                    TextButton(onClick = { open(View.ReleaseRadar) }) { Text("Play as a playlist", color = Accent) }
                }
            }
            item {
                val today = java.time.LocalDate.now()
                ScrollRow {
                    items(s.releases.items) { a -> Card(a.cover, a.title, "${a.artist} · ${NewReleases.ago(a, today)}") { open(View.CatalogAlbum(a)) } }
                }
            }
        }
        if (s.recent.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Section("Recently played")
                    TextButton(onClick = { open(View.History) }) { Text("Show all", color = TextDim) }
                }
            }
            item {
                ScrollRow {
                    // The same song from two places (file and preview) shows once.
                    items(s.recent.distinctBy { Match.norm(it.title) + "|" + Match.norm(it.artist) }) { it -> Card(it.art, it.title, it.artist) { ctx.player?.play(listOf(it), 0) } }
                }
            }
        }
        if (s.recentlyAdded.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Section("Recently added")
                    TextButton(onClick = { open(View.SmartList(AppState.Smart.ADDED)) }) { Text("Show all", color = TextDim) }
                }
            }
            item {
                ScrollRow {
                    items(s.recentlyAdded) { a -> Card(a.artUri, a.title, a.artist) { open(View.LocalList(a.title, "Album · ${a.artist}", a.artUri, a.tracks)) } }
                }
            }
        }
        if (s.library.isEmpty() && !s.scanning) item { Note("Add your music folder or your Navidrome server: tap the person icon at the top left.") }
    }
}

/** A hint on hover, for icon-only buttons. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun Tip(text: String, content: @Composable () -> Unit) = androidx.compose.foundation.TooltipArea(
    tooltip = { Text(text, color = TextMain, fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF2E2E38)).padding(8.dp, 4.dp)) },
    delayMillis = 400, content = content,
)

/** A sideways row a mouse can scroll: arrows at the ends when there's more, and a scrollbar (Shift+wheel works too). */
@Composable
private fun ScrollRow(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var width by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxWidth().onSizeChanged { width = it.width }) {
        Column {
            LazyRow(state = list, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
            if (list.canScrollForward || list.canScrollBackward) androidx.compose.foundation.HorizontalScrollbar(
                androidx.compose.foundation.rememberScrollbarAdapter(list), Modifier.fillMaxWidth().padding(24.dp, 4.dp),
                style = androidx.compose.foundation.LocalScrollbarStyle.current.copy(unhoverColor = TextMain.copy(alpha = 0.15f), hoverColor = TextMain.copy(alpha = 0.45f)))
        }
        @Composable
        fun Arrow(back: Boolean) = IconButton(
            onClick = { scope.launch { list.animateScrollBy(if (back) -width * 0.8f else width * 0.8f) } },
            modifier = Modifier.align(if (back) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 8.dp).size(40.dp)
                .clip(CircleShape).background(Color(0xE6222228)),
        ) { Icon(if (back) Icons.AutoMirrored.Rounded.KeyboardArrowLeft else Icons.AutoMirrored.Rounded.KeyboardArrowRight, if (back) "Scroll left" else "Scroll right", tint = TextMain) }
        if (list.canScrollBackward) Arrow(back = true)
        if (list.canScrollForward) Arrow(back = false)
    }
}

/* ---------- Pages ---------- */

/** A playlist's cover: its first four different album covers in a square, like Spotify (or one cover, or a note). */
@Composable
private fun Mosaic(arts: List<String?>, size: Int) {
    val distinct = arts.filterNotNull().distinct()
    if (distinct.size < 4) return Art(distinct.firstOrNull(), size)
    Column(Modifier.size(size.dp).clip(RoundedCornerShape(6.dp))) {
        for (row in 0..1) Row { for (col in 0..1) Art(distinct[row * 2 + col], size / 2, corner = 0) }
    }
}

/** How a song list is ordered; [CUSTOM] is the list's own order (a playlist's, an album's). */
enum class Sort(val label: String) { CUSTOM("Custom order"), TITLE("Title"), ARTIST("Artist"), ALBUM("Album"), PLAYS("Most played"), ADDED("Recently added"), LENGTH("Length") }

internal fun sorted(tracks: List<Track>, sort: Sort, plays: Map<String, Int>, added: Map<String, Long> = emptyMap()): List<Track> = when (sort) {
    Sort.ADDED -> tracks.sortedByDescending { added[it.id] ?: 0 }
    Sort.CUSTOM -> tracks
    Sort.TITLE -> tracks.sortedBy { it.title.lowercase() }
    Sort.ARTIST -> tracks.sortedWith(compareBy({ it.artist.lowercase() }, { it.album.lowercase() }, { it.trackNo }))
    Sort.ALBUM -> tracks.sortedWith(compareBy({ it.album.lowercase() }, { it.trackNo }))
    Sort.PLAYS -> tracks.sortedByDescending { plays[it.id] ?: 0 }
    Sort.LENGTH -> tracks.sortedBy { it.durationMs }
}

/** Ctrl+click toggles a song, Shift+click selects from the last clicked one; ids, so a re-sort keeps the selection. */
internal fun select(selected: Set<String>, list: List<Track>, anchor: Int, i: Int, ctrl: Boolean, shift: Boolean): Set<String> = when {
    ctrl -> if (list[i].id in selected) selected - list[i].id else selected + list[i].id
    shift && anchor in list.indices -> selected + list.subList(minOf(anchor, i), maxOf(anchor, i) + 1).map { it.id }
    else -> emptySet()
}

@Composable
private fun SongList(
    ctx: Ctx, title: String, subtitle: String, tracks: List<Track>, art: String? = null, back: (() -> Unit)? = null,
    inPlaylist: String? = null, onMove: ((Int, Int) -> Unit)? = null, mosaic: Boolean = false, filterable: Boolean = true,
    above: (androidx.compose.foundation.lazy.LazyListScope.() -> Unit)? = null,
    below: (androidx.compose.foundation.lazy.LazyListScope.() -> Unit)? = null, actions: @Composable () -> Unit = {},
) {
    var filter by remember(title) { mutableStateOf<String?>(null) }
    val reorder = remember { ReorderState() }
    var sort by remember(title) { mutableStateOf(Sort.CUSTOM) }
    var selected by remember(title) { mutableStateOf(emptySet<String>()) }
    var anchor by remember(title) { mutableIntStateOf(-1) }
    val shown = remember(tracks, sort, ctx.state.playCounts, filter) {
        val f = filter?.let(Match::norm).orEmpty()
        sorted(tracks, sort, ctx.state.playCounts, ctx.state.addedAt).filter { f.isEmpty() || Match.norm("${it.title} ${it.artist} ${it.album}").contains(f) }
    }
    val onSelect = { i: Int, ctrl: Boolean, shift: Boolean ->
        selected = select(selected, shown, anchor, i, ctrl, shift)
        if (!shift) anchor = i
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            val total = tracks.sumOf { it.durationMs } / 60_000
            val length = if (total >= 60) "${total / 60} h ${total % 60} min" else "$total min"
            Header(title, "$subtitle · ${tracks.size} songs" + (if (total > 0) " · $length" else "") + if (selected.size > 1) " · ${selected.size} selected" else "", art, back = back,
                mosaic = if (mosaic) tracks.map { it.artUri } else null) {
                actions()
                if (tracks.isNotEmpty() && ctx.player != null) Tip("Add all to the queue") {
                    IconButton(onClick = {
                        val here = shown.filterNot { LinkSync.isMissing(it.id) }
                        here.forEach { ctx.player.enqueue(ctx.state.item(it)) }; ctx.toast("${here.size} songs added to the queue")
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add all to the queue", tint = TextDim)
                    }
                }
                if (filterable && tracks.size > 1) IconButton(onClick = { filter = if (filter == null) "" else null }) {
                    Icon(Icons.Rounded.Search, "Find in this list", tint = if (filter != null) Accent else TextDim)
                }
                Spacer(Modifier.width(8.dp))
                var open by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { open = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, null, tint = TextDim); Spacer(Modifier.width(6.dp)); Text(sort.label, color = TextDim) }
                    DropdownMenu(open, { open = false }, modifier = Modifier.background(Color(0xFF2E2E38))) {
                        Sort.entries.forEach { o -> DropdownMenuItem({ Text(o.label, color = if (o == sort) Accent else TextMain) }, { sort = o; open = false }) }
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (tracks.size > 1) OutlinedButton(onClick = {
                    ctx.player?.let { p -> if (!p.shuffle) p.toggleShuffle() }
                    ctx.play(shown, shown.indices.random())
                }) { Icon(Icons.Rounded.Shuffle, null, tint = Accent); Text(" Shuffle", color = TextMain) }
                Spacer(Modifier.width(8.dp))
                if (tracks.isNotEmpty()) Button(onClick = { ctx.play(shown, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
            }
        }
        filter?.let { f ->
            item {
                TextBox(f, { filter = it }, placeholder = { Text("Find in $title") }, modifier = Modifier.padding(24.dp, 0.dp, 24.dp, 8.dp).width(360.dp),
                    onKey = { k -> if (k == Key.Escape) { filter = null; true } else false })
            }
        }
        above?.invoke(this)
        if (tracks.isEmpty() && above == null) item { Note("Nothing here yet. Right-click a song to add it.") }
        items(shown.size, key = { shown[it].id }) { i ->
            // Dragging moves songs only in the list's own order.
            if (onMove == null || sort != Sort.CUSTOM) TrackRow(ctx, shown, i, numbered = true, inPlaylist = inPlaylist, selected = selected, onSelect = onSelect)
            else Row(Modifier.reorderRow(reorder, i, shown.size), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { TrackRow(ctx, shown, i, numbered = true, inPlaylist = inPlaylist, selected = selected, onSelect = onSelect) }
                DragHandle(reorder, i, shown.size, shown[i].title, onMove)
            }
        }
        below?.invoke(this)
    }
}

@Composable
private fun Songs(ctx: Ctx) {
    val state = ctx.state
    var filter by remember { mutableStateOf("") }
    val all = remember(state.library) { Grouping.songsSorted(state.library) }
    val shown = remember(all, filter) { if (filter.isBlank()) all else AutoTree.search(all, filter, limit = 5000) }
    if (all.isEmpty() && !state.scanning) return Note("No music yet. Add a folder or your Navidrome server in Settings.")
    Column {
        Row(Modifier.padding(24.dp, 16.dp, 24.dp, 0.dp)) {
            TextBox(filter, { filter = it }, placeholder = { Text("Filter your songs") }, singleLine = true, modifier = Modifier.width(360.dp))
        }
        SongList(ctx, "Songs", "Your library", shown, filterable = false)
    }
}

@Composable
private fun Albums(state: AppState, onOpen: (Album) -> Unit, onOpenSaved: (RemoteAlbum) -> Unit) {
    var by by remember { mutableStateOf("Name") }
    val albums = remember(state.library, by, state.playCounts, state.addedAt) {
        val all = Grouping.albums(state.library)
        when (by) {
            "Artist" -> all.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            "Recently added" -> all.sortedByDescending { a -> a.tracks.maxOf { state.addedAt[it.id] ?: 0 } }
            "Most played" -> all.sortedByDescending { a -> a.tracks.sumOf { state.playCounts[it.id] ?: 0 } }
            else -> all
        }
    }
    Column {
    Row(Modifier.padding(24.dp, 12.dp, 24.dp, 0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Name", "Artist", "Recently added", "Most played").forEach { o ->
            if (o == by) Button(onClick = {}, contentPadding = PaddingValues(14.dp, 4.dp)) { Text(o, fontSize = 13.sp) }
            else OutlinedButton(onClick = { by = o }, contentPadding = PaddingValues(14.dp, 4.dp)) { Text(o, fontSize = 13.sp) }
        }
    }
    LazyVerticalGrid(GridCells.Adaptive(180.dp), Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(state.collection.albums) { a -> Card(a.cover, a.title, "Saved · ${a.artist}") { onOpenSaved(a) } }
        items(albums) { a -> Card(a.artUri, a.title, a.artist) { onOpen(a) } }
    }
    }
}

@Composable
private fun Card(art: String?, title: String, subtitle: String, onClick: () -> Unit) {
    Column(Modifier.width(180.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(8.dp)) {
        Art(art, 164)
        Text(title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(subtitle, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Artists(state: AppState, onOpen: (ArtistGroup) -> Unit, onOpenFollowed: (RemoteArtist) -> Unit) {
    val artists = remember(state.library) { Grouping.artists(state.library) }
    LazyColumn(Modifier.fillMaxSize()) {
        item { Header("Artists", "${artists.size} in your library · ${state.collection.followed.size} followed") }
        items(state.collection.followed) { a ->
            Row(Modifier.fillMaxWidth().clickable { onOpenFollowed(a) }.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Art(a.picture, 48, round = true)
                Spacer(Modifier.width(14.dp))
                Column { Text(a.name, color = TextMain); Text("Following", color = Accent, fontSize = 12.sp) }
            }
        }
        items(artists) { a ->
            Row(Modifier.fillMaxWidth().clickable { onOpen(a) }.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Art(state.artistPics[a.name.lowercase()]?.ifBlank { null } ?: a.artUri, 48, round = true)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(a.name, color = TextMain)
                    Text("${a.tracks.size} songs", color = TextDim, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun WantPage(ctx: Ctx, back: (() -> Unit)?) {
    val state = ctx.state
    val list = state.collection.want
    // Saves to the file chosen last time; [choose]: pick one (again) first.
    fun export(choose: Boolean) {
        state.launch {
            val home = System.getProperty("user.home")
            val path = state.settings.wantFile.takeUnless { choose } ?: when (val r = FolderPicker.file(true, state.settings.wantFile ?: "$home/Want to listen.txt", "Export Want to listen")) {
                is FolderPicker.Result.Picked -> r.path.let { if (File(it).extension.isEmpty()) "$it.txt" else it }
                FolderPicker.Result.NoPicker -> "$home/Want to listen.txt"
                FolderPicker.Result.Cancelled -> return@launch
                is FolderPicker.Result.Remote -> { ctx.toast(r.message); return@launch }
            }
            val text = io.github.realkhfan.onda.core.M3u.wantList(list)
            if (runCatching { withContext(kotlinx.coroutines.Dispatchers.IO) { File(path).writeText(text) } }.isSuccess) {
                state.setWantFile(path); ctx.toast("Saved ${list.size} songs to $path")
            } else if (!choose) export(true) // its folder is gone: choose another place
            else ctx.toast("Couldn't save to $path")
        }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header("Want to listen", "${list.size} songs you don't own yet", back = back) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { export(false) }) { Icon(Icons.Rounded.SaveAlt, null); Spacer(Modifier.width(6.dp)); Text("Export .txt", color = TextMain) }
                    if (list.isNotEmpty()) Button(onClick = { ctx.playRemote(list, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
                }
            }
        }
        state.settings.wantFile?.let { f ->
            item {
                Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Exports to $f", color = TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    TextButton(onClick = { export(true) }) { Text("Change", color = Accent) }
                }
            }
        }
        if (list.isEmpty()) item { Note("Songs you save with + in Search show up here.") }
        items(list.size) { i -> RemoteRow(ctx, list, i) }
    }
}

@Composable
private fun Search(ctx: Ctx) {
    val state = ctx.state
    var q by remember { mutableStateOf(state.searchFor.orEmpty()) }
    LaunchedEffect(state.searchFor) { state.searchFor?.let { q = it; state.searchFor = null } }
    var online by remember { mutableStateOf<List<RemoteTrack>>(emptyList()) }
    var artists by remember { mutableStateOf<List<RemoteArtist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<RemoteAlbum>>(emptyList()) }
    var free by remember { mutableStateOf<List<RemoteTrack>>(emptyList()) }
    var freeAlbums by remember { mutableStateOf<List<RemoteAlbum>>(emptyList()) }
    var problem by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf("All") }
    LaunchedEffect(q) {
        online = emptyList(); artists = emptyList(); albums = emptyList(); free = emptyList(); freeAlbums = emptyList(); problem = null
        if (q.trim().length < 2) return@LaunchedEffect
        delay(350) // wait until typing pauses
        coroutineScope {
            val dz = async { runCatching { Deezer.search(q, 20) } }
            val ar = async { runCatching { Deezer.searchArtists(q) } }
            val al = async { runCatching { Deezer.searchAlbums(q) } }
            val ov = async { runCatching { FreeCatalogs.openverse(q) } }
            val au = async { runCatching { FreeCatalogs.audius(q) } }
            val ia = async { runCatching { FreeCatalogs.archive(q) } }
            val ap = async { if (state.settings.apple) runCatching { Itunes.searchSongs(q) } else Result.success(emptyList()) }
            // AI "artists" never show; remixes, sped-up, live and karaoke only when the search words ask for them.
            val versions = Discovery.asksFor(q)
            fun ok(t: RemoteTrack) = !state.isAi(t.artist) && (versions || !Discovery.isUnwanted(t.title, t.album))
            online = Match.mergeTracks(dz.await().getOrDefault(emptyList()), ap.await().getOrDefault(emptyList())).filter(::ok)
            artists = ar.await().getOrDefault(emptyList()).filterNot { state.isAi(it.name) }
            albums = al.await().getOrDefault(emptyList()).filterNot { state.isAi(it.artist) }
            free = (ov.await().getOrDefault(emptyList()) + au.await().getOrDefault(emptyList())).filter(::ok).take(10)
            freeAlbums = ia.await().getOrDefault(emptyList()).filterNot { state.isAi(it.artist) }
            if (dz.await().isFailure && ov.await().isFailure && au.await().isFailure) problem = "No connection to the online catalogs."
        }
        delay(2000) // still the same words two seconds later: worth keeping as a recent search
        if (online.isNotEmpty() || artists.isNotEmpty()) state.rememberSearch(q)
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(24.dp)) {
                Text("Search", color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                TextBox(q, { q = it }, placeholder = { Text("Songs, artists and albums, here and online") }, singleLine = true, modifier = Modifier.width(480.dp))
                problem?.let { Text(it, color = TextDim, modifier = Modifier.padding(top = 8.dp)) }
                if (q.isNotBlank()) Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Songs", "Artists", "Albums", "Free songs").forEach { t ->
                        if (t == tab) Button(onClick = {}, contentPadding = PaddingValues(14.dp, 4.dp)) { Text(t, fontSize = 13.sp) }
                        else OutlinedButton(onClick = { tab = t }, contentPadding = PaddingValues(14.dp, 4.dp)) { Text(t, fontSize = 13.sp) }
                    }
                }
                if (q.isBlank() && state.settings.searches.isNotEmpty()) {
                    Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent searches", color = TextMain, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { state.forgetSearch(null) }) { Text("Clear", color = TextDim) }
                    }
                    state.settings.searches.forEach { r ->
                        Row(Modifier.width(480.dp).clip(RoundedCornerShape(6.dp)).clickable { q = r }.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.History, null, tint = TextDim, modifier = Modifier.size(18.dp))
                            Text(r, color = TextMain, modifier = Modifier.weight(1f).padding(start = 10.dp))
                            Tip("Remove") { IconButton(onClick = { state.forgetSearch(r) }) { Icon(Icons.Rounded.Close, "Remove", tint = TextDim, modifier = Modifier.size(18.dp)) } }
                        }
                    }
                }
            }
        }
        // Your own songs are searched in Songs (the filter there); this is the online catalog.
        if (tab == "All") artists.firstOrNull { Match.norm(it.name) == Match.norm(q) }?.let { top ->
            item { Section("Top result") }
            item {
                Row(Modifier.padding(horizontal = 24.dp).width(420.dp).clip(RoundedCornerShape(10.dp)).background(Raised)
                    .clickable { ctx.push(View.CatalogArtist(top)) }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art(top.picture, 92, round = true)
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text(top.name, color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        ArtistLabel(ctx.state, top, 14.sp)
                    }
                }
            }
        }
        if (online.isNotEmpty() && tab in setOf("All", "Songs")) {
            item { Section("Songs") }
            items(if (tab == "Songs") online.size else minOf(online.size, 8)) { i -> RemoteRow(ctx, online, i) }
        }
        if (artists.isNotEmpty() && tab in setOf("All", "Artists")) {
            item { Section("Artists") }
            item {
                ScrollRow {
                    items(artists) { a ->
                        Column(Modifier.width(140.dp).clip(RoundedCornerShape(8.dp)).clickable { ctx.push(View.CatalogArtist(a)) }.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Art(a.picture, 124, round = true)
                            Text(a.name, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                            ArtistLabel(ctx.state, a, 12.sp)
                        }
                    }
                }
            }
        }
        if (albums.isNotEmpty() && tab in setOf("All", "Albums")) {
            item { Section("Albums") }
            item {
                ScrollRow {
                    items(albums) { a -> Card(a.cover, a.title, a.artist) { ctx.push(View.CatalogAlbum(a)) } }
                }
            }
        }
        if (free.isNotEmpty() && tab in setOf("All", "Free songs")) {
            item { Section("Free full songs") }
            items(free.size) { i -> RemoteRow(ctx, free, i) }
        }
        if (freeAlbums.isNotEmpty() && tab in setOf("All", "Free songs", "Albums")) {
            item { Section("Free albums") }
            item { ScrollRow { items(freeAlbums) { a -> Card(a.cover, a.title, "${a.artist} · Internet Archive") { ctx.push(View.CatalogAlbum(a)) } } } }
        }
    }
}

@Composable
private fun CatalogArtistPage(ctx: Ctx, artist: RemoteArtist, back: (() -> Unit)?) {
    var picture by remember(artist.id) { mutableStateOf(artist.picture) }
    var top by remember(artist.id) { mutableStateOf<List<RemoteTrack>?>(null) }
    var releases by remember(artist.id) { mutableStateOf<List<RemoteAlbum>>(emptyList()) }
    LaunchedEffect(artist.id) {
        coroutineScope {
            val t = async { runCatching { Deezer.artistTop(artist.id, 10) }.getOrDefault(emptyList()) }
            val r = async { runCatching { Deezer.artistAlbums(artist.id, artist.name) }.getOrDefault(emptyList()) }
            if (picture == null) picture = runCatching { Deezer.artist(artist.id)?.picture }.getOrNull()
            top = t.await(); releases = r.await()
        }
    }
    val popular = top.orEmpty()
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            val s = ctx.state
            Header(artist.name, if (s.isAi(artist.name)) "Artist · marked as AI (hidden from suggestions)" else "Artist", picture, round = true, back = back) {
                val following = s.isFollowed(artist)
                // Following stands out (filled, with a check); Follow is an outline with a plus.
                if (following) Button(onClick = { s.toggleFollow(artist.copy(picture = picture)) }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)); Text(" Following")
                } else OutlinedButton(onClick = { s.toggleFollow(artist.copy(picture = picture)) }) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp), tint = TextMain); Text(" Follow", color = TextMain)
                }
                Spacer(Modifier.width(6.dp))
                TextButton(onClick = { s.toggleBlocked(artist.name) }) { Text(if (s.isBlocked(artist.name)) "Suggest again" else "Don't suggest", color = TextDim) }
                if (!s.onAiList(artist.name)) TextButton(onClick = { s.toggleAi(artist.name) }) { Text(if (s.isAi(artist.name)) "Not AI after all" else "This is AI", color = TextDim) }
                Spacer(Modifier.width(6.dp))
                popular.firstOrNull()?.let { s.item(it) }?.let { first ->
                    OutlinedButton(onClick = { ctx.radio(first, artist.name) }) { Icon(Icons.Rounded.Radio, null, tint = Accent); Text(" Radio", color = TextMain) }
                    Spacer(Modifier.width(6.dp))
                }
                if (popular.isNotEmpty()) Button(onClick = { ctx.playRemote(popular, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play popular") }
            }
        }
        val mine = ctx.state.library.count { Match.norm(it.artist) == Match.norm(artist.name) }
        if (mine > 0) item {
            Row(Modifier.fillMaxWidth().clickable { ctx.push(View.LibraryArtist(artist.name)) }.padding(24.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LibraryMusic, null, tint = Accent, modifier = Modifier.size(32.dp))
                Column(Modifier.padding(start = 16.dp).weight(1f)) {
                    Text("Your songs", color = TextMain)
                    Text("$mine song${if (mine == 1) "" else "s"} by ${artist.name} in your library", color = TextDim, fontSize = 12.sp)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = TextDim)
            }
        }
        if (top == null) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(24.dp), color = Accent, trackColor = Raised) }
        if (popular.isNotEmpty()) item { Section("Popular") }
        items(popular.size) { i -> RemoteRow(ctx, popular, i, numbered = true) }
        if (releases.isNotEmpty()) {
            item { Section("Releases") }
            item {
                ScrollRow {
                    items(releases) { a -> Card(a.cover, a.title, listOfNotNull(a.type?.replaceFirstChar { it.uppercase() }, a.year).joinToString(" · ")) { ctx.push(View.CatalogAlbum(a)) } }
                }
            }
        }
    }
}

@Composable
private fun CatalogAlbumPage(ctx: Ctx, album: RemoteAlbum, back: (() -> Unit)?) {
    var info by remember(album) { mutableStateOf(album) }
    var tracks by remember(album) { mutableStateOf<List<RemoteTrack>?>(null) }
    LaunchedEffect(album) {
        // Free albums (Internet Archive) open from their own catalog; everything else from Deezer.
        runCatching { if (album.source == io.github.realkhfan.onda.core.Source.ARCHIVE) FreeCatalogs.archiveItem(album.ref) else Deezer.album(album.id) }
            .getOrNull()?.let { (a, t) -> info = a; tracks = t } ?: run { tracks = emptyList() }
    }
    val list = tracks.orEmpty()
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header(info.title, listOfNotNull("Album · ${info.artist}", info.date).joinToString(" · "), info.cover, back = back) {
                val saved = ctx.state.isSaved(info)
                TextButton(onClick = { ctx.state.toggleAlbum(info) }) { Text(if (saved) "Saved to your library" else "Save to your library", color = if (saved) Accent else TextDim) }
                if (info.artistId > 0) TextButton(onClick = { ctx.push(View.CatalogArtist(RemoteArtist(info.artistId, info.artist, null))) }) { Text("Go to artist", color = Accent) }
                if (list.isNotEmpty()) DownloadAllRemote(ctx, list)
                if (list.isNotEmpty()) Button(onClick = { ctx.playRemote(list, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
            }
        }
        if (tracks == null) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(24.dp), color = Accent, trackColor = Raised) }
        items(list.size) { i -> RemoteRow(ctx, list, i, numbered = true) }
    }
}

@Composable
private fun SettingsPage(ctx: Ctx) {
    val state = ctx.state
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // only for connecting; never saved
    var connecting by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var typedFolder by remember { mutableStateOf("") }
    var folderNote by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Header("Settings", "Onda for Linux · no account, no tracking") }
        item { Section("Profile picture") }
        item {
            var note by remember { mutableStateOf<String?>(null) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProfilePic(state.settings.picture, 64.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("A photo or a GIF; a GIF keeps playing while Onda's window is the active one.", color = TextDim, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                when (val r = FolderPicker.file(false, System.getProperty("user.home") + "/Pictures", "Profile picture")) {
                                    is FolderPicker.Result.Picked -> note = if (state.setPicture(File(r.path))) null else "Pick a JPG, PNG, WebP or GIF up to 20 MB."
                                    FolderPicker.Result.NoPicker -> note = "No file picker found on this system."
                                    FolderPicker.Result.Cancelled -> {}
                                    is FolderPicker.Result.Remote -> note = r.message
                                }
                            }
                        }) { Text(if (state.settings.picture == null) "Choose" else "Change", color = TextMain) }
                        if (state.settings.picture != null) TextButton(onClick = { note = null; state.setPicture(null) }) { Text("Remove", color = TextDim) }
                    }
                    note?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
                }
            }
        }
        item { Section("Music folders") }
        items(state.settings.folders) { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f, color = TextMain, modifier = Modifier.weight(1f))
                Tip("Remove $f") { IconButton(onClick = { state.removeFolder(f) }) { Icon(Icons.Rounded.Close, "Remove $f", tint = TextDim) } }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            when (val r = FolderPicker.pick(System.getProperty("user.home") + "/Music")) {
                                is FolderPicker.Result.Picked -> { state.addFolder(r.path); folderNote = null }
                                FolderPicker.Result.Cancelled -> {}
                                FolderPicker.Result.NoPicker -> folderNote = "No folder picker found on this system. Type the folder's path below."
                                is FolderPicker.Result.Remote -> folderNote = r.message
                            }
                        }
                    }) { Text("Add folder") }
                    OutlinedButton(onClick = { state.rescan() }, enabled = !state.scanning) { Text("Rescan") }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextBox(typedFolder, { typedFolder = it }, label = { Text("Or type a folder path") },
                        placeholder = { Text("~/Music") }, singleLine = true, modifier = Modifier.width(420.dp))
                    OutlinedButton(onClick = {
                        val path = FolderPicker.resolve(typedFolder)
                        if (path != null) { state.addFolder(path); typedFolder = ""; folderNote = null } else folderNote = "That folder doesn't exist."
                    }, enabled = typedFolder.isNotBlank()) { Text("Add") }
                }
                folderNote?.let { Text(it, color = TextDim, fontSize = 13.sp) }
            }
        }
        item { Section("Music server (Navidrome)") }
        item {
            val cfg = state.settings.server
            if (cfg != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connected to ${cfg.url} as ${cfg.user} · ${state.serverSongs.size} songs, ${state.serverPlaylists.size} playlists", color = TextMain)
                    state.serverNote?.let { Text(it, color = TextDim) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { state.sync() }, enabled = !state.syncing) { Text("Sync now") }
                        TextButton(onClick = { state.disconnect() }) { Text("Disconnect", color = TextDim) }
                    }
                    Text("Streaming quality", color = TextDim)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Original", 320 to "320 kbps", 192 to "192 kbps", 128 to "128 kbps").forEach { (k, label) ->
                            FilterChip(state.settings.kbps == k, { state.setKbps(k) }, { Text(label) })
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextBox(url, { url = it }, label = { Text("Server address") }, placeholder = { Text("http://192.168.1.20:4533") }, singleLine = true, modifier = Modifier.width(420.dp))
                    TextBox(user, { user = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.width(420.dp))
                    TextBox(password, { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.width(420.dp),
                        visualTransformation = PasswordVisualTransformation())
                    Button(onClick = {
                        connecting = true; problem = null
                        scope.launch { problem = state.connect(url, user, password); password = ""; connecting = false }
                    }, enabled = !connecting && url.isNotBlank() && user.isNotBlank() && password.isNotEmpty()) { Text(if (connecting) "Connecting…" else "Connect") }
                    problem?.let { Text(it, color = ErrorRed) }
                    Text("Onda keeps a login token, not your password.", color = TextDim, fontSize = 12.sp)
                }
            }
        }
        item { Section("Match metadata online") }
        item {
            val m by Matcher.state.collectAsState()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Looks your songs up on Deezer. You review each change (Keep or Replace); songs without a match keep their own " +
                    "details, and artist photos are fetched too. Your audio files are never changed.", color = TextDim, fontSize = 13.sp)
                if (m.running) Button(onClick = { ctx.push(View.Match) }) { Text("Matching… view progress") }
                else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { Matcher.start(state.library, onlyNew = true, apple = state.settings.apple); ctx.push(View.Match) }) { Text("Match new songs") }
                    OutlinedButton(onClick = { Matcher.start(state.library, onlyNew = false, apple = state.settings.apple); ctx.push(View.Match) }) { Text("Match all songs") }
                    if (m.proposals.isNotEmpty()) OutlinedButton(onClick = { ctx.push(View.Match) }) { Text("Review ${m.proposals.size}") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { ctx.push(View.Rename) }) { Text("Rename files from tags") }
                    OutlinedButton(onClick = { ctx.push(View.Duplicates) }) { Text("Find duplicates") }
                }
                Text("Names your files “Artist - Title”. You see every change first, and can undo it.", color = TextDim, fontSize = 12.sp)
            }
        }
        item { Section("Import from Spotify") }
        item {
            val imp = SpotifyImports.state
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("In Spotify: Account → Privacy settings → Download your data → Account data. Spotify emails you a ZIP a few days " +
                    "later; pick it here. Exportify CSV files work too. Nothing is sent to Spotify.", color = TextDim, fontSize = 13.sp)
                if (imp.running) {
                    Text(if (imp.total == 0) "Reading the export…" else "Finding songs: ${imp.done} of ${imp.total}", color = TextMain, fontSize = 13.sp)
                    LinearProgressIndicator({ if (imp.total == 0) 0f else imp.done.toFloat() / imp.total }, Modifier.width(420.dp), color = Accent, trackColor = Raised)
                } else {
                    imp.result?.let { Text(it, color = TextMain, fontSize = 13.sp) }
                    imp.error?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
                    OutlinedButton(onClick = {
                        scope.launch {
                            when (val r = FolderPicker.file(false, System.getProperty("user.home") + "/Downloads", "Spotify export (ZIP, JSON or CSV)")) {
                                is FolderPicker.Result.Picked -> state.launch { SpotifyImports.run(File(r.path), state) }
                                FolderPicker.Result.NoPicker -> ctx.toast("No file picker found on this system.")
                                FolderPicker.Result.Cancelled -> {}
                                is FolderPicker.Result.Remote -> ctx.toast(r.message)
                            }
                        }
                    }) { Text("Choose Spotify export") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            when (val r = FolderPicker.file(false, System.getProperty("user.home") + "/Music", "Playlist file (M3U)")) {
                                is FolderPicker.Result.Picked -> {
                                    val (name, n) = withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { state.importPlaylistFile(File(r.path)) }.getOrDefault("" to -1) }
                                    ctx.toast(when (n) { -1 -> "Couldn't read that file."; 0 -> "None of its songs are in your library."; else -> "Playlist “$name”: $n songs" })
                                }
                                FolderPicker.Result.NoPicker -> ctx.toast("No file picker found on this system.")
                                FolderPicker.Result.Cancelled -> {}
                                is FolderPicker.Result.Remote -> ctx.toast(r.message)
                            }
                        }
                    }) { Text("Import playlist file (M3U)") }
                }
            }
        }
        item { Section("Downloads") }
        item {
            val dl = Downloads.state
            val target = Downloads.target(state)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (target != null) "Saved as music files in “${Downloads.FOLDER}” inside $target, so they're part of your library and play " +
                    "offline. Songs from your music server, and free songs whose artist allows it. Right-click a song → Download."
                else "Add a music folder first: downloads are saved as files there.", color = TextDim, fontSize = 13.sp)
                if (dl.running) {
                    Text("Downloading ${minOf(dl.done + 1, dl.total)} of ${dl.total}: ${dl.current.orEmpty()}", color = TextMain, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    LinearProgressIndicator({ dl.done.toFloat() / dl.total.coerceAtLeast(1) }, Modifier.width(420.dp), color = Accent, trackColor = Raised)
                } else if (dl.paused) Text("Paused without a connection: ${dl.total - dl.done} left, they continue the next time you open Onda.", color = TextMain, fontSize = 13.sp)
            }
        }
        item { Section("Equalizer") }
        item { EqualizerSettings(ctx) }
        item { Section("Display") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Size", color = TextMain, modifier = Modifier.width(120.dp))
                    OutlinedButton(onClick = { state.setZoom(state.settings.zoom - 0.1f) }) { Text("−") }
                    Text("${Math.round(state.settings.zoom * 100)} %", color = TextMain, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                    OutlinedButton(onClick = { state.setZoom(state.settings.zoom + 0.1f) }) { Text("+") }
                    TextButton(onClick = { state.setZoom(1f) }) { Text("Reset", color = TextDim) }
                    Text("Also Ctrl + and Ctrl −", color = TextDim, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Colour", color = TextMain, modifier = Modifier.width(120.dp))
                    accents.forEach { (name, argb) ->
                        Tip(name) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(Color(argb.toInt()))
                                .then(if (state.settings.accent == argb) Modifier.border(2.dp, TextMain, CircleShape) else Modifier)
                                .clickable { state.setAccent(argb) })
                        }
                    }
                }
                Toggle("Smaller sidebar", "Only icons (hover for names). Also Ctrl+B.", state.settings.compact, state::setCompact)
                if (state.hiddenSongs > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${state.hiddenSongs} songs hidden from your library", color = TextMain, modifier = Modifier.width(300.dp))
                    TextButton(onClick = { state.showHiddenSongs() }) { Text("Show them again", color = Accent) }
                }
                Toggle("Draw without the graphics card", "Turn on if Onda doesn't show up while a game is running. Takes effect the next time Onda starts.",
                    state.settings.software, state::setSoftware)
            }
        }
        item { Section("Playback and discovery") }
        if (state.settings.server != null) item {
            Column {
                Toggle("Playback cache", "Keeps server songs you play (up to 1 GB), so replays and seeking need no network.", state.settings.cache, state::setCache)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("%,d MB of 1,024 MB used".format(SongCache.bytes / 1_048_576), color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                    if (SongCache.bytes > 0) TextButton(onClick = { SongCache.clear() }) { Text("Clear", color = Accent, fontSize = 12.sp) }
                }
            }
        }
        item { Toggle("Gapless playback", "No pause between songs, so live albums and DJ mixes flow on. For your files and server songs.", state.settings.gapless) { state.setGapless(it); ctx.player?.gapless = it } }
        item {
            // The same setting as on the phone.
            var cf by remember { mutableIntStateOf(state.settings.crossfade) }
            Column(Modifier.width(520.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Crossfade", color = TextMain, modifier = Modifier.weight(1f))
                    Text(if (cf == 0) "Off" else "$cf s", color = if (cf == 0) TextDim else Accent)
                }
                Slider(cf.toFloat(), { cf = it.toInt() }, valueRange = 0f..12f, steps = 11,
                    onValueChangeFinished = { state.setCrossfade(cf); ctx.player?.crossfade = cf },
                    colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Raised))
                Text("The next song fades in while this one fades out. Songs of the same album play without a gap, as they were made.", color = TextDim, fontSize = 12.sp)
            }
        }
        item { Toggle("Start Onda when you log in", "Starts in the background, ready for media keys; open it from the app menu.", state.autostart) { state.setAutostart(it) } }
        item { Toggle("Song notifications", "A desktop notification with the cover when a new song starts and Onda isn't in front.", state.settings.songNotify, state::setSongNotify) }
        item { Toggle("Similar songs when the queue ends", "Keeps the music going with songs like the last ones.", state.settings.autoplay, state::setAutoplay) }
        item { Toggle("Keep playing when the window is closed", "Reopen Onda from the app menu or your desktop's media controls.", state.settings.background, state::setBackground) }
        item { Toggle("New release notifications", "A desktop notification when an artist you listen to releases something.", state.settings.releaseAlerts, state::setReleaseAlerts) }
        item {
            Column {
                Toggle("Normalize volume", "Every song at about the same loudness. Each song is measured once, in the background; your files aren't changed.",
                    state.settings.normalize) { state.setNormalize(it); ctx.player?.refreshVolume() }
                val l = Loudness.state
                Text(if (l.running) "Measuring loudness: ${l.done} of ${l.total} songs…" else "${Loudness.gains.size} songs measured. Songs not measured yet (and previews) play at your typical level.",
                    color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }
        if (state.settings.normalize) item {
            Toggle("Keep album dynamics", "Evens out whole albums instead of single songs, so an album's quiet intro stays quiet. Shuffled songs still match.",
                state.settings.albumGain) { state.setAlbumGain(it); ctx.player?.refreshVolume() }
        }
        item { Toggle("ListenBrainz suggestions", "Adds similar artists from ListenBrainz's open listening data to your mixes and similar songs. Only artist names are sent.", state.settings.listenBrainz, state::setListenBrainz) }
        item { Section("Your phone") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Toggle("Link with Onda on your phone", "Your phone syncs playlists, likes, Want to listen, follows, history and edited details with this PC, " +
                    "and can play here or take over what plays here. Over your Wi-Fi only, encrypted; only phones you pair get in.", Link.on) { Link.turn(it) }
                Link.problem?.let { Text(it, color = TextDim, fontSize = 12.sp) }
                if (Link.on) {
                    Text("This PC: ${Link.name}" + Link.addresses().takeIf { it.isNotEmpty() }?.joinToString(", ", " · ") .orEmpty() +
                        " (port ${io.github.realkhfan.onda.core.LinkCrypto.PORT})", color = TextDim, fontSize = 12.sp)
                    val code = Link.code
                    if (code == null) OutlinedButton(onClick = { Link.newCode() }) { Text("Pair a phone") }
                    else Column {
                        Text(code.chunked(3).joinToString(" "), color = Accent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text("On the phone: Settings → Your PC → Link, then type this code. It works once, for 2 minutes.", color = TextDim, fontSize = 12.sp)
                    }
                    Link.phones.forEach { ph ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PhoneAndroid, null, tint = if (Link.connected(ph)) Accent else TextDim)
                            Text(ph.name + if (Link.connected(ph)) " · connected" else "", color = TextMain, modifier = Modifier.padding(start = 10.dp).width(300.dp))
                            // Both ways at once (it merges; nothing is replaced): the phone syncs within seconds.
                            if (Link.connected(ph)) TextButton(onClick = { Link.syncNow(ph); ctx.toast("Syncing with ${ph.name}…") }) { Text("Sync now", color = Accent) }
                            TextButton(onClick = { Link.forget(ph) }) { Text("Unpair", color = TextDim) }
                        }
                    }
                    Link.receiving.values.forEach { Text("Getting from your phone: $it", color = TextDim, fontSize = 12.sp) }
                    if (Link.lastSync > 0) Text("Last synced ${java.time.Instant.ofEpochMilli(Link.lastSync).atZone(java.time.ZoneId.systemDefault()).toLocalTime().withNano(0)}",
                        color = TextDim, fontSize = 12.sp)
                }
            }
        }
        item { Section("Backup") }
        item {
            var note by remember { mutableStateOf<String?>(null) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Playlists, likes, history, follows, saved albums and settings in one file. Not your server login or folders.", color = TextDim, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // With a daily-backup folder: straight there, no dialog ("Save as…" picks another place).
                    if (state.settings.backupDir != null) OutlinedButton(onClick = {
                        scope.launch {
                            note = runCatching { "Saved to " + kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { state.backupToFolder() }.path }
                                .getOrElse { "Couldn't save the backup there (is the folder reachable?)." }
                        }
                    }) { Text("Back up now") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            val name = "onda-backup-${java.time.LocalDate.now()}.zip"
                            val path = when (val r = FolderPicker.file(true, System.getProperty("user.home") + "/" + name, "Save backup")) {
                                is FolderPicker.Result.Picked -> r.path
                                FolderPicker.Result.Cancelled -> return@launch
                                FolderPicker.Result.NoPicker -> System.getProperty("user.home") + "/" + name
                                is FolderPicker.Result.Remote -> { note = r.message; return@launch }
                            }
                            note = runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { state.backup(File(path)) }; "Saved to $path" }
                                .getOrElse { "Couldn't save the backup." }
                        }
                    }) { Text(if (state.settings.backupDir != null) "Save as…" else "Back up now") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            val r = FolderPicker.file(false, System.getProperty("user.home"), "Restore a backup")
                            if (r is FolderPicker.Result.Picked) note = if (runCatching { state.restore(File(r.path)) }.getOrDefault(false))
                                "Restored. Your previous data is kept in before-restore.zip." else "That file isn't an Onda backup."
                            if (r is FolderPicker.Result.NoPicker) note = "No file picker found on this system."
                            if (r is FolderPicker.Result.Remote) note = r.message
                        }
                    }) { Text("Restore") }
                }
                note?.let { Text(it, color = TextMain, fontSize = 13.sp) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.settings.backupDir?.let { "Daily backup: $it (newest 5 kept)" } ?: "Daily backup: off", color = TextMain, fontSize = 13.sp)
                    TextButton(onClick = {
                        scope.launch {
                            when (val r = FolderPicker.pick(state.settings.backupDir ?: System.getProperty("user.home"))) {
                                is FolderPicker.Result.Picked -> { state.setBackupDir(r.path); state.launch { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { state.dailyBackup() } } }
                                FolderPicker.Result.NoPicker -> note = "No folder picker found on this system."
                                FolderPicker.Result.Cancelled -> {}
                                is FolderPicker.Result.Remote -> note = r.message
                            }
                        }
                    }) { Text(if (state.settings.backupDir == null) "Choose folder" else "Change", color = Accent) }
                    if (state.settings.backupDir != null) TextButton(onClick = { state.setBackupDir(null) }) { Text("Off", color = TextDim) }
                }
            }
        }
        item {
            var note by remember { mutableStateOf<String?>(null) }
            var asking by remember { mutableStateOf<String?>(null) } // a folder was picked: which songs go there first?
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Copy new songs to a folder: every 30 minutes, songs that are new in your music folders are copied there, " +
                    "in the same folders (Artist/Album). Point your own backup at it to get them onto your phone. Nothing there is deleted or replaced, " +
                    "and songs your phone sent aren't copied back.", color = TextDim, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.settings.copyDir?.let { "Copy new songs to: $it" } ?: "Copy new songs: off", color = TextMain, fontSize = 13.sp)
                    TextButton(onClick = {
                        scope.launch {
                            when (val r = FolderPicker.pick(state.settings.copyDir ?: System.getProperty("user.home"))) {
                                is FolderPicker.Result.Picked -> {
                                    note = null
                                    if (state.settings.folders.any { File(r.path).absoluteFile.startsWith(File(it).absoluteFile) })
                                        note = "Pick a folder outside your music folders, or every song would show up twice."
                                    else asking = r.path
                                }
                                FolderPicker.Result.NoPicker -> note = "No folder picker found on this system."
                                FolderPicker.Result.Cancelled -> {}
                                is FolderPicker.Result.Remote -> note = r.message
                            }
                        }
                    }) { Text(if (state.settings.copyDir == null) "Choose folder" else "Change", color = Accent) }
                    if (state.settings.copyDir != null) {
                        TextButton(onClick = { note = null; state.launch { state.copyNewSongs() } }) { Text("Check now", color = Accent) }
                        TextButton(onClick = { state.setCopyDir(null) }) { Text("Off", color = TextDim) }
                    }
                }
                (note ?: state.copyNote)?.let { Text(it, color = TextMain, fontSize = 13.sp) }
            }
            asking?.let { dir ->
                AlertDialog(onDismissRequest = { asking = null }, title = { Text("Which songs?") },
                    text = { Text("Songs are only copied from your music folders on this PC into $dir; nothing comes back here, and nothing " +
                        "there is deleted or replaced.\n\nAll songs: your songs that aren't there yet are copied now, then new ones every 30 minutes. " +
                        "Only new ones: if that folder has your music already, only songs you add from now on are copied.") },
                    confirmButton = { TextButton(onClick = { state.setCopyDir(dir, everything = true); asking = null }) { Text("All songs", color = Accent) } },
                    dismissButton = { TextButton(onClick = { state.setCopyDir(dir); asking = null }) { Text("Only new ones", color = Accent) } })
            }
        }
        item { Section("Keyboard") }
        item {
            Text("Space play/pause · Ctrl+→ / Ctrl+← next / previous · → / ← seek 5 s · ↑ / ↓ volume · Ctrl+L like · Ctrl+F search · Ctrl+, settings · Ctrl + / Ctrl − size · Esc back · Ctrl+click or Shift+click selects several songs · F1 all shortcuts",
                color = TextDim, fontSize = 13.sp)
        }
        item { Section("Privacy") }
        item {
            Text("Search and previews come from Deezer's public API, free full songs from Openverse and Audius, lyrics from LRCLIB. They see " +
                "search words or song names and your IP address, nothing else. Media keys and the desktop's media controls only see what's playing.",
                color = TextDim, fontSize = 13.sp)
        }
        item { Toggle("Also search Apple's catalog", "Finds songs Deezer is missing. Apple then also sees your search words and IP address.", state.settings.apple, state::setApple) }
    }
}

/* ---------- Side panels ---------- */

@Composable
private fun PanelFrame(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.width(360.dp).fillMaxHeight().background(Side)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            Tip("Close $title") { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close $title", tint = TextDim) } }
        }
        content()
    }
}

@Composable
private fun QueuePanel(player: Player, recent: List<Player.Item>, onClear: () -> Unit, onSave: () -> Unit, onClose: () -> Unit) = PanelFrame("Queue", onClose) {
    val upcoming = player.upcoming()
    val reorder = remember { ReorderState() }
    LazyColumn {
        // What played before, to go back to (it plays now; the queue after it stays).
        if (recent.isNotEmpty()) {
            item { Section("Recently played") }
            items(recent) { r -> QueueRow(r, false, Modifier.alpha(0.6f)) { player.playNext(r); player.next() } }
        }
        if (player.queue.size > 1) item { TextButton(onClick = onSave, modifier = Modifier.padding(start = 8.dp)) { Text("Save queue as playlist", color = Accent) } }
        player.current?.let { c -> item { Section("Now playing") }; item { QueueRow(c, true, onClick = {}) } }
        if (upcoming.isNotEmpty()) item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Section("Next")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClear) { Text("Clear", color = Accent) }
            }
        }
        items(upcoming.size) { k ->
            val (i, it) = upcoming[k]
            QueueRow(it, false, Modifier.reorderRow(reorder, k, upcoming.size), onRemove = { player.removeAt(i) },
                handle = { DragHandle(reorder, k, upcoming.size, it.title) { from, to -> player.moveUpcoming(from, to) } }) { player.jumpTo(i) }
        }
    }
}

/** Your phone's queue while this PC controls it: the same list, changed on the phone. */
@Composable
private fun PhoneQueuePanel(n: Link.PhoneNow, onClose: () -> Unit) = PanelFrame("Queue on ${n.name}", onClose) {
    fun item(o: org.json.JSONObject) = Player.Item(o.optString("key"), o.optString("title"), o.optString("artist"), o.optString("album"),
        o.optString("art").takeIf { it.startsWith("http") }, "", durationMs = o.optLong("dur"))
    val next = n.s.optJSONArray("next")?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } }.orEmpty()
    val reorder = remember { ReorderState() }
    LazyColumn {
        item { Section("Now playing") }
        item { QueueRow(item(n.s), true, onClick = {}) }
        if (next.isNotEmpty()) item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Section("Next")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { Link.phoneCommand("clear") }) { Text("Clear", color = Accent) }
            }
        }
        items(next.size) { k ->
            val o = next[k]
            QueueRow(item(o), false, Modifier.reorderRow(reorder, k, next.size), onRemove = { Link.phoneCommand("remove") { put("i", k) } },
                handle = { DragHandle(reorder, k, next.size, o.optString("title")) { from, to -> Link.phoneCommand("move") { put("from", from); put("to", to) } } }) {
                Link.phoneCommand("jump") { put("i", k) }
            }
        }
    }
}

@Composable
private fun QueueRow(it: Player.Item, now: Boolean, modifier: Modifier = Modifier, onRemove: (() -> Unit)? = null,
                     handle: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Art(it.art, 36)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(it.title, color = if (now) Accent else TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
            Text(it.artist, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        }
        if (onRemove != null) Tip("Remove ${it.title} from the queue") { IconButton(onClick = onRemove, Modifier.size(36.dp)) { Icon(Icons.Rounded.Close, "Remove ${it.title} from the queue", tint = TextDim) } }
        handle?.invoke()
    }
}

/** [shift]: ms the lines are moved (+ = shown earlier), when the found timing is off. */
private class LyricsState(val full: Boolean, val lyrics: Lrclib.Lyrics?, val failed: Boolean, val shift: Long = 0)

/** Synced lyrics from LRCLIB for the song playing (full songs only). */
@Composable
private fun rememberLyrics(state: AppState, player: Player): LyricsState {
    val cur = player.current
    val full = cur != null && (!cur.preview || player.fullFrom != null)
    val (picked, shift) = cur?.let { state.lyricsPick(it.id) } ?: (0L to 0L)
    var lyrics by remember(cur?.id, full, picked) { mutableStateOf<Lrclib.Lyrics?>(null) }
    var failed by remember(cur?.id, full, picked) { mutableStateOf(false) }
    LaunchedEffect(cur?.id, full, picked) {
        if (cur == null || !full) return@LaunchedEffect
        val dur = ((cur.durationMs.takeIf { it > 0 } ?: player.durationMs) / 1000).toInt()
        lyrics = runCatching { if (picked > 0) Lrclib.byId(picked) else Lrclib.get(cur.title, cur.artist, cur.album, dur) }.getOrNull()
        failed = lyrics == null
    }
    return LyricsState(full, lyrics, failed, shift)
}

/** Timing shift and "Other lyrics": fix lyrics that are early, late, or for another song. */
@Composable
private fun LyricsTools(state: AppState, player: Player, ls: LyricsState, big: Boolean) {
    val cur = player.current ?: return
    if (!ls.full) return
    val (picked, shift) = state.lyricsPick(cur.id)
    var choosing by remember { mutableStateOf(false) }
    val tint = TextMain.copy(alpha = 0.8f) // on the cover's colour
    Row(Modifier.padding(if (big) 44.dp else 12.dp, 0.dp, 12.dp, 0.dp), verticalAlignment = Alignment.CenterVertically) {
        if (ls.lyrics?.synced?.isNotEmpty() == true) {
            TextButton(onClick = { state.setLyricsPick(cur.id, picked, shift + 500) }) { Text("Earlier", color = tint, fontSize = 12.sp) }
            Text(if (shift == 0L) "in time" else "%+.1f s".format(shift / 1000.0), color = tint, fontSize = 12.sp)
            TextButton(onClick = { state.setLyricsPick(cur.id, picked, shift - 500) }) { Text("Later", color = tint, fontSize = 12.sp) }
        }
        TextButton(onClick = { choosing = true }) { Text("Other lyrics…", color = tint, fontSize = 12.sp) }
        ls.lyrics?.let { l ->
            val text = l.synced.takeIf { it.isNotEmpty() }?.joinToString("\n") { it.text } ?: l.plain
            if (!text.isNullOrBlank()) TextButton(onClick = {
                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection("${cur.title} — ${cur.artist}\n\n$text"), null)
            }) { Text("Copy", color = tint, fontSize = 12.sp) }
        }
        TextButton(onClick = { state.setLyricsScale(state.settings.lyricsScale - 0.1f) }) { Text("A−", color = tint, fontSize = 12.sp) }
        TextButton(onClick = { state.setLyricsScale(state.settings.lyricsScale + 0.1f) }) { Text("A+", color = tint, fontSize = 14.sp) }
    }
    if (choosing) LyricsChoiceDialog(state, cur, picked, shift) { choosing = false }
}

@Composable
private fun LyricsChoiceDialog(state: AppState, cur: Player.Item, picked: Long, shift: Long, close: () -> Unit) {
    var title by remember { mutableStateOf(cur.title) }
    var artist by remember { mutableStateOf(cur.artist) }
    var found by remember { mutableStateOf<List<Lrclib.Choice>?>(null) }
    val scope = rememberCoroutineScope()
    fun search() { found = null; scope.launch { found = runCatching { Lrclib.choices(title, artist) }.getOrDefault(emptyList()) } }
    LaunchedEffect(Unit) { search() }
    AlertDialog(onDismissRequest = close, title = { Text("Other lyrics") }, modifier = Modifier.width(640.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextBox(title, { title = it }, label = { Text("Title") }, onEnter = ::search, modifier = Modifier.weight(1f))
                    TextBox(artist, { artist = it }, label = { Text("Artist") }, onEnter = ::search, modifier = Modifier.weight(1f))
                }
                Text("Pick the version that fits your song; the length helps.${if (cur.durationMs > 0) " Your song: ${time(cur.durationMs)}." else ""}", color = TextDim, fontSize = 12.sp)
                LazyColumn(Modifier.height(320.dp)) {
                    val list = found
                    if (list == null) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent, trackColor = Raised) }
                    else if (list.isEmpty()) item { Text("Nothing found. Try other words.", color = TextDim, modifier = Modifier.padding(8.dp)) }
                    else items(list) { c ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (c.id == picked) Raised else Color.Transparent)
                            .clickable { state.setLyricsPick(cur.id, c.id, shift); close() }.padding(8.dp)) {
                            Text("${c.track} — ${c.artist}", color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOf(c.album, time(c.durationSec * 1000L), if (c.lyrics.synced.isNotEmpty()) "synced" else "not synced").filter { it.isNotBlank() }.joinToString(" · "),
                                color = TextDim, fontSize = 12.sp)
                            Text((c.lyrics.synced.firstOrNull { it.text.isNotBlank() }?.text ?: c.lyrics.plain?.lineSequence()?.firstOrNull { it.isNotBlank() }).orEmpty(),
                                color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = close) { Text("Close", color = TextDim) } },
        dismissButton = if (picked > 0) ({ TextButton(onClick = { state.setLyricsPick(cur.id, 0, shift); close() }) { Text("Use the one found", color = Accent) } }) else null)
}

/** The lines; the current one is bigger and follows the song. Click a line to jump there. */
@Composable
private fun LyricsLines(state: AppState, player: Player, ls: LyricsState, big: Boolean) {
    val lines = ls.lyrics?.synced.orEmpty()
    val pos = rememberSmoothPosition(player)
    // Lit a moment early, as the line starts; recomposes only when the line changes, not every frame.
    val active by remember(lines, ls.shift) { derivedStateOf { if (lines.isEmpty()) -1 else Lrc.indexAt(lines, pos.longValue + ls.shift + 150) } }
    val list = rememberLazyListState()
    LaunchedEffect(active) {
        if (active < 0) return@LaunchedEffect
        // Glide so the sung line sits a third down; far away (a seek): jump there first.
        val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == active }
            ?: return@LaunchedEffect list.scrollToItem((active - if (big) 3 else 2).coerceAtLeast(0))
        val target = (list.layoutInfo.viewportSize.height * 0.3f).toInt()
        list.animateScrollBy((item.offset - target).toFloat(), tween(550, easing = FastOutSlowInEasing))
    }
    val k = state.settings.lyricsScale
    // One size for every line, so nothing below moves when the line changes; the sung one grows a little on top.
    val size = if (big) (34 * k).sp else (22 * k).sp
    val l = ls.lyrics
    val hint = when {
        l == null || l.instrumental -> null
        lines.isNotEmpty() -> if (l.verified) null else "These lyrics may be from another version of this song."
        l.plain != null -> if (l.verified) "These lyrics aren't synced to the song." else "These lyrics aren't synced and may be from another version of this song."
        else -> null
    }
    Column {
    hint?.let { Text(it, color = TextMain.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(if (big) 52.dp else 20.dp, 0.dp, 16.dp, 4.dp)) }
    LyricsTools(state, player, ls, big)
    when {
        player.current == null -> Note("Nothing is playing.")
        !ls.full -> Note("Lyrics are shown for full songs.")
        ls.failed -> Note("Couldn't load lyrics. Check your connection.")
        ls.lyrics == null -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(24.dp), color = Accent, trackColor = Raised)
        ls.lyrics.instrumental -> Note("Instrumental")
        lines.isNotEmpty() -> LazyColumn(state = list, contentPadding = PaddingValues(if (big) 48.dp else 16.dp, 8.dp, 16.dp, 240.dp)) {
            itemsIndexed(lines) { i, l ->
                // Like the phone: sung white, sung before dim, still to come in between; fading, not switching.
                val color by animateColorAsState(when { i == active -> TextMain; i < active -> TextMain.copy(alpha = 0.45f); else -> TextMain.copy(alpha = 0.7f) }, tween(300))
                val grow by animateFloatAsState(if (i == active) 1.06f else 1f, tween(300))
                Text(l.text.ifBlank { "♪" }, color = color, fontSize = size, fontWeight = FontWeight.Bold, lineHeight = size * 1.25f,
                    modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = grow; scaleY = grow; transformOrigin = TransformOrigin(0f, 0.5f) }
                        .clip(RoundedCornerShape(6.dp)).clickable { player.seek(l.timeMs - ls.shift) }.padding(vertical = if (big) 10.dp else 6.dp, horizontal = 4.dp))
            }
        }
        ls.lyrics.plain != null -> Text(ls.lyrics.plain, color = TextMain, fontSize = if (big) (28 * k).sp else (18 * k).sp,
            modifier = Modifier.padding(if (big) 48.dp else 16.dp).verticalScroll(rememberScrollState()))
        else -> Note("No lyrics for this song.")
    }
    }
}

/**
 * The play position between the player's own updates (every 0.4 s), moved on each frame while playing, so lyrics change
 * on time. Never more than a second past the player's last word (a stalled stream doesn't run away).
 */
@Composable
private fun rememberSmoothPosition(player: Player): MutableLongState {
    val pos = remember { mutableLongStateOf(player.positionMs) }
    LaunchedEffect(player) {
        var base = -1L; var at = 0L
        while (true) withFrameNanos { now ->
            if (player.positionMs != base) { base = player.positionMs; at = now }
            pos.longValue = if (player.playing) base + ((now - at) / 1_000_000).coerceAtMost(1000) else base
        }
    }
    return pos
}

/** The player's save button: your songs go to Liked Songs, a catalog song (a preview) to Want to listen, as on the phone. */
@Composable
private fun SaveButton(state: AppState, id: String) {
    val r = state.catalogSong(id)
    val saved = if (r != null) state.isWanted(r) else state.isLiked(id)
    val what = if (r != null) "Want to listen" else "Liked Songs"
    Tip(if (saved) "Remove from $what" else "Add to $what") {
        IconButton(onClick = { if (r != null) state.toggleWant(r) else state.toggleLike(id) }) {
            Icon(
                when { r != null && saved -> Icons.Rounded.Bookmark; r != null -> Icons.Rounded.BookmarkBorder; saved -> Icons.Rounded.Favorite; else -> Icons.Rounded.FavoriteBorder },
                if (saved) "Remove from $what" else "Add to $what", tint = if (saved) Accent else TextDim,
            )
        }
    }
}

@Composable
private fun LyricsPanel(state: AppState, player: Player, onExpand: () -> Unit, onClose: () -> Unit) {
    val ls = rememberLyrics(state, player)
    // On the cover's colour, like the full-screen lyrics (and Spotify's).
    Column(Modifier.width(360.dp).fillMaxHeight().background(rememberArtTint(player.current?.art))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Lyrics", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            Tip("Show lyrics full screen") { IconButton(onClick = onExpand) { Icon(Icons.Rounded.OpenInFull, "Show lyrics full screen", tint = TextMain) } }
            Tip("Close lyrics") { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close lyrics", tint = TextMain) } }
        }
        LyricsLines(state, player, ls, big = false)
    }
}

/** Lyrics over the whole content area, on the cover's colour (like Spotify). */
@Composable
private fun LyricsFull(state: AppState, player: Player, onCollapse: () -> Unit, onClose: () -> Unit) {
    val ls = rememberLyrics(state, player)
    val cur = player.current
    val tint = rememberArtTint(cur?.art)
    Column(Modifier.fillMaxSize().background(tint)) {
        Row(Modifier.padding(24.dp, 16.dp, 16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(cur?.title.orEmpty(), color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(cur?.artist.orEmpty(), color = TextMain.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Tip("Back to the side panel") { IconButton(onClick = onCollapse) { Icon(Icons.Rounded.CloseFullscreen, "Back to the side panel", tint = TextMain) } }
            Tip("Close lyrics") { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close lyrics", tint = TextMain) } }
        }
        LyricsLines(state, player, ls, big = true)
    }
}

/**
 * The cover's dominant colour, darkened for white text (the phone's ArtColor), fading from one song's to the next; the
 * accent's dark violet until the first is known.
 */
@Composable
private fun rememberArtTint(art: String?): Color {
    var c by remember { mutableStateOf(Color(0xFF2B2140)) }
    LaunchedEffect(art) {
        if (art == null) return@LaunchedEffect
        val argb = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                // A web cover the window has shown comes from the image cache, without the network.
                val local = if (art.startsWith("/")) art else cachedCover(art)?.removePrefix("file://")
                val img = if (local != null) javax.imageio.ImageIO.read(File(local))
                else java.net.URI(art).toURL().openConnection().apply { setRequestProperty("User-Agent", "Onda"); connectTimeout = 5000; readTimeout = 8000 }
                    .getInputStream().use { javax.imageio.ImageIO.read(it) }
                val small = java.awt.image.BufferedImage(24, 24, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                small.createGraphics().apply { drawImage(img, 0, 0, 24, 24, null); dispose() }
                io.github.realkhfan.onda.core.ArtColor.pick(small.getRGB(0, 0, 24, 24, null, 0, 24))
            }.getOrNull()
        }
        if (argb != null) c = Color(argb)
    }
    return androidx.compose.animation.animateColorAsState(c, androidx.compose.animation.core.tween(600)).value
}

/* ---------- Player bar ---------- */

@Composable
private fun PlayerBar(ctx: Ctx, panel: Panel, toggle: (Panel) -> Unit) {
    val player = ctx.player
    val state = ctx.state
    // Your phone playing (and this PC not): the bar shows it and its buttons control it.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Link.phoneNow != null) { while (Link.phoneNow != null) { now = System.currentTimeMillis(); delay(500) } }
    Link.activePhone(now)?.let { return PhonePlayerBar(ctx, it, now, panel, toggle) }
    val cur = player?.current
    Row(Modifier.fillMaxWidth().height(96.dp).background(Side).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (cur != null) {
                Box(Modifier.clickable { ctx.push(View.NowPlaying) }) { Art(cur.art, 56) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f, fill = false)) {
                    Text(cur.title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    val src = when { player.fullFrom != null -> "${player.fullFrom} · "; cur.preview -> "Preview · "; else -> "" }
                    Text(src + cur.artist, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    player.error?.let { Text(it, color = ErrorRed, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        ?: Text(qualityOf(cur).orEmpty(), color = Accent.copy(alpha = 0.85f), fontSize = 10.sp, maxLines = 1) // keeps its line: nothing jumps when it arrives
                }
                if (state.canSave(cur.id)) SaveButton(state, cur.id)
            }
        }
        Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tip("Shuffle") { IconButton(onClick = { player?.toggleShuffle() }) { Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (player?.shuffle == true) Accent else TextDim) } }
                Tip("Previous") { IconButton(onClick = { player?.previous() }) { Icon(Icons.Rounded.SkipPrevious, "Previous", tint = TextMain) } }
                IconButton(onClick = { player?.toggle() }, Modifier.size(44.dp).clip(CircleShape).background(Accent)) {
                    Icon(if (player?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (player?.playing == true) "Pause" else "Play", tint = Color.Black)
                }
                Tip("Next") { IconButton(onClick = { player?.next() }) { Icon(Icons.Rounded.SkipNext, "Next", tint = TextMain) } }
                IconButton(onClick = { player?.cycleRepeat() }) {
                    Icon(if (player?.repeat == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat", tint = if ((player?.repeat ?: 0) > 0) Accent else TextDim)
                }
            }
            val dur = player?.durationMs ?: 0L
            var dragging by remember { mutableStateOf(false) }
            var dragValue by remember { mutableFloatStateOf(0f) }
            val frac = if (dur > 0) (player?.positionMs ?: 0L).toFloat() / dur else 0f
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(time(player?.positionMs ?: 0), color = TextDim, fontSize = 11.sp)
                ThinSlider(if (dragging) dragValue else frac, "Position", { dragging = true; dragValue = it },
                    onDone = { if (dur > 0) player?.seek((dragValue * dur).toLong()); dragging = false },
                    modifier = Modifier.width(420.dp).padding(horizontal = 8.dp),
                    onWheel = { dy -> player?.let { it.seek((it.positionMs - dy * 5000).toLong().coerceIn(0, dur)) } })
                Text(time(dur), color = TextDim, fontSize = 11.sp)
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            OutputButton(state, player)
            SleepButton(player)
            Tip("Mini player") { IconButton(onClick = { state.mini = !state.mini }) { Icon(Icons.Rounded.PictureInPictureAlt, "Mini player", tint = if (state.mini) Accent else TextDim) } }
            Tip("Lyrics") { IconButton(onClick = { toggle(Panel.LYRICS) }) { Icon(Icons.Rounded.Lyrics, "Lyrics", tint = if (panel == Panel.LYRICS) Accent else TextDim) } }
            Tip("Queue") { IconButton(onClick = { toggle(Panel.QUEUE) }) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Queue", tint = if (panel == Panel.QUEUE) Accent else TextDim) } }
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = TextDim)
            ThinSlider((player?.volume ?: 0.8).toFloat(), "Volume", { player?.changeVolume(it.toDouble()) },
                onDone = { player?.let { state.setVolume(it.volume) } }, modifier = Modifier.width(120.dp).padding(start = 8.dp),
                onWheel = { dy -> player?.let { it.changeVolume((it.volume - dy * 0.05).coerceIn(0.0, 1.0)); state.setVolume(it.volume) } })
        }
    }
}

private val eqPresets = linkedMapOf(
    "Flat" to List(10) { 0.0 },
    "Bass boost" to listOf(6.0, 5.0, 4.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
    "Treble boost" to listOf(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 4.0, 5.0, 6.0),
    "Vocal" to listOf(-2.0, -2.0, -1.0, 1.0, 3.0, 4.0, 3.0, 1.0, 0.0, -1.0),
    "Rock" to listOf(4.0, 3.0, 2.0, 0.0, -1.0, -1.0, 1.0, 3.0, 4.0, 4.0),
    "Electronic" to listOf(5.0, 4.0, 1.0, 0.0, -2.0, 1.0, 0.0, 1.0, 4.0, 5.0),
    "Acoustic" to listOf(3.0, 3.0, 2.0, 1.0, 1.0, 1.0, 2.0, 2.0, 2.0, 1.0),
)

/** 10 bands from 30 Hz to 15 kHz, with presets; moving a band makes it "Custom". */
@Composable
private fun EqualizerSettings(ctx: Ctx) {
    val state = ctx.state
    val player = ctx.player
    var draft by remember(state.settings.eq) { mutableStateOf(state.settings.eq) }
    fun apply(gains: List<Double>, preset: String) { draft = gains; player?.equalize(gains); state.setEqualizer(gains, preset) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (player?.hasEqualizer == false) Text("Your system's GStreamer has no equalizer (it's in gst-plugins-good).", color = ErrorRed, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (eqPresets.keys + "Custom").forEach { name ->
                val on = state.settings.eqPreset == name
                if (on) Button(onClick = {}, contentPadding = PaddingValues(12.dp, 4.dp)) { Text(name, fontSize = 13.sp) }
                else if (name != "Custom") OutlinedButton(onClick = { apply(eqPresets.getValue(name), name) }, contentPadding = PaddingValues(12.dp, 4.dp)) { Text(name, fontSize = 13.sp) }
            }
        }
        listOf("30 Hz", "60 Hz", "120 Hz", "240 Hz", "480 Hz", "950 Hz", "1.9 kHz", "3.8 kHz", "7.5 kHz", "15 kHz").forEachIndexed { i, label ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextDim, fontSize = 12.sp, modifier = Modifier.width(70.dp))
                Slider(draft[i].toFloat(), { v -> draft = draft.toMutableList().also { it[i] = Math.round(v * 2) / 2.0 }; player?.equalize(draft) },
                    valueRange = -12f..12f, onValueChangeFinished = { state.setEqualizer(draft, "Custom") }, modifier = Modifier.width(320.dp),
                    colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Raised))
                Text((if (draft[i] > 0) "+" else "") + "${draft[i]} dB", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** A small window that stays on top: cover, song, controls and progress. */
@Composable
internal fun MiniPlayer(state: AppState, player: Player, openMain: () -> Unit) {
    val cur = player.current
    Row(Modifier.fillMaxSize().background(Side).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Art(cur?.art, 96)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(cur?.title ?: "Nothing playing", color = TextMain, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(cur?.artist.orEmpty(), color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // The line being sung, when the song has synced lyrics.
            val ls = rememberLyrics(state, player)
            val lines = ls.lyrics?.synced.orEmpty()
            Text(when {
                cur == null -> ""
                lines.isNotEmpty() -> lines.getOrNull(Lrc.indexAt(lines, player.positionMs + ls.shift))?.text?.ifBlank { "♪" } ?: "♪"
                ls.lyrics == null && !ls.failed && ls.full -> "Lyrics…"
                else -> "No synced lyrics for this song"
            }, color = if (lines.isNotEmpty()) Accent else TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tip("Previous") { IconButton(onClick = player::previous) { Icon(Icons.Rounded.SkipPrevious, "Previous", tint = TextMain) } }
                IconButton(onClick = player::toggle, Modifier.size(40.dp).clip(CircleShape).background(Accent)) {
                    Icon(if (player.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (player.playing) "Pause" else "Play", tint = Color.Black)
                }
                Tip("Next") { IconButton(onClick = { player.next() }) { Icon(Icons.Rounded.SkipNext, "Next", tint = TextMain) } }
                if (cur != null && state.canSave(cur.id)) SaveButton(state, cur.id)
                Spacer(Modifier.weight(1f))
                Tip("Open Onda") { IconButton(onClick = openMain) { Icon(Icons.Rounded.OpenInFull, "Open Onda", tint = TextDim) } }
            }
            val dur = player.durationMs
            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Raised)) {
                Box(Modifier.fillMaxWidth(if (dur > 0) (player.positionMs.toFloat() / dur).coerceIn(0f, 1f) else 0f).height(3.dp).background(TextMain))
            }
        }
    }
}

private val qualities = HashMap<String, String?>()

/** "FLAC · 24-bit · 96 kHz" for a file you have (read once per song). */
@Composable
private fun qualityOf(item: Player.Item): String? {
    if (!item.uri.startsWith("file:")) return null
    var q by remember(item.id) { mutableStateOf(qualities[item.id]) }
    LaunchedEffect(item.id) {
        if (item.id !in qualities) q = withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { Scanner.audioInfo(File(java.net.URI(item.uri)))?.badge }.getOrNull()
        }.also { qualities[item.id] = it }
    }
    return q
}

/** The player bar while your phone plays: its song, and buttons that control it there. */
@Composable
private fun PhonePlayerBar(ctx: Ctx, n: Link.PhoneNow, now: Long, panel: Panel, toggle: (Panel) -> Unit) {
    val s = n.s
    val title = s.optString("title"); val artist = s.optString("artist")
    // A catalog song has its cover link; for your own songs, the same song's cover here (if it's on this PC too).
    val art = s.optString("art").takeIf { it.startsWith("http") } ?: remember(title, artist) {
        val lib = LinkSync.Library(ctx.state.library, ctx.state.rawTracks, emptyMap())
        ctx.state.byId[lib.id(LinkSync.Song(null, title, artist))]?.artUri
    }
    val dur = s.optLong("dur"); val pos = n.position(now)
    val shuffle = s.optBoolean("shuffle"); val repeat = s.optInt("repeat")
    Row(Modifier.fillMaxWidth().height(96.dp).background(Side).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Art(art, 56)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f, fill = false)) {
                Text(title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(artist, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PhoneAndroid, null, tint = Accent, modifier = Modifier.size(14.dp))
                    Text(" Playing on ${n.name}", color = Accent, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
        Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tip("Shuffle") { IconButton(onClick = { Link.phoneCommand("shuffle") { put("on", !shuffle) } }) { Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (shuffle) Accent else TextDim) } }
                Tip("Previous") { IconButton(onClick = { Link.phoneCommand("prev") }) { Icon(Icons.Rounded.SkipPrevious, "Previous", tint = TextMain) } }
                IconButton(onClick = { Link.phoneCommand("toggle") }, Modifier.size(44.dp).clip(CircleShape).background(Accent)) {
                    Icon(if (n.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (n.playing) "Pause" else "Play", tint = Color.Black)
                }
                Tip("Next") { IconButton(onClick = { Link.phoneCommand("next") }) { Icon(Icons.Rounded.SkipNext, "Next", tint = TextMain) } }
                IconButton(onClick = { Link.phoneCommand("repeat") { put("mode", (repeat + 1) % 3) } }) {
                    Icon(if (repeat == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat", tint = if (repeat > 0) Accent else TextDim)
                }
            }
            var dragging by remember { mutableStateOf(false) }
            var dragValue by remember { mutableFloatStateOf(0f) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(time(pos), color = TextDim, fontSize = 11.sp)
                ThinSlider(if (dragging) dragValue else if (dur > 0) pos.toFloat() / dur else 0f, "Position", { dragging = true; dragValue = it },
                    onDone = { if (dur > 0) Link.phoneCommand("seek") { put("ms", (dragValue * dur).toLong()) }; dragging = false },
                    modifier = Modifier.width(420.dp).padding(horizontal = 8.dp))
                Text(time(dur), color = TextDim, fontSize = 11.sp)
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Tip("Queue on ${n.name}") { IconButton(onClick = { toggle(Panel.QUEUE) }) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Queue on ${n.name}", tint = if (panel == Panel.QUEUE) Accent else TextDim) } }
            OutputButton(ctx.state, ctx.player)
        }
    }
}

/** Speakers, headphones, HDMI: where Onda plays (the rest of the system keeps its own output). */
@Composable
private fun OutputButton(state: AppState, player: Player?) {
    if (player == null) return
    var open by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<Outputs.Device>>(emptyList()) }
    LaunchedEffect(open) { if (open) devices = withContext(kotlinx.coroutines.Dispatchers.IO) { Outputs.list() } }
    Box {
        val onPhone = Link.activePhone()
        Tip("Play on…") { IconButton(onClick = { open = true }) { Icon(if (onPhone != null) Icons.Rounded.PhoneAndroid else Icons.Rounded.Speaker, "Play on…", tint = if (state.settings.output != null || onPhone != null) Accent else TextDim) } }
        DropdownMenu(open, { open = false }, modifier = Modifier.background(Color(0xFF2E2E38))) {
            Text("Play on", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(16.dp, 4.dp))
            // Your phone plays: bring it here (it carries on here from the same place).
            if (onPhone != null) DropdownMenuItem({ Text("This PC", color = TextMain) }, { Link.takeFromPhone(); open = false },
                leadingIcon = { Icon(Icons.Rounded.Computer, null, tint = TextDim) })
            DropdownMenuItem({ Text("System default", color = if (state.settings.output == null) Accent else TextMain) },
                { state.setOutput(null); player.setOutput(null); open = false })
            devices.forEach { d ->
                DropdownMenuItem({ Text(d.label, color = if (state.settings.output == d.name) Accent else TextMain) },
                    { state.setOutput(d.name); player.setOutput(d.name); open = false })
            }
            if (devices.isEmpty()) Text("No other outputs found (needs pactl).", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(16.dp, 4.dp))
            // Your phone, when Onda is open on it: what plays here moves there.
            Link.phones.filter { Link.on && Link.connected(it) }.forEach { ph ->
                val active = onPhone?.phoneId == ph.id
                DropdownMenuItem({ Text(if (active) "${ph.name} · playing" else ph.name, color = if (active) Accent else TextMain) },
                    { if (!active) Link.playOnPhone(ph); open = false },
                    leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, null, tint = if (active) Accent else TextDim) })
            }
        }
    }
}

/** The newest releases of your artists as one list of songs (up to 3 per release). */
@Composable
private fun RadarPage(ctx: Ctx, back: (() -> Unit)?) {
    var list by remember { mutableStateOf<List<RemoteTrack>?>(null) }
    var round by remember { mutableIntStateOf(0) }
    LaunchedEffect(round) { list = null; list = ctx.state.releaseRadar(refresh = round > 0) }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header("Release radar", "New songs from artists you follow and play", back = back) {
                list?.takeIf { it.isNotEmpty() }?.let { l -> Button(onClick = { ctx.playRemote(l, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") } }
                if (list != null) TextButton(onClick = { round++ }) { Text("Refresh", color = TextDim) }
            }
        }
        val l = list
        when {
            l == null -> item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(24.dp), color = Accent, trackColor = Raised) }
            l.isEmpty() -> item { Note("Couldn't load the new releases right now.") }
            else -> items(l.size) { i -> RemoteRow(ctx, l, i, numbered = true) }
        }
    }
}

/** "<Artist> Mix": their songs from your library with songs of similar artists. */
@Composable
private fun ArtistMixPage(ctx: Ctx, name: String, back: (() -> Unit)?) {
    var tracks by remember(name) { mutableStateOf<List<Track>?>(null) }
    var round by remember(name) { mutableIntStateOf(0) }
    LaunchedEffect(name, round) { tracks = null; tracks = ctx.state.artistMix(name, refresh = round > 0) }
    val t = tracks ?: return Column { Header("$name Mix", "Making your mix…", back = back); LinearProgressIndicator(Modifier.fillMaxWidth().padding(24.dp), color = Accent, trackColor = Raised) }
    SongList(ctx, "$name Mix", "$name and similar artists", t, back = back, mosaic = true) { TextButton(onClick = { round++ }) { Text("Refresh", color = TextDim) } }
}

/** Built-in playlists made from your library and listening. */
@Composable
private fun SmartPage(ctx: Ctx) {
    val state = ctx.state
    LazyColumn(Modifier.fillMaxSize()) {
        item { Header("Smart playlists", "Made from your library and what you play; they update by themselves") }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppState.Smart.entries.forEach { k ->
                    val tracks = state.smart(k)
                    Column(Modifier.width(200.dp).clip(RoundedCornerShape(8.dp)).clickable { ctx.push(View.SmartList(k)) }.padding(8.dp)) {
                        Mosaic(tracks.map { it.artUri }, 184)
                        Text(k.title, color = TextMain, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        Text("${tracks.size} songs · ${k.about}", color = TextDim, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** F1 or Ctrl+/: every keyboard shortcut at a glance. */
@Composable
private fun ShortcutsDialog(close: () -> Unit) {
    val keys = listOf(
        "Space" to "Play / pause", "Ctrl + → / ←" to "Next / previous song", "→ / ←" to "Forward / back 5 seconds",
        "↑ / ↓" to "Volume", "Ctrl + L" to "Like the song playing", "Ctrl + K" to "Go to anything in your library",
        "Ctrl + F" to "Search online", "Ctrl + ," to "Settings", "Ctrl + + / − / 0" to "Bigger / smaller / normal size",
        "Ctrl + click, Shift + click" to "Select several songs", "Ctrl + B" to "Smaller / wider sidebar", "Esc" to "Back", "F1 or Ctrl + /" to "This list",
    )
    AlertDialog(onDismissRequest = close, title = { Text("Keyboard shortcuts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                keys.forEach { (k, v) -> Row { Text(k, color = Accent, fontSize = 13.sp, modifier = Modifier.width(200.dp)); Text(v, color = TextMain, fontSize = 13.sp) } }
            }
        },
        confirmButton = { TextButton(onClick = close) { Text("Close", color = Accent) } })
}

/** Sleep timer: pause after some minutes or when the song ends. */
@Composable
private fun SleepButton(player: Player?) {
    if (player == null) return
    var open by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(player.sleepAt) { while (player.sleepAt > 0) { now = System.currentTimeMillis(); delay(15_000) } }
    val on = player.sleepAt > 0 || player.sleepAfterSong
    Box {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tip("Sleep timer") { IconButton(onClick = { open = true }) { Icon(Icons.Rounded.Bedtime, "Sleep timer", tint = if (on) Accent else TextDim) } }
            if (player.sleepAt > 0) Text("${((player.sleepAt - now) / 60_000 + 1).coerceAtLeast(1)} min", color = Accent, fontSize = 11.sp)
        }
        DropdownMenu(open, { open = false }, modifier = Modifier.background(Color(0xFF2E2E38))) {
            Text("Pause after… (fades out over the last 30 s)", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(16.dp, 4.dp))
            listOf(15, 30, 45, 60, 90).forEach { m ->
                DropdownMenuItem({ Text("$m minutes", color = TextMain) }, { player.sleepAfterSong = false; player.sleepAt = System.currentTimeMillis() + m * 60_000L; player.refreshVolume(); now = System.currentTimeMillis(); open = false })
            }
            DropdownMenuItem({ Text("End of this song", color = TextMain) }, { player.sleepAt = 0; player.sleepAfterSong = true; open = false })
            if (on) DropdownMenuItem({ Text("Turn off", color = Accent) }, { player.sleepAt = 0; player.sleepAfterSong = false; player.refreshVolume(); open = false })
        }
    }
}

/** A thin white bar like the phone's: click or drag to set; screen readers can set it too. */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun ThinSlider(value: Float, label: String, onChange: (Float) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier, onWheel: ((Float) -> Unit)? = null) {
    var width by remember { mutableIntStateOf(1) }
    val change by rememberUpdatedState(onChange)
    val done by rememberUpdatedState(onDone)
    val v = value.coerceIn(0f, 1f)
    Box(
        modifier.height(24.dp).onSizeChanged { width = it.width.coerceAtLeast(1) }
            .onPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Scroll) { e -> onWheel?.invoke(e.changes.first().scrollDelta.y) }
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(v, 0f..1f)
                setProgress { change(it); done(); true }
            }
            .pointerInput(Unit) { detectTapGestures { change((it.x / width).coerceIn(0f, 1f)); done() } }
            .pointerInput(Unit) { detectHorizontalDragGestures(onDragEnd = { done() }) { c, _ -> change((c.position.x / width).coerceIn(0f, 1f)) } },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Raised))
        Box(Modifier.fillMaxWidth(v).height(4.dp).clip(CircleShape).background(TextMain))
    }
}

@Composable
private fun Toggle(title: String, detail: String, on: Boolean, set: (Boolean) -> Unit) {
    Row(Modifier.width(620.dp).clip(RoundedCornerShape(8.dp)).clickable { set(!on) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = TextMain)
            Text(detail, color = TextDim, fontSize = 12.sp)
        }
        androidx.compose.material3.Switch(checked = on, onCheckedChange = set)
    }
}

@Composable
private fun MixPage(ctx: Ctx, kind: Discovery.Kind, back: (() -> Unit)?) {
    val s = ctx.state
    val list = s.mixes[kind]?.tracks.orEmpty().filterNot(s::notWanted)
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header(kind.label, if (kind == Discovery.Kind.DAILY) "Fresh picks for today" else "New artists for this week", back = back) {
                TextButton(onClick = { s.refreshMixes(force = true) }, enabled = kind !in s.building) { Text("Make a new one", color = Accent) }
                if (list.isNotEmpty()) Button(onClick = { ctx.playRemote(list, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
            }
        }
        if (kind in s.building) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(24.dp), color = Accent, trackColor = Raised) }
        if (list.isEmpty() && kind !in s.building) item { Note("Play, like or follow some music first: mixes are made from your artists.") }
        items(list.size) { i -> RemoteRow(ctx, list, i, numbered = true) }
    }
}

/** Drag-to-reorder: the dragged row follows the pointer, the rows it passes make room. */
private class ReorderState {
    var dragging by mutableIntStateOf(-1)
    var offset by mutableFloatStateOf(0f)
    var rowPx = 0f
    fun target(count: Int) = if (rowPx <= 0f) dragging else (dragging + kotlin.math.round(offset / rowPx).toInt()).coerceIn(0, count - 1)
}

private fun Modifier.reorderRow(s: ReorderState, index: Int, count: Int): Modifier = this
    .zIndex(if (index == s.dragging) 1f else 0f)
    .onSizeChanged { if (s.rowPx == 0f || index == s.dragging) s.rowPx = it.height.toFloat() }
    .graphicsLayer {
        val d = s.dragging
        translationY = when {
            d < 0 -> 0f
            index == d -> s.offset
            else -> s.target(count).let { t -> if (d < t && index in d + 1..t) -s.rowPx else if (t < d && index in t until d) s.rowPx else 0f }
        }
    }

@Composable
private fun DragHandle(s: ReorderState, index: Int, count: Int, label: String, onMove: (Int, Int) -> Unit) {
    Icon(Icons.Rounded.DragHandle, "Move $label", tint = TextDim, modifier = Modifier.size(40.dp).padding(8.dp).dragToMove(s, index, count, onMove))
}

/** Drag this (a handle, or a whole row in the sidebar) to move its row; screen readers get "Move up" / "Move down". */
@Composable
private fun Modifier.dragToMove(s: ReorderState, index: Int, count: Int, onMove: (Int, Int) -> Unit): Modifier {
    val move by rememberUpdatedState(onMove) // the gesture outlives recompositions; a move must see the list as it is now
    return semantics {
        customActions = listOfNotNull(
            if (index > 0) CustomAccessibilityAction("Move up") { move(index, index - 1); true } else null,
            if (index < count - 1) CustomAccessibilityAction("Move down") { move(index, index + 1); true } else null,
        )
    }.pointerInput(index, count) {
        detectDragGestures(
            onDragStart = { s.dragging = index; s.offset = 0f },
            onDrag = { c, d -> c.consume(); s.offset += d.y },
            onDragEnd = { val f = s.dragging; val t = s.target(count); s.dragging = -1; s.offset = 0f; if (f >= 0 && t != f) move(f, t) },
            onDragCancel = { s.dragging = -1; s.offset = 0f },
        )
    }
}

/** The same for catalog songs (a free album): only those whose artist allows downloads. */
@Composable
private fun DownloadAllRemote(ctx: Ctx, list: List<RemoteTrack>) {
    val items = list.mapNotNull(Downloads::forRemote)
    if (items.isNotEmpty()) Tip("Download ${items.size} songs") { IconButton(onClick = { ctx.download(items) }) { Icon(Icons.Rounded.Download, "Download ${items.size} songs", tint = TextDim) } }
}

/** Downloads every song of a list that may be downloaded (server songs, free songs whose artist allows it). */
@Composable
private fun DownloadAll(ctx: Ctx, tracks: List<Track>) {
    val s = ctx.state
    val items = tracks.mapNotNull { t -> Downloads.forTrack(t, s.settings.server) ?: s.remoteFor(t)?.let(Downloads::forRemote) }
    if (items.isNotEmpty()) Tip("Download ${items.size} songs") { IconButton(onClick = { ctx.download(items) }) { Icon(Icons.Rounded.Download, "Download ${items.size} songs", tint = TextDim) } }
}

/** A text field whose keys stay in it: a space never pauses the music, arrows never seek. Enter runs [onEnter]. */
@Composable
internal fun TextBox(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null, singleLine: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    onEnter: (() -> Unit)? = null, onKey: ((Key) -> Boolean)? = null,
) = OutlinedTextField(value, onValueChange, modifier.onKeyEvent { e ->
    if (e.type != KeyEventType.KeyDown) false
    else if (onKey?.invoke(e.key) == true) true
    else if (e.key == Key.Enter && onEnter != null) { onEnter(); true }
    else !e.isCtrlPressed && e.key != Key.Escape // Ctrl+F and Esc still work while typing
}, label = label, placeholder = placeholder, singleLine = singleLine, visualTransformation = visualTransformation)

/** Edit a song's details by hand, or search the catalogs and pick the right song: its title, artist, album and cover. */
@Composable
internal fun EditDialog(state: AppState, t: Track, close: () -> Unit) {
    var title by remember { mutableStateOf(t.title) }
    var artist by remember { mutableStateOf(t.artist) }
    var album by remember { mutableStateOf(t.album) }
    var picked by remember { mutableStateOf<RemoteTrack?>(null) }
    var q by remember { mutableStateOf(io.github.realkhfan.onda.core.MetaMatch.query(t)) }
    var results by remember { mutableStateOf<List<RemoteTrack>?>(null) }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun search() {
        if (q.isBlank()) return
        searching = true
        scope.launch {
            results = coroutineScope {
                val dz = async { runCatching { Deezer.search(q, 15) }.getOrDefault(emptyList()) }
                val ap = async { if (state.settings.apple) runCatching { Itunes.searchSongs(q) }.getOrDefault(emptyList()) else emptyList() }
                Match.mergeTracks(dz.await(), ap.await())
            }
            searching = false
        }
    }
    LaunchedEffect(Unit) { search() }
    AlertDialog(onDismissRequest = close, title = { Text("Edit details") }, modifier = Modifier.width(720.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.width(280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextBox(title, { title = it }, label = { Text("Title") })
                    TextBox(artist, { artist = it }, label = { Text("Artist") })
                    TextBox(album, { album = it }, label = { Text("Album") })
                    Text("Shown in Onda; the file isn't changed. Right-click → Restore original tags undoes it.", color = TextDim, fontSize = 12.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextBox(q, { q = it }, label = { Text("Find the song online") }, onEnter = ::search, modifier = Modifier.fillMaxWidth())
                    Text(if (searching) "Searching…" else "Click the right song to fill in its details and cover.", color = TextDim, fontSize = 12.sp)
                    LazyColumn(Modifier.height(300.dp)) {
                        items(results.orEmpty()) { r ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (picked == r) Raised else Color.Transparent)
                                .clickable { picked = r; title = r.title; artist = r.artist; album = r.album.ifBlank { album } }.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Art(r.cover, 40)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(r.title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${r.artist} · ${r.album}", color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                // The length tells versions apart (radio edit, extended, live).
                                Text(time(r.durationSec * 1000L), color = TextDim, fontSize = 12.sp)
                            }
                        }
                        if (results?.isEmpty() == true) item { Text("Nothing found. Try other words.", color = TextDim, modifier = Modifier.padding(8.dp)) }
                    }
                    if (t.durationMs > 0) Text("Your file: ${time(t.durationMs)}", color = TextDim, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = picked
                if (p != null && p.title == title && p.artist == artist && (p.album.isBlank() || p.album == album)) Overrides.replace(t, p)
                else Overrides.edit(t, title, artist, album, cover = p?.cover)
                close()
            }) { Text("Save", color = Accent) }
        },
        dismissButton = { TextButton(onClick = close) { Text("Cancel", color = TextDim) } })
}

/** Top artists and songs from your listening history (with a linked phone, its plays too). */
@Composable
private fun StatsPage(ctx: Ctx) {
    val state = ctx.state
    var period by remember { mutableIntStateOf(0) }
    val periods = listOf("Last 4 weeks" to 28L, "Last 6 months" to 182L, "All time" to 0L)
    val plays = remember(state.plays, period) {
        val days = periods[period].second
        if (days == 0L) state.plays else (System.currentTimeMillis() - days * 86_400_000L).let { since -> state.plays.filter { it.atMs >= since } }
    }
    val artists = remember(plays) { plays.groupingBy { it.artist }.eachCount().entries.sortedByDescending { it.value }.take(10) }
    val songs = remember(plays, state.byId) {
        plays.groupBy { it.key }.entries.sortedByDescending { it.value.size }.take(20)
            .mapNotNull { (key, list) -> state.trackFor(key)?.let { it to list.size } }
    }
    val playable = songs.map { it.first }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            val minutes = plays.sumOf { p -> state.trackFor(p.key)?.durationMs?.takeIf { it > 0 } ?: 180_000L } / 60_000
            val phones = Link.phones.joinToString(" and ") { it.name }.ifEmpty { null }
            Header("Your stats", "${plays.size} plays · about ${"%,d".format(minutes)} minutes (${minutes / 60} hours) of music · " +
                (phones?.let { "on this PC and $it" } ?: "on this PC"))
        }
        item {
            Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                periods.forEachIndexed { i, (label, _) ->
                    if (i == period) Button(onClick = {}) { Text(label) } else OutlinedButton(onClick = { period = i }) { Text(label) }
                }
            }
        }
        if (plays.isEmpty()) item { Note("Play some songs and your top artists and songs show up here.") }
        else {
            item { Section("When you listen") }
            item { ListeningClock(plays) }
            item { Section("Top artists") }
            items(artists.size) { i ->
                val (name, n) = artists[i]
                Row(Modifier.fillMaxWidth().clickable { ctx.openArtistByName(name) }.padding(24.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", color = TextDim, modifier = Modifier.width(36.dp))
                    Art(state.artistPics[name.lowercase()]?.ifBlank { null }, 40, round = true)
                    Spacer(Modifier.width(12.dp))
                    Text(name, color = TextMain, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$n plays", color = TextDim, fontSize = 13.sp)
                }
            }
            val albums = plays.mapNotNull { state.trackFor(it.key) }.filterNot { state.isPreviewRow(it) }.groupingBy { it.albumKey }.eachCount()
                .entries.sortedByDescending { it.value }.take(10).mapNotNull { e -> Grouping.albums(state.library).firstOrNull { it.key == e.key }?.let { it to e.value } }
            if (albums.isNotEmpty()) {
                item { Section("Top albums") }
                item {
                    ScrollRow {
                        items(albums) { (al, n) -> Card(al.artUri, al.title, "${al.artist} · $n plays") { ctx.push(View.LocalList(al.title, "Album · ${al.artist}", al.artUri, al.tracks)) } }
                    }
                }
            }
            item { Section("Top songs") }
            items(songs.size) { i ->
                val (t, n) = songs[i]
                SongRow(i + 1, t.title, t.artist, t.album, t.durationMs, t.artUri, ctx.player?.current?.id == t.id, note = "$n plays") { ctx.play(playable, i) }
            }
        }
    }
}

/** What you played, newest first, by day (with a linked phone, its plays too). */
@Composable
private fun HistoryPage(ctx: Ctx) {
    val state = ctx.state
    // Songs played on your phone that aren't on this PC are listed greyed out (right-click still works).
    val rows = remember(state.plays, state.byId) {
        state.plays.asReversed().take(300).mapNotNull { p ->
            (state.trackFor(p.key) ?: p.takeIf { io.github.realkhfan.onda.core.Source.of(it.key) == null && it.title.isNotBlank() }
                ?.let { LinkSync.missingTrack(LinkSync.Library.PLACEHOLDER + it.artist + '\u001F' + it.title) })?.let { p to it }
        }
    }
    val tracks = rows.map { it.second }
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now()
    fun day(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    LazyColumn(Modifier.fillMaxSize()) {
        item { Header("History", "The last ${rows.size} songs you played" + (Link.phones.firstOrNull()?.let { " · here and on ${it.name}" } ?: "")) }
        if (rows.isEmpty()) item { Note("Songs you play show up here.") }
        rows.indices.groupBy { day(rows[it].first.atMs) }.forEach { (d, idx) ->
            item { Section(when (d) { today -> "Today"; today.minusDays(1) -> "Yesterday"; else -> d.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM")) }) }
            items(idx) { i ->
                val (p, t) = rows[i]
                val missing = LinkSync.isMissing(t.id)
                ContextMenuArea(items = { trackMenu(ctx, tracks, t, null, emptySet()) }) {
                    SongRow(null, t.title, t.artist, t.album, t.durationMs, t.artUri, ctx.player?.current?.id == t.id, dim = missing,
                        note = java.time.Instant.ofEpochMilli(p.atMs).atZone(zone).toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) +
                            if (missing) " · Not on this PC" else "") { if (!missing) ctx.play(tracks, i) }
                }
            }
        }
    }
}

/** The same song more than once (another file, the server's copy): hide the extra ones; the files stay. */
@Composable
private fun DuplicatesPage(ctx: Ctx, back: (() -> Unit)?) {
    val state = ctx.state
    val groups = remember(state.library) {
        state.library.groupBy { Match.norm(it.artist) + "|" + Match.norm(it.title) }.values.filter { it.size > 1 }.sortedBy { it[0].title.lowercase() }
    }
    val hidden = state.collection.hidden.count { it.startsWith("file:") || it.startsWith(Navidrome.PREFIX) }
    LazyColumn(Modifier.fillMaxSize()) {
        item { Header("Duplicates", "${groups.size} songs are in your library more than once · hiding keeps the file", back = back) }
        if (hidden > 0) item { TextButton(onClick = { state.showHiddenSongs() }, modifier = Modifier.padding(start = 16.dp)) { Text("Show $hidden hidden songs again", color = Accent) } }
        if (groups.isEmpty()) item { Note("No duplicates in your library.") }
        groups.forEach { g ->
            item { Section("${g[0].title} — ${g[0].artist}") }
            items(g) { t ->
                val where = if (t.id.startsWith("file:")) runCatching { java.io.File(java.net.URI(t.id)).path }.getOrDefault(t.fileName) else "On your music server"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        ContextMenuArea(items = { listOf(ContextMenuItem("Hide this copy") { state.toggleHidden(t.id); ctx.undoable("Hidden: ${t.title}") { state.toggleHidden(t.id) } }) + trackMenu(ctx, g, t, null, emptySet()) }) {
                            SongRow(null, t.album, where, "${ctx.state.playCounts[t.id] ?: 0} plays", t.durationMs, t.artUri, ctx.player?.current?.id == t.id) { ctx.play(listOf(t), 0) }
                        }
                    }
                    TextButton(onClick = { state.toggleHidden(t.id); ctx.undoable("Hidden: ${t.title}") { state.toggleHidden(t.id) } }, modifier = Modifier.padding(end = 16.dp)) { Text("Hide", color = TextDim) }
                }
            }
        }
    }
}

/** Your music by folder, as it is on disk. */
@Composable
private fun FolderPage(ctx: Ctx, path: String?, back: (() -> Unit)?) {
    val state = ctx.state
    val files = remember(state.library) {
        state.library.mapNotNull { t -> t.id.takeIf { it.startsWith("file:") }?.let { runCatching { java.io.File(java.net.URI(it)).parent }.getOrNull() }?.let { it to t } }
    }
    fun count(dir: String) = files.count { it.first == dir || it.first.startsWith("$dir/") }
    if (path == null) return LazyColumn(Modifier.fillMaxSize()) {
        item { Header("Folders", "Your music folders") }
        items(state.settings.folders) { f -> FolderRow(f, count(f)) { ctx.push(View.Folder(f)) } }
    }
    val here = remember(files, path) { files.filter { it.first == path }.map { it.second }.sortedWith(compareBy({ it.trackNo }, { it.fileName.lowercase() })) }
    val subs = remember(files, path) { files.map { it.first }.filter { it.startsWith("$path/") }.map { "$path/" + it.removePrefix("$path/").substringBefore('/') }.distinct().sortedBy { it.lowercase() } }
    SongList(ctx, java.io.File(path).name, path, here, back = back, above = {
        items(subs) { d -> FolderRow(d, count(d)) { ctx.push(View.Folder(d)) } }
    })
}

@Composable
private fun FolderRow(path: String, songs: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(24.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Folder, null, tint = TextDim, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(java.io.File(path).name, color = TextMain)
            Text("$songs songs", color = TextDim, fontSize = 12.sp)
        }
    }
}

/** One of your artists: photo, your most played songs, albums, all songs. */
@Composable
private fun LibraryArtistPage(ctx: Ctx, name: String, back: (() -> Unit)?) {
    val state = ctx.state
    val a = remember(state.library, name) { Grouping.artists(state.library).firstOrNull { Match.norm(it.name) == Match.norm(name) } }
        ?: return Note("No songs by $name in your library.")
    val albums = remember(a) { Grouping.albums(a.tracks) }
    val top = remember(a, state.playCounts) { a.tracks.filter { (state.playCounts[it.id] ?: 0) > 0 }.sortedByDescending { state.playCounts[it.id] ?: 0 }.take(5) }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header(a.name, "Artist · ${a.tracks.size} songs in your library", state.artistPics[a.name.lowercase()]?.ifBlank { null } ?: a.artUri, round = true, back = back) {
                OutlinedButton(onClick = { ctx.openArtistByName(a.name) }) { Text("More from ${a.name}") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { ctx.radio(ctx.state.item((top.firstOrNull() ?: a.tracks.random())), a.name) }) { Icon(Icons.Rounded.Radio, null, tint = Accent); Text(" Radio", color = TextMain) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { ctx.play(a.tracks, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
            }
        }
        if (top.isNotEmpty()) {
            item { Section("Your most played") }
            items(top.size) { i -> TrackRow(ctx, top, i, numbered = true) }
        }
        if (albums.isNotEmpty()) {
            item { Section("Albums") }
            item {
                ScrollRow {
                    items(albums) { al -> Card(al.artUri, al.title, "${al.tracks.size} songs") { ctx.push(View.LocalList(al.title, "Album · ${al.artist}", al.artUri, al.tracks)) } }
                }
            }
        }
        item {
            var related by remember(a.name) { mutableStateOf<List<RemoteArtist>>(emptyList()) }
            LaunchedEffect(a.name) {
                related = runCatching { Discover.artistId(a.name).takeIf { it > 0 }?.let { Deezer.related(it, 12) } }.getOrNull().orEmpty().filterNot { state.isAi(it.name) }
            }
            if (related.isNotEmpty()) Column {
                Section("Fans also like")
                ScrollRow {
                    items(related) { r ->
                        Column(Modifier.width(140.dp).clip(RoundedCornerShape(8.dp)).clickable { ctx.push(View.CatalogArtist(r)) }.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Art(r.picture, 124, round = true)
                            Text(r.name, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
        item { Section("All songs") }
        items(a.tracks.size) { i -> TrackRow(ctx, a.tracks, i, numbered = true) }
    }
}

/** Where a song is, its format and quality, and how often you played it. */
@Composable
private fun InfoDialog(state: AppState, t: Track, close: () -> Unit) {
    val file = remember(t.id) { t.id.takeIf { it.startsWith("file:") }?.let { runCatching { File(java.net.URI(it)) }.getOrNull() } }
    var info by remember(t.id) { mutableStateOf<Scanner.AudioInfo?>(null) }
    LaunchedEffect(t.id) { info = file?.let { withContext(kotlinx.coroutines.Dispatchers.IO) { Scanner.audioInfo(it) } } }
    val plays = state.plays.filter { it.key == t.id }
    val fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
    fun date(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).format(fmt)
    val rows = buildList {
        add("Title" to t.title); add("Artist" to t.artist); add("Album" to t.album)
        state.meta[t.id]?.let { add("Shown in Onda" to "Edited or matched; Right-click → Restore original tags shows the file's own") }
        add("Length" to time(t.durationMs))
        if (file != null) add("File" to file.path) else add("Where" to "On your music server")
        info?.let { i ->
            add("Format" to i.badge)
            if (i.channels.isNotBlank()) add("Channels" to i.channels)
            add("Size" to "%.1f MB".format(i.bytes / 1_048_576.0))
            add("Modified" to date(i.modified))
        }
        add("Plays" to "${plays.size}" + (plays.maxOfOrNull { it.atMs }?.let { " · last ${date(it)}" } ?: ""))
        if (state.settings.normalize) Loudness.gains[t.id]?.let { add("Loudness" to "%+.1f dB to even it out".format(20 * kotlin.math.log10(Loudness.factorFor(it.db, it.peak)))) }
    }
    AlertDialog(onDismissRequest = close, title = { Text("Song info") }, modifier = Modifier.width(620.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        text = {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { (k, v) ->
                        Row { Text(k, color = TextDim, fontSize = 13.sp, modifier = Modifier.width(120.dp)); Text(v, color = TextMain, fontSize = 13.sp) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = close) { Text("Close", color = Accent) } })
}

/** Name, description and cover image of one of your playlists. */
@Composable
private fun PlaylistDialog(state: AppState, id: String, close: () -> Unit) {
    val pl = state.collection.playlists.firstOrNull { it.id == id } ?: return close()
    var name by remember { mutableStateOf(pl.name) }
    var desc by remember { mutableStateOf(pl.description) }
    var cover by remember { mutableStateOf<File?>(null) }
    var removeCover by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = close, title = { Text("Edit playlist") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val shown = cover?.path ?: pl.cover?.takeIf { !removeCover }
                    if (shown != null) Art(shown, 140) else Mosaic(state.playlistTracks(id).map { it.artUri }, 140)
                    TextButton(onClick = {
                        scope.launch {
                            when (val r = FolderPicker.file(false, System.getProperty("user.home") + "/Pictures", "Cover image")) {
                                is FolderPicker.Result.Picked -> File(r.path).takeIf { it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
                                    ?.let { cover = it; removeCover = false } ?: run { note = "Pick a JPG, PNG or WebP image." }
                                FolderPicker.Result.NoPicker -> note = "No file picker found on this system."
                                FolderPicker.Result.Cancelled -> {}
                                is FolderPicker.Result.Remote -> note = r.message
                            }
                        }
                    }) { Text("Choose image", color = Accent) }
                    if (pl.cover != null || cover != null) TextButton(onClick = { cover = null; removeCover = true }) { Text("Use album covers", color = TextDim) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(300.dp)) {
                    TextBox(name, { name = it }, label = { Text("Name") })
                    TextBox(desc, { desc = it }, label = { Text("Description") }, singleLine = false, modifier = Modifier.height(120.dp))
                    note?.let { Text(it, color = ErrorRed, fontSize = 12.sp) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { state.editPlaylist(id, name, desc, cover, removeCover); close() }) { Text("Save", color = Accent) } },
        dismissButton = { TextButton(onClick = close) { Text("Cancel", color = TextDim) } })
}

/** Ctrl+K: jump to any song, album, artist or playlist of yours by typing; arrows and Enter. */
@Composable
private fun QuickSearch(ctx: Ctx, close: () -> Unit) {
    val state = ctx.state
    var q by remember { mutableStateOf("") }
    var sel by remember { mutableIntStateOf(0) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    class Hit(val label: String, val sub: String, val art: String?, val round: Boolean, val go: () -> Unit)
    val hits = remember(q, state.library, state.collection.playlists) {
        if (q.isBlank()) return@remember emptyList()
        val nq = Match.norm(q)
        val songs = AutoTree.search(state.library, q, limit = 6)
        val albums = Grouping.albums(state.library).filter { Match.norm(it.title).contains(nq) }.take(3)
        val artists = Grouping.artists(state.library).filter { Match.norm(it.name).contains(nq) }.take(3)
        val lists = state.collection.playlists.filter { Match.norm(it.name).contains(nq) }.take(3)
        artists.map { a -> Hit(a.name, "Artist", state.artistPics[a.name.lowercase()]?.ifBlank { null } ?: a.artUri, true) { ctx.push(View.LibraryArtist(a.name)) } } +
            lists.map { pl -> Hit(pl.name, "Playlist", pl.cover, false) { ctx.push(View.Playlist(pl.id)) } } +
            songs.mapIndexed { i, t -> Hit(t.title, "Song · ${t.artist}", t.artUri, false) { ctx.play(songs, i) } } +
            albums.map { al -> Hit(al.title, "Album · ${al.artist}", al.artUri, false) { ctx.push(View.LocalList(al.title, "Album · ${al.artist}", al.artUri, al.tracks)) } }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
    fun go(i: Int) { hits.getOrNull(i)?.let { close(); it.go() } }
    androidx.compose.ui.window.Dialog(onDismissRequest = close) {
        Column(Modifier.width(560.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF22222A)).padding(16.dp)) {
            TextBox(q, { q = it; sel = 0 }, placeholder = { Text("Go to a song, album, artist or playlist") }, onEnter = { go(sel) },
                onKey = { k -> when (k) {
                    androidx.compose.ui.input.key.Key.DirectionDown -> { sel = (sel + 1).coerceAtMost(hits.lastIndex.coerceAtLeast(0)); true }
                    androidx.compose.ui.input.key.Key.DirectionUp -> { sel = (sel - 1).coerceAtLeast(0); true }
                    androidx.compose.ui.input.key.Key.Escape -> { close(); true }
                    else -> false
                } },
                modifier = Modifier.fillMaxWidth().focusRequester(focus))
            hits.forEachIndexed { i, h ->
                Row(Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(6.dp)).background(if (i == sel) Raised else Color.Transparent)
                    .clickable { go(i) }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art(h.art, 36, h.round)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(h.label, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(h.sub, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (q.isNotBlank() && hits.isEmpty()) Text("Nothing in your library. Search online with Ctrl+F.", color = TextDim, modifier = Modifier.padding(8.dp))
        }
    }
}

/** A few lines around the one being sung (or the start of unsynced lyrics). */
@Composable
private fun NowPlayingLyrics(state: AppState, player: Player) {
    val ls = rememberLyrics(state, player)
    val l = ls.lyrics
    Text("Lyrics", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(Modifier.height(8.dp))
    val lines = l?.synced.orEmpty()
    Column(Modifier.height(260.dp)) {
        when {
            !ls.full -> Text("Lyrics are shown for full songs, not previews.", color = TextMain.copy(alpha = 0.7f))
            l == null && !ls.failed -> Text("Looking for lyrics…", color = TextMain.copy(alpha = 0.7f))
            l == null || (lines.isEmpty() && l.plain == null && !l.instrumental) -> Text("No lyrics for this song.", color = TextMain.copy(alpha = 0.7f))
            l.instrumental -> Text("Instrumental", color = TextMain.copy(alpha = 0.7f))
            lines.isNotEmpty() -> {
                val active = Lrc.indexAt(lines, player.positionMs + ls.shift)
                for (i in (active - 1).coerceAtLeast(0)..(active + 4).coerceAtMost(lines.lastIndex)) {
                    Text(lines[i].text.ifBlank { "♪" }, color = if (i == active) TextMain else TextMain.copy(alpha = 0.45f),
                        fontSize = if (i == active) 22.sp else 17.sp, fontWeight = if (i == active) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { player.seek(lines[i].timeMs - ls.shift) }.padding(vertical = 4.dp))
                }
            }
            else -> Text(l.plain.orEmpty(), color = TextMain, fontSize = 16.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
        }
    }
}

/** The song big: cover on its own colour, controls, and what's next (click the cover in the player bar). */
@Composable
private fun NowPlayingPage(ctx: Ctx, back: (() -> Unit)?) {
    val player = ctx.player ?: return Note("Playback isn't available.")
    val cur = player.current ?: return Note("Nothing is playing.")
    val tint = rememberArtTint(cur.art)
    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(tint, Bg)))) {
        Row(Modifier.fillMaxSize().padding(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Art(cur.art, 380)
                Spacer(Modifier.height(24.dp))
                Text(cur.title, color = TextMain, fontSize = 32.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Text(cur.artist, color = TextMain.copy(alpha = 0.8f), fontSize = 18.sp, modifier = Modifier.clickable { ctx.openArtistByName(cur.artist) })
                Text(cur.album, color = TextMain.copy(alpha = 0.6f), fontSize = 14.sp)
            }
            Column(Modifier.width(420.dp)) {
                NowPlayingLyrics(ctx.state, player)
                Spacer(Modifier.height(16.dp))
                Text("Up next", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                val next = player.upcoming().take(4)
                if (next.isEmpty()) Text(if (ctx.state.settings.autoplay) "Similar songs follow when this one ends." else "This is the last song.", color = TextMain.copy(alpha = 0.7f))
                next.forEach { (i, it) ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { player.jumpTo(i) }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Art(it.art, 40)
                        Spacer(Modifier.width(10.dp))
                        Column { Text(it.title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(it.artist, color = TextMain.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1) }
                    }
                }
            }
        }
        if (back != null) Tip("Back") { IconButton(onClick = back, modifier = Modifier.padding(12.dp)) { Icon(Icons.Rounded.CloseFullscreen, "Back", tint = TextMain) } }
    }
}

/* ---------- Match metadata, rename files ---------- */

/** Review of Deezer matches: keep your file's details or replace them, song by song or all at once. */
@Composable
private fun MatchPage(ctx: Ctx, back: (() -> Unit)?) {
    val s by Matcher.state.collectAsState()
    val confident = s.proposals.count { it.confidence == Confidence.HIGH }
    LazyColumn(Modifier.fillMaxSize()) {
        item { Header("Match metadata", "Your audio files are never changed", back = back) }
        item {
            Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.running) {
                    Text("Checking ${s.done} of ${s.total} songs on Deezer…", color = TextMain)
                    LinearProgressIndicator({ if (s.total == 0) 0f else s.done.toFloat() / s.total }, Modifier.width(480.dp), color = Accent, trackColor = Raised)
                    OutlinedButton(onClick = { Matcher.stop() }) { Text("Stop") }
                }
                s.error?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
                Text("${s.proposals.size} to review · ${s.alreadyRight} already correct · ${s.noMatch} not found (kept as they are)", color = TextDim, fontSize = 13.sp)
                if (s.proposals.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (confident > 0) Button(onClick = { Matcher.replaceAllConfident() }) { Text("Replace all confident ($confident)") }
                    OutlinedButton(onClick = { Matcher.keepAll() }) { Text("Keep all") }
                }
            }
        }
        items(s.proposals, key = { it.track.id }) { p -> ProposalCard(p) }
        if (!s.running && s.proposals.isEmpty()) item {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (s.total == 0) "No new songs to check." else "All done. Songs without a match keep their own details.", color = TextDim)
                OutlinedButton(onClick = { ctx.push(View.Rename) }) { Text("Rename files to match their tags") }
            }
        }
    }
}

@Composable
private fun ProposalCard(p: Matcher.Proposal) {
    val t = p.track
    val m = p.match
    Column(Modifier.padding(24.dp, 6.dp).width(760.dp).clip(RoundedCornerShape(8.dp)).background(Raised).padding(14.dp)) {
        Text(if (p.confidence == Confidence.HIGH) "Confident match" else "Possible match — check it",
            color = if (p.confidence == Confidence.HIGH) Accent else TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MatchSide("Your file", t.artUri, t.title, t.artist, t.album, t.durationMs, Modifier.weight(1f), null)
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = TextDim, modifier = Modifier.padding(horizontal = 10.dp))
            MatchSide(m.source.label, m.cover, m.title, m.artist, m.album.ifBlank { t.album }, m.durationSec * 1000L, Modifier.weight(1f), t)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { Matcher.decide(p, false) }) { Text("Keep", color = TextDim) }
            Spacer(Modifier.width(4.dp))
            Button(onClick = { Matcher.decide(p, true) }) { Text("Replace") }
        }
    }
}

/** One side of the comparison; on the Deezer side, what differs from the file is highlighted. */
@Composable
private fun MatchSide(label: String, art: String?, title: String, artist: String, album: String, durationMs: Long, modifier: Modifier, compare: Track?) {
    fun color(differs: Boolean) = if (compare != null && differs) Accent else TextMain
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Art(art, 56)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = TextDim, fontSize = 11.sp)
            Text(title, color = color(title != compare?.title), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(artist, color = color(artist != compare?.artist), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
            Text(album, color = color(album != compare?.album), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
            // A big length gap usually means another edit or version of the song.
            if (durationMs > 0) Text(time(durationMs), color = color(compare != null && compare.durationMs > 0 && kotlin.math.abs(compare.durationMs - durationMs) > 3000), fontSize = 13.sp)
        }
    }
}

/** Every rename is shown first; after renaming, Undo puts the old names back. */
@Composable
private fun RenamePage(ctx: Ctx, back: (() -> Unit)?) {
    val state = ctx.state
    val plan = remember(state.library) { Renames.plan(state.library) }
    var result by remember { mutableStateOf<Renames.Result?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun run(p: List<io.github.realkhfan.onda.core.FileNames.Rename>, undo: Boolean) {
        busy = true
        state.launch {
            val r = state.renameFiles(p)
            result = if (undo) null else r
            busy = false
            ctx.toast(if (undo) "Old names restored" else "Renamed ${r.moved.size} files")
        }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item { Header("Rename files from tags", "“Artist - Title”, from each song's details · the audio and tags stay as they are", back = back) }
        item {
            Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val r = result
                when {
                    busy -> LinearProgressIndicator(Modifier.width(480.dp), color = Accent, trackColor = Raised)
                    r != null -> {
                        Text("Renamed ${r.moved.size} files." + if (r.failed.isEmpty()) "" else " ${r.failed.size} couldn't be renamed (name taken or folder read-only).", color = TextMain)
                        if (r.undo.isNotEmpty()) OutlinedButton(onClick = { run(r.undo, undo = true) }) { Text("Undo") }
                    }
                    plan.isEmpty() -> Text("All your files are already named after their details.", color = TextDim)
                    else -> Button(onClick = { run(plan, undo = false) }) { Text("Rename ${plan.size} files") }
                }
            }
        }
        if (result == null && !busy) items(plan, key = { it.id }) { r ->
            Column(Modifier.padding(24.dp, 6.dp)) {
                Text(r.from, color = TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("→ ${r.to}", color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** "Get N from your phone": a list's greyed-out songs, sent by the phone while Onda runs there. */
@Composable
private fun GetFromPhone(ctx: Ctx, list: List<Track>) {
    val missing = list.filter { LinkSync.isMissing(it.id) }
    val ph = Link.phones.firstOrNull { Link.on && Link.connected(it) } ?: return
    if (missing.isEmpty()) return
    Tip("Copy them from ${ph.name} into “Onda from phone”") {
        TextButton(onClick = {
            Link.getFromPhone(missing.map { LinkSync.Song(null, it.title, it.artist) })
            ctx.toast("Asked ${ph.name} for ${missing.size} songs; they arrive in “Onda from phone”")
        }) { Text("Get ${missing.size} from ${ph.name}", color = Accent) }
    }
}

/** "Artist", or "✓ Following" in the accent colour for one you follow. */
@Composable
private fun ArtistLabel(s: AppState, a: RemoteArtist, size: androidx.compose.ui.unit.TextUnit) {
    if (s.isFollowed(a)) Text("✓ Following", color = Accent, fontSize = size, fontWeight = FontWeight.Medium)
    else Text("Artist", color = TextDim, fontSize = size)
}
