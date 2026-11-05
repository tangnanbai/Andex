package com.android.andex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.android.andex.data.VolcAsrClient
import com.android.andex.data.VolcAsrSettings
import com.android.andex.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

interface VoiceRecorder {
    /** Requests permission when needed. Caller should ask the user to hold again after granting. */
    fun start(): Boolean
    suspend fun stop(): ByteArray
    fun cancel()
}

@Composable
expect fun rememberVoiceRecorder(): VoiceRecorder

val LocalVoiceInputSettings = staticCompositionLocalOf { VolcAsrSettings() }

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
    val recorder = rememberVoiceRecorder()
    val scope = rememberCoroutineScope()
    val client = remember { VolcAsrClient() }
    val recognize by rememberUpdatedState(onRecognized)
    var recording by remember { mutableStateOf(false) }
    var recognizing by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    DisposableEffect(recorder) {
        onDispose { recorder.cancel(); client.close() }
    }
    LaunchedEffect(notice) {
        if (notice != null && !recording && !recognizing) {
            kotlinx.coroutines.delay(6000)
            notice = null
        }
    }
    Box(modifier.then(if (!settings.isConfigured) Modifier else Modifier.pointerInput(settings) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            // Keyboard-open gestures belong entirely to the text field (paste/selection).
            // When closed, consume from DOWN so the field cannot focus during a voice hold.
            if (keyboardOpen || recognizing) return@awaitEachGesture
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
                if (!recorder.start()) {
                    notice = "请允许麦克风权限，然后再次按住说话"
                    return@awaitEachGesture
                }
                recording = true
                notice = "正在聆听 · 松开识别，上滑取消"
                val up = withTimeout(60_000) {
                    var release: androidx.compose.ui.input.pointer.PointerInputChange? = null
                    while (release == null) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == held.id } ?: break
                        change.consume()
                        if (change.position.y < held.position.y - 64.dp.toPx()) break
                        if (!change.pressed) release = change
                    }
                    release
                }
                if (up != null) {
                    completed = true
                    recognizing = true
                    notice = "正在识别…"
                    scope.launch {
                        try {
                            val audio = recorder.stop()
                            recognize(client.transcribe(settings, audio))
                            notice = null
                        } catch (cancel: CancellationException) { throw cancel }
                        catch (error: Exception) { notice = error.message ?: "语音识别失败，请重试" }
                        finally { recognizing = false; recorder.cancel() }
                    }
                } else notice = "已取消录音"
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { notice = error.message ?: "录音失败，请重试" }
            finally {
                recording = false
                if (!completed) recorder.cancel()
            }
        }
    })) {
        content(if (settings.isConfigured && !keyboardOpen) notice ?: "发消息或按住说话" else null, Modifier.focusRequester(focusRequester))
        if (notice != null) {
            val gap = with(LocalDensity.current) { 12.dp.roundToPx() }
            val positionProvider = remember(gap) {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize,
                    ): IntOffset {
                        val maxX = (windowSize.width - popupContentSize.width - gap).coerceAtLeast(gap)
                        val maxY = (windowSize.height - popupContentSize.height - gap).coerceAtLeast(gap)
                        return IntOffset(
                            (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(gap, maxX),
                            (anchorBounds.top - popupContentSize.height - gap).coerceIn(gap, maxY),
                        )
                    }
                }
            }
            Popup(
                popupPositionProvider = positionProvider,
                properties = PopupProperties(focusable = false),
            ) {
                Text(
                    text = notice!!,
                    modifier = Modifier.widthIn(max = 320.dp)
                        .background(AndexSurfaceHigh, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    color = AndexOnSurface,
                    style = MaterialTheme.typography.bodySmall,
                    softWrap = true,
                )
            }
        }
    }
}
