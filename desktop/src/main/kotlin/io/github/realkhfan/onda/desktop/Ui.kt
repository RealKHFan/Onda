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
                        LyricsFull(player, onCollapse = { lyricsBig = false }) { panel = Panel.NONE; lyricsBig = false }
                    } else when (val v = stack.last()) {
                        View.Home -> HomePage(ctx, ::push)
                        View.Songs -> Songs(ctx)
                        View.Albums -> Albums(state, { a -> push(View.LocalList(a.title, "Album · ${a.artist}", a.artUri, a.tracks)) }) { push(View.CatalogAlbum(it)) }
                        View.Artists -> Artists(state, { a -> push(View.LocalList(a.name, "Artist", a.artUri, a.tracks)) }) { push(View.CatalogArtist(it)) }
                        is View.Mix -> MixPage(ctx, v.kind, back)
                        View.Search -> Search(ctx)
                        View.Settings -> SettingsPage(ctx)
                        View.Match -> MatchPage(ctx, back)
                        View.Rename -> RenamePage(ctx, back)
                        View.Liked -> SongList(ctx, "Liked Songs", "Playlist", state.likedTracks, back = back)
                        View.Want -> WantPage(ctx, back)
                        is View.Playlist -> {
                            val pl = state.collection.playlists.firstOrNull { it.id == v.id }
                            if (pl == null) LaunchedEffect(Unit) { go(View.Songs) } else {
                                SongList(ctx, pl.name, "Playlist", state.playlistTracks(pl.id), back = back, inPlaylist = pl.id,
                                    onMove = { from, to -> state.movePlaylistTrack(pl.id, from, to) }) {
                                    IconButton(onClick = { naming = Naming.Rename(pl.id, pl.name) }) { Icon(Icons.Rounded.Edit, "Rename playlist", tint = TextDim) }
                                    IconButton(onClick = { naming = Naming.Delete(pl.id, pl.name) }) { Icon(Icons.Rounded.Delete, "Delete playlist", tint = TextDim) }
                                    DownloadAll(ctx, state.playlistTracks(pl.id))
                                }
                            }
                        }
                        is View.ServerPlaylist -> SongList(ctx, v.p.name, "Playlist · Music server", state.serverPlaylistTracks(v.p), back = back) {
                            DownloadAll(ctx, state.serverPlaylistTracks(v.p))
                        }
                        is View.LocalList -> SongList(ctx, v.title, v.subtitle, v.tracks, art = v.art, back = back)
                        is View.CatalogArtist -> CatalogArtistPage(ctx, v.a, back)
                        is View.CatalogAlbum -> CatalogAlbumPage(ctx, v.a, back)
                    }
                }
                if (player != null && panel == Panel.QUEUE) QueuePanel(player) { panel = Panel.NONE }
                if (player != null && panel == Panel.LYRICS && !lyricsBig) LyricsPanel(player, onExpand = { lyricsBig = true }) { panel = Panel.NONE }
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
    fun download(items: List<Downloads.Item>) = toast(Downloads.start(state, items) ?: "Downloading to “${Downloads.FOLDER}” in your music folder")
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
    data class New(val first: String? = null) : Naming
    data class Rename(val id: String, val name: String) : Naming
    data class Delete(val id: String, val name: String) : Naming
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
    var name by remember { mutableStateOf(if (n is Naming.Rename) n.name else "") }
    AlertDialog(onDismissRequest = close, title = { Text(if (n is Naming.Rename) "Rename playlist" else "New playlist") },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, placeholder = { Text("Name") }) },
        confirmButton = {
            TextButton(onClick = {
                when (n) {
                    is Naming.New -> state.createPlaylist(name.trim(), listOfNotNull(n.first))
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
private fun Art(model: String?, size: Int, round: Boolean = false) {
    Box(Modifier.size(size.dp).clip(if (round) CircleShape else RoundedCornerShape(6.dp)).background(Raised), contentAlignment = Alignment.Center) {
        if (model == null) Icon(Icons.Rounded.MusicNote, null, tint = Accent)
        else AsyncImage(if (model.startsWith("/")) File(model) else model, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

private fun time(ms: Long): String = (ms / 1000).let { "%d:%02d".format(it / 60, it % 60) }

@Composable
private fun Header(title: String, subtitle: String, art: String? = null, round: Boolean = false, back: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    Row(Modifier.padding(16.dp, 20.dp, 24.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) IconButton(onClick = back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TextMain) } else Spacer(Modifier.width(8.dp))
        if (art != null || round) { Art(art, 120, round); Spacer(Modifier.width(20.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = TextDim)
        }
        actions()
    }
}

@Composable
private fun Section(text: String) = Text(text, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(24.dp, 16.dp, 24.dp, 6.dp))

@Composable
private fun SongRow(
    n: Int?, title: String, artist: String, album: String, durMs: Long, art: String?, playing: Boolean,
    note: String? = null, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 24.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
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

/** A song from your library, with its right-click menu. */
@Composable
private fun TrackRow(ctx: Ctx, list: List<Track>, i: Int, numbered: Boolean, inPlaylist: String? = null) {
    val t = list[i]
    val s = ctx.state
    ContextMenuArea(items = {
        buildList {
            ctx.player?.let { p ->
                add(ContextMenuItem("Play next") { p.playNext(s.item(t)) })
                add(ContextMenuItem("Add to queue") { p.enqueue(s.item(t)) })
            }
            add(ContextMenuItem(if (s.isLiked(t.id)) "Remove from Liked Songs" else "Add to Liked Songs") { s.toggleLike(t.id) })
            s.collection.playlists.filter { it.id != inPlaylist }.forEach { pl -> add(ContextMenuItem("Add to “${pl.name}”") { s.addToPlaylist(pl.id, t.id) }) }
            add(ContextMenuItem("Add to new playlist…") { ctx.dialog(Naming.New(t.id)) })
            if (inPlaylist != null) add(ContextMenuItem("Remove from this playlist") { s.removeFromPlaylist(inPlaylist, t.id) })
            add(ContextMenuItem("Go to album") {
                Grouping.albums(s.library).firstOrNull { it.key == t.albumKey }?.let { a -> ctx.push(View.LocalList(a.title, "Album · ${a.artist}", a.artUri, a.tracks)) }
            })
            add(ContextMenuItem("Go to artist") {
                Grouping.artists(s.library).firstOrNull { Match.norm(it.name) == Match.norm(t.artist) }?.let { a -> ctx.push(View.LocalList(a.name, "Artist", a.artUri, a.tracks)) }
            })
            Downloads.forTrack(t, s.settings.server)?.let { d -> add(ContextMenuItem("Download") { ctx.download(listOf(d)) }) }
            val r = s.remoteFor(t)?.takeIf { it.source == io.github.realkhfan.onda.core.Source.DEEZER || it.source == io.github.realkhfan.onda.core.Source.ITUNES }
            add(ContextMenuItem(if (r != null) "Copy link" else "Copy song name") { ctx.copy(shareText(t.title, t.artist, r), if (r != null) "Link" else "Song name") })
            if (s.meta.containsKey(t.id)) add(ContextMenuItem("Restore original tags") { io.github.realkhfan.onda.data.Overrides.restore(t.id); ctx.toast("Original tags restored") })
        }
    }) {
        SongRow(if (numbered) i + 1 else null, t.title, t.artist, t.album, t.durationMs, t.artUri, ctx.player?.current?.id == t.id,
            note = if (s.isPreviewRow(t)) "Preview" else null,
            trailing = { if (s.isLiked(t.id)) Icon(Icons.Rounded.Favorite, "Liked", tint = Accent, modifier = Modifier.padding(12.dp)) }) { ctx.play(list, i) }
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
        Row(Modifier.padding(top = 18.dp, start = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Your library", color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = newPlaylist, Modifier.size(32.dp)) { Icon(Icons.Rounded.Add, "New playlist", tint = TextDim) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Entry(View.Songs, "Songs", Icons.Rounded.MusicNote)
            Entry(View.Albums, "Albums", Icons.Rounded.Album)
            Entry(View.Artists, "Artists", Icons.Rounded.Person)
            Spacer(Modifier.height(8.dp))
            Entry(View.Liked, "Liked Songs", Icons.Rounded.Favorite)
            Entry(View.Want, "Want to listen", Icons.Rounded.Bookmark)
            state.collection.playlists.forEach { Entry(View.Playlist(it.id), it.name, Icons.AutoMirrored.Rounded.QueueMusic) }
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

@Composable
private fun SongList(
    ctx: Ctx, title: String, subtitle: String, tracks: List<Track>, art: String? = null, back: (() -> Unit)? = null,
    inPlaylist: String? = null, onMove: ((Int, Int) -> Unit)? = null, actions: @Composable () -> Unit = {},
) {
    val reorder = remember { ReorderState() }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header(title, "$subtitle · ${tracks.size} songs", art, back = back) {
                actions()
                Spacer(Modifier.width(8.dp))
                if (tracks.isNotEmpty()) Button(onClick = { ctx.play(tracks, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
            }
        }
        if (tracks.isEmpty()) item { Note("Nothing here yet. Right-click a song to add it.") }
        items(tracks.size) { i ->
            if (onMove == null) TrackRow(ctx, tracks, i, numbered = true, inPlaylist = inPlaylist)
            else Row(Modifier.reorderRow(reorder, i, tracks.size), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { TrackRow(ctx, tracks, i, numbered = true, inPlaylist = inPlaylist) }
                DragHandle(reorder, i, tracks.size, tracks[i].title, onMove)
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
            OutlinedTextField(filter, { filter = it }, placeholder = { Text("Filter your songs") }, singleLine = true, modifier = Modifier.width(360.dp))
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
                OutlinedTextField(q, { q = it }, placeholder = { Text("Songs, artists and albums, here and online") }, singleLine = true, modifier = Modifier.width(480.dp))
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
                    OutlinedTextField(typedFolder, { typedFolder = it }, label = { Text("Or type a folder path") },
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
                    OutlinedTextField(url, { url = it }, label = { Text("Server address") }, placeholder = { Text("http://192.168.1.20:4533") }, singleLine = true, modifier = Modifier.width(420.dp))
                    OutlinedTextField(user, { user = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.width(420.dp))
                    OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.width(420.dp),
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
                    Button(onClick = { Matcher.start(state.library, onlyNew = true); ctx.push(View.Match) }) { Text("Match new songs") }
                    OutlinedButton(onClick = { Matcher.start(state.library, onlyNew = false); ctx.push(View.Match) }) { Text("Match all songs") }
                    if (m.proposals.isNotEmpty()) OutlinedButton(onClick = { ctx.push(View.Match) }) { Text("Review ${m.proposals.size}") }
                }
                OutlinedButton(onClick = { ctx.push(View.Rename) }) { Text("Rename files from tags") }
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
        item { Section("Playback and discovery") }
        item { Toggle("Similar songs when the queue ends", "Keeps the music going with songs like the last ones.", state.settings.autoplay, state::setAutoplay) }
        item { Toggle("Keep playing when the window is closed", "Reopen Onda from the app menu or your desktop's media controls.", state.settings.background, state::setBackground) }
        item { Toggle("New release notifications", "A desktop notification when an artist you listen to releases something.", state.settings.releaseAlerts, state::setReleaseAlerts) }
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
            Text("Space play/pause · Ctrl+→ / Ctrl+← next / previous · → / ← seek 5 s · ↑ / ↓ volume · Ctrl+L like · Ctrl+F search · Ctrl+, settings · Esc back",
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
private fun QueuePanel(player: Player, onClose: () -> Unit) = PanelFrame("Queue", onClose) {
    val upcoming = player.upcoming()
    val reorder = remember { ReorderState() }
    LazyColumn {
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

private class LyricsState(val full: Boolean, val lyrics: Lrclib.Lyrics?, val failed: Boolean)

/** Synced lyrics from LRCLIB for the song playing (full songs only). */
@Composable
private fun rememberLyrics(player: Player): LyricsState {
    val cur = player.current
    val full = cur != null && (!cur.preview || player.fullFrom != null)
    var lyrics by remember(cur?.id, full) { mutableStateOf<Lrclib.Lyrics?>(null) }
    var failed by remember(cur?.id, full) { mutableStateOf(false) }
    LaunchedEffect(cur?.id, full) {
        if (cur == null || !full) return@LaunchedEffect
        val dur = ((cur.durationMs.takeIf { it > 0 } ?: player.durationMs) / 1000).toInt()
        lyrics = runCatching { Lrclib.get(cur.title, cur.artist, cur.album, dur) }.getOrNull()
        failed = lyrics == null
    }
    return LyricsState(full, lyrics, failed)
}

/** The lines; the current one is bigger and follows the song. Click a line to jump there. */
@Composable
private fun LyricsLines(player: Player, ls: LyricsState, big: Boolean) {
    val lines = ls.lyrics?.synced.orEmpty()
    val active = if (lines.isEmpty()) -1 else Lrc.indexAt(lines, player.positionMs)
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
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { player.seek(l.timeMs) }.padding(vertical = if (big) 10.dp else 6.dp, horizontal = 4.dp))
            }
        }
        ls.lyrics.plain != null -> Text(ls.lyrics.plain, color = TextMain, fontSize = if (big) 28.sp else 18.sp,
            modifier = Modifier.padding(if (big) 48.dp else 16.dp).verticalScroll(rememberScrollState()))
        else -> Note("No lyrics for this song.")
    }
    }
}

@Composable
private fun LyricsPanel(player: Player, onExpand: () -> Unit, onClose: () -> Unit) {
    val ls = rememberLyrics(player)
    Column(Modifier.width(360.dp).fillMaxHeight().background(Side)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Lyrics", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onExpand) { Icon(Icons.Rounded.OpenInFull, "Show lyrics full screen", tint = TextDim) }
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close lyrics", tint = TextDim) }
        }
        LyricsLines(player, ls, big = false)
    }
}

/** Lyrics over the whole content area, on the cover's colour (like Spotify). */
@Composable
private fun LyricsFull(player: Player, onCollapse: () -> Unit, onClose: () -> Unit) {
    val ls = rememberLyrics(player)
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
        LyricsLines(player, ls, big = true)
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
            IconButton(onClick = { toggle(Panel.LYRICS) }) { Icon(Icons.Rounded.Lyrics, "Lyrics", tint = if (panel == Panel.LYRICS) Accent else TextDim) }
            IconButton(onClick = { toggle(Panel.QUEUE) }) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Queue", tint = if (panel == Panel.QUEUE) Accent else TextDim) }
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = TextDim)
            ThinSlider((player?.volume ?: 0.8).toFloat(), "Volume", { player?.changeVolume(it.toDouble()) },
                onDone = { player?.let { state.setVolume(it.volume) } }, modifier = Modifier.width(120.dp).padding(start = 8.dp))
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
            MatchSide("Deezer", m.cover, m.title, m.artist, m.album.ifBlank { t.album }, m.durationSec * 1000L, Modifier.weight(1f), t)
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
