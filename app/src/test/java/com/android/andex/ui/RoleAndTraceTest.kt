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
        assertEquals("""{"command":"pwd"}""", steps[1].command)
        assertEquals("/workspace", steps[1].result)
    }

    @Test fun pendingTracePreservesTextAndStatus() {
        val steps = buildWorkTrace(emptyList(), emptyList(), listOf(
            AssistantResponseBlock.Status("s", "搜索中", "Bing"),
            AssistantResponseBlock.Text("t", "正在查询"),
        ), "正在查询")
        assertEquals(2, steps.size)
        assertEquals("Bing", steps.first().detail)
    }

    @Test fun toolProgressReplacesPreviousSnapshotWithoutMovingTheStep() {
        val running = ChatToolInvocation("tool", "bash", """{"command":"pwd"}""", isRunning = true)
        val completed = running.copy(outputJson = "/workspace", isRunning = false)
        val messages = listOf(
            ChatMessage("u", MessageAuthor.User, "查看目录"),
            ChatMessage("a", MessageAuthor.Agent, "", toolInvocations = listOf(running)),
        )
        val steps = buildWorkTrace(messages, listOf(completed), emptyList(), "")
        assertEquals(listOf("message:u", "tool:tool"), steps.map { it.id })
        assertFalse(steps.last().running)
        assertEquals("/workspace", steps.last().result)
    }

    @Test fun blankUserMessageStillSeparatesRequests() {
        val steps = buildWorkTrace(listOf(
            ChatMessage("u1", MessageAuthor.User, "第一个任务"),
            ChatMessage("a1", MessageAuthor.Agent, "完成"),
            ChatMessage("u2", MessageAuthor.User, ""),
        ), emptyList(), emptyList(), "")
        assertEquals(listOf("输入", "回复", "输入"), steps.map { it.kind })
        assertEquals("message:u2", steps.last().id)
    }
}
