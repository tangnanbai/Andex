package com.android.andex.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.unit.dp
import kotlin.io.encoding.Base64
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import com.android.andex.data.AppSettings
import com.android.andex.data.activeRole

@Composable
internal fun SharedRolePicker(settings: AppSettings, onSelected: (String) -> Unit) {
    RoleCapsule(settings.roles, settings.selectedRoleId, settings.defaultRoleId,
        avatar = { value, modifier ->
            val bitmap = remember(value) { if (value.startsWith("data:image/")) runCatching {
                Base64.decode(value.substringAfter(",")).decodeToImageBitmap()
            }.getOrNull() else null }
            if (bitmap != null) Image(bitmap, "角色头像", modifier.clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            else Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) { Text("🤖") }
        }, onSelected = onSelected)

}

internal fun buildIosWorkTrace(messages: List<SharedChatMessage>): List<WorkTraceStep> = buildList {
    val positions = mutableMapOf<String, Int>()
    fun tool(item: SharedChatToolInvocation) {
        val step = WorkTraceStep("tool:${item.id}", workTraceToolLabel(item.name, item.isRunning),
            "参数\n${item.argumentsJson}\n\n结果\n${item.outputJson.ifBlank { item.output }}",
            "工具", item.startedAtMillis, item.isRunning,
            toolName = item.name, command = item.argumentsJson, result = item.outputJson.ifBlank { item.output })
        val index = positions[item.id]
        if (index == null) { positions[item.id] = size; add(step) } else set(index, step)
    }
    messages.forEach { message ->
        if (message.fromUser) {
            val input = message.text.ifBlank { if (message.attachments.isNotEmpty()) "附件消息" else "空消息" }
            add(WorkTraceStep(message.id, input, input, "输入", message.createdAtMillis))
        }
        message.responseBlocks.forEach { block ->
            when (block) {
                is SharedAssistantResponseBlock.ToolGroup -> block.tools.forEach(::tool)
                is SharedAssistantResponseBlock.Reasoning -> {
                    val timeline = block.trace.chunks.map { it.timelineOrder to it } +
                        block.trace.toolInvocations.map { it.timelineOrder to it }
                    timeline.sortedBy { it.first }.forEach { (_, event) ->
                        when (event) {
                            is SharedChatToolInvocation -> tool(event)
                            is SharedReasoningSummaryChunk -> add(WorkTraceStep("reason:${event.id}",
                                event.title.ifBlank { event.detail.ifBlank { event.rawText } },
                                listOf(event.detail, event.rawText).filter { it.isNotBlank() }.distinct().joinToString("\n\n"),
                                "记录", event.createdAtMillis, event.isPending))
                        }
                    }
                    if (block.trace.chunks.isEmpty() && block.trace.rawText.isNotBlank()) {
                        add(WorkTraceStep("reason:${block.trace.id}", block.trace.rawText, block.trace.rawText,
                            "记录", block.trace.startedAtMillis))
                    }
                }
                is SharedAssistantResponseBlock.Status -> add(WorkTraceStep(block.id, block.text, block.detail, "状态", running = message.isStreaming))
                is SharedAssistantResponseBlock.Text -> Unit
            }
        }
        message.tools.forEach(::tool)
        if (!message.fromUser && message.text.isNotBlank()) add(WorkTraceStep(message.id, message.text, message.text, "回复", message.createdAtMillis, message.isStreaming))
        if (message.status.isNotBlank()) add(WorkTraceStep("status:${message.id}", message.status, message.statusDetail, "状态"))
    }
}.distinctBy { it.id }
