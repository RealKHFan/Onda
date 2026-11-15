package io.github.realkhfan.onda.desktop

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
        var panel by remember { mutableStateOf(Panel.NONE) }
        var lyricsBig by remember { mutableStateOf(false) }
        var naming by remember { mutableStateOf<Naming?>(null) }
        val scope = rememberCoroutineScope()
        val ctx = Ctx(state, player, ::push, { naming = it }) { name ->
            scope.launch { Deezer.searchArtists(name).firstOrNull { Match.norm(it.name) == Match.norm(name) }?.let { push(View.CatalogArtist(it)) } }
        }
        Column(Modifier.fillMaxSize().background(Bg)) {
            Row(Modifier.weight(1f)) {
                Sidebar(state, stack.first(), ::go) { naming = Naming.New() }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    if (player == null) Note("Onda needs GStreamer to play music. On Bazzite it's already there; on other systems install gstreamer1 and its plugins.")
                    if (player != null && panel == Panel.LYRICS && lyricsBig) {
                        LyricsFull(player, onCollapse = { lyricsBig = false }) { panel = Panel.NONE; lyricsBig = false }
                    } else when (val v = stack.last()) {
                        View.Home -> HomePage(ctx, ::push)
                        View.Songs -> Songs(ctx)
                        View.Albums -> Albums(state) { a -> push(View.LocalList(a.title, "Album · ${a.artist}", a.artUri, a.tracks)) }
                        View.Artists -> Artists(state) { a -> push(View.LocalList(a.name, "Artist", a.artUri, a.tracks)) }
                        View.Search -> Search(ctx)
                        View.Settings -> SettingsPage(state)
                        View.Liked -> SongList(ctx, "Liked Songs", "Playlist", state.likedTracks, back = back)
                        View.Want -> WantPage(ctx, back)
                        is View.Playlist -> {
                            val pl = state.collection.playlists.firstOrNull { it.id == v.id }
                            if (pl == null) LaunchedEffect(Unit) { go(View.Songs) } else {
                                SongList(ctx, pl.name, "Playlist", state.playlistTracks(pl.id), back = back, inPlaylist = pl.id) {
                                    IconButton(onClick = { naming = Naming.Rename(pl.id, pl.name) }) { Icon(Icons.Rounded.Edit, "Rename playlist", tint = TextDim) }
                                    IconButton(onClick = { naming = Naming.Delete(pl.id, pl.name) }) { Icon(Icons.Rounded.Delete, "Delete playlist", tint = TextDim) }
                                }
                            }
                        }
                        is View.ServerPlaylist -> SongList(ctx, v.p.name, "Playlist · Music server", state.serverPlaylistTracks(v.p), back = back)
                        is View.LocalList -> SongList(ctx, v.title, v.subtitle, v.tracks, art = v.art, back = back)
                        is View.CatalogArtist -> CatalogArtistPage(ctx, v.a, back)
                        is View.CatalogAlbum -> CatalogAlbumPage(ctx, v.a, back)
                    }
                }
                if (player != null && panel == Panel.QUEUE) QueuePanel(player) { panel = Panel.NONE }
                if (player != null && panel == Panel.LYRICS && !lyricsBig) LyricsPanel(player, onExpand = { lyricsBig = true }) { panel = Panel.NONE }
            }
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
    val openArtistByName: (String) -> Unit,
) {
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
        }
    }) {
        SongRow(if (numbered) i + 1 else null, t.title, t.artist, t.album, t.durationMs, t.artUri, ctx.player?.current?.id == t.id,
            note = if (s.isPreviewRow(t)) "Preview" else null,
            trailing = { if (s.isLiked(t.id)) Icon(Icons.Rounded.Favorite, "Liked", tint = Accent, modifier = Modifier.padding(12.dp)) }) { ctx.play(list, i) }
    }
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
            add(ContextMenuItem("Go to artist") { ctx.openArtistByName(r.artist) })
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
    inPlaylist: String? = null, actions: @Composable () -> Unit = {},
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header(title, "$subtitle · ${tracks.size} songs", art, back = back) {
                actions()
                Spacer(Modifier.width(8.dp))
                if (tracks.isNotEmpty()) Button(onClick = { ctx.play(tracks, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
            }
        }
        if (tracks.isEmpty()) item { Note("Nothing here yet. Right-click a song to add it.") }
        items(tracks.size) { i -> TrackRow(ctx, tracks, i, numbered = true, inPlaylist = inPlaylist) }
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
private fun Albums(state: AppState, onOpen: (Album) -> Unit) {
    val albums = remember(state.library) { Grouping.albums(state.library) }
    LazyVerticalGrid(GridCells.Adaptive(180.dp), Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
private fun Artists(state: AppState, onOpen: (ArtistGroup) -> Unit) {
    val artists = remember(state.library) { Grouping.artists(state.library) }
    LazyColumn(Modifier.fillMaxSize()) {
        item { Header("Artists", "${artists.size} artists") }
        items(artists) { a ->
            Row(Modifier.fillMaxWidth().clickable { onOpen(a) }.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Art(a.artUri, 48, round = true)
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
            online = dz.await().getOrDefault(emptyList())
            artists = ar.await().getOrDefault(emptyList())
            albums = al.await().getOrDefault(emptyList())
            free = (ov.await().getOrDefault(emptyList()) + au.await().getOrDefault(emptyList())).take(10)
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
            Header(artist.name, "Artist", picture, round = true, back = back) {
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
                if (info.artistId > 0) TextButton(onClick = { ctx.push(View.CatalogArtist(RemoteArtist(info.artistId, info.artist, null))) }) { Text("Go to artist", color = Accent) }
                if (list.isNotEmpty()) Button(onClick = { ctx.playRemote(list, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
            }
        }
        if (tracks == null) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(24.dp), color = Accent, trackColor = Raised) }
        items(list.size) { i -> RemoteRow(ctx, list, i, numbered = true) }
    }
}

@Composable
private fun SettingsPage(state: AppState) {
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
        item { Section("Privacy") }
        item {
            Text("Search and previews come from Deezer's public API, free full songs from Openverse and Audius, lyrics from LRCLIB. They see " +
                "search words or song names and your IP address, nothing else. Media keys and the desktop's media controls only see what's playing.",
                color = TextDim, fontSize = 13.sp)
        }
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
    LazyColumn {
        player.current?.let { c -> item { Section("Now playing") }; item { QueueRow(c, true) {} } }
        if (upcoming.isNotEmpty()) item { Section("Next") }
        items(upcoming) { (i, it) -> QueueRow(it, false) { player.jumpTo(i) } }
    }
}

@Composable
private fun QueueRow(it: Player.Item, now: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Art(it.art, 36)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(it.title, color = if (now) Accent else TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
            Text(it.artist, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        }
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
