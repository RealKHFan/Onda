package io.github.realkhfan.onda.desktop

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bookmark
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.material.icons.rounded.Folder
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
val Accent = Color(0xFF9D7BFF)
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
        fun go(v: View) { stack.clear(); stack.add(v) }
        fun push(v: View) { stack.add(v) }
        val back: (() -> Unit)? = if (stack.size > 1) ({ stack.removeAt(stack.lastIndex) }) else null
        LaunchedEffect(state.navRequest) {
            when (state.navRequest) {
                "search" -> go(View.Search)
                "settings" -> go(View.Settings)
                "back" -> if (stack.size > 1) stack.removeAt(stack.lastIndex)
            }
            state.navRequest = null
        }
        var panel by remember { mutableStateOf(Panel.NONE) }
        var lyricsBig by remember { mutableStateOf(false) }
        var naming by remember { mutableStateOf<Naming?>(null) }
        val scope = rememberCoroutineScope()
        var toast by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(toast) { if (toast != null) { delay(3500); toast = null } }
        val ctx = Ctx(state, player, ::push, { naming = it }, { toast = it }) { name ->
            scope.launch { Deezer.searchArtists(name).firstOrNull { Match.norm(it.name) == Match.norm(name) }?.let { push(View.CatalogArtist(it)) } }
        }
        // Folders dropped on the window (from Dolphin, say) become music folders.
        val drop = remember(state) {
            object : DragAndDropTarget {
                @OptIn(ExperimentalComposeUiApi::class)
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val files = (event.dragData() as? DragData.FilesList)?.readFiles().orEmpty()
                    val dirs = files.mapNotNull { runCatching { File(java.net.URI(it)) }.getOrNull() }.filter { it.isDirectory }
                    dirs.forEach { state.addFolder(it.absolutePath) }
                    toast = if (dirs.isNotEmpty()) "Added ${dirs.joinToString { it.name }} to your music folders" else "Drop a folder to add it to your music folders."
                    return dirs.isNotEmpty()
                }
            }
        }
        Column(Modifier.fillMaxSize().background(Bg).dragAndDropTarget(shouldStartDragAndDrop = { true }, target = drop)) {
            Row(Modifier.weight(1f)) {
                Sidebar(state, stack.first(), ::go) { naming = Naming.New() }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    if (player == null) Note("Onda needs GStreamer to play music. On Bazzite it's already there; on other systems install gstreamer1 and its plugins.")
                    if (player != null && panel == Panel.LYRICS && lyricsBig) {
                        LyricsFull(state, player, onCollapse = { lyricsBig = false }) { panel = Panel.NONE; lyricsBig = false }
                    } else when (val v = stack.last()) {
                        View.Home -> HomePage(ctx, ::push)
                        View.Songs -> Songs(ctx)
                        View.Albums -> Albums(state, { a -> push(View.LocalList(a.title, "Album · ${a.artist}", a.artUri, a.tracks)) }) { push(View.CatalogAlbum(it)) }
                        View.Artists -> Artists(state, { a -> push(View.LibraryArtist(a.name)) }) { push(View.CatalogArtist(it)) }
                        is View.Mix -> MixPage(ctx, v.kind, back)
                        View.Stats -> StatsPage(ctx)
                        View.History -> HistoryPage(ctx)
                        View.Duplicates -> DuplicatesPage(ctx, back)
                        is View.Folder -> FolderPage(ctx, v.path, back)
                        is View.LibraryArtist -> LibraryArtistPage(ctx, v.name, back)
                        View.Search -> Search(ctx)
                        View.Settings -> SettingsPage(ctx)
                        View.Match -> MatchPage(ctx, back)
                        View.Rename -> RenamePage(ctx, back)
                        View.Liked -> SongList(ctx, "Liked Songs", "Playlist", state.likedTracks, back = back)
                        View.Want -> WantPage(ctx, back)
                        is View.Playlist -> {
                            val pl = state.collection.playlists.firstOrNull { it.id == v.id }
                            if (pl == null) LaunchedEffect(Unit) { go(View.Songs) } else {
                                SongList(ctx, pl.name, "Playlist", state.playlistTracks(pl.id), back = back, inPlaylist = pl.id, mosaic = true,
                                    onMove = { from, to -> state.movePlaylistTrack(pl.id, from, to) }) {
                                    IconButton(onClick = { state.togglePin(pl.id) }) { Icon(Icons.Rounded.PushPin, if (pl.id in state.settings.pinned) "Unpin" else "Pin to the top", tint = if (pl.id in state.settings.pinned) Accent else TextDim) }
                                    IconButton(onClick = { naming = Naming.Rename(pl.id, pl.name) }) { Icon(Icons.Rounded.Edit, "Rename playlist", tint = TextDim) }
                                    IconButton(onClick = { naming = Naming.Delete(pl.id, pl.name) }) { Icon(Icons.Rounded.Delete, "Delete playlist", tint = TextDim) }
                                    DownloadAll(ctx, state.playlistTracks(pl.id))
                                    IconButton(onClick = { ctx.exportM3u(pl.name, state.playlistTracks(pl.id)) }) { Icon(Icons.Rounded.SaveAlt, "Save as playlist file (M3U)", tint = TextDim) }
                                }
                            }
                        }
                        is View.ServerPlaylist -> SongList(ctx, v.p.name, "Playlist · Music server", state.serverPlaylistTracks(v.p), back = back) {
                            DownloadAll(ctx, state.serverPlaylistTracks(v.p))
                        }
                        is View.LocalList -> {
                            val tracks = v.tracks.map { state.byId[it.id] ?: it } // edited details show at once
                            SongList(ctx, v.title, v.subtitle, tracks, art = v.art, back = back) {
                                IconButton(onClick = { naming = Naming.EditMany(tracks.filterNot(state::isPreviewRow)) }) { Icon(Icons.Rounded.Edit, "Edit details of all songs", tint = TextDim) }
                            }
                        }
                        is View.CatalogArtist -> CatalogArtistPage(ctx, v.a, back)
                        is View.CatalogAlbum -> CatalogAlbumPage(ctx, v.a, back)
                    }
                }
                if (player != null && panel == Panel.QUEUE) QueuePanel(player, onSave = { naming = Naming.New(null, player.queue.map { it.id }.filter(state::canSave).distinct()) }) { panel = Panel.NONE }
                if (player != null && panel == Panel.LYRICS && !lyricsBig) LyricsPanel(state, player, onExpand = { lyricsBig = true }) { panel = Panel.NONE }
            }
            toast?.let { Text(it, color = TextMain, modifier = Modifier.fillMaxWidth().background(Color(0xFF2E2E38)).padding(24.dp, 10.dp)) }
            PlayerBar(ctx, panel) { panel = if (panel == it) Panel.NONE else it }
        }
        naming?.let { n -> NamingDialog(state, n) { naming = null } }
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
    val openArtistByName: (String) -> Unit,
) {
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
            }
        }
    }

    fun copy(text: String, what: String) {
        java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(text), null)
        toast("$what copied")
    }
    fun play(list: List<Track>, i: Int) { player?.play(list.map(state::item), i) }
    fun playRemote(list: List<RemoteTrack>, i: Int) {
        val items = list.mapNotNull(state::item)
        if (items.isEmpty()) return
        player?.play(items, items.indexOfFirst { it.title == list[i].title && it.artist == list[i].artist }.coerceAtLeast(0))
    }
}

private sealed interface Naming {
    data class New(val first: String? = null, val more: List<String> = emptyList()) : Naming
    data class Rename(val id: String, val name: String) : Naming
    data class Delete(val id: String, val name: String) : Naming
    data class Edit(val t: Track) : Naming
    /** The same artist or album for every song of a list (an album, an artist). */
    data class EditMany(val tracks: List<Track>) : Naming
}

@Composable
private fun NamingDialog(state: AppState, n: Naming, close: () -> Unit) {
    if (n is Naming.Delete) {
        AlertDialog(onDismissRequest = close, title = { Text("Delete “${n.name}”?") },
            text = { Text("Only the playlist goes; your songs stay.", color = TextDim) },
            confirmButton = { TextButton(onClick = { state.deletePlaylist(n.id); close() }) { Text("Delete", color = ErrorRed) } },
            dismissButton = { TextButton(onClick = close) { Text("Cancel", color = TextDim) } })
        return
    }
    if (n is Naming.Edit) return EditDialog(state, n.t, close)
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
    var name by remember { mutableStateOf(if (n is Naming.Rename) n.name else "") }
    AlertDialog(onDismissRequest = close, title = { Text(if (n is Naming.Rename) "Rename playlist" else "New playlist") },
        text = { TextBox(name, { name = it }, singleLine = true, placeholder = { Text("Name") }) },
        confirmButton = {
            TextButton(onClick = {
                when (n) {
                    is Naming.New -> state.createPlaylist(name.trim(), listOfNotNull(n.first) + n.more)
                    is Naming.Rename -> state.renamePlaylist(n.id, name.trim())
                    else -> Unit
                }
                close()
            }) { Text(if (n is Naming.Rename) "Rename" else "Create", color = Accent) }
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
    note: String? = null, trailing: (@Composable () -> Unit)? = null, selected: Boolean = false,
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

/** A song from your library, with its right-click menu (for all selected songs when several are selected). */
@Composable
private fun TrackRow(
    ctx: Ctx, list: List<Track>, i: Int, numbered: Boolean, inPlaylist: String? = null,
    selected: Set<String> = emptySet(), onSelect: ((Int, Boolean, Boolean) -> Unit)? = null,
) {
    val t = list[i]
    val s = ctx.state
    ContextMenuArea(items = {
        val many = list.filter { it.id in selected }.takeIf { t.id in selected && it.size > 1 }
        if (many != null) buildList {
            val n = many.size
            ctx.player?.let { p ->
                add(ContextMenuItem("Play $n songs") { ctx.play(many, 0) })
                add(ContextMenuItem("Play next") { many.reversed().forEach { p.playNext(s.item(it)) } })
                add(ContextMenuItem("Add to queue") { many.forEach { p.enqueue(s.item(it)) } })
            }
            val canLike = many.filter { s.canSave(it.id) }
            if (canLike.any { !s.isLiked(it.id) }) add(ContextMenuItem("Add $n to Liked Songs") { canLike.filterNot { s.isLiked(it.id) }.forEach { s.toggleLike(it.id) } })
            else if (canLike.isNotEmpty()) add(ContextMenuItem("Remove $n from Liked Songs") { canLike.forEach { s.toggleLike(it.id) } })
            s.collection.playlists.filter { it.id != inPlaylist }.forEach { pl -> add(ContextMenuItem("Add $n to “${pl.name}”") { many.forEach { s.addToPlaylist(pl.id, it.id) } }) }
            add(ContextMenuItem("Add $n to new playlist…") { ctx.dialog(Naming.New(null, many.map { it.id })) })
            if (inPlaylist != null) add(ContextMenuItem("Remove $n from this playlist") { many.forEach { s.removeFromPlaylist(inPlaylist, it.id) } })
            many.mapNotNull { Downloads.forTrack(it, s.settings.server) }.takeIf { it.isNotEmpty() }?.let { d -> add(ContextMenuItem("Download ${d.size}") { ctx.download(d) }) }
            val editable = many.filterNot(s::isPreviewRow)
            if (editable.any { s.meta.containsKey(it.id) }) add(ContextMenuItem("Restore original tags") { editable.forEach { Overrides.restore(it.id) }; ctx.toast("Original tags restored") })
            if (editable.isNotEmpty()) add(ContextMenuItem("Edit details of ${editable.size} songs…") { ctx.dialog(Naming.EditMany(editable)) })
        } else buildList {
            ctx.player?.let { p ->
                add(ContextMenuItem("Play next") { p.playNext(s.item(t)) })
                add(ContextMenuItem("Add to queue") { p.enqueue(s.item(t)) })
                add(ContextMenuItem("Start radio") { ctx.radio(s.item(t), t.artist) })
            }
            add(ContextMenuItem(if (s.isLiked(t.id)) "Remove from Liked Songs" else "Add to Liked Songs") { s.toggleLike(t.id) })
            s.collection.playlists.filter { it.id != inPlaylist }.forEach { pl -> add(ContextMenuItem("Add to “${pl.name}”") { s.addToPlaylist(pl.id, t.id) }) }
            add(ContextMenuItem("Add to new playlist…") { ctx.dialog(Naming.New(t.id)) })
            if (inPlaylist != null) add(ContextMenuItem("Remove from this playlist") { s.removeFromPlaylist(inPlaylist, t.id) })
            add(ContextMenuItem("Go to album") {
                Grouping.albums(s.library).firstOrNull { it.key == t.albumKey }?.let { a -> ctx.push(View.LocalList(a.title, "Album · ${a.artist}", a.artUri, a.tracks)) }
            })
            add(ContextMenuItem("Go to artist") {
                Grouping.artists(s.library).firstOrNull { Match.norm(it.name) == Match.norm(t.artist) }?.let { a -> ctx.push(View.LibraryArtist(a.name)) }
            })
            Downloads.forTrack(t, s.settings.server)?.let { d -> add(ContextMenuItem("Download") { ctx.download(listOf(d)) }) }
            val r = s.remoteFor(t)?.takeIf { it.source == io.github.realkhfan.onda.core.Source.DEEZER || it.source == io.github.realkhfan.onda.core.Source.ITUNES }
            add(ContextMenuItem(if (r != null) "Copy link" else "Copy song name") { ctx.copy(shareText(t.title, t.artist, r), if (r != null) "Link" else "Song name") })
            if (s.meta.containsKey(t.id)) add(ContextMenuItem("Restore original tags") { Overrides.restore(t.id); ctx.toast("Original tags restored") })
            if (!s.isPreviewRow(t)) add(ContextMenuItem("Edit details…") { ctx.dialog(Naming.Edit(t)) })
        }
    }) {
        SongRow(if (numbered) i + 1 else null, t.title, t.artist, t.album, t.durationMs, t.artUri, ctx.player?.current?.id == t.id,
            note = if (s.isPreviewRow(t)) "Preview" else null, selected = t.id in selected,
            onCtrlClick = onSelect?.let { f -> { f(i, true, false) } }, onShiftClick = onSelect?.let { f -> { f(i, false, true) } },
            trailing = { if (s.isLiked(t.id)) Icon(Icons.Rounded.Favorite, "Liked", tint = Accent, modifier = Modifier.padding(12.dp)) }) {
            onSelect?.invoke(i, false, false)
            ctx.play(list, i)
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
            add(ContextMenuItem(if (s.isLiked(r.key)) "Remove from Liked Songs" else "Add to Liked Songs") { s.remember(r); s.toggleLike(r.key) })
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
private fun Sidebar(state: AppState, top: View, go: (View) -> Unit, newPlaylist: () -> Unit) {
    @Composable
    fun Entry(v: View, label: String, icon: ImageVector, big: Boolean = false) {
        val on = top == v
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (on) Raised else Color.Transparent)
            .clickable { go(v) }.padding(horizontal = 10.dp, vertical = if (big) 10.dp else 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (on) Accent else TextDim, modifier = Modifier.size(if (big) 24.dp else 20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, color = if (on) TextMain else TextDim, fontWeight = FontWeight.SemiBold, fontSize = if (big) 16.sp else 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Column(Modifier.width(240.dp).fillMaxHeight().background(Side).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)) {
            // Settings live behind your avatar, like on the phone.
            IconButton(onClick = { go(View.Settings) }, Modifier.size(36.dp).clip(CircleShape).background(if (top == View.Settings) Accent else Raised)) {
                Icon(Icons.Rounded.Person, "Settings", tint = if (top == View.Settings) Color.Black else TextMain)
            }
            Spacer(Modifier.width(10.dp))
            Text("Onda", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
        Entry(View.Home, "Home", Icons.Rounded.Home, big = true)
        Entry(View.Search, "Search", Icons.Rounded.Search, big = true)
        Entry(View.Stats, "Your stats", Icons.Rounded.BarChart)
        Entry(View.History, "History", Icons.Rounded.History)
        Row(Modifier.padding(top = 18.dp, start = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Your library", color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = newPlaylist, Modifier.size(32.dp)) { Icon(Icons.Rounded.Add, "New playlist", tint = TextDim) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Entry(View.Songs, "Songs", Icons.Rounded.MusicNote)
            Entry(View.Albums, "Albums", Icons.Rounded.Album)
            Entry(View.Artists, "Artists", Icons.Rounded.Person)
            if (state.settings.folders.isNotEmpty()) Entry(View.Folder(null), "Folders", Icons.Rounded.Folder)
            Spacer(Modifier.height(8.dp))
            Entry(View.Liked, "Liked Songs", Icons.Rounded.Favorite)
            Entry(View.Want, "Want to listen", Icons.Rounded.Bookmark)
            // Pinned playlists first (newest pin on top), then the rest; right-click to pin or unpin.
            val pins = state.settings.pinned
            state.collection.playlists.sortedBy { pins.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } }.forEach { pl ->
                ContextMenuArea(items = { listOf(ContextMenuItem(if (pl.id in pins) "Unpin" else "Pin to the top") { state.togglePin(pl.id) }) }) {
                    Entry(View.Playlist(pl.id), pl.name, if (pl.id in pins) Icons.Rounded.PushPin else Icons.AutoMirrored.Rounded.QueueMusic)
                }
            }
            state.serverPlaylists.forEach { Entry(View.ServerPlaylist(it), it.name, Icons.Rounded.Dns) }
        }
        if (state.scanning || state.syncing) {
            Text(if (state.scanning) "Scanning your folders…" else "Syncing your server…", color = TextDim, fontSize = 12.sp)
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
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        if (s.releases.items.isNotEmpty()) {
            item { Section("New releases") }
            item {
                val today = java.time.LocalDate.now()
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.releases.items) { a -> Card(a.cover, a.title, "${a.artist} · ${NewReleases.ago(a, today)}") { open(View.CatalogAlbum(a)) } }
                }
            }
        }
        if (s.recent.isNotEmpty()) {
            item { Section("Recently played") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.recent) { it -> Card(it.art, it.title, it.artist) { ctx.player?.play(listOf(it), 0) } }
                }
            }
        }
        if (s.recentlyAdded.isNotEmpty()) {
            item { Section("Recently added") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.recentlyAdded) { a -> Card(a.artUri, a.title, a.artist) { open(View.LocalList(a.title, "Album · ${a.artist}", a.artUri, a.tracks)) } }
                }
            }
        }
        if (s.library.isEmpty() && !s.scanning) item { Note("Add your music folder or your Navidrome server: tap the person icon at the top left.") }
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
enum class Sort(val label: String) { CUSTOM("Custom order"), TITLE("Title"), ARTIST("Artist"), ALBUM("Album"), PLAYS("Most played"), LENGTH("Length") }

internal fun sorted(tracks: List<Track>, sort: Sort, plays: Map<String, Int>): List<Track> = when (sort) {
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
    inPlaylist: String? = null, onMove: ((Int, Int) -> Unit)? = null, mosaic: Boolean = false,
    above: (androidx.compose.foundation.lazy.LazyListScope.() -> Unit)? = null, actions: @Composable () -> Unit = {},
) {
    val reorder = remember { ReorderState() }
    var sort by remember(title) { mutableStateOf(Sort.CUSTOM) }
    var selected by remember(title) { mutableStateOf(emptySet<String>()) }
    var anchor by remember(title) { mutableIntStateOf(-1) }
    val shown = remember(tracks, sort, ctx.state.playCounts) { sorted(tracks, sort, ctx.state.playCounts) }
    val onSelect = { i: Int, ctrl: Boolean, shift: Boolean ->
        selected = select(selected, shown, anchor, i, ctrl, shift)
        if (!shift) anchor = i
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header(title, "$subtitle · ${tracks.size} songs" + if (selected.size > 1) " · ${selected.size} selected" else "", art, back = back,
                mosaic = if (mosaic) tracks.map { it.artUri } else null) {
                actions()
                Spacer(Modifier.width(8.dp))
                var open by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { open = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, null, tint = TextDim); Spacer(Modifier.width(6.dp)); Text(sort.label, color = TextDim) }
                    DropdownMenu(open, { open = false }, modifier = Modifier.background(Color(0xFF2E2E38))) {
                        Sort.entries.forEach { o -> DropdownMenuItem({ Text(o.label, color = if (o == sort) Accent else TextMain) }, { sort = o; open = false }) }
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (tracks.isNotEmpty()) Button(onClick = { ctx.play(shown, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
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
        SongList(ctx, "Songs", "Your library", shown)
    }
}

@Composable
private fun Albums(state: AppState, onOpen: (Album) -> Unit, onOpenSaved: (RemoteAlbum) -> Unit) {
    val albums = remember(state.library) { Grouping.albums(state.library) }
    LazyVerticalGrid(GridCells.Adaptive(180.dp), Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(state.collection.albums) { a -> Card(a.cover, a.title, "Saved · ${a.artist}") { onOpenSaved(a) } }
        items(albums) { a -> Card(a.artUri, a.title, a.artist) { onOpen(a) } }
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
    val list = ctx.state.collection.want
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header("Want to listen", "${list.size} songs you don't own yet", back = back) {
                if (list.isNotEmpty()) Button(onClick = { ctx.playRemote(list, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
            }
        }
        if (list.isEmpty()) item { Note("Songs you save with + in Search show up here.") }
        items(list.size) { i -> RemoteRow(ctx, list, i) }
    }
}

@Composable
private fun Search(ctx: Ctx) {
    val state = ctx.state
    var q by remember { mutableStateOf("") }
    var online by remember { mutableStateOf<List<RemoteTrack>>(emptyList()) }
    var artists by remember { mutableStateOf<List<RemoteArtist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<RemoteAlbum>>(emptyList()) }
    var free by remember { mutableStateOf<List<RemoteTrack>>(emptyList()) }
    var problem by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(q) {
        online = emptyList(); artists = emptyList(); albums = emptyList(); free = emptyList(); problem = null
        if (q.trim().length < 2) return@LaunchedEffect
        delay(350) // wait until typing pauses
        coroutineScope {
            val dz = async { runCatching { Deezer.search(q, 20) } }
            val ar = async { runCatching { Deezer.searchArtists(q) } }
            val al = async { runCatching { Deezer.searchAlbums(q) } }
            val ov = async { runCatching { FreeCatalogs.openverse(q) } }
            val au = async { runCatching { FreeCatalogs.audius(q) } }
            val ap = async { if (state.settings.apple) runCatching { Itunes.searchSongs(q) } else Result.success(emptyList()) }
            // AI "artists" never show; remixes, sped-up, live and karaoke only when the search words ask for them.
            val versions = Discovery.asksFor(q)
            fun ok(t: RemoteTrack) = !state.isAi(t.artist) && (versions || !Discovery.isUnwanted(t.title, t.album))
            online = Match.mergeTracks(dz.await().getOrDefault(emptyList()), ap.await().getOrDefault(emptyList())).filter(::ok)
            artists = ar.await().getOrDefault(emptyList()).filterNot { state.isAi(it.name) }
            albums = al.await().getOrDefault(emptyList()).filterNot { state.isAi(it.artist) }
            free = (ov.await().getOrDefault(emptyList()) + au.await().getOrDefault(emptyList())).filter(::ok).take(10)
            if (dz.await().isFailure && ov.await().isFailure && au.await().isFailure) problem = "No connection to the online catalogs."
        }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(24.dp)) {
                Text("Search", color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                TextBox(q, { q = it }, placeholder = { Text("Songs, artists and albums, here and online") }, singleLine = true, modifier = Modifier.width(480.dp))
                problem?.let { Text(it, color = TextDim, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        // Your own songs are searched in Songs (the filter there); this is the online catalog.
        artists.firstOrNull { Match.norm(it.name) == Match.norm(q) }?.let { top ->
            item { Section("Top result") }
            item {
                Row(Modifier.padding(horizontal = 24.dp).width(420.dp).clip(RoundedCornerShape(10.dp)).background(Raised)
                    .clickable { ctx.push(View.CatalogArtist(top)) }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art(top.picture, 92, round = true)
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text(top.name, color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("Artist", color = TextDim)
                    }
                }
            }
        }
        if (online.isNotEmpty()) {
            item { Section("Songs") }
            items(minOf(online.size, 8)) { i -> RemoteRow(ctx, online, i) }
        }
        if (artists.isNotEmpty()) {
            item { Section("Artists") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(artists) { a ->
                        Column(Modifier.width(140.dp).clip(RoundedCornerShape(8.dp)).clickable { ctx.push(View.CatalogArtist(a)) }.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Art(a.picture, 124, round = true)
                            Text(a.name, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                            Text("Artist", color = TextDim, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        if (albums.isNotEmpty()) {
            item { Section("Albums") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(albums) { a -> Card(a.cover, a.title, a.artist) { ctx.push(View.CatalogAlbum(a)) } }
                }
            }
        }
        if (free.isNotEmpty()) {
            item { Section("Free full songs") }
            items(free.size) { i -> RemoteRow(ctx, free, i) }
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
                OutlinedButton(onClick = { s.toggleFollow(artist.copy(picture = picture)) }) { Text(if (following) "Following" else "Follow", color = if (following) Accent else TextMain) }
                Spacer(Modifier.width(6.dp))
                TextButton(onClick = { s.toggleBlocked(artist.name) }) { Text(if (s.isBlocked(artist.name)) "Suggest again" else "Don't suggest", color = TextDim) }
                if (!s.onAiList(artist.name)) TextButton(onClick = { s.toggleAi(artist.name) }) { Text(if (s.isAi(artist.name)) "Not AI after all" else "This is AI", color = TextDim) }
                Spacer(Modifier.width(6.dp))
                if (popular.isNotEmpty()) Button(onClick = { ctx.playRemote(popular, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play popular") }
            }
        }
        if (top == null) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(24.dp), color = Accent, trackColor = Raised) }
        if (popular.isNotEmpty()) item { Section("Popular") }
        items(popular.size) { i -> RemoteRow(ctx, popular, i, numbered = true) }
        if (releases.isNotEmpty()) {
            item { Section("Releases") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        runCatching { Deezer.album(album.id) }.getOrNull()?.let { (a, t) -> info = a; tracks = t } ?: run { tracks = emptyList() }
    }
    val list = tracks.orEmpty()
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header(info.title, listOfNotNull("Album · ${info.artist}", info.date).joinToString(" · "), info.cover, back = back) {
                val saved = ctx.state.isSaved(info)
                TextButton(onClick = { ctx.state.toggleAlbum(info) }) { Text(if (saved) "Saved to your library" else "Save to your library", color = if (saved) Accent else TextDim) }
                if (info.artistId > 0) TextButton(onClick = { ctx.push(View.CatalogArtist(RemoteArtist(info.artistId, info.artist, null))) }) { Text("Go to artist", color = Accent) }
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
        item { Section("Music folders") }
        items(state.settings.folders) { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f, color = TextMain, modifier = Modifier.weight(1f))
                IconButton(onClick = { state.removeFolder(f) }) { Icon(Icons.Rounded.Close, "Remove $f", tint = TextDim) }
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
                Toggle("Draw without the graphics card", "Turn on if Onda doesn't show up while a game is running. Takes effect the next time Onda starts.",
                    state.settings.software, state::setSoftware)
            }
        }
        item { Section("Playback and discovery") }
        item { Toggle("Gapless playback", "No pause between songs, so live albums and DJ mixes flow on. For your files and server songs.", state.settings.gapless) { state.setGapless(it); ctx.player?.gapless = it } }
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
        item { Toggle("ListenBrainz suggestions", "Adds similar artists from ListenBrainz's open listening data to your mixes and similar songs. Only artist names are sent.", state.settings.listenBrainz, state::setListenBrainz) }
        item { Section("Backup") }
        item {
            var note by remember { mutableStateOf<String?>(null) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Playlists, likes, history, follows, saved albums and settings in one file. Not your server login or folders.", color = TextDim, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            val name = "onda-backup-${java.time.LocalDate.now()}.zip"
                            val path = when (val r = FolderPicker.file(true, System.getProperty("user.home") + "/" + name, "Save backup")) {
                                is FolderPicker.Result.Picked -> r.path
                                FolderPicker.Result.Cancelled -> return@launch
                                FolderPicker.Result.NoPicker -> System.getProperty("user.home") + "/" + name
                            }
                            note = runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { state.backup(File(path)) }; "Saved to $path" }
                                .getOrElse { "Couldn't save the backup." }
                        }
                    }) { Text("Back up now") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            val r = FolderPicker.file(false, System.getProperty("user.home"), "Restore a backup")
                            if (r is FolderPicker.Result.Picked) note = if (runCatching { state.restore(File(r.path)) }.getOrDefault(false))
                                "Restored. Your previous data is kept in before-restore.zip." else "That file isn't an Onda backup."
                            if (r is FolderPicker.Result.NoPicker) note = "No file picker found on this system."
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
                            }
                        }
                    }) { Text(if (state.settings.backupDir == null) "Choose folder" else "Change", color = Accent) }
                    if (state.settings.backupDir != null) TextButton(onClick = { state.setBackupDir(null) }) { Text("Off", color = TextDim) }
                }
            }
        }
        item { Section("Keyboard") }
        item {
            Text("Space play/pause · Ctrl+→ / Ctrl+← next / previous · → / ← seek 5 s · ↑ / ↓ volume · Ctrl+L like · Ctrl+F search · Ctrl+, settings · Ctrl + / Ctrl − size · Esc back · Ctrl+click or Shift+click selects several songs",
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
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close $title", tint = TextDim) }
        }
        content()
    }
}

@Composable
private fun QueuePanel(player: Player, onSave: () -> Unit, onClose: () -> Unit) = PanelFrame("Queue", onClose) {
    val upcoming = player.upcoming()
    val reorder = remember { ReorderState() }
    LazyColumn {
        if (player.queue.size > 1) item { TextButton(onClick = onSave, modifier = Modifier.padding(start = 8.dp)) { Text("Save queue as playlist", color = Accent) } }
        player.current?.let { c -> item { Section("Now playing") }; item { QueueRow(c, true, onClick = {}) } }
        if (upcoming.isNotEmpty()) item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Section("Next")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { player.clearUpcoming() }) { Text("Clear", color = Accent) }
            }
        }
        items(upcoming.size) { k ->
            val (i, it) = upcoming[k]
            QueueRow(it, false, Modifier.reorderRow(reorder, k, upcoming.size), onRemove = { player.removeAt(i) },
                handle = { DragHandle(reorder, k, upcoming.size, it.title) { from, to -> player.moveUpcoming(from, to) } }) { player.jumpTo(i) }
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
        if (onRemove != null) IconButton(onClick = onRemove, Modifier.size(36.dp)) { Icon(Icons.Rounded.Close, "Remove ${it.title} from the queue", tint = TextDim) }
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
    val tint = if (big) TextMain.copy(alpha = 0.8f) else TextDim
    Row(Modifier.padding(if (big) 44.dp else 12.dp, 0.dp, 12.dp, 0.dp), verticalAlignment = Alignment.CenterVertically) {
        if (ls.lyrics?.synced?.isNotEmpty() == true) {
            TextButton(onClick = { state.setLyricsPick(cur.id, picked, shift + 500) }) { Text("Earlier", color = tint, fontSize = 12.sp) }
            Text(if (shift == 0L) "in time" else "%+.1f s".format(shift / 1000.0), color = tint, fontSize = 12.sp)
            TextButton(onClick = { state.setLyricsPick(cur.id, picked, shift - 500) }) { Text("Later", color = tint, fontSize = 12.sp) }
        }
        TextButton(onClick = { choosing = true }) { Text("Other lyrics…", color = tint, fontSize = 12.sp) }
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
    val active = if (lines.isEmpty()) -1 else Lrc.indexAt(lines, player.positionMs + ls.shift)
    val list = rememberLazyListState()
    LaunchedEffect(active) { if (active >= 0) list.animateScrollToItem((active - if (big) 4 else 3).coerceAtLeast(0)) }
    val (on, off) = if (big) 40.sp to 32.sp else 26.sp to 20.sp
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
                Text(l.text.ifBlank { "♪" }, color = if (i == active) TextMain else if (big) TextMain.copy(alpha = 0.45f) else TextDim,
                    fontSize = if (i == active) on else off, fontWeight = if (i == active) FontWeight.Bold else FontWeight.SemiBold,
                    lineHeight = (if (i == active) on else off) * 1.2f,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { player.seek(l.timeMs - ls.shift) }.padding(vertical = if (big) 10.dp else 6.dp, horizontal = 4.dp))
            }
        }
        ls.lyrics.plain != null -> Text(ls.lyrics.plain, color = TextMain, fontSize = if (big) 28.sp else 18.sp,
            modifier = Modifier.padding(if (big) 48.dp else 16.dp).verticalScroll(rememberScrollState()))
        else -> Note("No lyrics for this song.")
    }
    }
}

@Composable
private fun LyricsPanel(state: AppState, player: Player, onExpand: () -> Unit, onClose: () -> Unit) {
    val ls = rememberLyrics(state, player)
    Column(Modifier.width(360.dp).fillMaxHeight().background(Side)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Lyrics", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onExpand) { Icon(Icons.Rounded.OpenInFull, "Show lyrics full screen", tint = TextDim) }
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close lyrics", tint = TextDim) }
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
            IconButton(onClick = onCollapse) { Icon(Icons.Rounded.CloseFullscreen, "Back to the side panel", tint = TextMain) }
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close lyrics", tint = TextMain) }
        }
        LyricsLines(state, player, ls, big = true)
    }
}

/** The cover's dominant colour, darkened for white text (the phone's ArtColor); the accent's dark violet while loading. */
@Composable
private fun rememberArtTint(art: String?): Color {
    var c by remember(art) { mutableStateOf(Color(0xFF2B2140)) }
    LaunchedEffect(art) {
        if (art == null) return@LaunchedEffect
        val argb = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val img = if (art.startsWith("/")) javax.imageio.ImageIO.read(File(art))
                else java.net.URL(art).openConnection().apply { setRequestProperty("User-Agent", "Onda"); connectTimeout = 5000; readTimeout = 8000 }
                    .getInputStream().use { javax.imageio.ImageIO.read(it) }
                val small = java.awt.image.BufferedImage(24, 24, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                small.createGraphics().apply { drawImage(img, 0, 0, 24, 24, null); dispose() }
                io.github.realkhfan.onda.core.ArtColor.pick(small.getRGB(0, 0, 24, 24, null, 0, 24))
            }.getOrNull()
        }
        if (argb != null) c = Color(argb)
    }
    return c
}

/* ---------- Player bar ---------- */

@Composable
private fun PlayerBar(ctx: Ctx, panel: Panel, toggle: (Panel) -> Unit) {
    val player = ctx.player
    val state = ctx.state
    val cur = player?.current
    Row(Modifier.fillMaxWidth().height(96.dp).background(Side).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (cur != null) {
                Art(cur.art, 56)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f, fill = false)) {
                    Text(cur.title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    val src = when { player.fullFrom != null -> "${player.fullFrom} · "; cur.preview -> "Preview · "; else -> "" }
                    Text(src + cur.artist, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    player.error?.let { Text(it, color = ErrorRed, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                if (state.canSave(cur.id)) {
                    val liked = state.isLiked(cur.id)
                    IconButton(onClick = { state.toggleLike(cur.id) }) {
                        Icon(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (liked) "Remove from Liked Songs" else "Add to Liked Songs",
                            tint = if (liked) Accent else TextDim)
                    }
                }
            }
        }
        Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { player?.toggleShuffle() }) { Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (player?.shuffle == true) Accent else TextDim) }
                IconButton(onClick = { player?.previous() }) { Icon(Icons.Rounded.SkipPrevious, "Previous", tint = TextMain) }
                IconButton(onClick = { player?.toggle() }, Modifier.size(44.dp).clip(CircleShape).background(Accent)) {
                    Icon(if (player?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (player?.playing == true) "Pause" else "Play", tint = Color.Black)
                }
                IconButton(onClick = { player?.next() }) { Icon(Icons.Rounded.SkipNext, "Next", tint = TextMain) }
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
                    modifier = Modifier.width(420.dp).padding(horizontal = 8.dp))
                Text(time(dur), color = TextDim, fontSize = 11.sp)
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            SleepButton(player)
            IconButton(onClick = { state.mini = !state.mini }) { Icon(Icons.Rounded.PictureInPictureAlt, "Mini player", tint = if (state.mini) Accent else TextDim) }
            IconButton(onClick = { toggle(Panel.LYRICS) }) { Icon(Icons.Rounded.Lyrics, "Lyrics", tint = if (panel == Panel.LYRICS) Accent else TextDim) }
            IconButton(onClick = { toggle(Panel.QUEUE) }) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Queue", tint = if (panel == Panel.QUEUE) Accent else TextDim) }
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = TextDim)
            ThinSlider((player?.volume ?: 0.8).toFloat(), "Volume", { player?.changeVolume(it.toDouble()) },
                onDone = { player?.let { state.setVolume(it.volume) } }, modifier = Modifier.width(120.dp).padding(start = 8.dp))
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = player::previous) { Icon(Icons.Rounded.SkipPrevious, "Previous", tint = TextMain) }
                IconButton(onClick = player::toggle, Modifier.size(40.dp).clip(CircleShape).background(Accent)) {
                    Icon(if (player.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (player.playing) "Pause" else "Play", tint = Color.Black)
                }
                IconButton(onClick = { player.next() }) { Icon(Icons.Rounded.SkipNext, "Next", tint = TextMain) }
                if (cur != null && state.canSave(cur.id)) {
                    val liked = state.isLiked(cur.id)
                    IconButton(onClick = { state.toggleLike(cur.id) }) {
                        Icon(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (liked) "Remove from Liked Songs" else "Add to Liked Songs", tint = if (liked) Accent else TextDim)
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = openMain) { Icon(Icons.Rounded.OpenInFull, "Open Onda", tint = TextDim) }
            }
            val dur = player.durationMs
            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Raised)) {
                Box(Modifier.fillMaxWidth(if (dur > 0) (player.positionMs.toFloat() / dur).coerceIn(0f, 1f) else 0f).height(3.dp).background(TextMain))
            }
        }
    }
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
            IconButton(onClick = { open = true }) { Icon(Icons.Rounded.Bedtime, "Sleep timer", tint = if (on) Accent else TextDim) }
            if (player.sleepAt > 0) Text("${((player.sleepAt - now) / 60_000 + 1).coerceAtLeast(1)} min", color = Accent, fontSize = 11.sp)
        }
        DropdownMenu(open, { open = false }, modifier = Modifier.background(Color(0xFF2E2E38))) {
            Text("Pause after…", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(16.dp, 4.dp))
            listOf(15, 30, 45, 60, 90).forEach { m ->
                DropdownMenuItem({ Text("$m minutes", color = TextMain) }, { player.sleepAfterSong = false; player.sleepAt = System.currentTimeMillis() + m * 60_000L; now = System.currentTimeMillis(); open = false })
            }
            DropdownMenuItem({ Text("End of this song", color = TextMain) }, { player.sleepAt = 0; player.sleepAfterSong = true; open = false })
            if (on) DropdownMenuItem({ Text("Turn off", color = Accent) }, { player.sleepAt = 0; player.sleepAfterSong = false; open = false })
        }
    }
}

/** A thin white bar like the phone's: click or drag to set; screen readers can set it too. */
@Composable
private fun ThinSlider(value: Float, label: String, onChange: (Float) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    var width by remember { mutableIntStateOf(1) }
    val change by rememberUpdatedState(onChange)
    val done by rememberUpdatedState(onDone)
    val v = value.coerceIn(0f, 1f)
    Box(
        modifier.height(24.dp).onSizeChanged { width = it.width.coerceAtLeast(1) }
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
    Icon(Icons.Rounded.DragHandle, "Move $label", tint = TextDim,
        modifier = Modifier.size(40.dp).padding(8.dp)
            .semantics {
                customActions = listOfNotNull(
                    if (index > 0) CustomAccessibilityAction("Move up") { onMove(index, index - 1); true } else null,
                    if (index < count - 1) CustomAccessibilityAction("Move down") { onMove(index, index + 1); true } else null,
                )
            }
            .pointerInput(index, count) {
                detectDragGestures(
                    onDragStart = { s.dragging = index; s.offset = 0f },
                    onDrag = { c, d -> c.consume(); s.offset += d.y },
                    onDragEnd = { val f = s.dragging; val t = s.target(count); s.dragging = -1; s.offset = 0f; if (f >= 0 && t != f) onMove(f, t) },
                    onDragCancel = { s.dragging = -1; s.offset = 0f },
                )
            })
}

/** Downloads every song of a list that may be downloaded (server songs, free songs whose artist allows it). */
@Composable
private fun DownloadAll(ctx: Ctx, tracks: List<Track>) {
    val s = ctx.state
    val items = tracks.mapNotNull { t -> Downloads.forTrack(t, s.settings.server) ?: s.remoteFor(t)?.let(Downloads::forRemote) }
    if (items.isNotEmpty()) IconButton(onClick = { ctx.download(items) }) { Icon(Icons.Rounded.Download, "Download ${items.size} songs", tint = TextDim) }
}

/** A text field whose keys stay in it: a space never pauses the music, arrows never seek. Enter runs [onEnter]. */
@Composable
internal fun TextBox(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null, singleLine: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    onEnter: (() -> Unit)? = null,
) = OutlinedTextField(value, onValueChange, modifier.onKeyEvent { e ->
    if (e.type != KeyEventType.KeyDown) false
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

/** Top artists and songs from your own listening history (kept on this PC only). */
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
        item { Header("Your stats", "${plays.size} plays · counted on this PC only, never sent anywhere") }
        item {
            Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                periods.forEachIndexed { i, (label, _) ->
                    if (i == period) Button(onClick = {}) { Text(label) } else OutlinedButton(onClick = { period = i }) { Text(label) }
                }
            }
        }
        if (plays.isEmpty()) item { Note("Play some songs and your top artists and songs show up here.") }
        else {
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
            item { Section("Top songs") }
            items(songs.size) { i ->
                val (t, n) = songs[i]
                SongRow(i + 1, t.title, t.artist, t.album, t.durationMs, t.artUri, ctx.player?.current?.id == t.id, note = "$n plays") { ctx.play(playable, i) }
            }
        }
    }
}

/** What you played, newest first, by day (this PC only). */
@Composable
private fun HistoryPage(ctx: Ctx) {
    val state = ctx.state
    val rows = remember(state.plays, state.byId) { state.plays.asReversed().take(300).mapNotNull { p -> state.trackFor(p.key)?.let { p to it } } }
    val tracks = rows.map { it.second }
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now()
    fun day(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    LazyColumn(Modifier.fillMaxSize()) {
        item { Header("History", "The last ${rows.size} songs you played · kept on this PC only") }
        if (rows.isEmpty()) item { Note("Songs you play show up here.") }
        rows.indices.groupBy { day(rows[it].first.atMs) }.forEach { (d, idx) ->
            item { Section(when (d) { today -> "Today"; today.minusDays(1) -> "Yesterday"; else -> d.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM")) }) }
            items(idx) { i ->
                val (p, t) = rows[i]
                SongRow(null, t.title, t.artist, t.album, t.durationMs, t.artUri, ctx.player?.current?.id == t.id,
                    note = java.time.Instant.ofEpochMilli(p.atMs).atZone(zone).toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))) { ctx.play(tracks, i) }
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
                        SongRow(null, t.album, where, "${ctx.state.playCounts[t.id] ?: 0} plays", t.durationMs, t.artUri, ctx.player?.current?.id == t.id) { ctx.play(listOf(t), 0) }
                    }
                    TextButton(onClick = { state.toggleHidden(t.id) }, modifier = Modifier.padding(end = 16.dp)) { Text("Hide", color = TextDim) }
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
                androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(albums) { al -> Card(al.artUri, al.title, "${al.tracks.size} songs") { ctx.push(View.LocalList(al.title, "Album · ${al.artist}", al.artUri, al.tracks)) } }
                }
            }
        }
        item { Section("All songs") }
        items(a.tracks.size) { i -> TrackRow(ctx, a.tracks, i, numbered = true) }
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
            val r = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Renames.apply(p) }
            state.rescanNow()
            state.migrateIds(r.moved)
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
