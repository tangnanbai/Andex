package com.android.andex.overlay

import com.android.andex.AppForegroundTracker
import com.android.andex.data.ChatStateStore
import com.android.andex.data.SessionExecutionManager
import com.android.andex.data.SessionTurnOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class TaskStatusPublisher(
    private val manager: SessionExecutionManager,
    private val chats: ChatStateStore,
    private val foreground: AppForegroundTracker,
    scope: CoroutineScope,
    private val waitingSessions: () -> Set<String> = { emptySet() },
) {
    private data class Result(val state: String, val time: Long, val duration: Long)
    private val results = MutableStateFlow<Map<String, Result>>(emptyMap())
    private val _hasRecentResults = MutableStateFlow(false)
    val hasRecentResults = _hasRecentResults.asStateFlow()
    init {
        scope.launch {
            manager.turnEvents.collect { event ->
                val state = when (event.outcome) {
                    SessionTurnOutcome.Success -> "completed"
                    SessionTurnOutcome.Failure, SessionTurnOutcome.ValidationError -> "failed"
                    SessionTurnOutcome.Neutral -> "cancelled"
                }
                results.update { it + (event.sessionId to Result(
                    state, System.currentTimeMillis(), event.durationMillis ?: 0,
                )) }
                _hasRecentResults.value = true
            }
        }
    }

    fun snapshot(): JSONObject {
        val now = System.currentTimeMillis()
        val sessions = chats.state.value.sessions.associateBy { it.id }
        val tasks = JSONArray()
        val active = manager.executionStates.value.values.filter { it.isRunning }
        val activeIds = active.map { it.sessionId }.toSet()
        results.update { values -> values.filter { (_, result) ->
            now - result.time < OverlayPolicy.RESULT_VISIBLE_MILLIS
        } }
        _hasRecentResults.value = results.value.isNotEmpty()
        val waiting = waitingSessions()
        active.take(20).forEach { state ->
            val tools = state.pendingToolInvocations
            val start = state.activeTurnStartedAtMillis ?: now
            val tool = tools.lastOrNull { it.isRunning }
            tasks.put(JSONObject().apply {
                put("id", state.sessionId)
                put("run_id", "${state.sessionId}:$start")
                put("title", sessions[state.sessionId]?.title?.take(120) ?: "Andex 任务")
                val phase = if (state.sessionId in waiting) "waiting" else if (tool != null) "tool" else "running"
                put("state", phase)
                put("label", OverlayPolicy.label(phase, tool?.toolName.orEmpty()))
                put("status", if (state.sessionId in waiting) "等待你的确认或输入" else state.pendingStatusText.take(180).ifBlank {
                    tool?.toolName?.let { "正在执行 $it" } ?: "正在运行"
                })
                put("started_at", start)
                put("elapsed_ms", (now - start).coerceAtLeast(0))
                put("completed_tools", tools.count { !it.isRunning && it.completedAtMillis != null })
                put("running_tools", tools.count { it.isRunning })
                // Agent turns have no known total. Tool count is not a completion percentage.
                put("progress", JSONObject.NULL)
            })
        }
        results.value.entries.filter { it.key !in activeIds }.take(10).forEach { (id, result) ->
            tasks.put(JSONObject().apply {
                put("id", id)
                put("run_id", "$id:result:${result.time}")
                put("title", sessions[id]?.title?.take(120) ?: "Andex 任务")
                put("state", result.state)
                put("label", OverlayPolicy.label(result.state))
                put("status", when (result.state) {
                    "completed" -> "任务已完成，点此查看结果"
                    "failed" -> "任务执行出错，点此查看会话"
                    else -> "任务已停止"
                })
                put("elapsed_ms", result.duration)
                put("progress", if (result.state == "completed") 100 else JSONObject.NULL)
            })
        }
        return JSONObject().apply {
            put("version", 1)
            put("timestamp", now)
            put("foreground", foreground.isForeground.value)
            put("tasks", tasks)
        }
    }
}
