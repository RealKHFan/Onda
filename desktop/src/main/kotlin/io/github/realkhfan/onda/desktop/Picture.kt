package io.github.realkhfan.onda.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import java.io.File

/**
 * Your profile picture (Settings → Profile picture), or the person icon. A GIF or animated WebP plays in a loop while
 * Onda's window is the active one; otherwise it stands still.
 */
@Composable
internal fun ProfilePic(path: String?, size: Dp, highlighted: Boolean = false, modifier: Modifier = Modifier) {
    val codec = remember(path) { path?.let { runCatching { Codec.makeFromData(Data.makeFromBytes(File(it).readBytes())) }.getOrNull() } }
    if (codec == null) {
        Box(modifier.size(size).clip(CircleShape).background(if (highlighted) Accent else Raised), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Person, null, tint = if (highlighted) Color.Black else TextMain)
        }
        return
    }
    val active = LocalWindowInfo.current.isWindowFocused
    var image by remember(codec) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(codec, active) {
        val frames = codec.frameCount
        val bitmap = Bitmap().apply { allocPixels(codec.imageInfo) }
        var i = 0
        while (true) {
            // Each frame is drawn over the one before (one decode per frame, also for long GIFs).
            val frame = i
            image = withContext(Dispatchers.Default) {
                if (frame == 0) bitmap.erase(0) // a new round starts from a clear picture
                codec.readPixels(bitmap, frame, frame - 1)
                org.jetbrains.skia.Image.makeFromBitmap(bitmap).toComposeImageBitmap()
            }
            if (frames <= 1 || !active) break
            // Like browsers: a frame without a real duration shows for 100 ms.
            delay(codec.getFrameInfo(frame).duration.let { if (it <= 10) 100L else it.toLong() })
            i = (i + 1) % frames
        }
    }
    val shown = image
    Box(modifier.size(size).clip(CircleShape).background(Raised).then(if (highlighted) Modifier.border(2.dp, Accent, CircleShape) else Modifier)) {
        if (shown != null) Image(shown, "Your picture", Modifier.size(size), contentScale = ContentScale.Crop)
    }
}
