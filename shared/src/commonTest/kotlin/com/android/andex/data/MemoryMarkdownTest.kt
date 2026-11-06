package com.android.andex.data

import kotlin.test.*

class MemoryMarkdownTest {
    @Test fun importsChineseUtf8AndUtf16WithoutCorruption() {
        val text = "# 记忆\n喜欢中文 ✅"
        assertEquals(text, decodeMemoryMarkdown(text.encodeToByteArray()))
        assertEquals(text, decodeMemoryMarkdown(("\uFEFF" + text).encodeToByteArray()))
        for (little in listOf(true, false)) {
            val bytes = mutableListOf(if (little) 0xff.toByte() else 0xfe.toByte(), if (little) 0xfe.toByte() else 0xff.toByte())
            text.forEach { char ->
                val low = char.code.toByte(); val high = (char.code shr 8).toByte()
                bytes.add(if (little) low else high); bytes.add(if (little) high else low)
            }
            assertEquals(text, decodeMemoryMarkdown(bytes.toByteArray()))
        }
    }
    @Test fun malformedTextIsRejectedInsteadOfSavingReplacementCharacters() {
        assertFails { decodeMemoryMarkdown(byteArrayOf(0xff.toByte(), 0xfe.toByte(), 1)) }
        assertFails { decodeMemoryMarkdown(byteArrayOf(0xff.toByte(), 1)) }
    }
}
