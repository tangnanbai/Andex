package com.android.andex.data

import kotlin.test.*

class ResponseModelIdentityTest {
    @Test fun responseMetadataOverridesTheCurrentlyConfiguredModel() {
        val identity = responseModelIdentity("""{"provider":"custom-proxy","model":"deepseek-v4"}""",
            "openai", "gpt-5")
        assertEquals("deepseek-v4", identity.modelId)
        assertEquals("deepseek", identity.brandId())
    }

    @Test fun historicalNestedMetadataAndMalformedPayloadHaveExplicitFallbacks() {
        assertEquals("anthropic", responseModelIdentity(
            """{"piAssistantMessage":{"provider":"openrouter","model":"anthropic/claude-sonnet-4"}}""").brandId())
        assertFalse(responseModelIdentity("invalid").isKnown)
        assertEquals("", responseModelIdentity("").brandId())
        assertEquals(ResponseModelIdentity("google", "gemini-image"),
            responseModelIdentity("[]", "google", "gemini-image"))
        assertEquals("gpt-5", responseModelIdentity("""{"model":null}""", modelId = "gpt-5").modelId)
    }

    @Test fun imageAndChatModelsUseTheirOwnBrandRatherThanTheGatewayBrand() {
        mapOf("gpt-image-2.5" to "openai", "gemini-nano-banana-2" to "google",
            "nano-banana" to "google", "mimo-v2" to "xiaomi", "glm-5" to "zai").forEach { (model, brand) ->
            val identity = ResponseModelIdentity("openrouter", model)
            assertEquals(brand, identity.brandId())
            assertEquals(identity, responseModelIdentity(identity.toPayloadJson()))
        }
        assertEquals("", ResponseModelIdentity("openai-compatible", "unknown-model").brandId())
    }
}
