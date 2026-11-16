package io.github.realkhfan.onda.ui

import android.graphics.drawable.Animatable
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import coil3.ImageLoader
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.gif.repeatCount
import coil3.request.ImageRequest
import io.github.realkhfan.onda.data.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private var animatedLoader: ImageLoader? = null

/** Your picture (a GIF or animated WebP keeps playing while Onda is on screen), or the letter. */
@Composable
internal fun ProfilePic(size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val file = Profile.picture
    if (file == null) {
        Box(modifier.size(size).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
            Text("R", color = Color(0xFF141018), fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
        }
        return
    }
    val loader = animatedLoader ?: ImageLoader.Builder(ctx.applicationContext).components {
        add(if (Build.VERSION.SDK_INT >= 28) AnimatedImageDecoder.Factory() else GifDecoder.Factory())
    }.build().also { animatedLoader = it }
    val request = remember(file) { ImageRequest.Builder(ctx).data(file).repeatCount(-1).build() } // -1: loop forever
    // Plays only while Onda is in front; stopped behind other apps.
    val state by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    var animation by remember(file) { mutableStateOf<Animatable?>(null) }
    LaunchedEffect(animation, state) { animation?.let { if (state.isAtLeast(Lifecycle.State.RESUMED)) it.start() else it.stop() } }
    AsyncImage(
        request, "Your picture", loader, modifier.size(size).clip(CircleShape).background(Raised),
        contentScale = ContentScale.Crop,
        onSuccess = { s -> animation = s.result.image.asDrawable(ctx.resources) as? Animatable },
    )
}

/** Settings → Profile picture: a photo or a GIF from your phone, or none. */
@Composable
internal fun ProfileSettings() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { Profile.set(ctx, uri) }
            if (!ok) Toast.makeText(ctx, "That picture can't be used (a JPG, PNG, WebP or GIF up to 20 MB)", Toast.LENGTH_LONG).show()
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ProfilePic(64.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("A photo or a GIF; a GIF keeps playing while Onda is open.", color = TextDim, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(if (Profile.picture == null) "Choose" else "Change")
                }
                if (Profile.picture != null) TextButton(onClick = { Profile.clear(ctx) }) { Text("Remove", color = TextDim) }
            }
        }
    }
}
