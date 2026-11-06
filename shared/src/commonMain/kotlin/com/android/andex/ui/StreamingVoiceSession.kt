package com.android.andex.ui

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal enum class VoicePhase { Idle, Listening, Finishing }

/** All state transitions run on the UI scope. The microphone only sends copied PCM. */
internal class StreamingVoiceSession(
    private val scope: CoroutineScope,
    private val recorder: VoiceRecorder,
    private val transcribe: suspend (Flow<ByteArray>, (String) -> Unit) -> String,
    private val onState: (VoicePhase, String?) -> Unit,
    private val onResult: (String) -> Unit,
) {
    private var generation = 0L
    private var request: Job? = null
    private var previousRequest: Job? = null
    private var stopping: Job? = null
    private var chunks: Channel<ByteArray>? = null
    var phase = VoicePhase.Idle
        private set

    private fun state(value: VoicePhase, notice: String?) {
        phase = value
        onState(value, notice)
    }

    fun start(): Boolean {
        if (phase != VoicePhase.Idle) return false
        val current = ++generation
        // 60 seconds of 100 ms microphone chunks. A stalled service cannot grow memory.
        val audio = Channel<ByteArray>(640)
        chunks = audio
        try {
            if (!recorder.start { bytes ->
                if (audio.trySend(bytes).isFailure) audio.close(IllegalStateException("语音网络发送过慢，请重试。"))
            }) {
                audio.cancel()
                state(VoicePhase.Idle, "请允许麦克风权限，然后再次按住说话")
                return false
            }
        } catch (error: Exception) {
            audio.cancel(); recorder.cancel()
            state(VoicePhase.Idle, error.message ?: "无法开始录音，请重试")
            return false
        }
        state(VoicePhase.Listening, "正在聆听 · 松开识别，上滑取消")
        val cleanup = previousRequest
        previousRequest = null
        request = scope.launch {
            try {
                cleanup?.join()
                val text = transcribe(audio.receiveAsFlow()) { preview ->
                    if (generation == current && phase == VoicePhase.Listening)
                        onState(phase, preview.ifBlank { "正在聆听…" })
                }
                if (generation == current) {
                    check(phase == VoicePhase.Finishing) { "语音服务提前结束，请重试。" }
                    check(text.isNotBlank()) { "没有识别到语音，请重试。" }
                    onResult(text)
                    state(VoicePhase.Idle, null)
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                if (generation == current) state(VoicePhase.Idle, error.message ?: "语音识别失败，请重试")
            } finally {
                // A rapid cancel/start/cancel must not drop the preceding socket cleanup.
                withContext(NonCancellable) { cleanup?.join() }
                if (generation == current) {
                    stopping?.cancel()
                    recorder.cancel()
                    if (phase != VoicePhase.Idle) state(VoicePhase.Idle, "语音已中断，请重试")
                }
                audio.cancel()
            }
        }
        return true
    }

    fun finish() {
        if (phase != VoicePhase.Listening) return
        val current = generation
        val audio = chunks ?: return
        state(VoicePhase.Finishing, "正在识别完整语句…")
        stopping = scope.launch {
            try {
                recorder.stop()
                audio.close() // EOF sends LAST and FinishSession; it does not cancel ASR.
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                if (generation == current) cancel(error.message ?: "录音中断，请重试")
            }
        }
    }

    fun cancel(notice: String? = "已取消录音") {
        generation++ // Invalidate any callback already queued by a previous request.
        stopping?.cancel(); stopping = null
        request?.let { it.cancel(); previousRequest = it }
        request = null
        chunks?.cancel(); chunks = null
        recorder.cancel()
        state(VoicePhase.Idle, notice)
    }
}
