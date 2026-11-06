package com.android.andex.runtime

import java.io.File
import java.nio.file.Files
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder

class BundledMemoryUpgradeTest {
    @get:Rule val temporary = TemporaryFolder()
    private val old = mapOf("andex.ts" to "old entry", "store.ts" to "old store", "package.json" to "{}")
    private val next = old.mapValues { (_, value) -> "$value updated".toByteArray() }
    private val hashes = old.mapValues { (_, value) -> memoryFileHash(value.toByteArray()) }
    private fun directory() = temporary.newFolder().also { dir -> old.forEach { (name, value) -> File(dir, name).writeText(value) } }
    @Test fun upgradesOriginalCodePreservingMemoryAndUserFilesAndIsIdempotent() {
        val dir = directory()
        val data = File(dir, "MEMORY.md").apply { writeText("personal memory") }
        val marker = File(temporary.root, "removed")
        assertTrue(upgradeBundledMemory(dir, marker, next, hashes))
        assertArrayEquals(next.getValue("store.ts"), File(dir, "store.ts").readBytes())
        assertEquals("personal memory", data.readText())
        assertFalse(upgradeBundledMemory(dir, marker, next, hashes))
    }
    @Test fun customCodeOrRemovedEntrySkipsTheEntireUpgrade() {
        val dir = directory()
        val marker = File(temporary.root, "removed")
        File(dir, "store.ts").writeText("user edit")
        assertFalse(upgradeBundledMemory(dir, marker, next, hashes))
        assertEquals("old entry", File(dir, "andex.ts").readText())
        File(dir, "store.ts").writeText(old.getValue("store.ts"))
        marker.writeText("removed")
        assertFalse(upgradeBundledMemory(dir, marker, next, hashes))
    }
    @Test fun resumesPartialReplacementAndMissingFilesAreNotRecreated() {
        val dir = directory()
        val marker = File(temporary.root, "removed")
        File(dir, "andex.ts").writeBytes(next.getValue("andex.ts"))
        assertTrue(upgradeBundledMemory(dir, marker, next, hashes))
        File(dir, "store.ts").delete()
        assertFalse(upgradeBundledMemory(dir, marker, next, hashes))
        assertFalse(File(dir, "store.ts").exists())
    }
    @Test fun upgradesExactKnownRepairButDoesNotAcceptOtherEdits() {
        val dir = directory()
        val marker = File(temporary.root, "removed")
        val repaired = "old store with corrected credential filter"
        val compatible = mapOf("store.ts" to setOf(memoryFileHash(repaired.toByteArray())))
        File(dir, "store.ts").writeText("$repaired user change")
        assertFalse(upgradeBundledMemory(dir, marker, next, hashes, compatible))
        File(dir, "store.ts").writeText(repaired)
        assertTrue(upgradeBundledMemory(dir, marker, next, hashes, compatible))
        assertArrayEquals(next.getValue("store.ts"), File(dir, "store.ts").readBytes())
    }
}
