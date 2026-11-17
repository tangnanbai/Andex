package com.android.andex.ui

import com.android.andex.data.ResponseModelIdentity
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkTraceModelIdentityTest {
    @Test fun historicalSplitBlocksAndPendingMentionKeepTheirActualModels() {
        val deepseek = ResponseModelIdentity("proxy", "deepseek-chat")
        val image = ResponseModelIdentity("proxy", "gpt-image-2")
        val steps = buildWorkTrace(
            messages = listOf(
                ChatMessage("u1", MessageAuthor.User, "搜索"),
                ChatMessage("tools", MessageAuthor.Agent, "", responseGroupId = "g1",
                    toolInvocations = listOf(ChatToolInvocation("tool1", "bash", "{}"))),
                ChatMessage("a1", MessageAuthor.Agent, "结果", responseGroupId = "g1",
                    providerPayloadJson = deepseek.toPayloadJson()),
                ChatMessage("u2", MessageAuthor.User, "画图"),
            ),
            pendingTools = listOf(ChatToolInvocation("tool2", "generate_image", "{}", isRunning = true)),
            pendingBlocks = emptyList(), pendingText = "正在画图", pendingModel = image,
        )
        assertEquals(listOf(deepseek, deepseek, deepseek, image, image, image), steps.map { it.model })
    }

    @Test fun imageToolUsesTheImageModelRatherThanTheTextModelThatCalledIt() {
        val text = ResponseModelIdentity("proxy", "deepseek-chat")
        val image = ResponseModelIdentity("proxy", "gpt-image-2.5")
        val steps = buildWorkTrace(listOf(
            ChatMessage("u1", MessageAuthor.User, "生成图标"),
            ChatMessage("a1", MessageAuthor.Agent, "已生成", providerPayloadJson = text.toPayloadJson(),
                toolInvocations = listOf(ChatToolInvocation("draw", "generate_image", "{}",
                    outputJson = image.toPayloadJson()))),
        ), emptyList(), emptyList(), "")
        assertEquals(image, steps.single { it.id == "tool:draw" }.model)
        assertEquals(text, steps.single { it.id == "message:a1" }.model)
    }
}
