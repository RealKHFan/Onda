package io.github.realkhfan.onda.player

import android.content.Intent
import android.view.KeyEvent
import androidx.media3.common.MediaItem
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaButtonsTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val saved = File(app.filesDir, "session.json")

    @After fun clean() { saved.delete() }

    private fun press(code: Int) = MediaButtons().onReceive(app,
        Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, code)))

    @Test fun otherAppsSeeOndaAsAMusicPlayer() {
        // KLCK, Tasker etc. look for this receiver; Media3 needs exactly one.
        val receivers = app.packageManager.queryBroadcastReceivers(Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(app.packageName), 0)
        assertEquals(listOf(MediaButtons::class.java.name), receivers.map { it.activityInfo.name })
        val music = app.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC).setPackage(app.packageName), 0)
        assertEquals(1, music.size)
    }

    @Test fun playStartsOndaOnlyWithSomethingToPlay() {
        press(KeyEvent.KEYCODE_MEDIA_PLAY)
        assertNull("nothing saved: a started service would have nothing to play", shadowOf(app).nextStartedService)

        val song = MediaItem.Builder().setMediaId("a").setUri("content://music/a").build()
        LastSession.save(saved, LastSession.Saved(listOf(song), 0, 0, 0, false, emptyList(), LastSession.Source("Song", "", "")))
        press(KeyEvent.KEYCODE_MEDIA_NEXT)
        assertNull("only Play may start it", shadowOf(app).nextStartedService)
        press(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        val started = shadowOf(app).nextStartedService
        assertEquals(PlaybackService::class.java.name, started.component?.className)
        assertEquals(Intent.ACTION_MEDIA_BUTTON, started.action)
    }
}
