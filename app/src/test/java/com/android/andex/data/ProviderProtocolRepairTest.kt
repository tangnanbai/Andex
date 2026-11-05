package com.android.andex.data

import org.junit.Assert.*
import org.junit.Test

class ProviderProtocolRepairTest {
    @Test fun brandDoesNotFollowWireFormat() {
        val config = LlmProviderConfig(providerId = "ds", name = "DS",
            piProviderId = "openai-responses-custom", apiKey = "", baseUrl = "https://api.deepseek.com/v1",
            modelId = "deepseek-v4-flash")
        assertEquals("deepseek", config.brandDefinition().id)
        assertEquals("openai-responses-custom", config.piProviderId)
    }
    @Test fun acceptsFullEndpointWithoutDuplicatingPath() {
        assertEquals("https://api.deepseek.com/v1", providerRequestBaseUrl(
            "https://api.deepseek.com/v1/responses/", "openai-responses"))
        assertEquals("https://example.com/v1", providerRequestBaseUrl(
            "https://example.com/v1/chat/completions", "openai-completions"))
    }
    @Test fun bundledExtensionsAreEnabledByDefault() {
        assertTrue(DefaultDisabledPreinstalledExtensionPaths.isEmpty())
        assertTrue(DefaultDisabledPreinstalledExtensionIds.isEmpty())
    }
}
