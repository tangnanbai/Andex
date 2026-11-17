package com.android.andex.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The model used for this response, independent of the conversation's current selection. */
data class ResponseModelIdentity(val providerId: String = "", val modelId: String = "") {
    val isKnown: Boolean get() = providerId.isNotBlank() || modelId.isNotBlank()
    val description: String get() = modelId.ifBlank { providerId }.ifBlank { "未知模型" }

    fun toPayloadJson(): String = buildJsonObject {
        put("provider", providerId)
        put("model", modelId)
    }.toString()
}

fun responseModelIdentity(
    payloadJson: String,
    providerId: String = "",
    modelId: String = "",
): ResponseModelIdentity {
    val payload = runCatching { Json.parseToJsonElement(payloadJson) as? JsonObject }.getOrNull()
    val assistant = payload?.get("piAssistantMessage") as? JsonObject
    fun JsonObject?.field(name: String): String =
        ((this?.get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content).orEmpty().trim()
    return ResponseModelIdentity(
        providerId = payload.field("provider").ifBlank { assistant.field("provider") }.ifBlank { providerId },
        modelId = payload.field("model").ifBlank { assistant.field("model") }.ifBlank { modelId },
    )
}

/** Recognizable model families take precedence over the gateway's request format. */
fun ResponseModelIdentity.brandId(): String {
    val model = modelId.trim().lowercase().substringAfterLast('/')
    return when {
        model.startsWith("deepseek") -> "deepseek"
        model.startsWith("claude") -> "anthropic"
        model.startsWith("gemini") || model.startsWith("imagen") ||
            model.startsWith("nano-banana") || model.startsWith("nanobanana") -> "google"
        model.startsWith("gpt") || model.matches(Regex("o[134]([-.].*)?")) ||
            Regex("^image[-_]?2(?:[.\\-_]|$)").containsMatchIn(model) ||
            model.startsWith("chatgpt") || model.startsWith("dall-e") -> "openai"
        model.startsWith("grok") -> "xai"
        model.startsWith("kimi") || model.startsWith("moonshot") -> "moonshotai"
        model.startsWith("mimo") -> "xiaomi"
        model.startsWith("glm") -> "zai"
        model.startsWith("minimax") || model.startsWith("abab") -> "minimax"
        model.startsWith("mistral") || model.startsWith("mixtral") ||
            model.startsWith("codestral") -> "mistral"
        model.startsWith("nemotron") -> "nvidia"
        else -> when (val provider = providerId.trim().lowercase()) {
            "anthropic-custom" -> "anthropic"
            "openai-responses-custom", "openai-compatible" -> ""
            else -> PiProviderCatalog.find(provider)?.takeIf { it.isBuiltIn }?.id.orEmpty()
        }
    }
}
