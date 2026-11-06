package com.android.andex.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class IosWorkTraceTest {
    @Test
    fun latestToolSnapshotReplacesRunningStateAndKeepsItsPosition() {
        val running = SharedChatToolInvocation(
            id = "tool", name = "bash", summary = "查看目录",
            argumentsJson = """{"command":"pwd"}""", isRunning = true,
        )
        val completed = running.copy(isRunning = false, outputJson = "/workspace")
        val steps = buildIosWorkTrace(listOf(
            SharedChatMessage(id = "u", text = "查看目录", fromUser = true),
            SharedChatMessage(
                id = "a", text = "完成", fromUser = false, tools = listOf(completed),
                responseBlocks = listOf(SharedAssistantResponseBlock.ToolGroup("g", listOf(running))),
            ),
        ))
        assertEquals(listOf("输入", "工具", "回复"), steps.map { it.kind })
        assertFalse(steps[1].running)
        assertEquals("""{"command":"pwd"}""", steps[1].command)
        assertEquals("/workspace", steps[1].result)
    }

    @Test
    fun reasoningAndToolsFollowTimelineOrderAndBlankInputStartsANewTask() {
        val tool = SharedChatToolInvocation(
            id = "tool", name = "web_search", summary = "搜索",
            timelineOrder = 2, isRunning = false,
        )
        val trace = SharedReasoningTrace(
            id = "r",
            chunks = listOf(
                SharedReasoningSummaryChunk(id = "before", title = "准备", timelineOrder = 1),
                SharedReasoningSummaryChunk(id = "after", title = "整理", timelineOrder = 3),
            ),
            toolInvocations = listOf(tool),
        )
        val steps = buildIosWorkTrace(listOf(
            SharedChatMessage(id = "u1", text = "搜索新闻", fromUser = true),
            SharedChatMessage(
                id = "a", text = "", fromUser = false,
                responseBlocks = listOf(SharedAssistantResponseBlock.Reasoning("r", trace)),
            ),
            SharedChatMessage(id = "u2", text = "", fromUser = true),
        ))
        assertEquals(listOf("u1", "reason:before", "tool:tool", "reason:after", "u2"), steps.map { it.id })
        assertEquals(listOf("搜索新闻", "空消息"), groupWorkTraceSteps(steps).map { it.title })
    }
}
