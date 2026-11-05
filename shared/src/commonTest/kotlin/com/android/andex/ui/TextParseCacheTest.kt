package com.android.andex.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TextParseCacheTest {
    @Test fun accessKeepsRecentTreesAndEvictsOldestByTextSize() {
        val cache = TextParseCache<String>(6)
        cache.put("aa", "first")
        cache.put("bb", "second")
        cache.put("cc", "third")
        assertEquals("first", cache.get("aa"))
        cache.put("ddd", "fourth")
        assertNull(cache.get("bb"))
        assertNull(cache.get("cc"))
        assertEquals("first", cache.get("aa"))
        assertEquals("fourth", cache.get("ddd"))
    }

    @Test fun offsetAndReplacementDoNotCorruptBudgetOrPositions() {
        val cache = TextParseCache<String>(8)
        cache.put("same", "at zero")
        cache.put("same", "at four", 4)
        cache.put("same", "updated")
        assertEquals("at four", cache.get("same", 4))
        assertEquals("updated", cache.get("same"))
        cache.put("too long for this cache", "skip")
        assertNull(cache.get("too long for this cache"))
        assertEquals("updated", cache.get("same"))
    }
}
