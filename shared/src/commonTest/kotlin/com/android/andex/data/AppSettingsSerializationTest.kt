package com.android.andex.data



import kotlinx.serialization.json.JsonObject

import kotlin.test.Test

import kotlin.test.assertEquals



class AppSettingsSerializationTest {

    @Test


    fun providerServiceTierSurvivesStorageAndModelSelection() {

        val provider = LlmProviderConfig(

            id = "tier-provider", providerId = "custom", name = "Custom",

            piProviderId = "openai-compatible", apiKey = "test",

            baseUrl = "https://example.com/v1", modelId = "test-model",

            serviceTier = "ultrafast",

        )

        val restored = parseProviderConfigs("[${provider.toJsonObject()}]").single()

        assertEquals("ultrafast", restored.serviceTier)

        val option = listOf(restored).availableModelOptions().single()

        assertEquals("ultrafast", AppSettings().withModelOption(option).serviceTier)

        val legacy = JsonObject(provider.toJsonObject().filterKeys { it != "serviceTier" })

        assertEquals("", parseProviderConfigs("[$legacy]").single().serviceTier)

    }




    @Test
    fun completeSettingsRoundTrip() {

        val settings = AppSettings(

            piProviderId = "anthropic",

            providerAuthMethod = ProviderAuthMethod.OAuth,

            oauthCredentialJson = "{\"access\":\"token\"}",

            providerEnvironmentVariables = listOf(PiProviderEnvironmentVariable("REGION", "test")),

            customHeaders = listOf(LlmCustomHeader("X-Test", "value")),

            serviceTier = "ultrafast",

            reasoningEffort = "high",

            systemPrompt = "shared prompt",

            keepTasksRunningInBackground = false,

            notifyOnTaskCompletion = false,

            agentWorkspaceMode = AgentWorkspaceMode.PerSession,

            enabledRuntimeIds = setOf(LocalRuntimeId.Alpine),

            defaultRuntimeId = LocalRuntimeId.Alpine,

            alpinePackageProfiles = mapOf("chrome" to PackageProfileState("chrome", installed = true)),

            alpineEnvironmentVariables = listOf(AlpineEnvironmentVariable("A", "B")),

            language = AppLanguage.SimplifiedChinese,

            themeMode = AppThemeMode.Dark,
            agentModeDisplayOrientation = AgentModeDisplayOrientation.Landscape,

            defaultChatModelKey = "provider/chat-model",

            defaultTitleModelKey = "provider/title-model",

            defaultNamingModelKey = "provider/naming-model",

            defaultCompactingModelKey = "provider/compacting-model",

            defaultSelectedSkillIds = listOf("review", "research"),

            onboardingSeenVersion = CurrentOnboardingVersion,

            onboardingCompletedVersion = CurrentOnboardingVersion,

            privacyPolicyAccepted = true,

            lastUpdateCheckAtMillis = 1234L,

        )



        assertEquals(settings, parseAppSettings(serializeAppSettings(settings)))

    }



    @Test

    fun unknownFieldsRemainForwardCompatible() {

        val parsed = parseAppSettings("""{"themeMode":"Dark","futureValue":42}""")



        assertEquals(AppThemeMode.Dark, parsed.themeMode)

        assertEquals(DefaultReasoningEffort, parsed.reasoningEffort)

    }



    @Test

    fun acceptedPrivacyPolicyCannotBeOverwrittenByStaleSettings() {

        assertEquals(true, privacyPolicyAccepted(persisted = true, requested = false))

        assertEquals(true, privacyPolicyAccepted(persisted = false, requested = true))

        assertEquals(false, privacyPolicyAccepted(persisted = false, requested = false))

    }



    @Test

    fun thinkingCatalogCacheRoundTripPreservesResolvedEmptyModels() {

        val cache = SharedThinkingCatalogCache(

            source = ModelsDevThinkingCatalogSource,

            levelsByProviderModel = mapOf(

                "openai/gpt-5" to listOf("off", "medium", "high"),

                "anthropic/claude-haiku" to emptyList(),

            ),

            clampsByProviderModel = mapOf(

                "openai/gpt-5" to mapOf("max" to "high"),

            ),

            reasoningModels = setOf("openai/gpt-5"),

        )



        assertEquals(cache, parseSharedThinkingCatalogCache(serializeSharedThinkingCatalogCache(cache)))

    }



    @Test

    fun legacyThinkingCatalogCacheHasNoModelsDevSource() {

        val cache = parseSharedThinkingCatalogCache(

            """{"levelsByProviderModel":{"openai/gpt-5":["high"]}}""",

        )



        assertEquals("", cache.source)

    }

}
