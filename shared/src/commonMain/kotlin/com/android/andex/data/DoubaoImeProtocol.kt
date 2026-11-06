package com.android.andex.data

import kotlinx.serialization.json.*

/** Independently implemented wire format documented in docs/doubao-ime-asr.md. */
internal object DoubaoImeProtocol {
    fun request(id: String, method: String, token: String = "", payload: String = "",
                audio: ByteArray = byteArrayOf(), state: Int = 0): ByteArray {
        val output = ArrayList<Byte>()
        fun variable(number: Long) {
            var n = number
            while (n >= 128) { output += ((n and 127) or 128).toByte(); n = n ushr 7 }
            output += n.toByte()
        }
        fun bytes(field: Int, value: ByteArray) {
            if (value.isEmpty()) return
            variable((field * 8 + 2).toLong()); variable(value.size.toLong()); output.addAll(value.toList())
        }
        bytes(2, token.encodeToByteArray()); bytes(3, "ASR".encodeToByteArray())
        bytes(5, method.encodeToByteArray()); bytes(6, payload.encodeToByteArray())
        bytes(7, audio); bytes(8, id.encodeToByteArray())
        if (state != 0) { variable(72); variable(state.toLong()) }
        return output.toByteArray()
    }

    data class Response(val type: String, val code: Long, val json: JsonObject?, val message: String = "")
    fun response(bytes: ByteArray): Response {
        require(bytes.size <= 1_048_576) { "ASR 响应过大。" }
        var at = 0
        fun variable(): Long {
            var value = 0L
            repeat(10) { index ->
                require(at < bytes.size) { "ASR 响应不完整。" }
                val b = bytes[at++].toInt() and 255
                value = value or ((b and 127).toLong() shl (index * 7))
                if (b < 128) return value
            }
            error("ASR 响应格式错误。")
        }
        var type = ""; var code = 0L; var message = ""; var json: JsonObject? = null
        while (at < bytes.size) {
            val tag = variable().toInt()
            when (tag and 7) {
                0 -> { val value = variable(); if (tag ushr 3 == 5) code = value }
                2 -> {
                    val count = variable()
                    require(count >= 0 && count <= bytes.size - at) { "ASR 响应长度错误。" }
                    val end = at + count.toInt()
                    if (tag ushr 3 == 4) type = bytes.copyOfRange(at, end).decodeToString()
                    if (tag ushr 3 == 6) message = bytes.copyOfRange(at, end).decodeToString()
                    if (tag ushr 3 == 7) json = Json.parseToJsonElement(bytes.copyOfRange(at, end).decodeToString()).jsonObject
                    at = end
                }
                1 -> at += 8
                5 -> at += 4
                else -> error("ASR 响应格式不受支持。")
            }
            require(at <= bytes.size) { "ASR 响应不完整。" }
        }
        return Response(type, code, json, message)
    }

    fun session(deviceId: String): String = buildJsonObject {
        putJsonObject("audio_info") { put("channel", 1); put("format", "speech_opus"); put("sample_rate", 16000) }
        put("enable_punctuation", true); put("enable_speech_rejection", false)
        putJsonObject("extra") {
            put("app_name", "com.android.andex"); put("cell_compress_rate", 8); put("did", deviceId)
            put("enable_asr_threepass", true); put("enable_asr_twopass", true); put("input_mode", "tool")
        }
    }.toString()
}

/** Interim text replaces the active segment; final segments are accumulated with overlap removal. */
internal class DoubaoTranscript {
    private var committed = ""
    private var interim = ""
    private var lastFinal = ""
    var hasFinal = false
        private set
    val preview: String get() = merge(committed, interim)
    val finalText: String get() = committed
    fun accept(json: JsonObject?): String? {
        val results = json?.get("results") as? JsonArray ?: return null
        val outer = json["extra"] as? JsonObject
        if (outer?.get("vad_start")?.jsonPrimitive?.booleanOrNull == true && interim.isBlank()) return null
        val entries = results.mapNotNull { it as? JsonObject }
        val text = entries.mapNotNull { it["text"]?.jsonPrimitive?.contentOrNull }.lastOrNull()?.trim().orEmpty()
        if (text.isEmpty()) return null
        val final = entries.any {
            (it["extra"] as? JsonObject)?.get("nonstream_result")?.jsonPrimitive?.booleanOrNull == true ||
                (it["is_interim"]?.jsonPrimitive?.booleanOrNull == false &&
                    it["is_vad_finished"]?.jsonPrimitive?.booleanOrNull == true)
        }
        if (final) {
            if (text != lastFinal) committed = merge(committed, text)
            lastFinal = text; interim = ""; hasFinal = true
        } else interim = text
        return preview
    }
    companion object {
        private fun merge(prefix: String, incoming: String): String {
            if (prefix.isEmpty()) return incoming
            if (incoming.isEmpty() || prefix.endsWith(incoming)) return prefix
            if (incoming.startsWith(prefix)) return incoming
            val overlap = (minOf(prefix.length, incoming.length) downTo 1)
                .firstOrNull { prefix.endsWith(incoming.take(it)) } ?: 0
            return prefix + incoming.drop(overlap)
        }
    }
}
