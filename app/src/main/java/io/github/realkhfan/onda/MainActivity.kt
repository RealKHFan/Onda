package io.github.realkhfan.onda

import kotlinx.coroutines.Dispatchers
import io.github.realkhfan.onda.data.Backup
import io.github.realkhfan.onda.data.Downloads
import io.github.realkhfan.onda.data.AppData
import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import io.github.realkhfan.onda.core.RemoteAlbum
import io.github.realkhfan.onda.data.ArtistIds
import io.github.realkhfan.onda.data.Prefs
import io.github.realkhfan.onda.data.ReleaseJob
import io.github.realkhfan.onda.data.Releases
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.realkhfan.onda.data.Lrclib
import io.github.realkhfan.onda.data.Overrides
import io.github.realkhfan.onda.data.PlaylistStore
import io.github.realkhfan.onda.data.Mixes
import io.github.realkhfan.onda.data.PlayLog
import io.github.realkhfan.onda.data.Server
import io.github.realkhfan.onda.player.OpenWith
import io.github.realkhfan.onda.player.PlaybackService
import io.github.realkhfan.onda.player.PlayerState
import io.github.realkhfan.onda.scan.MusicLibrary
import io.github.realkhfan.onda.ui.LibraryActions
import io.github.realkhfan.onda.ui.OndaApp
import io.github.realkhfan.onda.ui.OndaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var controller: MediaController? = null
    private var player by mutableStateOf<PlayerState?>(null)

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                MusicLibrary.addFolder(this, uri)
                rescan()
            }
        }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openReleaseFrom(intent)
        OpenWith.from(this, intent)
    }

    /** A tapped release notification carries the album id; the app opens that album page. */
    private fun openReleaseFrom(intent: Intent?) {
        val id = intent?.getLongExtra(Releases.EXTRA_ALBUM, 0L) ?: 0L
        if (id <= 0L) return
        intent?.removeExtra(Releases.EXTRA_ALBUM) // not again after rotation
        Releases.open.value = Releases.items.value.firstOrNull { it.id == id } ?: RemoteAlbum(id, "", "", null, null, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.setProperty("http.agent", "Onda") // default would send the phone model to preview/art servers

        AppData.load(this)
        Downloads.resume(this) // downloads that didn't finish last time
        lifecycleScope.launch(Dispatchers.IO) { Backup.autoIfDue(applicationContext) }
        if (Prefs.releaseAlerts(this)) ReleaseJob.schedule(this)
        openReleaseFrom(intent)
        OpenWith.from(this, intent)
        if (savedInstanceState == null) Server.sync() // refresh the server's song list once per app start
        LibraryActions.removeFolder = { uriString -> MusicLibrary.removeFolder(this, uriString) }
        LibraryActions.addFolder = { folderPicker.launch(null) }
        LibraryActions.rescan = { rescan() }

        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        connectController()
        rescan()

        setContent {
            OndaTheme {
                OndaApp(player = player)
            }
        }
    }

    private fun connectController() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener(
            {
                try {
                    val c = future.get()
                    controller = c
                    player = PlayerState(c)
                } catch (_: Throwable) {
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    private fun rescan() {
        lifecycleScope.launch { MusicLibrary.rescan(this@MainActivity) }
    }

    override fun onDestroy() {
        controller?.release()
        controller = null
        super.onDestroy()
    }
}
