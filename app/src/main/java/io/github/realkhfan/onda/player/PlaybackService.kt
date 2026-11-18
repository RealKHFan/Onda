package io.github.realkhfan.onda.player

import kotlinx.coroutines.launch
import io.github.realkhfan.onda.scan.MusicLibrary
import io.github.realkhfan.onda.data.Overrides
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import android.content.Intent
import androidx.media3.common.AudioAttributes
import io.github.realkhfan.onda.data.PlayLog
import io.github.realkhfan.onda.data.Prefs
import androidx.media3.common.C
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import io.github.realkhfan.onda.data.AppData
import io.github.realkhfan.onda.data.Loudness
import io.github.realkhfan.onda.data.Resume
import io.github.realkhfan.onda.data.Link
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.json.JSONObject

/** Background playback with a media-style notification, lock-screen, Bluetooth and Android Auto. */
class PlaybackService : MediaLibraryService() {

    private var session: MediaLibrarySession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        // The service can start without the Activity (e.g. Bluetooth resume), so set this here too.
        System.setProperty("http.agent", "Onda") // default would send the phone model to preview/art servers
        AppData.load(this) // a car can start Onda without its screen
        PreviewResolver.freeSources = { Prefs.freeSearch(this) to Prefs.jamendoId(this) }
        // Previews queued as "dzpreview:<id>" get a fresh link right before they load.
        // Streams (http/https) go through the playback cache; your files are read directly.
        val cached = CacheDataSource.Factory()
            .setCache(PlaybackCache.get(this))
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
            .setCacheKeyFactory { spec -> PlaybackCache.key(spec.uri.toString()) }
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        // Songs from your PC ("ondalink:") are read over the link.
        val sources = ResolvingDataSource.Factory(RoutingDataSource.Factory(DefaultDataSource.Factory(this, cached)), PreviewResolver)
        val music = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build()
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(sources))
            .setAudioAttributes(music, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        exo = player
        // Crossfade's second player: the next song's first seconds (audio focus stays with the main player).
        val helper = ExoPlayer.Builder(this).setMediaSourceFactory(DefaultMediaSourceFactory(sources)).setAudioAttributes(music, false).build()
        crossfade = Crossfade(player, helper, scope, { Prefs.crossfade(this) }) { item ->
            if (!Prefs.normalize(this)) 1f else Math.pow(10.0, Loudness.gainDb(item.mediaId, emptyList()) / 20).coerceAtMost(1.0).toFloat()
        }.also { it.start() }
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Leaving a long song: remember where (a song that ended by itself starts over next time).
                lastId?.let { id -> if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) Resume.left(id, 0, lastDur) else Resume.left(id, lastPos, lastDur) }
                lastId = mediaItem?.mediaId; lastPos = 0; lastDur = 0
                mediaItem?.mediaId?.let { id -> Resume.at(id).takeIf { it > 0 }?.let { player.seekTo(it) } }
                levelFor(mediaItem?.mediaId)
                extendIfEnding(player)
            }
            override fun onAudioSessionIdChanged(audioSessionId: Int) = Effects.attach(audioSessionId)
            override fun onIsPlayingChanged(isPlaying: Boolean) { if (!isPlaying) remember(player) }
        })
        player.addListener(PlayCounter(player) { item -> // counts with the screen closed too
            PlayLog.add(item.mediaId, item.mediaMetadata.artist?.toString().orEmpty(), item.mediaMetadata.title?.toString().orEmpty())
        })
        Effects.setVolume = { v -> player.volume = v }
        Effects.normalize = Prefs.normalize(this)
        Effects.apply(io.github.realkhfan.onda.data.Prefs.eq(this))
        Effects.attach(player.audioSessionId)
        // Every control goes through this one: Previous goes back to what actually played.
        val back = HistoryPlayer(player).also { this.back = it }
        session = MediaLibrarySession.Builder(this, back, AutoLibrary(this, scope)).build()
        // Playing on your PC: the session (player bar, notification, lock screen) controls the PC instead.
        val remote = RemotePlayer(mainLooper, scope).also { remotePlayer = it }
        scope.launch {
            Remote.on.collect { on ->
                val s = session ?: return@collect
                if (on && s.player !== remote) {
                    crossfade?.cancel(); player.pause(); s.player = remote; remote.startPolling()
                    reporting?.cancel()
                    launch { Link.reportNow(this@PlaybackService, JSONObject().put("has", false)) } // the PC plays, not this phone
                } else if (!on && s.player !== back) { remote.stopPolling(); s.player = back; reportSoon() }
            }
        }
        // What the PC sends ("Play on… your phone", a sync, a song it wants), while this service runs.
        Link.onPlay = { q ->
            Remote.stop()
            val lib = AppData.library(MusicLibrary.tracks.value, AppData.serverTracks(), Overrides.meta.value)
            val r = Link.resolve(q, lib)
            if (r.items.isNotEmpty()) {
                // The PC's order, shuffle and repeat carry on here.
                player.repeatMode = RemotePlayer.exoRepeat(q.repeat)
                Shuffle.play(back, r.items, r.start, q.shuffle, if (q.positionMs > 3000) q.positionMs else 0, keepOrder = true)
            }
            android.widget.Toast.makeText(this, when {
                r.items.isEmpty() -> "None of those songs are on this phone"
                r.missing > 0 -> "Playing from your PC here · ${r.missing} not on this phone"
                else -> "Playing here (from your PC)"
            }, android.widget.Toast.LENGTH_SHORT).show()
        }
        // The PC as this phone's remote, and the PC starting to play.
        Link.onMessage = { m -> onPcMessage(m, back) }
        scope.launch { Link.listen(this@PlaybackService) }
        // What plays here reaches the PC (it shows it and can control it): on every change, and every 10 s while playing.
        player.addListener(object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) {
                if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_POSITION_DISCONTINUITY,
                        Player.EVENT_REPEAT_MODE_CHANGED, Player.EVENT_MEDIA_METADATA_CHANGED, Player.EVENT_TIMELINE_CHANGED)) reportSoon()
            }
        })
        scope.launch { androidx.compose.runtime.snapshotFlow { Shuffle.on }.collect { reportSoon() } }
        scope.launch { while (true) { kotlinx.coroutines.delay(10_000); if (player.isPlaying) reportSoon() } }
        Effects.refresh = { levelFor(player.currentMediaItem?.mediaId) }
        // Where a long song is, every 10 s, so it's known even if Onda is closed while playing.
        scope.launch { while (true) { kotlinx.coroutines.delay(10_000); if (player.isPlaying) remember(player) } }
    }

    private var exo: ExoPlayer? = null
    private var back: HistoryPlayer? = null
    private var reporting: kotlinx.coroutines.Job? = null

    /** Tells the PC what plays here, once things settle (a skip fires several events). */
    private fun reportSoon() {
        if (Remote.on.value) return
        reporting?.cancel()
        reporting = scope.launch { kotlinx.coroutines.delay(300); exo?.takeIf { !Remote.on.value }?.let { Link.reportNow(this@PlaybackService, nowOf(it)) } }
    }

    private fun nowOf(p: Player): JSONObject {
        val m = p.currentMediaItem ?: return JSONObject().put("has", false)
        val meta = p.mediaMetadata
        return JSONObject().put("has", true).put("playing", p.isPlaying).put("key", m.mediaId)
            .put("title", meta.title?.toString().orEmpty()).put("artist", meta.artist?.toString().orEmpty()).put("album", meta.albumTitle?.toString().orEmpty())
            .put("art", meta.artworkUri?.toString()?.takeIf { it.startsWith("http") }).put("pos", p.currentPosition.coerceAtLeast(0))
            .put("dur", p.duration.coerceAtLeast(0)).put("shuffle", Shuffle.on).put("repeat", RemotePlayer.pcRepeat(p.repeatMode))
            // What's next too, for the PC's queue panel while it controls this phone.
            .put("next", org.json.JSONArray(upcoming(p, 50).map { i ->
                val it = p.getMediaItemAt(i).mediaMetadata
                JSONObject().put("key", p.getMediaItemAt(i).mediaId).put("title", it.title?.toString().orEmpty()).put("artist", it.artist?.toString().orEmpty())
                    .put("album", it.albumTitle?.toString().orEmpty()).put("art", it.artworkUri?.toString()?.takeIf { a -> a.startsWith("http") }).put("dur", it.durationMs ?: 0L)
            }))
    }

    /** The songs still to come, in play order (as [max] timeline indexes): what the PC's queue panel shows and changes. */
    private fun upcoming(p: Player, max: Int = Int.MAX_VALUE): List<Int> {
        val t = p.currentTimeline
        if (t.isEmpty) return emptyList()
        val out = ArrayList<Int>()
        var i = t.getNextWindowIndex(p.currentMediaItemIndex, Player.REPEAT_MODE_OFF, p.shuffleModeEnabled)
        while (i != androidx.media3.common.C.INDEX_UNSET && out.size < max) { out += i; i = t.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, p.shuffleModeEnabled) }
        return out
    }

    /** The PC controls this phone, hands over, or started playing itself. */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun onPcMessage(m: JSONObject, p: Player) {
        val player = exo ?: return
        if (m.optBoolean("pcPlaying")) {
            // Your PC plays and this phone doesn't: show it here (this phone becomes the remote), as with Spotify's devices.
            if (!player.isPlaying && !Remote.on.value) Remote.start()
            return
        }
        if (Remote.on.value) return // the PC plays; it controls itself
        m.optJSONObject("add")?.let { a ->
            val lib = AppData.library(MusicLibrary.tracks.value, AppData.serverTracks(), Overrides.meta.value)
            p.enqueue(Link.resolve(io.github.realkhfan.onda.core.LinkSync.queueFromJson(a), lib).items, next = m.optBoolean("next"))
        }
        when (m.optString("cmd")) {
            "toggle" -> if (player.isPlaying) p.pause() else { p.prepare(); p.play() }
            "next" -> p.seekToNext()
            "prev" -> p.seekToPrevious()
            "seek" -> p.seekTo(m.optLong("ms").coerceAtLeast(0))
            "shuffle" -> Shuffle.set(p, m.optBoolean("on"))
            "repeat" -> p.repeatMode = RemotePlayer.exoRepeat(m.optInt("mode"))
            // The PC's queue panel ([i], [from], [to] count from the first song still to come).
            "jump" -> upcoming(player).getOrNull(m.optInt("i", -1))?.let { p.seekTo(it, 0) }
            "remove" -> upcoming(player).drop(m.optInt("i", -1).coerceAtLeast(0)).take(m.optInt("n", 1).coerceIn(1, 50))
                .takeIf { m.optInt("i", -1) >= 0 }?.sortedDescending()?.forEach(p::removeMediaItem)
            "move" -> upcoming(player).let { up ->
                val f = m.optInt("from", -1); val t = m.optInt("to", -1)
                if (f in up.indices && t in up.indices) p.moveMediaItem(up[f], up[t])
            }
            "clear" -> upcoming(player).sortedDescending().forEach(p::removeMediaItem)
        }
        if (m.optBoolean("handover")) {
            // "This PC" on the PC: what plays here carries on there, from the same place.
            val cur = player.currentMediaItemIndex.takeIf { it >= 0 } ?: return
            val q = RemotePlayer.queueOf((cur until player.mediaItemCount).map(player::getMediaItemAt), player.currentPosition)
                .copy(shuffle = Shuffle.on, repeat = RemotePlayer.pcRepeat(player.repeatMode))
            p.pause()
            scope.launch { if (Link.playThere(q) != null) Remote.start() }
        }
        reportSoon()
    }
    private var crossfade: Crossfade? = null
    private var remotePlayer: RemotePlayer? = null
    private var lastId: String? = null
    private var lastPos = 0L
    private var lastDur = 0L

    private fun remember(p: Player) {
        if (p.duration < 20 * 60_000) return
        lastPos = p.currentPosition; lastDur = p.duration
        p.currentMediaItem?.mediaId?.let { Resume.left(it, lastPos, lastDur) }
    }

    /** The song's loudness level (or its album's, if that setting is on). */
    private fun levelFor(id: String?) {
        Effects.normalize = Prefs.normalize(this)
        if (!Effects.normalize) return
        val album = if (id == null || !Prefs.albumGain(this)) emptyList() else {
            val lib = AppData.library(MusicLibrary.tracks.value, AppData.serverTracks(), Overrides.meta.value)
            lib.firstOrNull { it.id == id }?.let { t -> lib.filter { it.albumKey == t.albumKey }.map { it.id } }.orEmpty()
        }
        Effects.songGainDb = Loudness.gainDb(id, album)
    }

    private var extending = false

    /** On the last song (repeat off), add similar songs so the music doesn't stop, if Settings allows. */
    private fun extendIfEnding(p: Player) {
        if (extending || !Prefs.autoplay(this) || p.repeatMode != Player.REPEAT_MODE_OFF) return
        if (p.mediaItemCount == 0 || p.currentMediaItemIndex != p.mediaItemCount - 1) return
        val items = (0 until p.mediaItemCount).map { p.getMediaItemAt(it) }
        val artists = items.takeLast(5).reversed().mapNotNull { it.mediaMetadata.artist?.toString() }
        extending = true
        scope.launch {
            try {
                val lib = AppData.library(MusicLibrary.tracks.value, AppData.serverTracks(), Overrides.meta.value)
                val more = Autoplay.similar(artists, items.map { it.mediaId }.toSet(), lib, listenBrainz = Prefs.listenBrainz(this@PlaybackService))
                // Still on the last song (nothing was added meanwhile)?
                if (more.isNotEmpty() && p.currentMediaItemIndex == p.mediaItemCount - 1) p.addMediaItems(more)
            } catch (_: Exception) {
            } finally {
                extending = false
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    /** Swiping Onda away from Recents pauses and stops it, unless Settings says to keep playing. */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        // As your PC's remote: closing Onda here stops being the remote, and the PC keeps playing.
        if (!Prefs.keepPlaying(this) && Remote.on.value) { remotePlayer?.stopPolling(); back?.let { session?.player = it }; Remote.stop() }
        if (Prefs.keepPlaying(this)) super.onTaskRemoved(rootIntent) else pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        Effects.release()
        Link.onPlay = null
        Link.onMessage = null
        crossfade?.release()
        remotePlayer?.release()
        exo?.release()
        session?.release()
        session = null
        scope.cancel()
        super.onDestroy()
    }
}
