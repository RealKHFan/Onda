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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.runtime.mutableStateOf
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
    onBack: () -> Unit,
    onPlayList: (List<RemoteTrack>, Int) -> Unit,
    onMenu: (RemoteTrack) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        BackTopBar(title, onBack)
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
