package com.android.andex.ui

import com.android.andex.data.*
import org.junit.Assert.*
import org.junit.Test

class RoleAndTraceTest {
    @Test fun rolePersistenceAndPromptSelection() {
        val roles = listOf(AgentRole("one", "翻译", "只翻译", "🌐"), AgentRole("two", "开发", "写代码", "💻"))
        val settings = AppSettings(systemPrompt = "基础", roles = roles, defaultRoleId = "one", selectedRoleId = "two")
        val loaded = parseAppSettings(serializeAppSettings(settings))
        assertEquals(roles, loaded.roles)
        assertEquals("two", loaded.activeRole()?.id)
        assertTrue(loaded.effectiveSystemPrompt().contains("基础"))
        assertTrue(loaded.effectiveSystemPrompt().contains("写代码"))
        assertFalse(loaded.effectiveSystemPrompt().contains("只翻译"))
        assertEquals("one", loaded.copy(selectedRoleId = "deleted").activeRole()?.id)
        assertEquals("基础", loaded.copy(roles = emptyList()).effectiveSystemPrompt())
    }

    @Test fun traceKeepsArgumentsOutputAndDeduplicatesTools() {
        val tool = ChatToolInvocation("id", "bash", """{"command":"pwd"}""", "/workspace")
        val trace = ReasoningTrace("trace", toolInvocations = listOf(tool))
        val messages = listOf(
            ChatMessage("u", MessageAuthor.User, "查看目录"),
            ChatMessage("a", MessageAuthor.Agent, "完成", toolInvocations = listOf(tool), reasoningTrace = trace),
        )
        val steps = buildWorkTrace(messages, listOf(tool), emptyList(), "")
        assertEquals(listOf("输入", "工具", "回复"), steps.map { it.kind })
        assertTrue(steps[1].detail.contains("pwd"))
        assertTrue(steps[1].detail.contains("/workspace"))
    }

    @Test fun pendingTracePreservesTextAndStatus() {
        val steps = buildWorkTrace(emptyList(), emptyList(), listOf(
            AssistantResponseBlock.Status("s", "搜索中", "Bing"),
            AssistantResponseBlock.Text("t", "正在查询"),
        ), "正在查询")
        assertEquals(2, steps.size)
        assertEquals("Bing", steps.first().detail)
    }
}
