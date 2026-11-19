package io.github.realkhfan.onda.player

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import java.io.File

/**
 * Play on headphones, a car or a lock-screen app while Onda is closed: starts Onda on what played last.
 * Declaring it also makes Onda count as a music player for apps like KLCK and Tasker (they look for this receiver).
 * Media3 hands it to Android for the session; while Onda runs, buttons go straight to the session instead.
 */
class MediaButtons : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) return
        val key = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java) ?: return
        if (key.action != KeyEvent.ACTION_DOWN || key.repeatCount != 0) return
        // Only Play, and only with something to play: a service started this way must play at once or Android crashes it.
        if (key.keyCode !in PLAY) return
        if (LastSession.load(File(context.filesDir, "session.json"))?.items.isNullOrEmpty()) return
        try {
            ContextCompat.startForegroundService(context, Intent(intent).setComponent(ComponentName(context, PlaybackService::class.java)))
        } catch (_: IllegalStateException) {
            // Android didn't allow a start from the background right now; the button does nothing, as before.
        }
    }

    private companion object {
        val PLAY = setOf(KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK)
    }
}
