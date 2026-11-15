package io.github.realkhfan.onda.ui

import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import io.github.realkhfan.onda.data.UserCollection
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.realkhfan.onda.core.Discovery
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.data.Mixes

private fun subtitle(kind: Discovery.Kind) =
    if (kind == Discovery.Kind.DAILY) "Fresh picks for today" else "New artists for this week"

/** Square cover: a 2×2 mosaic of the mix's covers with its name on top, like Spotify's made-for-you tiles. */
@Composable
internal fun MixCover(kind: Discovery.Kind, tracks: List<RemoteTrack>, modifier: Modifier = Modifier, side: Dp? = null) {
    val covers = tracks.mapNotNull { it.cover }.distinct().take(4)
    Box(
        (if (side != null) modifier.size(side) else modifier.fillMaxWidth().aspectRatio(1f)).clip(RoundedCornerShape(8.dp)).background(Raised),
    ) {
        if (covers.size == 4) {
            Column(Modifier.fillMaxSize()) {
                listOf(covers.take(2), covers.drop(2)).forEach { row ->
                    Row(Modifier.weight(1f)) {
                        row.forEach { c -> Art(c, 0.dp, Modifier.weight(1f).fillMaxSize()) }
                    }
                }
            }
        }
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Accent.copy(alpha = 0.85f))),
        )
        Text(
            kind.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (side != null && side < 120.dp) 14.sp else 20.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        )
    }
}

/** Home card for one mix. */
@Composable
internal fun MixCard(kind: Discovery.Kind, mix: Mixes.Mix?, building: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick)) {
        MixCover(kind, mix?.tracks.orEmpty())
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                building && mix == null -> "Building your mix…"
                mix != null && mix.basedOn.isNotEmpty() -> "With artists like ${mix.basedOn.first()}"
                else -> subtitle(kind)
            },
            color = TextDim, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A mix's page: header, Play, and its songs (previews; + saves to Want to listen). */
@Composable
internal fun MixScreen(
    kind: Discovery.Kind,
    mix: Mixes.Mix?,
    building: Boolean,
    problem: String?,
    noTaste: Boolean,
    row: RemoteRowContext,
    onBack: () -> Unit,
    onPlayList: (List<RemoteTrack>, Int) -> Unit,
    onRetry: () -> Unit,
    onMenu: (RemoteTrack) -> Unit = {},
) {
    val hidden by UserCollection.hidden.collectAsState()
    val blocked by UserCollection.blocked.collectAsState()
    val list = remember(mix, hidden, blocked) { mix?.tracks.orEmpty().filterNot(UserCollection::notWanted) }
    Column(Modifier.fillMaxSize()) {
        BackTopBar(kind.label, onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp, 0.dp, 16.dp, 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MixCover(kind, list, side = 200.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(subtitle(kind), color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    if (mix != null && mix.basedOn.isNotEmpty()) {
                        Text("Based on ${mix.basedOn.joinToString(", ")}", color = TextDim, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                    Text(
                        if (kind == Discovery.Kind.DAILY) "A new mix every day · remixes, sped-up and live versions left out"
                        else "A new mix every Monday · only artists you don't have yet",
                        color = TextDim, fontSize = 12.sp, textAlign = TextAlign.Center,
                    )
                    if (list.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { onPlayList(list, -1) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
                    }
                }
            }
            when {
                building && list.isEmpty() -> item {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp), color = Accent, trackColor = Raised)
                }
                noTaste && list.isEmpty() -> item {
                    Text("Play, like or save a few songs first: your mixes grow from what you listen to.",
                        color = TextDim, modifier = Modifier.padding(24.dp))
                }
                problem != null && list.isEmpty() -> item {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(problem, color = TextDim)
                        OutlinedButton(onClick = onRetry) { Text("Try again") }
                    }
                }
                mix != null && list.isEmpty() -> item {
                    Text("Nothing new to suggest right now. Try again tomorrow.", color = TextDim, modifier = Modifier.padding(24.dp))
                }
            }
            itemsIndexed(list) { i, t -> RemoteRow(t, row, onPlay = { onPlayList(list, i) }, number = i + 1, onMenu = { onMenu(t) }) }
        }
    }
}

/** A new release on Home: cover, title, and who / when. */
@Composable
internal fun ReleaseCard(a: io.github.realkhfan.onda.core.RemoteAlbum, today: java.time.LocalDate, onClick: () -> Unit) {
    Column(Modifier.width(140.dp).clickable(onClick = onClick)) {
        Art(a.cover, 140.dp)
        Spacer(Modifier.height(6.dp))
        Text(a.title, color = TextMain, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${a.artist} · ${io.github.realkhfan.onda.core.NewReleases.ago(a, today)}",
            color = TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
