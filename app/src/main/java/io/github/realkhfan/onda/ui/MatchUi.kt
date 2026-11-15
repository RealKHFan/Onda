package io.github.realkhfan.onda.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.realkhfan.onda.core.Confidence
import io.github.realkhfan.onda.data.Matcher

/** Review of Deezer matches: Keep your file's details or Replace them, song by song or all at once. */
@Composable
internal fun MatchScreen(onBack: () -> Unit, onRename: () -> Unit) {
    val s by Matcher.state.collectAsState()
    val confident = s.proposals.count { it.confidence == Confidence.HIGH }
    Column(Modifier.fillMaxSize()) {
        BackTopBar("Match metadata", onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (s.running) {
                Text("Checking ${s.done} of ${s.total} songs on Deezer…", color = TextMain)
                LinearProgressIndicator(
                    progress = { if (s.total == 0) 0f else s.done.toFloat() / s.total },
                    modifier = Modifier.fillMaxWidth(), color = Accent, trackColor = Raised,
                )
                OutlinedButton(onClick = { Matcher.stop() }) { Text("Stop") }
            }
            s.error?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
            Text(
                "${s.proposals.size} to review · ${s.alreadyRight} already correct · ${s.noMatch} not found (kept as they are)",
                color = TextDim, fontSize = 13.sp,
            )
            if (s.proposals.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (confident > 0) Button(onClick = { Matcher.replaceAllConfident() }) { Text("Replace all confident ($confident)") }
                    OutlinedButton(onClick = { Matcher.keepAll() }) { Text("Keep all") }
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(s.proposals, key = { it.track.id }) { p ->
                ProposalCard(p, onKeep = { Matcher.decide(p, false) }, onReplace = { Matcher.decide(p, true) })
            }
            if (!s.running && s.proposals.isEmpty() && s.total > 0) {
                item {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("All done. Songs without a match keep their own details. Matching never changes your audio files.",
                            color = TextDim)
                        OutlinedButton(onClick = onRename) { Text("Rename files to match their tags") }
                    }
                }
            }
            if (!s.running && s.total == 0) {
                item { Text("No new songs to check.", color = TextDim, modifier = Modifier.padding(24.dp)) }
            }
        }
    }
}

@Composable
private fun ProposalCard(p: Matcher.Proposal, onKeep: () -> Unit, onReplace: () -> Unit) {
    val t = p.track
    val m = p.match
    Column(
        Modifier.fillMaxWidth().padding(16.dp, 6.dp).clip(RoundedCornerShape(8.dp)).background(CardBg).padding(12.dp),
    ) {
        Text(
            if (p.confidence == Confidence.HIGH) "Confident match" else "Possible match — check it",
            color = if (p.confidence == Confidence.HIGH) Accent else TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Side("Your file", t.artUri, t.title, t.artist, t.album, t.durationMs, Modifier.weight(1f), null)
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = TextDim, modifier = Modifier.padding(horizontal = 6.dp))
            Side("Deezer", m.cover, m.title, m.artist, m.album.ifBlank { t.album }, m.durationSec * 1000L, Modifier.weight(1f), t)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onKeep) { Text("Keep", color = TextDim) }
            Spacer(Modifier.width(4.dp))
            Button(onClick = onReplace) { Text("Replace") }
        }
    }
}

/** One side of the comparison; on the Deezer side, values that differ from the file are highlighted. */
@Composable
private fun Side(
    label: String, art: String?, title: String, artist: String, album: String, durationMs: Long,
    modifier: Modifier, compare: io.github.realkhfan.onda.core.Track?,
) {
    fun color(differs: Boolean): Color = if (compare != null && differs) Accent else TextMain
    Column(modifier) {
        Text(label, color = TextDim, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Art(art, 56.dp)
        Spacer(Modifier.height(4.dp))
        Text(title, color = color(title != compare?.title), maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
        Text(artist, color = color(artist != compare?.artist), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        Text(album, color = color(album != compare?.album), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        if (durationMs > 0) {
            // A big length gap usually means another edit or version of the song.
            val far = compare != null && compare.durationMs > 0 && kotlin.math.abs(compare.durationMs - durationMs) > 3000
            Text(fmt(durationMs), color = color(far), fontSize = 12.sp)
        }
    }
}
