package com.android.andex.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/** Online voice uses the independently implemented Doubao IME protocol client. */
@Serializable
data class VoiceInputSettings(val enabled: Boolean = true) {
    val isConfigured: Boolean get() = enabled
}

fun serializeVoiceInputSettings(settings: VoiceInputSettings): String =
    buildJsonObject { put("enabled", settings.enabled) }.toString()

/** Deliberately discard the previous paid-service endpoint and credentials. */
fun parseVoiceInputSettings(value: String): VoiceInputSettings = runCatching {
    val json = Json.parseToJsonElement(value).jsonObject
    VoiceInputSettings(json["enabled"]?.jsonPrimitive?.booleanOrNull
        ?: (json["provider"]?.jsonPrimitive?.contentOrNull != "disabled"))
}.getOrDefault(VoiceInputSettings())

internal fun clearLegacyVoiceConfiguration(value: String): String = runCatching {
    val root = Json.parseToJsonElement(value).jsonObject
    val voice = root["voiceInput"] ?: return value
    JsonObject(root + ("voiceInput" to Json.parseToJsonElement(
        serializeVoiceInputSettings(parseVoiceInputSettings(voice.toString()))))).toString()
}.getOrDefault(value)
