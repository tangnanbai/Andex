package com.android.andex.data

import kotlin.test.*

class ModelMentionsTest {
    private val configs = listOf("渠道一", "渠道二").mapIndexed { index, name ->
        LlmProviderConfig(id = "config-$index", providerId = "custom-$index", name = name,
            piProviderId = "openai-compatible", apiKey = "test", baseUrl = "https://example.test/v1",
            modelId = "gpt-image-2")
    }
    @Test fun routesDuplicateModelIdsByConfiguredChannel() {
        val options = configs.availableModelOptions()
        assertEquals(options[1], resolveModelMention(options[1].mentionToken() + " 画一个圆", options))
        assertEquals("画一个圆", stripModelMentions(options[1].mentionToken() + " 画一个圆"))
    }
    @Test fun rejectsUnavailableAndMultipleModelsInsteadOfSilentFallback() {
        val options = configs.availableModelOptions()
        assertFailsWith<IllegalArgumentException> { resolveModelMention("@「已删除 / gpt-image-2」 画图", options) }
        assertFailsWith<IllegalArgumentException> { resolveModelMention(options.joinToString(" ") { it.mentionToken() }, options) }
        assertNull(resolveModelMention("请回复 test@example.com", options))
    }
    @Test fun mentionPickerUsesCursorAndIgnoresEmailsAndCompletedTokens() {
        assertEquals(ModelMentionQuery(3, 7, "gpt"), modelMentionQuery("画图 @gpt 后面文字", 7))
        assertNull(modelMentionQuery("a@b.com", 7))
        assertNull(modelMentionQuery("@「v2 / gpt-image-2」", 18))
        assertNotNull(modelMentionQuery("请用@", 3))
    }
    @Test fun detectsImageModelsWithoutTreatingVisionChatModelsAsGenerators() {
        assertTrue(isImageGenerationModel("gpt-image-2.5-flare"))
        assertTrue(isImageGenerationModel("gemini-nano-banana-2.1"))
        assertFalse(isImageGenerationModel("gemini-2.5-pro"))
    }
}
