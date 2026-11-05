package com.android.andex.termux

import android.content.Context
import android.os.Build
import android.system.Os
import com.android.andex.phonecontrol.shellQuote
import com.android.andex.runtime.AlpineTerminalLaunchSpec
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val MaxEmbeddedDispatchOutputBytes = 2L * 1024 * 1024

/**
 * Official Termux packages retain their compiled prefix inside PRoot. Nothing is installed
 * in the external Termux application's directory, even when that application is present.
 */
class EmbeddedTermuxRuntime(context: Context) {
    private val context = context.applicationContext
    private val root = File(this.context.filesDir, "runtimes/termux")
    private val files = File(root, "files")
    private val hostBin = File(root, "host")
    private val installMutex = Mutex()
    @Volatile private var nativeCommandsReady = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processes = ConcurrentHashMap.newKeySet<Process>()
    private val api by lazy { EmbeddedTermuxApi(this.context) }
    private val nativeAbi = Build.SUPPORTED_ABIS.firstOrNull { it == "arm64-v8a" || it == "x86_64" }
    private val bootstrapAsset
        get() = if (nativeAbi == "x86_64") X86BootstrapAsset else BootstrapAsset
    private val bootstrapSha256
        get() = if (nativeAbi == "x86_64") X86BootstrapSha256 else BootstrapSha256

    val isInstalled: Boolean
        get() = File(files, "usr/.andex-bootstrap").let { it.isFile && it.readText().trim() == bootstrapSha256 } &&
            File(files, "usr/bin/bash").isFile && File(hostBin, "proot").isFile

    suspend fun initialize(): TermuxSetupState = withContext(Dispatchers.IO) {
        installMutex.withLock {
            if (isInstalled) {
                if (!nativeCommandsReady) {
                    installNativeApiCommands()
                    nativeCommandsReady = true
                }
                return@withLock TermuxSetupState()
            }
            if (nativeAbi == null) {
                return@withLock TermuxSetupState(
                    TermuxSetupIssue.DispatchFailed, "Embedded Termux requires arm64-v8a or x86_64.",
                )
            }
            try {
                root.mkdirs()
                hostBin.mkdirs()
                File(root, "tmp").mkdirs()
                File(root, "cache/apt/archives/partial").mkdirs()
                File(files, "home").mkdirs()
                listOf("proot.bin" to "proot", "loader.bin" to "loader", "libtalloc.so.2" to "libtalloc.so.2")
                    .forEach { (asset, name) ->
                        val assetRoot = if (nativeAbi == "x86_64") "runtimes/termux/x86_64"
                            else "runtimes/alpine/arm64-v8a"
                        context.assets.open("$assetRoot/$asset").use { input ->
                            File(hostBin, name).outputStream().use { input.copyTo(it) }
                        }
                        Os.chmod(File(hostBin, name).path, 448)
                    }
                if (nativeAbi == "x86_64") {
                    context.assets.open("runtimes/termux/x86_64/libandroid-shmem.so").use { input ->
                        File(hostBin, "libandroid-shmem.so").outputStream().use { input.copyTo(it) }
                    }
                }
                val archive = File(root, "bootstrap.zip")
                context.assets.open(bootstrapAsset).use { input ->
                    archive.outputStream().use { input.copyTo(it) }
                }
                val digest = MessageDigest.getInstance("SHA-256")
                archive.inputStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val size = input.read(buffer)
                        if (size < 0) break
                        digest.update(buffer, 0, size)
                    }
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
                check(actual == bootstrapSha256) { "Termux bootstrap checksum mismatch." }
                val staging = File(files, "usr-installing")
                // Only installer-owned files are retried. The user's home is never cleared.
                staging.deleteRecursively()
                staging.mkdirs()
                val links = mutableListOf<Pair<String, String>>()
                var expandedBytes = 0L
                ZipInputStream(archive.inputStream()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.name == "SYMLINKS.txt") {
                            val text = zip.readBytes().toString(Charsets.UTF_8)
                            text.lineSequence().filter(String::isNotBlank).forEach { line ->
                                val parts = line.split('\u2190')
                                require(parts.size == 2) { "Malformed Termux symlink." }
                                safeBootstrapPath(staging, parts[1])
                                links += parts[0] to parts[1]
                            }
                        } else {
                            val target = safeBootstrapPath(staging, entry.name)
                            if (entry.isDirectory) {
                                target.mkdirs()
                            } else {
                                target.parentFile!!.mkdirs()
                                target.outputStream().use { output ->
                                    val buffer = ByteArray(64 * 1024)
                                    while (true) {
                                        val count = zip.read(buffer)
                                        if (count < 0) break
                                        expandedBytes += count
                                        require(expandedBytes < 512L * 1024 * 1024) { "Bootstrap exceeds size limit." }
                                        output.write(buffer, 0, count)
                                    }
                                }
                                Os.chmod(target.path, if (
                                    entry.name.startsWith("bin/") || entry.name.startsWith("libexec/") ||
                                    entry.name.startsWith("lib/apt/")
                                ) 448 else 384)
                            }
                        }
                        zip.closeEntry()
                    }
                }
                check(links.isNotEmpty() && File(staging, "bin/bash").isFile) { "Incomplete Termux bootstrap." }
                links.forEach { (destination, name) ->
                    val link = safeBootstrapPath(staging, name)
                    link.parentFile!!.mkdirs()
                    Os.symlink(destination, link.path)
                }
                File(staging, "tmp").mkdirs()
                File(staging, ".andex-bootstrap").writeText(bootstrapSha256)
                val prefix = File(files, "usr")
                // Preserve the old ARM packages when upgrading an emulator to its native ABI.
                if (nativeAbi == "x86_64" && File(prefix, ".andex-bootstrap").let {
                    it.isFile && it.readText().trim() == BootstrapSha256
                }) {
                    check(prefix.renameTo(File(files, "usr-arm64-backup"))) {
                        "Cannot preserve the previous ARM64 prefix."
                    }
                }
                check(!prefix.exists()) { "Incomplete prefix exists; refusing to overwrite installed packages." }
                check(staging.renameTo(prefix)) { "Cannot publish Termux bootstrap." }
                archive.delete()
                installNativeApiCommands()
                nativeCommandsReady = true
                TermuxSetupState()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                TermuxSetupState(TermuxSetupIssue.DispatchFailed, error.message.orEmpty())
            }
        }
    }

    suspend fun execute(
        script: String,
        workingDirectory: String = TermuxContract.HomeDirectory,
        timeoutMillis: Long = 60_000,
    ): String = withContext(Dispatchers.IO) {
        if (!isInstalled || !nativeCommandsReady) {
            val setup = initialize()
            if (!setup.isReady) return@withContext result(-1, "", "", setup.detail)
        }
        val dispatch = File(files, ".dispatch/${UUID.randomUUID()}").apply { mkdirs() }
        val guestDispatch = "/data/data/com.termux/files/.dispatch/${dispatch.name}"
        File(dispatch, "command.sh").writeText(script)
        val stdout = File(dispatch, "stdout")
        val stderr = File(dispatch, "stderr")
        val exit = File(dispatch, "exit")
        // The outer shell records completion independently of PRoot: a detached adb server
        // or managed bash run may keep its tracer alive after the requested shell exits.
        val wrapper = "${TermuxContract.BashPath} ${shellQuote("$guestDispatch/command.sh")}; " +
            "rc=\$?; printf '%s' \"\$rc\" > ${shellQuote("$guestDispatch/exit.tmp")}; " +
            "${TermuxContract.PrefixDirectory}/bin/mv ${shellQuote("$guestDispatch/exit.tmp")} " +
            "${shellQuote("$guestDispatch/exit")}; exit \"\$rc\""
        var process: Process? = null
        var completed = false
        try {
            process = ProcessBuilder(
                *(hostLaunchCommand() + listOf(
                "--link2symlink", "-r", "/", "-b", "${root.path}:/data/data/com.termux",
                "-b", "${root.path}:/data/user/0/com.termux",
                "-w", workingDirectory, TermuxContract.BashPath, "-c", wrapper,
                )).toTypedArray(),
            ).apply {
                directory(root)
                redirectOutput(stdout)
                redirectError(stderr)
                environment().apply {
                    put("HOME", TermuxContract.HomeDirectory)
                    put("PREFIX", TermuxContract.PrefixDirectory)
                    put("PATH", "${TermuxContract.HomeDirectory}/.local/bin:${TermuxContract.PrefixDirectory}/bin:/system/bin")
                    put("TMPDIR", "${TermuxContract.PrefixDirectory}/tmp")
                    put("PROOT_LOADER", File(hostBin, "loader").path)
                    put("PROOT_TMP_DIR", File(root, "tmp").path)
                    put("LD_LIBRARY_PATH", "${hostBin.path}:${TermuxContract.PrefixDirectory}/lib")
                    put("TERM", "xterm-256color")
                    put("TERMUX_APP_PACKAGE_MANAGER", "apt")
                    put("TERMUX_APP_PACKAGE_NAME", context.packageName)
                    put("ANDEX_APP_PACKAGE", context.packageName)
                    remove("LD_PRELOAD")
                    putAll(api.environment)
                }
            }.start()
            process.outputStream.close()
            processes.add(process)
            val deadline = System.nanoTime() + timeoutMillis.coerceAtLeast(1) * 1_000_000
            while (!exit.isFile && process.isAlive && System.nanoTime() < deadline) delay(40)
            val code = exit.takeIf(File::isFile)?.readText()?.trim()?.toIntOrNull()
            completed = code != null
            result(
                code ?: -1, readTail(stdout), readTail(stderr),
                if (stdout.length() > MaxEmbeddedDispatchOutputBytes || stderr.length() > MaxEmbeddedDispatchOutputBytes) {
                    "Embedded Termux output exceeds 2 MiB. Use managed streaming or chunked file reads."
                } else if (code != null) "" else if (process.isAlive) "Embedded Termux command timed out."
                else "Embedded Termux process exited before completing the command.",
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            result(-1, readTail(stdout), readTail(stderr), error.message.orEmpty())
        } finally {
            File(dispatch, "command.sh").delete()
            val running = process
            if (!completed) running?.destroy()
            scope.launch {
                while (running?.isAlive == true) delay(500)
                if (running != null) processes.remove(running)
                dispatch.deleteRecursively()
            }
        }
    }

    fun stop() {
        processes.toList().forEach { it.destroy() }
        processes.clear()
    }

    suspend fun createTerminalLaunchSpec(): AlpineTerminalLaunchSpec = withContext(Dispatchers.IO) {
        val setup = initialize()
        check(setup.isReady) { setup.detail }
        val hostCommand = hostLaunchCommand()
        val executable = hostCommand.first()
        AlpineTerminalLaunchSpec(
            executable = executable,
            arguments = (hostCommand + listOf(
                "--link2symlink", "-r", "/",
                "-b", "${root.path}:/data/data/com.termux", "-b", "${root.path}:/data/user/0/com.termux",
                "-w", TermuxContract.HomeDirectory,
                TermuxContract.BashPath, "--rcfile", "${TermuxContract.HomeDirectory}/.andex/terminal.rc", "-i",
            )).toTypedArray(),
            environment = (listOf(
                "HOME=${TermuxContract.HomeDirectory}", "PREFIX=${TermuxContract.PrefixDirectory}",
                "PATH=${TermuxContract.HomeDirectory}/.local/bin:${TermuxContract.PrefixDirectory}/bin:/system/bin",
                "TMPDIR=${TermuxContract.PrefixDirectory}/tmp",
                "PROOT_LOADER=${File(hostBin, "loader").path}", "PROOT_TMP_DIR=${File(root, "tmp").path}",
                "LD_LIBRARY_PATH=${hostBin.path}:${TermuxContract.PrefixDirectory}/lib",
                "TERM=xterm-256color", "COLORTERM=truecolor",
                "TERMUX_APP_PACKAGE_MANAGER=apt",
                "TERMUX_APP_PACKAGE_NAME=${context.packageName}", "ANDEX_APP_PACKAGE=${context.packageName}",
            ) + api.environment.map { (key, value) -> "$key=$value" }).toTypedArray(),
            workingDirectory = root.path,
        )
    }

    private fun hostLaunchCommand(): List<String> {
        val proot = File(hostBin, "proot").path
        // Android 9's linker cannot be invoked as an executable launcher. Android 10+
        // requires the linker path when executing binaries from writable app storage.
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) listOf(proot)
        else listOf("/system/bin/linker64", proot)
    }

    private fun installNativeApiCommands() {
        val home = File(files, "home")
        val commands = File(home, ".local/bin").apply { mkdirs() }
        listOf("am", "termux-am").forEach { name ->
            File(commands, name).apply {
                writeText(
                    "#!${TermuxContract.BashPath}\n" +
                        "printf '%s\\0' \"\$@\" | ${TermuxContract.PrefixDirectory}/bin/curl --silent --show-error --fail-with-body --max-time 15 " +
                        "-H \"Authorization: Bearer \$ANDEX_TERMUX_API_TOKEN\" " +
                        "--data-binary @- \"\$ANDEX_TERMUX_API_URL/android-am\"\n",
                )
                Os.chmod(path, 448)
            }
        }
        mapOf("andex-phone-control" to "phone-control", "andex-launch" to "launch-app").forEach { (name, method) ->
            File(commands, name).apply {
                writeText(
                    "#!${TermuxContract.BashPath}\n" +
                        "printf '%s' \"\$*\" | ${TermuxContract.PrefixDirectory}/bin/curl --silent --show-error --fail-with-body --max-time 45 " +
                        "-H \"Authorization: Bearer \$ANDEX_TERMUX_API_TOKEN\" " +
                        "--data-binary @- \"\$ANDEX_TERMUX_API_URL/$method\"\n",
                )
                Os.chmod(path, 448)
            }
        }
        listOf("battery-status", "clipboard-get", "clipboard-set", "toast", "vibrate", "setup-storage").forEach { method ->
            val file = File(commands, "termux-$method")
            val body = if (method == "clipboard-set") "" else "printf '%s' \"\$*\" | "
            file.writeText(
                "#!${TermuxContract.BashPath}\n" +
                    "${body}${TermuxContract.PrefixDirectory}/bin/curl --silent --show-error --fail-with-body --max-time 10 " +
                    "-H \"Authorization: Bearer \$ANDEX_TERMUX_API_TOKEN\" " +
                    "--data-binary @- \"\$ANDEX_TERMUX_API_URL/$method\"\n",
            )
            Os.chmod(file.path, 448)
        }
        val metadata = File(home, ".andex").apply { mkdirs() }
        File(metadata, "terminal.rc").writeText(
            "[ ! -f \"\$PREFIX/etc/profile\" ] || source \"\$PREFIX/etc/profile\"\n" +
                "[ ! -f \"\$HOME/.bashrc\" ] || source \"\$HOME/.bashrc\"\n" +
                "export PATH=\"\$HOME/.local/bin:\$PREFIX/bin:\$PATH\"\n",
        )
        val storage = File(home, "storage").apply { mkdirs() }
        val shared = android.os.Environment.getExternalStorageDirectory()
        mapOf(
            "shared" to shared,
            "downloads" to File(shared, "Download"),
            "documents" to File(shared, "Documents"),
            "dcim" to File(shared, "DCIM"),
            "pictures" to File(shared, "Pictures"),
            "music" to File(shared, "Music"),
            "movies" to File(shared, "Movies"),
        ).forEach { (name, target) ->
            target.mkdirs()
            val link = File(storage, name)
            if (!java.nio.file.Files.exists(link.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                Os.symlink(target.path, link.path)
            }
        }
    }

    companion object {
        const val BootstrapAsset = "runtimes/termux/arm64-v8a/bootstrap.zip"
        const val BootstrapSha256 = "c671bde0d9923ede9d9e972634a04042c93b1589b9a2ca9bc5c38848234e8c5b"
        const val X86BootstrapAsset = "runtimes/termux/x86_64/bootstrap.zip"
        const val X86BootstrapSha256 = "9933e09342ec96df3f912483db622f7b7ba78d4e627c80951d0123b6b2fff750"

        private fun result(code: Int, stdout: String, stderr: String, error: String): String =
            JSONObject().put("ok", code == 0 && error.isEmpty()).put("exit_code", code)
                .put("stdout", stdout).put("stderr", stderr).put("errmsg", error).toString()

        private fun readTail(file: File): String {
            if (!file.isFile) return ""
            return RandomAccessFile(file, "r").use { input ->
                val count = minOf(input.length(), MaxEmbeddedDispatchOutputBytes).toInt()
                input.seek(input.length() - count)
                ByteArray(count).also { input.readFully(it) }.toString(Charsets.UTF_8)
            }
        }
    }
}

internal fun safeBootstrapPath(root: File, entry: String): File {
    require(entry.isNotBlank() && !entry.startsWith("/") && '\\' !in entry) { "Invalid bootstrap entry." }
    val base = root.canonicalFile
    val target = File(base, entry).canonicalFile
    require(target.path.startsWith(base.path + File.separator)) { "Bootstrap entry escapes prefix." }
    return target
}
