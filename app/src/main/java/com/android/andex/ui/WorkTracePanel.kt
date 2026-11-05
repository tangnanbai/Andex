package com.android.andex.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.android.andex.ui.theme.*

internal fun buildWorkTrace(messages: List<ChatMessage>, pendingTools: List<ChatToolInvocation>,
                            pendingBlocks: List<AssistantResponseBlock>, pendingText: String): List<WorkTraceStep> {
    val steps = mutableListOf<WorkTraceStep>()
    val toolsSeen = mutableSetOf<String>()
    fun tool(tool: ChatToolInvocation) {
        if (!toolsSeen.add(tool.id)) return
        val status = if (tool.isRunning) "运行中" else if (tool.outputJson.isBlank()) "已结束" else "有结果"
        steps += WorkTraceStep("tool:${tool.id}", "${tool.toolName} · $status",
            "参数\n${tool.argumentsJson}\n\n结果\n${tool.outputJson}", "工具", tool.startedAtMillis, tool.isRunning)
    }
    fun trace(trace: ReasoningTrace) {
        val timeline = trace.chunks.map { it.timelineOrder to it } +
            trace.toolInvocations.map { it.timelineOrder to it }
        timeline.sortedBy { it.first }.forEach { (_, event) ->
            when (event) {
                is ChatToolInvocation -> tool(event)
                is ReasoningSummaryChunk -> steps.add(WorkTraceStep("reason:${event.id}",
                    event.title.ifBlank { event.detail.ifBlank { event.rawText } },
                    listOf(event.detail, event.rawText).filter { it.isNotBlank() }.distinct().joinToString("\n\n"),
                    "记录", event.createdAtMillis, event.isPending))
            }
        }
        if (trace.chunks.isEmpty() && trace.rawText.isNotBlank()) {
            steps += WorkTraceStep("reason:${trace.id}", trace.rawText, trace.rawText, "记录", trace.startedAtMillis)
        }
    }
    messages.forEach { message ->
        if (message.author == MessageAuthor.User && message.text.isNotBlank()) steps += WorkTraceStep("message:${message.id}", message.text,
            message.text, "输入", message.createdAtMillis)
        message.reasoningTrace?.let(::trace)
        message.toolInvocations.sortedBy { it.timelineOrder }.forEach(::tool)
        if (message.author != MessageAuthor.User && message.text.isNotBlank()) steps += WorkTraceStep("message:${message.id}", message.text,
            message.text, "回复", message.createdAtMillis)
        if (message.statusText.isNotBlank()) steps += WorkTraceStep("status:${message.id}",
            message.statusText, message.statusDetail, "状态", message.createdAtMillis)
    }
    pendingBlocks.forEach { block ->
        when (block) {
            is AssistantResponseBlock.ToolGroup -> block.toolInvocations.forEach(::tool)
            is AssistantResponseBlock.Reasoning -> trace(block.trace)
            is AssistantResponseBlock.Text -> steps.add(WorkTraceStep("pending:${block.id}", block.text, block.text, "回复"))
            is AssistantResponseBlock.Status -> steps.add(WorkTraceStep("pending:${block.id}", block.text, block.detail, "状态"))
        }
    }
    pendingTools.forEach(::tool)
    if (pendingText.isNotBlank() && pendingBlocks.none { it is AssistantResponseBlock.Text && it.text == pendingText })
        steps += WorkTraceStep("pending:text", pendingText, pendingText, "回复", running = true)
    return steps.distinctBy { it.id }
}


@Composable
internal fun WorkTraceOverlay(messages: List<ChatMessage>, pendingTools: List<ChatToolInvocation>,
    pendingBlocks: List<AssistantResponseBlock>, pendingText: String,
    leftDrawerActive: Boolean, onRightGestureActive: (Boolean) -> Unit,
    content: @Composable () -> Unit) {
    SharedWorkTraceOverlay(backHandler = { open, close -> BackHandler(enabled = open, onBack = close) },
        leftDrawerActive = leftDrawerActive, onRightGestureActive = onRightGestureActive,
        stepsProvider = {
            val steps by produceState<List<WorkTraceStep>>(emptyList(), messages, pendingTools, pendingBlocks, pendingText) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    buildWorkTrace(messages, pendingTools, pendingBlocks, pendingText)
                }
            }
            steps
        }, content = content)
}
