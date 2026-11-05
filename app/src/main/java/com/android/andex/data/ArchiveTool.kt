package com.android.andex.data

import android.content.Context
import com.android.andex.runtime.BundledNativeTools
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ArchiveTool(context: Context) {
    private val context = context.applicationContext
    private val tools = BundledNativeTools(this.context)

    suspend fun execute(raw: String): String = withContext(Dispatchers.IO) {
        try {
            val args = JSONObject(raw)
            val action = args.getString("action")
            if (action == "version") return@withContext tools.execute("7z", listOf("i")).toString()
            val archive = resolve(args.getString("archive"))
            val result = when (action) {
                "list" -> tools.execute("7z", listOf("l", "-slt", "--", archive.path))
                "extract" -> {
                    require(archive.isFile) { "Archive does not exist." }
                    val destination = resolve(args.getString("destination"))
                    require(!destination.exists() || destination.isDirectory && destination.list().isNullOrEmpty()) {
                        "Extract into a new or empty directory to avoid overwriting files."
                    }
                    val listing = tools.execute("7z", listOf("l", "-slt", "--", archive.path))
                    check(listing.optBoolean("ok")) { listing.optString("stderr") }
                    check(!listing.optBoolean("stdout_truncated")) { "Archive listing exceeds the safe inspection limit." }
                    validateArchiveListing(listing.getString("stdout"))
                    destination.mkdirs()
                    tools.execute("7z", listOf("x", "-y", "-o${destination.path}", "--", archive.path), 120000)
                        .put("destination", destination.path)
                }
                "compress" -> {
                    require(!archive.exists()) { "Output archive already exists." }
                    val sources = args.getJSONArray("sources")
                    require(sources.length() in 1..100) { "Provide 1..100 source paths." }
                    val files = (0 until sources.length()).map { resolve(sources.getString(it)) }
                    require(files.all { it.exists() }) { "A source file is missing." }
                    val format = args.optString("format", "7z")
                    require(format in listOf("7z", "zip", "tar", "gzip"))
                    archive.parentFile!!.mkdirs()
                    tools.execute("7z", listOf("a", "-t$format", "--", archive.path) + files.map { it.path }, 120000)
                        .put("archive", archive.path)
                }
                else -> error("action must be version, list, extract, or compress.")
            }
            result.toString()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            JSONObject().put("ok", false).put("errmsg", failure.message.orEmpty()).toString()
        }
    }

    private fun resolve(path: String): File {
        require(path.startsWith("/")) { "Use an absolute path from the uploaded file or workspace." }
        val home = "/data/data/com.termux/files/"
        val mapped = when {
            path.startsWith(home) -> File(context.filesDir, "runtimes/termux/files/" + path.removePrefix(home))
            path.startsWith("/workspace/") -> File(context.filesDir, "runtimes/alpine/workspace/" + path.removePrefix("/workspace/"))
            path.startsWith("/root/") -> File(context.filesDir, "runtimes/alpine/rootfs/root/" + path.removePrefix("/root/"))
            else -> File(path)
        }.canonicalFile
        val roots = listOf(context.filesDir, context.cacheDir, android.os.Environment.getExternalStorageDirectory())
        require(roots.any { mapped.path.startsWith(it.canonicalPath + File.separator) }) {
            "Path must be inside Andex files or authorized shared storage."
        }
        return mapped
    }

    companion object {
        internal fun validateArchiveListing(listing: String) {
            require(listing.contains("----------")) { "Unable to inspect archive entries." }
            require(!listing.contains("Symbolic Link =", true) && !listing.contains("Hard Link =", true) &&
                !Regex("""(?m)^Attributes = .*\bl[rwx-]{9}""").containsMatchIn(listing)) {
                "Archives with symbolic or hard links are not supported."
            }
            val entries = listing.substringAfter("----------", "").split(Regex("""\r?\n\r?\n"""))
            require(entries.size <= 10000) { "Too many archive entries." }
            var expanded = 0L
            entries.forEach { entry ->
                val path = Regex("""(?m)^Path = (.+)$""").find(entry)?.groupValues?.get(1)?.trim() ?: return@forEach
                require(!path.startsWith("/") && !path.startsWith("\\") && !Regex("^[A-Za-z]:").containsMatchIn(path) &&
                    path.replace('\\', '/').split('/').none { it == ".." }) { "Unsafe archive path." }
                val size = Regex("""(?m)^Size = (\d+)""").find(entry)?.groupValues?.get(1)?.toLong() ?: 0
                expanded += size
                require(expanded <= 512L * 1024 * 1024) { "Extracted files exceed 512 MiB." }
            }
        }

        fun definition(): JSONObject = JSONObject().put("name", "andex_archive")
            .put("description", "Bundled 7zip: list, extract uploaded archives, or compress files without downloads. action version/list/extract/compress; archive absolute path; extract destination must be new/empty; compress sources array and format 7z/zip/tar/gzip. Supports Termux/Alpine guest paths and authorized shared storage. Extraction rejects traversal/links and limits expanded data to 512 MiB.")
            .put("execution_mode", "sequential")
            .put("parameters", JSONObject().put("type", "object").put("required", JSONArray(listOf("action")))
                .put("properties", JSONObject().apply {
                    listOf("action", "archive", "destination", "format").forEach { put(it, JSONObject().put("type", "string")) }
                    put("sources", JSONObject().put("type", "array").put("items", JSONObject().put("type", "string")))
                }))
    }
}
