package com.android.andex.runtime

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.security.MessageDigest

internal val OriginalMemoryFileHashes = mapOf(
    "andex.ts" to "fb1923d5758da0f296d0a85c56a0606958390df1c8f521fa6cc465ceb946201b",
    "store.ts" to "96595d81e7190eb85d2922c95e18462338830eeb661bd569d8f057359e168808",
    "package.json" to "d2424fb5f54c9596124d08dee3254056c89c33309d0492e3c081082fc9f25c78",
)

// A previously shipped/repaired 0.98 store corrected the password keyword and
// added a trailing blank line. Match its exact bytes; arbitrary edits still opt out.
internal val CompatibleMemoryFileHashes = mapOf(
    "andex.ts" to setOf("660bcc7b85c9af236abc5bfc1a624286546e4404c188b7211d60d8e86fc21ce5", "1601f2e951bd7f365b44ff286686a3c0a9e813c137b3c9e3c41b261f1928d4cd"),
    "store.ts" to setOf("a75b88005d05bd0be4979a836e7c8fa6c1494d4dfed2dd522b0663c0aabce1be",
        "b899b70a0800504ddbd4b40e0e615003a4daa174733361bdb0773142ef20a3c6", "1c3380ce9af10fd0ae823b0b95ec040b0bb1dd2812b21a8e159c488086a9b3e9"),
)

internal fun memoryFileHash(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

/** Upgrade known bundled code only. User edits, removed entries and memory data stay intact. */
@Synchronized
internal fun upgradeBundledMemory(
    directory: File,
    removalMarker: File,
    bundled: Map<String, ByteArray>,
    originals: Map<String, String> = OriginalMemoryFileHashes,
    compatibleOriginals: Map<String, Set<String>> = CompatibleMemoryFileHashes,
): Boolean {
    if (Files.exists(removalMarker.toPath(), LinkOption.NOFOLLOW_LINKS)) return false
    if (!directory.isDirectory || bundled.keys != originals.keys) return false
    if (generateSequence(directory.toPath().toAbsolutePath()) { it.parent }.any(Files::isSymbolicLink)) return false
    val updates = linkedMapOf<File, ByteArray>()
    for ((name, originalHash) in originals) {
        val target = File(directory, name)
        if (!Files.isRegularFile(target.toPath(), LinkOption.NOFOLLOW_LINKS) || target.length() > 2_097_152) return false
        val bundledBytes = bundled.getValue(name)
        val currentHash = memoryFileHash(target.readBytes())
        val nextHash = memoryFileHash(bundledBytes)
        // Recognize partially completed upgrades so an interrupted atomic replacement can resume.
        if (currentHash != originalHash && currentHash != nextHash &&
            currentHash !in compatibleOriginals[name].orEmpty()) return false
        if (currentHash != nextHash) updates[target] = bundledBytes
    }
    for ((target, bytes) in updates) {
        val temporary = File.createTempFile(".andex-upgrade-", ".tmp", directory)
        try {
            temporary.outputStream().use { it.write(bytes); it.fd.sync() }
            try {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally { temporary.delete() }
    }
    return updates.isNotEmpty()
}
