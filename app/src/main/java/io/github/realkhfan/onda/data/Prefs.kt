package io.github.realkhfan.onda.data

import android.content.Context
import org.json.JSONObject

/** Small user settings, in the same "onda" prefs file the library uses for its folders. */
object Prefs {
    private fun p(ctx: Context) = ctx.getSharedPreferences("onda", Context.MODE_PRIVATE)

    fun appleSearch(ctx: Context): Boolean = p(ctx).getBoolean("apple_search", true)
    fun setAppleSearch(ctx: Context, on: Boolean) = p(ctx).edit().putBoolean("apple_search", on).apply()

    /** Audius + Internet Archive full songs in search. */
    fun freeSearch(ctx: Context): Boolean = p(ctx).getBoolean("free_search", true)
    fun setFreeSearch(ctx: Context, on: Boolean) = p(ctx).edit().putBoolean("free_search", on).apply()

    /** Jamendo needs a free client ID from its developer portal; blank = Jamendo off. */
    fun jamendoId(ctx: Context): String = p(ctx).getString("jamendo_id", "") ?: ""
    fun setJamendoId(ctx: Context, id: String) = p(ctx).edit().putString("jamendo_id", id.trim()).apply()

    /** Music server login: address, user and token (never the password). Private app storage. */
    fun server(ctx: Context): Subsonic.Config? {
        val p = p(ctx)
        return Subsonic.Config(
            url = p.getString("srv_url", null) ?: return null,
            user = p.getString("srv_user", null) ?: return null,
            salt = p.getString("srv_salt", null) ?: return null,
            token = p.getString("srv_token", null) ?: return null,
        )
    }

    fun saveServer(ctx: Context, c: Subsonic.Config) = p(ctx).edit()
        .putString("srv_url", c.url).putString("srv_user", c.user).putString("srv_salt", c.salt).putString("srv_token", c.token)
        .apply()

    fun clearServer(ctx: Context) = p(ctx).edit().remove("srv_url").remove("srv_user").remove("srv_salt").remove("srv_token").apply()

    /** Playlists pinned to the top (newest pin first), and how the rest are ordered: "Custom", "A–Z" or "Most songs". */
    fun pinned(ctx: Context): List<String> = p(ctx).getString("pinned", "")!!.split('\n').filter { it.isNotBlank() }
    fun setPinned(ctx: Context, ids: List<String>) = p(ctx).edit().putString("pinned", ids.joinToString("\n")).apply()
    fun playlistOrder(ctx: Context): String = p(ctx).getString("playlist_order", "Custom") ?: "Custom"
    fun setPlaylistOrder(ctx: Context, order: String) = p(ctx).edit().putString("playlist_order", order).apply()

    /** Even out loudness (measured on the phone), and keep an album's own dynamics when playing it. */
    fun normalize(ctx: Context): Boolean = p(ctx).getBoolean("normalize", false)
    fun setNormalize(ctx: Context, on: Boolean) = p(ctx).edit().putBoolean("normalize", on).apply()
    fun albumGain(ctx: Context): Boolean = p(ctx).getBoolean("album_gain", false)
    fun setAlbumGain(ctx: Context, on: Boolean) = p(ctx).edit().putBoolean("album_gain", on).apply()

    /** Lyrics you picked for a song (LRCLIB id, 0 = the one found) and their shift in ms (+ = shown earlier). */
    fun lyricsPick(ctx: Context, id: String): Pair<Long, Long> =
        runCatching { org.json.JSONObject(p(ctx).getString("lyrics_picks", "{}")!!).optJSONArray(id)?.let { it.getLong(0) to it.getLong(1) } }.getOrNull() ?: (0L to 0L)
    fun setLyricsPick(ctx: Context, id: String, lrclibId: Long, shiftMs: Long) {
        val o = runCatching { org.json.JSONObject(p(ctx).getString("lyrics_picks", "{}")!!) }.getOrDefault(org.json.JSONObject())
        if (lrclibId == 0L && shiftMs == 0L) o.remove(id) else o.put(id, org.json.JSONArray().put(lrclibId).put(shiftMs))
        p(ctx).edit().putString("lyrics_picks", o.toString()).apply()
    }
    fun lyricsScale(ctx: Context): Float = p(ctx).getFloat("lyrics_scale", 1f)
    fun setLyricsScale(ctx: Context, scale: Float) = p(ctx).edit().putFloat("lyrics_scale", (Math.round(scale * 10) / 10f).coerceIn(0.6f, 1.8f)).apply()

    /** Your last 10 searches, newest first (on this phone only). */
    fun searches(ctx: Context): List<String> = p(ctx).getString("searches", "")!!.split('\n').filter { it.isNotBlank() }
    fun rememberSearch(ctx: Context, q: String) {
        val t = q.trim().takeIf { it.length >= 2 } ?: return
        p(ctx).edit().putString("searches", (listOf(t) + searches(ctx).filterNot { it.equals(t, true) }).take(10).joinToString("\n")).apply()
    }
    fun forgetSearch(ctx: Context, q: String?) =
        p(ctx).edit().putString("searches", if (q == null) "" else (searches(ctx) - q).joinToString("\n")).apply()

    /** Highlight colour (ARGB). */
    fun accent(ctx: Context): Long = p(ctx).getLong("accent", 0xFF9D7BFF)
    fun setAccent(ctx: Context, argb: Long) = p(ctx).edit().putLong("accent", argb).apply()

    /** Equalizer: 10 gains in dB (30 Hz … 15 kHz, like the desktop) and the preset they came from. */
    fun eq(ctx: Context): List<Double> = p(ctx).getString("eq", null)?.split(',')?.mapNotNull { it.toDoubleOrNull() }?.takeIf { it.size == 10 } ?: List(10) { 0.0 }
    fun eqPreset(ctx: Context): String = p(ctx).getString("eq_preset", "Flat") ?: "Flat"
    fun setEq(ctx: Context, gains: List<Double>, preset: String) = p(ctx).edit().putString("eq", gains.joinToString(",")).putString("eq_preset", preset).apply()

    /** Max streaming bitrate in kbps; 0 = the original file. */
    fun streamKbps(ctx: Context): Int = p(ctx).getInt("stream_kbps", 0)
    fun setStreamKbps(ctx: Context, kbps: Int) = p(ctx).edit().putInt("stream_kbps", kbps).apply()

    /** On by default: a notification when an artist you listen to releases something. */
    fun releaseAlerts(ctx: Context): Boolean = p(ctx).getBoolean("release_alerts", true)
    fun setReleaseAlerts(ctx: Context, on: Boolean) = p(ctx).edit().putBoolean("release_alerts", on).apply()

    /** Off by default: downloads wait for Wi-Fi (or any unmetered network). */
    fun mobileDownloads(ctx: Context): Boolean = p(ctx).getBoolean("mobile_downloads", false)
    fun setMobileDownloads(ctx: Context, on: Boolean) = p(ctx).edit().putBoolean("mobile_downloads", on).apply()

    /** On by default: ListenBrainz adds similar artists to Deezer's for mixes and autoplay. */
    fun listenBrainz(ctx: Context): Boolean = p(ctx).getBoolean("listenbrainz", true)
    fun setListenBrainz(ctx: Context, on: Boolean) = p(ctx).edit().putBoolean("listenbrainz", on).apply()

    /** On by default: when the queue runs out, similar songs keep playing. */
    fun autoplay(ctx: Context): Boolean = p(ctx).getBoolean("autoplay", true)
    fun setAutoplay(ctx: Context, on: Boolean) = p(ctx).edit().putBoolean("autoplay", on).apply()

    /** Folder for the daily automatic backup (a tree URI), and when it last ran. */
    fun backupTree(ctx: Context): String? = p(ctx).getString("backup_tree", null)
    fun setBackupTree(ctx: Context, uri: String?) = p(ctx).edit().putString("backup_tree", uri).apply()
    fun lastBackup(ctx: Context): Long = p(ctx).getLong("last_backup", 0)
    fun setLastBackup(ctx: Context, at: Long) = p(ctx).edit().putLong("last_backup", at).apply()

    // Settings that go into a backup. Not the server login (it's a credential) and not folders (their access is per phone).
    private val bools = listOf("apple_search", "free_search", "release_alerts", "keep_playing", "mobile_downloads", "autoplay", "listenbrainz")

    fun export(ctx: Context): JSONObject = JSONObject().also { o ->
        val p = p(ctx)
        bools.forEach { k -> if (p.contains(k)) o.put(k, p.getBoolean(k, false)) }
        p.getString("jamendo_id", null)?.let { o.put("jamendo_id", it) }
        if (p.contains("stream_kbps")) o.put("stream_kbps", p.getInt("stream_kbps", 0))
    }

    fun import(ctx: Context, o: JSONObject) {
        val e = p(ctx).edit()
        bools.forEach { k -> if (o.has(k)) e.putBoolean(k, o.optBoolean(k)) }
        if (o.has("jamendo_id")) e.putString("jamendo_id", o.optString("jamendo_id").trim())
        if (o.has("stream_kbps")) e.putInt("stream_kbps", o.optInt("stream_kbps"))
        e.apply()
    }

    /** Off by default: closing Onda from Recents pauses it and ends background playback. */
    fun keepPlaying(ctx: Context): Boolean = p(ctx).getBoolean("keep_playing", false)
    fun setKeepPlaying(ctx: Context, on: Boolean) = p(ctx).edit().putBoolean("keep_playing", on).apply()
}
