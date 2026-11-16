package io.github.realkhfan.onda.data

import android.content.Context
import org.json.JSONObject
import java.io.File

/** Where long songs (20 minutes and more: mixes, audiobooks) were left, so they go on from there. Newest 200. */
object Resume {
    private var file: File? = null
    private val at = LinkedHashMap<String, Long>()

    fun load(context: Context) {
        val f = File(context.filesDir, "resume.json").also { file = it }
        runCatching { JSONObject(f.readText()).let { o -> o.keys().forEach { k -> at[k] = o.getLong(k) } } }
    }

    fun at(id: String): Long = at[id] ?: 0

    /** Near the start or the end: start over next time. */
    fun left(id: String, posMs: Long, durMs: Long) {
        if (durMs < 20 * 60_000) return
        at.remove(id)
        if (posMs in 60_000..(durMs - 60_000)) at[id] = posMs
        while (at.size > 200) at.remove(at.keys.first())
        val f = file ?: return
        runCatching { File(f.path + ".tmp").apply { writeText(JSONObject(at as Map<*, *>).toString()) }.renameTo(f) }
    }
}
