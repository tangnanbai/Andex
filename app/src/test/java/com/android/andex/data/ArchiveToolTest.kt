package com.android.andex.data

import org.junit.Test

class ArchiveToolTest {
    @Test fun acceptsSafeEntries() {
        ArchiveTool.validateArchiveListing("----------\nPath = docs/readme.txt\nSize = 42\n\nPath = image.png\nSize = 100\n")
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsTraversal() {
        ArchiveTool.validateArchiveListing("----------\nPath = a/../../secret\nSize = 1\n")
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsWindowsAbsolutePath() {
        ArchiveTool.validateArchiveListing("----------\nPath = C:\\secret.txt\nSize = 1\n")
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsLinks() {
        ArchiveTool.validateArchiveListing("----------\nPath = link\nSymbolic Link = /secret\n")
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsExcessiveExpansion() {
        ArchiveTool.validateArchiveListing("----------\nPath = large.bin\nSize = 536870913\n")
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnparseableOutput() {
        ArchiveTool.validateArchiveListing("unrecognized output")
    }
}
