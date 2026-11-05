package com.android.andex.termux

import org.junit.Assert.*
import org.junit.Test

class EmbeddedAndroidIntentTest {
    @Test fun preservesIntentArgumentsWithoutShellExpansion() {
        val intent = parseEmbeddedAndroidIntent(listOf("start", "-W", "-a", "android.intent.action.VIEW",
            "-d", "https://example.org/a?q=x y", "--es", "query", "中文 ; $(id)"))
        assertEquals("https://example.org/a?q=x y", intent.data)
        assertEquals("中文 ; $(id)", intent.stringExtras["query"])
    }
    @Test fun supportsPackageAndComponentLaunch() {
        assertEquals("com.android.settings", parseEmbeddedAndroidIntent(listOf("start", "-p", "com.android.settings")).packageName)
        assertEquals("com.android.settings/.Settings", parseEmbeddedAndroidIntent(
            listOf("start", "-n", "com.android.settings/.Settings")).component)
    }
    @Test fun rejectsPrivilegedActionsAndMissingArguments() {
        listOf(listOf("force-stop", "com.example.app"), listOf("start", "-a"),
            listOf("start", "--user", "10", "-p", "com.example.app")).forEach {
            assertTrue(runCatching { parseEmbeddedAndroidIntent(it) }.isFailure)
        }
    }
}
