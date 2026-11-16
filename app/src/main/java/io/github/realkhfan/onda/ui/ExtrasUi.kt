package io.github.realkhfan.onda.ui

import androidx.compose.ui.draw.alpha
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.realkhfan.onda.core.Discovery
import io.github.realkhfan.onda.core.Grouping
import io.github.realkhfan.onda.core.Smart
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.Prefs
import io.github.realkhfan.onda.player.Effects
import io.github.realkhfan.onda.player.PlayerState
import io.github.realkhfan.onda.player.SleepTimer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/* The desktop's pages and settings, on the phone. */

/** Top artists, songs and albums, minutes listened and when you listen (counted on this phone only). */
@Composable
internal fun StatsScreen(allPlays: List<Discovery.Play>, library: List<Track>, trackFor: (String) -> Track?, artistPic: (String) -> String?,
                         player: PlayerState?, onBack: () -> Unit, onOpenAlbum: (List<Track>, String, String, String?) -> Unit) {
    var period by remember { mutableIntStateOf(0) }
    val periods = listOf("4 weeks" to 28L, "6 months" to 182L, "All time" to 0L)
    val plays = remember(allPlays, period) {
        val days = periods[period].second
        if (days == 0L) allPlays else (System.currentTimeMillis() - days * 86_400_000L).let { since -> allPlays.filter { it.atMs >= since } }
    }
    val artists = remember(plays) { plays.groupingBy { it.artist }.eachCount().entries.sortedByDescending { it.value }.take(10) }
    val songs = remember(plays) { plays.groupBy { it.key }.entries.sortedByDescending { it.value.size }.take(20).mapNotNull { (k, l) -> trackFor(k)?.let { it to l.size } } }
    val albums = remember(plays, library) {
        val all = Grouping.albums(library)
        plays.mapNotNull { trackFor(it.key) }.groupingBy { it.albumKey }.eachCount().entries.sortedByDescending { it.value }.take(10)
            .mapNotNull { e -> all.firstOrNull { it.key == e.key }?.let { it to e.value } }
    }
    val minutes = remember(plays) { plays.sumOf { p -> trackFor(p.key)?.durationMs?.takeIf { it > 0 } ?: 180_000L } / 60_000 }
    Column(Modifier.fillMaxSize()) {
        BackTopBar("Your stats", onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text("${plays.size} plays · about ${"%,d".format(minutes)} minutes (${minutes / 60} hours) · counted on this phone only",
                    color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(16.dp, 0.dp))
            }
            item {
                Row(Modifier.padding(16.dp, 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    periods.forEachIndexed { i, (label, _) -> Chip(label, i == period) { period = i } }
                }
            }
            if (plays.isEmpty()) { item { Text("Play some songs and your top artists and songs show up here.", color = TextDim, modifier = Modifier.padding(16.dp)) }; return@LazyColumn }
            item { SectionLabel("When you listen") }
            item { ListeningClock(plays) }
            item { SectionLabel("Top artists") }
            items(artists.size) { i ->
                val (name, n) = artists[i]
                Row(Modifier.fillMaxWidth().padding(16.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", color = TextDim, modifier = Modifier.width(28.dp))
                    Art(artistPic(name), 44.dp, round = true)
                    Spacer(Modifier.width(12.dp))
                    Text(name, color = TextMain, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$n plays", color = TextDim, fontSize = 13.sp)
                }
            }
            if (albums.isNotEmpty()) {
                item { SectionLabel("Top albums") }
                item {
                    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(albums) { (a, n) ->
                            Column(Modifier.width(132.dp).clickable { onOpenAlbum(a.tracks, a.title, a.artist, a.artUri) }) {
                                Art(a.artUri, 132.dp)
                                Text(a.title, color = TextMain, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                                Text("${a.artist} · $n plays", color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            item { SectionLabel("Top songs") }
            val list = songs.map { it.first }
            items(songs.size) { i ->
                val (t, n) = songs[i]
                Row(Modifier.fillMaxWidth().clickable { player?.play(list, i, "Playlist", "Your top songs") }.padding(16.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", color = TextDim, modifier = Modifier.width(28.dp))
                    Art(t.artUri, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, color = if (player?.currentId == t.id) Accent else TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${t.artist} · $n plays", color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** Bars per hour of the day and per weekday. */
@Composable
internal fun ListeningClock(plays: List<Discovery.Play>) {
    val (hours, days) = remember(plays) { Smart.clock(plays, ZoneId.systemDefault()) }
    @Composable
    fun Bars(values: IntArray, labels: List<String>, barWidth: Int, every: Int) {
        val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            values.forEachIndexed { i, v ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics { contentDescription = "${labels[i]}: $v plays" }) {
                    Box(Modifier.width(barWidth.dp).height((4 + 72f * v / max).dp).clip(RoundedCornerShape(2.dp)).background(if (v == max) Accent else Accent.copy(alpha = 0.45f)))
                    Text(if (i % every == 0) labels[i] else "", color = TextDim, fontSize = 9.sp)
                }
            }
        }
    }
    Column(Modifier.padding(16.dp, 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Bars(hours, (0..23).map { "%02d".format(it) }, 11, 6)
        Bars(days, listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"), 36, 1)
    }
}

/** What you played, newest first, by day. */
@Composable
internal fun HistoryScreen(plays: List<Discovery.Play>, trackFor: (String) -> Track?, player: PlayerState?, onMenu: (Track) -> Unit = {}, onBack: () -> Unit) {
    // Songs played on your PC that aren't on this phone are listed greyed out (their ⋮ menu still works).
    val rows = remember(plays) {
        plays.asReversed().take(300).mapNotNull { p ->
            (trackFor(p.key) ?: p.takeIf { io.github.realkhfan.onda.core.Source.of(it.key) == null && it.title.isNotBlank() }
                ?.let { io.github.realkhfan.onda.core.LinkSync.missingTrack(io.github.realkhfan.onda.core.LinkSync.Library.PLACEHOLDER + it.artist + '\u001F' + it.title) })
                ?.let { p to it }
        }
    }
    val tracks = rows.map { it.second }.filterNot { io.github.realkhfan.onda.core.LinkSync.isMissing(it.id) }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val time = DateTimeFormatter.ofPattern("HH:mm")
    Column(Modifier.fillMaxSize()) {
        BackTopBar("History", onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            if (rows.isEmpty()) item { Text("Songs you play show up here.", color = TextDim, modifier = Modifier.padding(16.dp)) }
            rows.indices.groupBy { Instant.ofEpochMilli(rows[it].first.atMs).atZone(zone).toLocalDate() }.forEach { (d, idx) ->
                item { SectionLabel(when (d) { today -> "Today"; today.minusDays(1) -> "Yesterday"; else -> d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")) }) }
                items(idx) { i ->
                    val (p, t) = rows[i]
                    val missing = io.github.realkhfan.onda.core.LinkSync.isMissing(t.id)
                    Row(Modifier.fillMaxWidth().clickable(enabled = !missing) { player?.play(tracks, tracks.indexOf(t).coerceAtLeast(0), "Playlist", "History") }
                        .padding(start = 16.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).alpha(if (missing) 0.4f else 1f), verticalAlignment = Alignment.CenterVertically) {
                            Art(t.artUri, 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.title, color = if (player?.currentId == t.id) Accent else TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (missing) "${t.artist} · not on this phone" else t.artist, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(Instant.ofEpochMilli(p.atMs).atZone(zone).toLocalTime().format(time), color = TextDim, fontSize = 12.sp)
                        }
                        IconButton(onClick = { onMenu(t) }) { Icon(Icons.Rounded.MoreVert, "More options for ${t.title}", tint = TextDim) }
                    }
                }
            }
        }
    }
}

/** The four smart playlists, each opening as a normal song list. */
@Composable
internal fun SmartScreen(lists: Map<Smart.Kind, List<Track>>, onBack: () -> Unit, onOpen: (Smart.Kind) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BackTopBar("Smart playlists", onBack)
        Text("Made from your library and what you play; they update by themselves.", color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp))
        LazyColumn {
            items(Smart.Kind.entries) { k ->
                val l = lists[k].orEmpty()
                Row(Modifier.fillMaxWidth().clickable { onOpen(k) }.padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art(l.firstNotNullOfOrNull { it.artUri }, 56.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(k.title, color = TextMain, fontWeight = FontWeight.SemiBold)
                        Text("${songCount(l.size)} · ${k.about}", color = TextDim, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** Sleep timer: minutes (fading out over the last 30 s) or the end of the song. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SleepSheet(player: PlayerState, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardBg) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text("Sleep timer", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 4.dp))
            Text(when {
                SleepTimer.endsAt > 0 -> "Pausing in ${((SleepTimer.endsAt - System.currentTimeMillis()) / 60_000 + 1).coerceAtLeast(1)} min (fades out over the last 30 s)"
                SleepTimer.afterSong -> "Pausing when this song ends"
                else -> "Fades out over the last 30 seconds"
            }, color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp))
            listOf(15, 30, 45, 60, 90).forEach { m ->
                Text("$m minutes", color = TextMain, modifier = Modifier.fillMaxWidth().clickable { SleepTimer.start(scope, player, m); onDismiss() }.padding(16.dp, 14.dp))
            }
            Text("End of this song", color = TextMain, modifier = Modifier.fillMaxWidth().clickable { SleepTimer.endOfSong(player); onDismiss() }.padding(16.dp, 14.dp))
            if (SleepTimer.on) Text("Turn off", color = Accent, modifier = Modifier.fillMaxWidth().clickable { SleepTimer.cancel(player); onDismiss() }.padding(16.dp, 14.dp))
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

/** The desktop's equalizer: presets and 10 bands; the phone's own bands follow the curve. */
@Composable
internal fun EqualizerSettings(ctx: Context) {
    var gains by remember { mutableStateOf(Prefs.eq(ctx)) }
    var preset by remember { mutableStateOf(Prefs.eqPreset(ctx)) }
    fun save(g: List<Double>, p: String) { gains = g; preset = p; Effects.apply(g); Prefs.setEq(ctx, g, p) }
    Column {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (eqPresets.keys + "Custom").forEach { name ->
                if (name != "Custom" || preset == "Custom") Chip(name, preset == name) { eqPresets[name]?.let { save(it, name) } }
            }
        }
        listOf("30 Hz", "60 Hz", "120 Hz", "240 Hz", "480 Hz", "950 Hz", "1.9 kHz", "3.8 kHz", "7.5 kHz", "15 kHz").forEachIndexed { i, label ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextDim, fontSize = 12.sp, modifier = Modifier.width(64.dp))
                Slider(gains[i].toFloat(), { v -> gains = gains.toMutableList().also { it[i] = Math.round(v * 2) / 2.0 }; Effects.apply(gains) },
                    valueRange = -12f..12f, onValueChangeFinished = { save(gains, "Custom") }, modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Raised))
                Text((if (gains[i] > 0) "+" else "") + "${gains[i]} dB", color = TextDim, fontSize = 12.sp, modifier = Modifier.width(56.dp).padding(start = 6.dp))
            }
        }
    }
}

/** The highlight colours, as on the desktop. */
@Composable
internal fun AccentPicker(ctx: Context) {
    Row(Modifier.padding(vertical = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        accents.forEach { (name, argb) ->
            val on = Accent == Color(argb.toInt())
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(argb.toInt()))
                .then(if (on) Modifier.border(3.dp, TextMain, CircleShape) else Modifier)
                .clickable { accentState.value = Color(argb.toInt()); Prefs.setAccent(ctx, argb) }
                .semantics { contentDescription = name + if (on) ", chosen" else "" })
        }
    }
}

/** Where a song is, how long, how often you played it. */
@Composable
internal fun SongInfoDialog(t: Track, plays: List<Discovery.Play>, shownFromMatch: Boolean, onDismiss: () -> Unit) {
    val quality = rememberQuality(t)
    val mine = plays.filter { it.key == t.id }
    val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
    fun date(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(dateFmt)
    val rows = buildList {
        add("Title" to t.title); add("Artist" to t.artist); add("Album" to t.album)
        if (shownFromMatch) add("Shown in Onda" to "Matched or edited; ⋮ → Restore original tags shows the file's own")
        if (t.durationMs > 0) add("Length" to fmt(t.durationMs))
        quality?.let { add("Format" to it) }
        add("Where" to when {
            t.id.startsWith("content:") -> "${t.folderName} · ${t.fileName}"
            t.id.contains(':') && !t.id.startsWith("/") -> "On your music server"
            else -> t.fileName
        })
        add("Plays" to "${mine.size}" + (mine.maxOfOrNull { it.atMs }?.let { " · last ${date(it)}" } ?: ""))
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Song info") },
        text = {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { (k, v) -> Row { Text(k, color = TextDim, fontSize = 13.sp, modifier = Modifier.width(96.dp)); Text(v, color = TextMain, fontSize = 13.sp) } }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
