package com.android.andex.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Last request size, not the sum of all requests in an agent turn. Pi counts cache separately. */
fun lastRequestContextTokens(payloadJson: String): Long? {
    val payload = runCatching { Json.parseToJsonElement(payloadJson) as? JsonObject }.getOrNull() ?: return null
    val assistant = payload["piAssistantMessage"] as? JsonObject
    val usage = assistant?.get("usage") as? JsonObject
    fun JsonObject.number(vararg names: String): Long? =
        names.firstNotNullOfOrNull { (this[it] as? JsonPrimitive)?.longOrNull?.takeIf { n -> n >= 0 } }
    if (usage != null) {
        val input = usage.number("input", "input_tokens")
        if (input != null) {
            return input + (usage.number("output", "output_tokens") ?: 0) +
                (usage.number("cacheRead", "cached_input_tokens") ?: 0) +
                (usage.number("cacheWrite", "cache_write_tokens") ?: 0)
        }
    }
    val singleRequestUsage = payload["usage"] as? JsonObject ?: return null
    if ((singleRequestUsage.number("request_count") ?: 1) != 1L) return null
    val input = singleRequestUsage.number("input_tokens") ?: return null
    return input + (singleRequestUsage.number("output_tokens") ?: 0) +
        (singleRequestUsage.number("cached_input_tokens") ?: 0) +
        (singleRequestUsage.number("cache_write_tokens") ?: 0)
}
