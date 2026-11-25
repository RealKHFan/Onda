package io.github.realkhfan.onda.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.async
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.PlaylistStore
import io.github.realkhfan.onda.data.Server
import java.io.File

/** One of your own songs in a list of catalog songs (an artist mix): it's found again in your library, so it plays in full. */
internal fun Track.asRemote() = RemoteTrack(0, title, artist, album, artUri, null, (durationMs / 1000).toInt())

/** A list of catalog songs with a mosaic cover: an artist mix or the release radar. [list] null = still loading. */
@Composable
internal fun RemoteListScreen(
    title: String,
    about: String,
    list: List<RemoteTrack>?,
    emptyText: String,
    row: RemoteRowContext,
    onRefresh: (() -> Unit)? = null,
    onBack: () -> Unit,
    onPlayList: (List<RemoteTrack>, Int) -> Unit,
    onMenu: (RemoteTrack) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        BackTopBar(title, onBack) { if (onRefresh != null && list != null) TextButton(onClick = onRefresh) { Text("Refresh", color = TextDim) } }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp, 0.dp, 16.dp, 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MixCover(title, list.orEmpty(), side = 200.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(about, color = TextDim, fontSize = 13.sp, textAlign = TextAlign.Center)
                    if (!list.isNullOrEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { onPlayList(list, -1) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
                    }
                }
            }
            when {
                list == null -> item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp), color = Accent, trackColor = Raised) }
                list.isEmpty() -> item { Text(emptyText, color = TextDim, modifier = Modifier.padding(24.dp)) }
                else -> itemsIndexed(list) { i, t -> RemoteRow(t, row, onPlay = { onPlayList(list, i) }, onMenu = { onMenu(t) }) }
            }
        }
    }
}

/** "<Artist> Mix" on Home. */
@Composable
internal fun TopMixCard(name: String, art: String?, onClick: () -> Unit) {
    Column(Modifier.width(140.dp).clickable(onClickLabel = "Open $name Mix", onClick = onClick)) {
        Box(contentAlignment = Alignment.BottomStart) {
            Art(art, 140.dp)
            Text("$name Mix", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(140.dp).background(Color(0xAA000000)).padding(8.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text("$name and similar artists", color = TextDim, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** "Recommended songs" under a playlist, like Spotify's: Add puts one in; Refresh finds others. */
internal fun LazyListScope.recommended(
    recs: List<RemoteTrack>?,
    row: RemoteRowContext,
    onRefresh: () -> Unit,
    onAdd: (RemoteTrack) -> Unit,
    onPlay: (List<RemoteTrack>, Int) -> Unit,
) {
    item {
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Recommended songs", Modifier.weight(1f).padding(16.dp, 0.dp))
            TextButton(onClick = onRefresh) { Text("Refresh", color = TextDim) }
        }
    }
    when {
        recs == null -> item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp), color = Accent, trackColor = Raised) }
        recs.isEmpty() -> item { Text("Add a few songs and similar ones show up here.", color = TextDim, modifier = Modifier.padding(16.dp, 8.dp)) }
        else -> itemsIndexed(recs, key = { _, r -> "rec" + r.key }) { i, r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { RemoteRow(r, row, onPlay = { onPlay(recs, i) }) }
                TextButton(onClick = { onAdd(r) }, modifier = Modifier.padding(end = 8.dp)) { Text("Add", color = Accent) }
            }
        }
    }
}

/** Songs that are in your library more than once; hiding one copy keeps its file. */
@Composable
internal fun DuplicatesScreen(
    groups: List<List<Track>>,
    counts: Map<String, Int>,
    hiddenCount: Int,
    currentId: String?,
    onBack: () -> Unit,
    onPlay: (Track) -> Unit,
    onHide: (Track) -> Unit,
    onShowHidden: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        BackTopBar("Duplicates", onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text("${songCount(groups.size)} in your library more than once · hiding keeps the file", color = TextDim, fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 4.dp))
            }
            if (hiddenCount > 0) item {
                TextButton(onClick = onShowHidden, modifier = Modifier.padding(start = 4.dp)) { Text("Show $hiddenCount hidden song${if (hiddenCount == 1) "" else "s"} again", color = Accent) }
            }
            if (groups.isEmpty()) item { Text("No duplicates in your library.", color = TextDim, modifier = Modifier.padding(24.dp)) }
            groups.forEach { g ->
                item { SectionLabel("${g[0].title} — ${g[0].artist}") }
                items(g, key = { it.id }) { t ->
                    val where = if (t.id.startsWith(Server.PREFIX)) "On your music server" else "${t.folderName}/${t.fileName}".trim('/')
                    Row(Modifier.fillMaxWidth().clickable { onPlay(t) }.padding(start = 16.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Art(t.artUri, 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.album.ifBlank { t.title }, color = if (t.id == currentId) Accent else TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$where · ${counts[t.id] ?: 0} plays", color = TextDim, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = { onHide(t) }) { Text("Hide", color = TextDim) }
                    }
                }
            }
        }
    }
}

/** Name, description and cover of a playlist (the cover is your photo, or the songs' covers when none). */
@Composable
internal fun PlaylistDetailsDialog(
    pl: PlaylistStore.Playlist,
    art: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, cover: Uri?, removeCover: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(pl.name) }
    var about by remember { mutableStateOf(pl.description) }
    var picked by remember { mutableStateOf<Uri?>(null) }
    var removed by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) { picked = uri; removed = false } }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(name.trim(), about.trim(), picked, removed) }) { Text("Save", color = Accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextDim) } },
        title = { Text("Edit details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Art(picked?.toString() ?: if (removed) null else art, 72.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        TextButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Choose photo", color = Accent) }
                        if (picked != null || (pl.cover != null && !removed)) {
                            TextButton(onClick = { picked = null; removed = true }) { Text("Use the songs' covers", color = TextDim) }
                        }
                    }
                }
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Name") })
                OutlinedTextField(about, { about = it.take(300) }, label = { Text("Description") }, minLines = 2, maxLines = 4)
            }
        },
        containerColor = CardBg,
    )
}

/** Copies a picked photo into Onda's own files (a new name each time, so the old picture isn't shown from cache). */
internal fun saveCover(ctx: Context, playlistId: String, uri: Uri): String? {
    val dir = File(ctx.filesDir, "covers").apply { mkdirs() }
    val out = File(dir, "$playlistId-${System.currentTimeMillis()}.img")
    return runCatching { copyImage(ctx, uri, out) }.getOrNull() ?: run { out.delete(); null }
}

private fun copyImage(ctx: Context, uri: Uri, out: File): String? {
    ctx.contentResolver.openInputStream(uri)!!.use { input ->
        out.outputStream().use { o ->
            val buf = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > 20L * 1024 * 1024) error("too big") // a photo, not a whole album
                o.write(buf, 0, n)
            }
        }
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(out.path, bounds)
    return if (bounds.outWidth > 0) out.path else null
}

/** Up to [max] bytes; null when there's more (not the small text file we expected). */
internal fun java.io.InputStream.readUpTo(max: Int): ByteArray? {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(16 * 1024)
    while (true) {
        val n = read(buf)
        if (n < 0) return out.toByteArray()
        out.write(buf, 0, n)
        if (out.size() > max) return null
    }
}

/* ---------------- batch 3 ---------------- */

/** Title, artist and album by hand, or from the right song found online (its cover too). The file isn't changed. */
@Composable
internal fun EditDetailsDialog(t: Track, apple: Boolean, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(t.title) }
    var artist by remember { mutableStateOf(t.artist) }
    var album by remember { mutableStateOf(t.album) }
    var picked by remember { mutableStateOf<RemoteTrack?>(null) }
    var q by remember { mutableStateOf(io.github.realkhfan.onda.core.MetaMatch.query(t)) }
    var results by remember { mutableStateOf<List<RemoteTrack>?>(null) }
    var round by remember { mutableIntStateOf(0) }
    LaunchedEffect(round) {
        if (q.isBlank()) return@LaunchedEffect
        results = null
        results = kotlinx.coroutines.coroutineScope {
            val dz = async { runCatching { io.github.realkhfan.onda.data.Deezer.search(q, 15) }.getOrDefault(emptyList()) }
            val ap = async { if (apple) runCatching { io.github.realkhfan.onda.data.Itunes.searchSongs(q) }.getOrDefault(emptyList()) else emptyList() }
            io.github.realkhfan.onda.core.Match.mergeTracks(dz.await(), ap.await())
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit details") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { OutlinedTextField(title, { title = it }, singleLine = true, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(artist, { artist = it }, singleLine = true, label = { Text("Artist") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(album, { album = it }, singleLine = true, label = { Text("Album") }, modifier = Modifier.fillMaxWidth()) }
                item { Text("Shown in Onda; the file isn't changed. ⋮ → Restore original tags undoes it.", color = TextDim, fontSize = 12.sp) }
                item {
                    OutlinedTextField(q, { q = it }, singleLine = true, label = { Text("Find the song online") }, modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { TextButton(onClick = { round++ }) { Text("Search", color = Accent) } })
                }
                item {
                    Text((if (results == null) "Searching… " else "Tap the right song to fill in its details and cover.") +
                        if (t.durationMs > 0) " Your file: ${mmss(t.durationMs)}." else "", color = TextDim, fontSize = 12.sp)
                }
                if (results?.isEmpty() == true) item { Text("Nothing found. Try other words.", color = TextDim) }
                items(results.orEmpty()) { r ->
                    Row(
                        Modifier.fillMaxWidth().background(if (picked == r) Raised else Color.Transparent)
                            .clickable { picked = r; title = r.title; artist = r.artist; album = r.album.ifBlank { album } }.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Art(r.cover, 40.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(r.title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${r.artist} · ${r.album}", color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        // The length tells versions apart (radio edit, extended, live).
                        Text(mmss(r.durationSec * 1000L), color = TextDim, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = picked
                if (p != null && p.title == title && p.artist == artist && (p.album.isBlank() || p.album == album)) io.github.realkhfan.onda.data.Overrides.replace(t, p)
                else io.github.realkhfan.onda.data.Overrides.edit(t, title, artist, album, cover = p?.cover)
                onDismiss()
            }) { Text("Save", color = Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextDim) } },
        containerColor = CardBg,
    )
}

/** One artist and/or album for many songs at once; titles stay. */
@Composable
internal fun EditManyDialog(tracks: List<Track>, onDismiss: () -> Unit) {
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${songCount(tracks.size)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(artist, { artist = it }, singleLine = true, label = { Text("Artist for all") }, placeholder = { Text("Empty: keep each song's") })
                OutlinedTextField(album, { album = it }, singleLine = true, label = { Text("Album for all") }, placeholder = { Text("Empty: keep each song's") })
                Text("Titles stay. Shown in Onda; the files aren't changed.", color = TextDim, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { tracks.forEach { io.github.realkhfan.onda.data.Overrides.edit(it, it.title, artist, album) }; onDismiss() },
                enabled = artist.isNotBlank() || album.isNotBlank()) { Text("Save", color = Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextDim) } },
        containerColor = CardBg,
    )
}

internal fun mmss(ms: Long): String = (ms / 1000).let { "%d:%02d".format(it / 60, it % 60) }

/** "Fans also like" and "More from <artist>" under one of your artists. */
internal fun LazyListScope.fansAlsoLike(name: String, related: List<io.github.realkhfan.onda.core.RemoteArtist>, onMore: () -> Unit, onOpen: (io.github.realkhfan.onda.core.RemoteArtist) -> Unit) {
    item { OutlinedButton(onClick = onMore, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp)) { Text("More from $name", color = TextMain) } }
    if (related.isEmpty()) return
    item { SectionLabel("Fans also like") }
    item {
        androidx.compose.foundation.lazy.LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(related, key = { it.id }) { a ->
                Column(Modifier.width(104.dp).clickable { onOpen(a) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Art(a.picture, 104.dp, round = true)
                    Text(a.name, color = TextMain, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

/** A switch with a title and a line under it, as in the rest of Settings. */
@Composable
internal fun SettingSwitch(title: String, about: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = TextMain)
            Text(about, color = TextDim, fontSize = 12.sp)
        }
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Settings → Loudness: even out loudness, measured on the phone; keep an album's dynamics. */
@Composable
internal fun LoudnessSettings(ctx: Context, library: List<Track>) {
    var on by remember { mutableStateOf(io.github.realkhfan.onda.data.Prefs.normalize(ctx)) }
    var album by remember { mutableStateOf(io.github.realkhfan.onda.data.Prefs.albumGain(ctx)) }
    val progress by io.github.realkhfan.onda.data.Loudness.progress.collectAsState()
    val gains by io.github.realkhfan.onda.data.Loudness.gains.collectAsState()
    val files = remember(library) { library.filter { it.uri.startsWith("content:") } }
    val measured = remember(files, gains) { files.count { it.id in gains } }
    fun changed() { io.github.realkhfan.onda.player.Effects.normalize = on; io.github.realkhfan.onda.player.Effects.refresh?.invoke() }
    SettingSwitch("Even out loudness", "Songs play at one level. Each of your songs is measured once, on this phone; previews and server songs get the usual level.", on) {
        on = it; io.github.realkhfan.onda.data.Prefs.setNormalize(ctx, it); changed()
        if (it) io.github.realkhfan.onda.data.Loudness.measure(ctx, files) else io.github.realkhfan.onda.data.Loudness.stop(ctx)
    }
    if (on) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (progress.running) "Measuring… ${progress.done} of ${progress.total}" else "$measured of ${songCount(files.size)} measured",
                color = TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (progress.running) TextButton(onClick = { io.github.realkhfan.onda.data.Loudness.stop(ctx) }) { Text("Stop", color = Accent) }
            else if (measured < files.size) TextButton(onClick = { io.github.realkhfan.onda.data.Loudness.measure(ctx, files) }) { Text("Measure the rest", color = Accent) }
        }
        if (progress.running) LinearProgressIndicator({ progress.done / progress.total.coerceAtLeast(1).toFloat() }, Modifier.fillMaxWidth(), color = Accent, trackColor = Raised)
        SettingSwitch("Keep album dynamics", "Songs of one album get one level, so its quiet and loud songs stay as they were made.", album) {
            album = it; io.github.realkhfan.onda.data.Prefs.setAlbumGain(ctx, it); changed()
        }
    }
}

/** "FLAC · 24-bit · 96 kHz" for a file you have, read once per song (null for previews and server songs). */
private val qualities = java.util.concurrent.ConcurrentHashMap<String, String>()

@Composable
internal fun rememberQuality(t: Track?): String? {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var q by remember(t?.id) { mutableStateOf(t?.id?.let { qualities[it] }) }
    LaunchedEffect(t?.id) {
        if (t == null || !t.uri.startsWith("content:") || q != null) return@LaunchedEffect
        q = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { readQuality(ctx, t) }?.also { qualities[t.id] = it }
    }
    return q
}

private fun readQuality(ctx: Context, t: Track): String? {
    val r = android.media.MediaMetadataRetriever()
    return try {
        r.setDataSource(ctx, Uri.parse(t.uri))
        val kbps = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()?.div(1000) ?: 0
        val (rate, bits) = if (android.os.Build.VERSION.SDK_INT >= 31) {
            (r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 0) to
                (r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)?.toIntOrNull() ?: 0)
        } else 0 to 0
        io.github.realkhfan.onda.core.Quality.badge(
            io.github.realkhfan.onda.core.Quality.formatOf(r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE), t.fileName), kbps, rate, bits,
        ).ifBlank { null }
    } catch (_: Exception) {
        null
    } finally {
        runCatching { r.release() }
    }
}

/** A folder's playlists; ⋮ renames or deletes the folder (its playlists stay). */
@Composable
internal fun FolderScreen(
    f: PlaylistStore.Folder, playlists: List<PlaylistStore.Playlist>, playlistArt: (PlaylistStore.Playlist) -> String?, pinned: Set<String>,
    onBack: () -> Unit, onOpen: (String) -> Unit, onRename: () -> Unit, onDelete: () -> Unit,
    /** A drag in your own order ([shown]: the ids as listed); null when another order is picked. */
    onMove: ((shown: List<String>, from: Int, to: Int) -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scroller = rememberDragScroller(listState) // a dragged row scrolls the list at its edges
    val reorder = rememberReorder(scroller)
    val movable = onMove != null && playlists.size > 1
    Column(Modifier.fillMaxSize()) {
        BackTopBar(f.name, onBack) {
            Box {
                androidx.compose.material3.IconButton(onClick = { open = true }) { Icon(androidx.compose.material.icons.Icons.Rounded.MoreVert, "More for ${f.name}", tint = TextDim) }
                androidx.compose.material3.DropdownMenu(open, { open = false }) {
                    androidx.compose.material3.DropdownMenuItem({ Text("Rename folder") }, { open = false; onRename() })
                    androidx.compose.material3.DropdownMenuItem({ Text("Delete folder") }, { open = false; onDelete() })
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize().dragScrollArea(scroller), state = listState) {
            if (playlists.isEmpty()) item {
                Text("No playlists here yet. In a playlist's ⋮ menu, choose Move to a folder.", color = TextDim, modifier = Modifier.padding(24.dp))
            }
            itemsIndexed(playlists, key = { _, pl -> pl.id }) { i, pl ->
                val move: (Int, Int) -> Unit = { from, to -> onMove?.invoke(playlists.map { it.id }, from, to) }
                Row((if (movable) Modifier.reorderRow(reorder, i, playlists.size) else Modifier).fillMaxWidth().clickable { onOpen(pl.id) }
                    .then(if (movable) Modifier.holdToMove(reorder, i, playlists.size, move) else Modifier)
                    .padding(16.dp, 8.dp, if (movable) 4.dp else 16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art(playlistArt(pl), 52.dp)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(pl.name, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text((if (pl.id in pinned) "Pinned · " else "") + "Playlist · ${songCount(pl.trackIds.size)}", color = TextDim, fontSize = 12.sp)
                    }
                    if (movable) DragHandle(reorder, i, playlists.size, pl.name, move)
                }
            }
        }
    }
}

/** Where a playlist goes: one of your folders, a new one, or none. */
@Composable
internal fun MoveToFolderDialog(
    pl: PlaylistStore.Playlist, folders: List<PlaylistStore.Folder>, onDismiss: () -> Unit, onNew: () -> Unit, onPick: (String?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move “${pl.name}”") },
        text = {
            LazyColumn {
                items(folders, key = { it.id }) { f ->
                    Text(f.name, color = if (pl.folder == f.id) Accent else TextMain,
                        modifier = Modifier.fillMaxWidth().clickable { onPick(f.id) }.padding(vertical = 12.dp))
                }
                item { Text("New folder…", color = Accent, modifier = Modifier.fillMaxWidth().clickable(onClick = onNew).padding(vertical = 12.dp)) }
                if (pl.folder != null) item {
                    Text("Not in a folder", color = TextDim, modifier = Modifier.fillMaxWidth().clickable { onPick(null) }.padding(vertical = 12.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextDim) } },
        containerColor = CardBg,
    )
}

/** Settings → Playback: crossfade length (songs of one album still play gaplessly). */
@Composable
internal fun CrossfadeSetting(ctx: Context) {
    var s by remember { mutableIntStateOf(io.github.realkhfan.onda.data.Prefs.crossfade(ctx)) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Crossfade", color = TextMain, modifier = Modifier.weight(1f))
            Text(if (s == 0) "Off" else "$s s", color = if (s == 0) TextDim else Accent)
        }
        androidx.compose.material3.Slider(s.toFloat(), { s = it.toInt() }, valueRange = 0f..12f, steps = 11,
            onValueChangeFinished = { io.github.realkhfan.onda.data.Prefs.setCrossfade(ctx, s) },
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Raised))
        Text("The next song fades in while this one fades out. Songs of the same album play without a gap, as they were made.", color = TextDim, fontSize = 12.sp)
    }
}
