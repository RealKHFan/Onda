package io.github.realkhfan.onda.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.realkhfan.onda.core.FileNames
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.Renamer
import kotlinx.coroutines.launch

/**
 * "Rename files": shows every change (old -> new) before anything happens. Uses the tags as shown in Onda,
 * so accepted Deezer matches count. Only the names change; Undo puts them back.
 */
@Composable
internal fun RenameScreen(
    localTracks: List<Track>,
    folders: List<String>,
    onBack: () -> Unit,
    onReAddFolder: () -> Unit,
    onRescan: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val readOnly = remember(folders) { folders.filterNot { Renamer.canWrite(ctx, it) } }
    val writable = remember(localTracks, readOnly) { localTracks.filter { t -> readOnly.none { t.id.startsWith(it) } } }
    val plan = remember(writable) { FileNames.plan(writable) }
    var selected by remember(plan) { mutableStateOf(plan.map { it.id }.toSet()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var undo by remember { mutableStateOf<List<FileNames.Rename>>(emptyList()) }

    fun run(changes: List<FileNames.Rename>, undoing: Boolean) {
        busy = true
        scope.launch {
            val r = Renamer.apply(ctx, changes)
            undo = if (undoing) emptyList() else r.undo
            message = buildString {
                append(
                    if (undoing) "Put back ${r.renamed} old ${if (r.renamed == 1) "name" else "names"}."
                    else "Renamed ${r.renamed} ${if (r.renamed == 1) "file" else "files"}.",
                )
                if (r.failed.isNotEmpty()) append(" ${r.failed.size} couldn't be renamed and kept their names.")
            }
            busy = false
            onRescan()
        }
    }

    Column(Modifier.fillMaxSize()) {
        BackTopBar("Rename files", onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Names your files “Artist - Title” from their tags, including matches you accepted. Only the file " +
                    "names change: the music and its tags stay as they are. Your playlists and likes follow along.",
                color = TextDim, fontSize = 13.sp,
            )
            if (readOnly.isNotEmpty()) {
                Text(
                    "Onda may only read ${if (readOnly.size == 1) "one of your folders" else "${readOnly.size} of your folders"} " +
                        "(added before this version). Re-add it once with the same folder to allow renaming.",
                    color = ErrorRed, fontSize = 13.sp,
                )
                OutlinedButton(onClick = onReAddFolder) { Text("Re-add folder") }
            }
            message?.let { Text(it, color = TextMain, fontSize = 13.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (plan.isNotEmpty()) {
                    Button(onClick = { run(plan.filter { it.id in selected }, undoing = false) }, enabled = !busy && selected.isNotEmpty()) {
                        Text(if (busy) "Renaming…" else "Rename ${songCount(selected.size).replace("song", "file")}")
                    }
                }
                if (undo.isNotEmpty()) {
                    OutlinedButton(onClick = { run(undo, undoing = true) }, enabled = !busy) { Text("Undo") }
                }
            }
            if (plan.isEmpty() && !busy) {
                Text("All file names already match their tags.", color = TextDim, modifier = Modifier.padding(vertical = 16.dp))
            }
        }
        LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(plan, key = { it.id }) { r ->
                val on = r.id in selected
                Row(
                    Modifier.fillMaxWidth().clickable { selected = if (on) selected - r.id else selected + r.id }.padding(8.dp, 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(on, { selected = if (on) selected - r.id else selected + r.id }, colors = CheckboxDefaults.colors(checkedColor = Accent))
                    Column(Modifier.weight(1f)) {
                        Text(r.from, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textDecoration = TextDecoration.LineThrough)
                        Text(r.to, color = TextMain, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
