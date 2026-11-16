package io.github.realkhfan.onda.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import io.github.realkhfan.onda.data.Prefs
import io.github.realkhfan.onda.data.SongCopy
import kotlinx.coroutines.launch

/** Settings → Backup → Copy new songs to a folder (as on the desktop). */
@Composable
internal fun CopySongsSetting() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tree by remember { mutableStateOf(Prefs.copyTree(ctx)) }
    var asking by remember { mutableStateOf<Uri?>(null) } // a folder was picked: which songs go there first?
    var problem by remember { mutableStateOf<String?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if (uri != null) { problem = null; asking = uri } }
    Text("Copy new songs to a folder: every 30 minutes, songs that are new in your music folders are copied there, in the same folders. " +
        "Songs only go from your music folders into it; nothing there is deleted or replaced.", color = TextDim, fontSize = 12.sp)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(tree?.let { "Copy new songs to: ${prettyFolder(it)}" } ?: "Copy new songs: off", color = TextMain, fontSize = 13.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = { pick.launch(null) }) { Text(if (tree == null) "Choose folder" else "Change", color = Accent) }
        if (tree != null) TextButton(onClick = { scope.launch { SongCopy.setFolder(ctx, null, false); tree = null } }) { Text("Off", color = TextDim) }
    }
    if (tree != null) TextButton(onClick = { scope.launch { SongCopy.run(ctx) } }) { Text("Check now", color = Accent) }
    (problem ?: SongCopy.note)?.let { Text(it, color = TextMain, fontSize = 13.sp) }
    asking?.let { uri ->
        fun choose(everything: Boolean) {
            asking = null
            scope.launch {
                problem = SongCopy.setFolder(ctx, uri, everything)
                if (problem == null) { tree = uri.toString(); SongCopy.run(ctx) }
            }
        }
        AlertDialog(onDismissRequest = { asking = null }, title = { Text("Which songs?") },
            text = { Text("Songs are only copied from your music folders into ${prettyFolder(uri.toString())}; nothing comes back, and nothing there " +
                "is deleted or replaced.\n\nAll songs: your songs that aren't there yet are copied now, then new ones every 30 minutes. " +
                "Only new ones: if that folder has your music already, only songs you add from now on are copied.") },
            confirmButton = { TextButton(onClick = { choose(true) }) { Text("All songs", color = Accent) } },
            dismissButton = { TextButton(onClick = { choose(false) }) { Text("Only new ones", color = Accent) } })
    }
}
