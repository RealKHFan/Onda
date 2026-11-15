package io.github.realkhfan.onda.player

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
        val sources = ResolvingDataSource.Factory(DefaultDataSource.Factory(this), PreviewResolver)
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
        session = MediaLibrarySession.Builder(this, player, AutoLibrary(this, scope)).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    /** Swiping Onda away from Recents pauses and stops it, unless Settings says to keep playing. */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (Prefs.keepPlaying(this)) super.onTaskRemoved(rootIntent) else pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        scope.cancel()
        super.onDestroy()
    }
}
