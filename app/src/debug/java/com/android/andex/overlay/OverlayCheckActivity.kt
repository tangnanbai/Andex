package com.android.andex.overlay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.android.andex.AndexApplication
import com.android.andex.data.SessionExecutionState
import com.android.andex.data.SessionTurnEvent
import com.android.andex.data.SessionTurnOutcome
import com.android.andex.ui.ChatToolInvocation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Debug-only UI fixture. Never invokes tools or mutates persisted conversations. */
class OverlayCheckActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val runtime = (application as AndexApplication).runtime
        val manager = runtime.sessionExecutionManager
        @Suppress("UNCHECKED_CAST")
        val states = manager.javaClass.getDeclaredField("_executionStates").run {
            isAccessible = true
            get(manager) as MutableStateFlow<Map<String, SessionExecutionState>>
        }
        val id = intent.getStringExtra("session") ?: "overlay-debug-fixture"
        val remove = intent.getBooleanExtra("clear", false)
        val run = intent.getLongExtra("run", System.currentTimeMillis())
        val outcome = intent.getStringExtra("outcome").orEmpty()
        if (remove) {
            states.update { it - id }
        } else if (states.value[id]?.isRunning != true || states.value[id]?.pendingStatusText == "药丸界面测试") {
            states.update { it + (id to SessionExecutionState(
                sessionId = id, isRunning = true,
                pendingStatusText = "药丸界面测试", activeTurnStartedAtMillis = run,
                pendingToolInvocations = intent.getStringExtra("tool")?.let { tool ->
                    listOf(ChatToolInvocation("overlay-check-tool", tool, "{}", isRunning = true))
                }.orEmpty(),
            )) }
            CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                delay(180_000)
                states.update { current ->
                    if (current[id]?.activeTurnStartedAtMillis == run &&
                        current[id]?.pendingStatusText == "药丸界面测试") current - id else current
                }
            }
        }
        if (outcome.isNotEmpty()) {
            states.update { it - id }
            @Suppress("UNCHECKED_CAST")
            val events = manager.javaClass.getDeclaredField("_turnEvents").run {
                isAccessible = true
                get(manager) as kotlinx.coroutines.flow.MutableSharedFlow<SessionTurnEvent>
            }
            CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                events.emit(SessionTurnEvent(id,
                    if (outcome == "completed") SessionTurnOutcome.Success else SessionTurnOutcome.Failure,
                    durationMillis = 65000,
                ))
            }
        }
        if (intent.getBooleanExtra("settings", false)) {
            startActivity(Intent(this, PillSettingsActivity::class.java))
        }
        finish()
    }
}
