package com.android.andex.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.*

class DoubaoImeProtocolTest {
    @Test fun requestUsesDocumentedFieldNumbers() {
        val packet = DoubaoImeProtocol.request("r", "StartTask", "t")
        assertContentEquals(byteArrayOf(18, 1, 116, 26, 3, 65, 83, 82, 42, 9) +
            "StartTask".encodeToByteArray() + byteArrayOf(66, 1, 114), packet)
    }
    @Test fun truncatedAndOversizedResponsesFailClosed() {
        assertFailsWith<IllegalArgumentException> { DoubaoImeProtocol.response(byteArrayOf(34, 10, 65)) }
        assertFailsWith<IllegalArgumentException> { DoubaoImeProtocol.response(ByteArray(1_048_577)) }
    }
    @Test fun serverStatusMessageIsNotDiscarded() {
        val status = "concurrency quota exceeded".encodeToByteArray()
        val response = DoubaoImeProtocol.response(
            byteArrayOf(34, 13) + "SessionFailed".encodeToByteArray() +
                byteArrayOf(50, status.size.toByte()) + status,
        )
        assertEquals("SessionFailed", response.type)
        assertEquals("concurrency quota exceeded", response.message)
    }
    @Test fun partialRevisionsReplaceRatherThanAppendAndFinalSegmentsAccumulate() {
        val text = DoubaoTranscript()
        fun event(value: String, final: Boolean = false) = Json.parseToJsonElement(
            """{"results":[{"text":"$value","is_interim":${!final},"is_vad_finished":$final}]}"""
        ).jsonObject
        assertEquals("今天", text.accept(event("今天")))
        assertEquals("今天下雨", text.accept(event("今天下雨")))
        assertEquals("今天晴天。", text.accept(event("今天晴天。", true)))
        assertEquals("今天晴天。", text.accept(event("今天晴天。", true)))
        assertEquals("今天晴天。出去散步", text.accept(event("出去散步")))
        assertEquals("今天晴天。出去散步。", text.accept(event("出去散步。", true)))
        assertTrue(text.hasFinal)
        assertEquals("今天晴天。出去散步。", text.finalText)
    }
}
