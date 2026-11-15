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
            return@withContext if (code == 0 && File(out).isDirectory) Result.Picked(out) else Result.Cancelled
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
            return@withContext if (code == 0 && out.isNotEmpty() && (save || File(out).isFile)) Result.Picked(out) else Result.Cancelled
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
