package com.android.andex.runtime

import android.content.Context
import android.os.Build
import android.system.Os
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** ADB and 7zip do not need PRoot or an apt installation to start. */
class BundledNativeTools(context: Context) {
    private val context = context.applicationContext
    val root = File(this.context.filesDir, "runtimes/native-tools")
    companion object { private val lock = Any() }
    val adbHome = File(root, "home").apply { mkdirs() }
    private val abi = if (Build.SUPPORTED_ABIS.firstOrNull() == "x86_64") "x86_64" else "arm64-v8a"

    fun initialize() = synchronized(lock) {
        val asset = "runtimes/native-tools/$abi"
        val manifest = JSONObject(context.assets.open("$asset/manifest.json").bufferedReader().use { it.readText() })
        val expected = manifest.getString("archive_sha256")
        if (File(root, ".ready").takeIf { it.isFile }?.readText() == expected &&
            File(root, "bin/adb").isFile && (File(root, "bin/7z").isFile || File(root, "bin/7zz").isFile)) return@synchronized
        root.mkdirs()
        val archive = File(root, "tools.zip")
        context.assets.open("$asset/tools.zip").use { input -> archive.outputStream().use { output -> input.copyTo(output) } }
        val actual = MessageDigest.getInstance("SHA-256").digest(archive.readBytes()).joinToString("") { "%02x".format(it.toInt() and 255) }
        check(actual == expected) { "Bundled tools checksum mismatch." }
        ZipInputStream(archive.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val file = File(root, entry.name)
                require(file.canonicalPath.startsWith(root.canonicalPath + File.separator)) { "Invalid bundled path." }
                if (!entry.isDirectory) {
                    file.parentFile!!.mkdirs()
                    file.outputStream().use { zip.copyTo(it) }
                    Os.chmod(file.path, if (entry.name.startsWith("bin/")) 448 else 384)
                }
            }
        }
        archive.delete()
        File(root, ".ready").writeText(expected)
    }

    fun executable(name: String): File {
        require(name in listOf("adb", "7z", "7zz"))
        initialize()
        if (name != "adb" && File(root, "bin/7zz").isFile) return File(root, "bin/7zz")
        return File(root, "bin/$name").takeIf { it.isFile }
            ?: File(root, "bin/7zz").takeIf { name != "adb" && it.isFile }
            ?: error("Bundled $name is unavailable.")
    }

    suspend fun execute(name: String, arguments: List<String>, timeout: Long = 30000, stdoutFile: File? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val binary = executable(name)
            val command = if (Build.VERSION.SDK_INT < 29) listOf(binary.path) + arguments
                else listOf("/system/bin/linker64", binary.path) + arguments
            val temporary = File(root, "runs/${java.util.UUID.randomUUID()}").apply { mkdirs() }
            val stdout = stdoutFile ?: File(temporary, "stdout")
            val stderr = File(temporary, "stderr")
            try {
                val process = ProcessBuilder(command).apply {
                    redirectOutput(stdout)
                    redirectError(stderr)
                    directory(adbHome)
                    environment().apply {
                        put("HOME", adbHome.path)
                        put("ANDROID_USER_HOME", adbHome.path)
                        put("LD_LIBRARY_PATH", File(root, "lib").path)
                        put("TMPDIR", context.cacheDir.path)
                        put("ADB_SERVER_SOCKET", "tcp:5038")
                    }
                }.start()
                process.outputStream.close()
                val completed = process.waitFor(timeout, TimeUnit.MILLISECONDS)
                if (!completed) process.destroyForcibly()
                JSONObject().put("ok", completed && process.exitValue() == 0)
                    .put("exit_code", if (completed) process.exitValue() else -1)
                    .put("stdout", if (stdoutFile == null) readBounded(stdout) else "")
                    .put("stdout_truncated", stdoutFile == null && stdout.length() > 2 * 1024 * 1024)
                    .put("stderr", readBounded(stderr))
                    .put("errmsg", if (completed) "" else "$name timed out.")
            } finally {
                temporary.deleteRecursively()
            }
        }

    private fun readBounded(file: File): String = if (!file.isFile) "" else
        file.inputStream().use { input ->
            val bytes = ByteArray(2 * 1024 * 1024)
            var count = 0
            while (count < bytes.size) {
                val read = input.read(bytes, count, bytes.size - count)
                if (read < 0) break
                count += read
            }
            bytes.copyOf(count).toString(Charsets.UTF_8)
        }
}
