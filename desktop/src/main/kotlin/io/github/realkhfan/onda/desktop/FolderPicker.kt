package io.github.realkhfan.onda.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** The desktop's own folder picker: KDE's kdialog or GNOME's zenity (Java's built-in dialog can't draw text on some systems). */
object FolderPicker {
    sealed interface Result {
        data class Picked(val path: String) : Result
        data object Cancelled : Result
        /** Neither tool is installed: the typed path field is the way. */
        data object NoPicker : Result
        /** A network place (smb://…) that isn't mounted as a folder on this PC: only KDE apps can reach those. */
        data class Remote(val url: String) : Result {
            val message get() = "That's a network place ($url), which Onda can't open. Mount the share once so it's a normal folder " +
                "on this PC (README.txt shows how), then choose it again."
        }
    }

    /** The picker's answer: a local path, the same place inside a mounted share, or a network place Onda can't use. */
    private fun answer(out: String, ok: (File) -> Boolean): Result {
        if (!Regex("^[a-z][a-z0-9+.-]*://", RegexOption.IGNORE_CASE).containsMatchIn(out) || out.startsWith("file://")) {
            val f = File(out.removePrefix("file://"))
            return if (ok(f)) Result.Picked(f.path) else Result.Cancelled
        }
        val local = runCatching { mounted(out, File("/proc/mounts").readText()) }.getOrNull()
        return if (local != null && ok(File(local))) Result.Picked(local) else Result.Remote(out)
    }

    /** smb://host/share/a/b -> the same folder below where //host/share is mounted (cifs), if it is. */
    internal fun mounted(url: String, mounts: String): String? {
        val u = java.net.URI(url.replace(" ", "%20"))
        if (!u.scheme.equals("smb", true) || u.host == null) return null
        val parts = (u.path ?: "").trim('/').split('/').filter { it.isNotEmpty() }
        val share = parts.firstOrNull() ?: return null
        val octal = Regex("""\\([0-7]{3})""") // /proc/mounts writes a space as \040
        return mounts.lineSequence().map { it.split(' ') }.firstNotNullOfOrNull { f ->
            val src = f.getOrNull(0)?.replace(octal) { it.groupValues[1].toInt(8).toChar().toString() } ?: return@firstNotNullOfOrNull null
            val at = f.getOrNull(1)?.replace(octal) { it.groupValues[1].toInt(8).toChar().toString() }
            if (f.getOrNull(2) in setOf("cifs", "smb3") && at != null && src.trimEnd('/').equals("//${u.host}/$share", true))
                (listOf(at) + parts.drop(1)).joinToString("/") else null
        }
    }

    suspend fun pick(start: String): Result = withContext(Dispatchers.IO) {
        val tries = listOf(
            listOf("kdialog", "--title", "Add a music folder", "--getexistingdirectory", start),
            listOf("zenity", "--file-selection", "--directory", "--title=Add a music folder", "--filename=$start/"),
        )
        for (cmd in tries) {
            val p = try { ProcessBuilder(cmd).start() } catch (_: Exception) { continue } // not installed: try the next
            val out = p.inputStream.bufferedReader().readText().trim()
            val code = p.waitFor()
            return@withContext if (code == 0 && out.isNotEmpty()) answer(out) { it.isDirectory } else Result.Cancelled
        }
        Result.NoPicker
    }

    /** A file to open, or a place to save one ([save]); same pickers as for folders. */
    suspend fun file(save: Boolean, start: String, title: String): Result = withContext(Dispatchers.IO) {
        val tries = listOf(
            listOf("kdialog", "--title", title, if (save) "--getsavefilename" else "--getopenfilename", start),
            listOfNotNull("zenity", "--file-selection", "--title=$title", if (save) "--save" else null, "--filename=$start"),
        )
        for (cmd in tries) {
            val p = try { ProcessBuilder(cmd).start() } catch (_: Exception) { continue }
            val out = p.inputStream.bufferedReader().readText().trim()
            val code = p.waitFor()
            return@withContext if (code == 0 && out.isNotEmpty()) answer(out) { save || it.isFile } else Result.Cancelled
        }
        Result.NoPicker
    }

    /** "~/Music" or a pasted path -> an existing folder, or null. */
    fun resolve(typed: String): String? {
        val t = typed.trim().removeSurrounding("\"").removePrefix("file://")
        if (t.isEmpty()) return null
        val f = File(if (t == "~" || t.startsWith("~/")) System.getProperty("user.home") + t.drop(1) else t)
        return f.takeIf { it.isDirectory }?.canonicalPath
    }
}
