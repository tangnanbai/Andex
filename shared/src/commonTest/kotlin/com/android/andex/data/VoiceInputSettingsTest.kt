package com.android.andex.data

import kotlinx.serialization.json.*
import kotlin.test.*

class VoiceInputSettingsTest {
    @Test fun legacyPaidCredentialsAreRemovedWithoutAffectingModelKeys() {
        val raw = """{"apiKey":"model-key","voiceInput":{"apiKey":"old-key","appId":"old-app","accessKey":"old-token","endpoint":"https://old.example","resourceId":"old"}}"""
        val migrated = clearLegacyVoiceConfiguration(raw)
        assertFalse(migrated.contains("old-"))
        assertFalse(migrated.contains("old.example"))
        val json = Json.parseToJsonElement(migrated).jsonObject
        assertEquals("model-key", json["apiKey"]!!.jsonPrimitive.content)
        assertEquals(VoiceInputSettings(), parseVoiceInputSettings(json["voiceInput"].toString()))
        assertEquals(migrated, clearLegacyVoiceConfiguration(migrated))
    }
    @Test fun defaultsAndExplicitDisableRoundTrip() {
        assertTrue(parseVoiceInputSettings("{}").enabled)
        assertFalse(parseVoiceInputSettings("""{"provider":"disabled"}""").enabled)
        assertFalse(parseVoiceInputSettings(serializeVoiceInputSettings(VoiceInputSettings(false))).enabled)
        val settings = AppSettings(themeAccent = "mint", voiceInput = VoiceInputSettings(false))
        assertEquals(settings, parseAppSettings(serializeAppSettings(settings)))
    }
}
