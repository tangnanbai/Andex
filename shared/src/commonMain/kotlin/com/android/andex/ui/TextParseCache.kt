package com.android.andex.ui

/** UI-thread-owned LRU for immutable parse trees, bounded by source characters. */
class TextParseCache<T>(private val maxCharacters: Int = 512_000) {
    private val entries = LinkedHashMap<Pair<String, Int>, T>()
    private var characters = 0

    fun get(text: String, offset: Int = 0): T? {
        val key = text to offset
        val value = entries.remove(key) ?: return null
        entries[key] = value
        return value
    }

    fun put(text: String, value: T, offset: Int = 0) {
        val weight = text.length.coerceAtLeast(1)
        if (weight > maxCharacters) return
        val key = text to offset
        if (entries.remove(key) != null) characters -= weight
        entries[key] = value
        characters += weight
        while (characters > maxCharacters || entries.size > 128) {
            val oldest = entries.keys.first()
            entries.remove(oldest)
            characters -= oldest.first.length.coerceAtLeast(1)
        }
    }
}
