package com.android.andex.termux

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddedTermuxBootstrapTest {
    @Test fun emulatorBootstrapAndHostToolsAreNativeX86() {
        val archive = File("src/main/assets/${EmbeddedTermuxRuntime.X86BootstrapAsset}")
        val hash = MessageDigest.getInstance("SHA-256").digest(archive.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        assertEquals(EmbeddedTermuxRuntime.X86BootstrapSha256, hash)
        ZipFile(archive).use { zip ->
            val header = zip.getInputStream(zip.getEntry("bin/bash")).use { it.readNBytes(20) }
            assertEquals(62, header[18].toInt() and 255)
        }
        listOf("proot.bin", "loader.bin", "libtalloc.so.2", "libandroid-shmem.so").forEach {
            val header = File(archive.parentFile, it).inputStream().use { stream -> stream.readNBytes(20) }
            assertEquals("$it architecture", 62, header[18].toInt() and 255)
        }
    }
    @Test fun rejectsZipSlipAndAbsolutePaths() {
        val root = Files.createTempDirectory("andex-bootstrap-test").toFile()
        try {
            listOf("../escape", "../../escape", "/etc/passwd", "bin\\escape", "").forEach {
                assertTrue("Accepted $it", runCatching { safeBootstrapPath(root, it) }.isFailure)
            }
            assertEquals(File(root, "bin/bash").canonicalFile, safeBootstrapPath(root, "bin/bash"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun bundledBootstrapMatchesPinnedChecksumAndArchitecture() {
        val archive = File("src/main/assets/${EmbeddedTermuxRuntime.BootstrapAsset}")
        assertTrue("Missing bundled Termux bootstrap", archive.isFile)
        val digest = MessageDigest.getInstance("SHA-256")
        archive.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        assertEquals(
            EmbeddedTermuxRuntime.BootstrapSha256,
            digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) },
        )
        ZipFile(archive).use { zip ->
            assertTrue(zip.getEntry("SYMLINKS.txt") != null)
            assertTrue(zip.getEntry("bin/curl") != null)
            val header = zip.getInputStream(zip.getEntry("bin/bash")).use { it.readNBytes(20) }
            assertEquals(127, header[0].toInt())
            assertEquals("ELF", header.copyOfRange(1, 4).toString(Charsets.US_ASCII))
            assertEquals(2, header[4].toInt())
            assertEquals(183, header[18].toInt() and 255)
        }
    }
}
