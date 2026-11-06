package com.android.andex.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.android.andex.data.VoiceInputSettings
import com.android.andex.data.rememberDoubaoImeAsr
import com.android.andex.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

interface VoiceRecorder {
    /** Requests permission when needed. Caller should ask the user to hold again after granting. */
    fun start(onPcm: ((ByteArray) -> Unit)? = null): Boolean
    suspend fun stop(): ByteArray
    fun cancel()
}

@Composable
expect fun rememberVoiceRecorder(): VoiceRecorder

@Composable
expect fun ObserveVoiceInputInterruptions(onInterrupted: () -> Unit)

val LocalVoiceInputSettings = staticCompositionLocalOf { VoiceInputSettings() }

/** Observes the initial pointer pass so a configured hold can take over text selection. */
@Composable
fun VoiceInputField(
    modifier: Modifier = Modifier,
    onRecognized: (String) -> Unit,
    content: @Composable BoxScope.(String?, Modifier) -> Unit,
) {
    val settings = LocalVoiceInputSettings.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val keyboardOpen by rememberUpdatedState(WindowInsets.ime.getBottom(LocalDensity.current) > 0)
    val microphone = rememberVoiceRecorder()
    val scope = rememberCoroutineScope()
    var levels by remember { mutableStateOf(List(48) { 0f }) }
    val feedback = LocalVoiceFeedback.current
    val recorder = remember(microphone, scope) {
        object : VoiceRecorder by microphone {
            override fun start(onPcm: ((ByteArray) -> Unit)?): Boolean {
                levels = List(48) { 0f }
                return microphone.start { bytes ->
                    var peak = 0
                    for (i in 0 until bytes.size - 1 step 2) {
                        val sample = ((bytes[i].toInt() and 255) or (bytes[i + 1].toInt() shl 8)).toShort().toInt()
                        peak = maxOf(peak, kotlin.math.abs(sample))
                    }
                    val amplitude = (peak / 10000f).coerceIn(0f, 1f)
                    scope.launch { levels = levels.drop(1) + amplitude }
                    onPcm?.invoke(bytes)
                }
            }
        }
    }
    val doubao = rememberDoubaoImeAsr()
    val recognize by rememberUpdatedState(onRecognized)
    var phase by remember { mutableStateOf(VoicePhase.Idle) }
    var notice by remember { mutableStateOf<String?>(null) }
    var cancelArmed by remember { mutableStateOf(false) }
    val voice = remember(recorder, doubao, scope) {
        StreamingVoiceSession(scope, recorder, { pcm, preview -> doubao.transcribe(pcm, preview) },
            { state, text -> phase = state; notice = text }, { recognize(it) })
    }
    ObserveVoiceInputInterruptions {
        if (phase != VoicePhase.Idle) voice.cancel("录音已中断")
    }
    DisposableEffect(voice) {
        onDispose { voice.cancel(null); doubao.close(); feedback?.overlay = null }
    }
    SideEffect {
        feedback?.overlay = if (phase == VoicePhase.Idle) null else VoiceOverlayState(phase, notice, levels, cancelArmed)
    }
    LaunchedEffect(phase, notice) {
        if (phase == VoicePhase.Idle && !notice.isNullOrBlank()) {
            feedback?.toast = notice
            notice = null
        }
    }
    Box(modifier
        .then(if (!settings.isConfigured) Modifier else Modifier.pointerInput(settings) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            // Keyboard-open gestures belong entirely to the text field (paste/selection).
            // When closed, consume from DOWN so the field cannot focus during a voice hold.
            if (keyboardOpen || phase != VoicePhase.Idle) return@awaitEachGesture
            down.consume()
            var held = down
            var shortTap = false
            var abandoned = false
            val releasedEarly = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    change?.consume()
                    if (change == null || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                        abandoned = true
                        break
                    }
                    held = change
                    if (!change.pressed) { shortTap = true; break }
                }
                true
            }
            if (releasedEarly == true || abandoned) {
                if (shortTap) { focusRequester.requestFocus(); keyboard?.show() }
                return@awaitEachGesture
            }
            var completed = false
            try {
                if (!voice.start()) { completed = true; return@awaitEachGesture }
                cancelArmed = false
                var cancelled = false
                withTimeoutOrNull(60_000) {
                    var release: androidx.compose.ui.input.pointer.PointerInputChange? = null
                    while (release == null) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (voice.phase != VoicePhase.Listening) break
                        val change = event.changes.firstOrNull { it.id == held.id }
                        if (change == null) { cancelled = true; break }
                        change.consume()
                        cancelArmed = change.position.y < held.position.y - 64.dp.toPx()
                        if (!change.pressed) { cancelled = cancelArmed; release = change }
                    }
                }
                if (voice.phase != VoicePhase.Listening) {
                    // A network failure already supplied its error. Do not overwrite it on UP.
                    completed = true
                } else if (!cancelled) { voice.finish(); completed = true }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { voice.cancel(error.message ?: "录音失败，请重试"); completed = true }
            finally {
                cancelArmed = false
                if (!completed) voice.cancel()
            }
        }
    })) {
        content(if (settings.isConfigured) "发消息或按住说话" else null, Modifier.focusRequester(focusRequester))
    }
}
