package io.github.realkhfan.onda.data

import android.content.Context

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

    /** Max streaming bitrate in kbps; 0 = the original file. */
    fun streamKbps(ctx: Context): Int = p(ctx).getInt("stream_kbps", 0)
    fun setStreamKbps(ctx: Context, kbps: Int) = p(ctx).edit().putInt("stream_kbps", kbps).apply()

    /** On by default: a notification when an artist you listen to releases something. */
    fun releaseAlerts(ctx: Context): Boolean = p(ctx).getBoolean("release_alerts", true)
    fun setReleaseAlerts(ctx: Context, on: Boolean) = p(ctx).edit().putBoolean("release_alerts", on).apply()

    /** Off by default: downloads wait for Wi-Fi (or any unmetered network). */
    fun mobileDownloads(ctx: Context): Boolean = p(ctx).getBoolean("mobile_downloads", false)
    fun setMobileDownloads(ctx: Context, on: Boolean) = p(ctx).edit().putBoolean("mobile_downloads", on).apply()

    /** Off by default: closing Onda from Recents pauses it and ends background playback. */
    fun keepPlaying(ctx: Context): Boolean = p(ctx).getBoolean("keep_playing", false)
    fun setKeepPlaying(ctx: Context, on: Boolean) = p(ctx).edit().putBoolean("keep_playing", on).apply()
}
