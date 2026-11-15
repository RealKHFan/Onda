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
import io.github.realkhfan.onda.data.Prefs
import androidx.media3.common.C
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import io.github.realkhfan.onda.data.AppData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

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
        val sources = ResolvingDataSource.Factory(DefaultDataSource.Factory(this, cached), PreviewResolver)
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(sources))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = extendIfEnding(player)
            override fun onAudioSessionIdChanged(audioSessionId: Int) = Effects.attach(audioSessionId)
        })
        Effects.apply(io.github.realkhfan.onda.data.Prefs.eq(this))
        Effects.attach(player.audioSessionId)
        session = MediaLibrarySession.Builder(this, player, AutoLibrary(this, scope)).build()
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
        if (Prefs.keepPlaying(this)) super.onTaskRemoved(rootIntent) else pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        Effects.release()
        session?.run {
            player.release()
            release()
        }
        session = null
        scope.cancel()
        super.onDestroy()
    }
}
