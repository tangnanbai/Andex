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
    val seen = mutableSetOf<String>()
    fun tool(item: SharedChatToolInvocation) {
        if (seen.add(item.id)) add(WorkTraceStep("tool:${item.id}", item.name,
            "参数\n${item.argumentsJson}\n\n结果\n${item.outputJson.ifBlank { item.output }}",
            "工具", item.startedAtMillis, item.isRunning))
    }
    messages.forEach { message ->
        if (message.fromUser && message.text.isNotBlank()) add(WorkTraceStep(message.id, message.text, message.text, "输入", message.createdAtMillis))
        message.responseBlocks.forEach { block ->
            when (block) {
                is SharedAssistantResponseBlock.ToolGroup -> block.tools.forEach(::tool)
                is SharedAssistantResponseBlock.Reasoning -> {
                    block.trace.chunks.forEach { chunk -> add(WorkTraceStep(chunk.id,
                        chunk.title.ifBlank { chunk.detail }, chunk.detail.ifBlank { chunk.rawText }, "记录", chunk.createdAtMillis)) }
                    block.trace.toolInvocations.forEach(::tool)
                }
                is SharedAssistantResponseBlock.Status -> add(WorkTraceStep(block.id, block.text, block.detail, "状态"))
                is SharedAssistantResponseBlock.Text -> Unit
            }
        }
        message.tools.forEach(::tool)
        if (!message.fromUser && message.text.isNotBlank()) add(WorkTraceStep(message.id, message.text, message.text, "回复", message.createdAtMillis, message.isStreaming))
        if (message.status.isNotBlank()) add(WorkTraceStep("status:${message.id}", message.status, message.statusDetail, "状态"))
    }
}.distinctBy { it.id }
