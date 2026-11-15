package io.github.realkhfan.onda.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.realkhfan.onda.data.Server
import io.github.realkhfan.onda.data.Subsonic
import kotlinx.coroutines.launch

/** Settings section for the Navidrome / OpenSubsonic server. */
@Composable
internal fun ServerSection() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cfg by Server.config.collectAsState()
    val songs by Server.songs.collectAsState()
    val syncing by Server.syncing.collectAsState()
    val problem by Server.problem.collectAsState()
    val kbps by Server.kbps.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Music server (Navidrome)", Modifier.padding(0.dp))
        val c = cfg
        if (c == null) {
            var url by rememberSaveable { mutableStateOf("") }
            var user by rememberSaveable { mutableStateOf("") }
            var pass by remember { mutableStateOf("") } // plain remember: never written to saved state
            var connecting by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            Text(
                "Stream your whole library in full from Navidrome (or another OpenSubsonic server) on your Pi or NAS: " +
                    "at home, or anywhere through WireGuard.",
                color = TextDim, fontSize = 13.sp,
            )
            OutlinedTextField(
                url, { url = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text("Server address") }, placeholder = { Text("192.168.1.20:4533") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            OutlinedTextField(user, { user = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("Username") })
            OutlinedTextField(
                pass, { pass = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            Button(
                onClick = {
                    connecting = true; error = null
                    scope.launch {
                        error = Server.connect(ctx, url, user, pass)
                        if (error == null) pass = ""
                        connecting = false
                    }
                },
                enabled = !connecting && url.isNotBlank() && user.isNotBlank() && pass.isNotEmpty(),
            ) { Text(if (connecting) "Connecting…" else "Connect") }
            error?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
            Text("Onda keeps a login token, not your password.", color = TextDim, fontSize = 12.sp)
        } else {
            Text("Connected to ${c.url} as ${c.user}", color = TextMain)
            Text(
                when {
                    syncing -> "Syncing your library…"
                    problem != null -> problem!!
                    else -> "${songCount(songs.size)} on the server"
                },
                color = if (problem != null && !syncing) ErrorRed else TextDim, fontSize = 13.sp,
            )
            if (c.url.startsWith("http://") && !Subsonic.isPrivateHost(c.url)) {
                Text(
                    "This address is outside your home network and uses plain http, so your login token travels " +
                        "unencrypted. Use https, or your WireGuard/LAN address.",
                    color = ErrorRed, fontSize = 12.sp,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { Server.sync() }, enabled = !syncing) { Text("Sync now") }
                OutlinedButton(onClick = { Server.disconnect(ctx) }) { Text("Disconnect") }
            }
            Text("Streaming quality", color = TextMain)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Original", 320 to "320 kbps", 128 to "128 kbps").forEach { (k, label) ->
                    Chip(label, kbps == k) { Server.setKbps(ctx, k) }
                }
            }
            Text(
                "Lower uses less mobile data; your server converts on the fly. Applies to songs you start after changing it.",
                color = TextDim, fontSize = 12.sp,
            )
        }
    }
}
