package com.android.andex.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.andex.ui.theme.*

internal data class VoiceOverlayState(val phase: VoicePhase, val preview: String?, val levels: List<Float>, val cancelArmed: Boolean)
internal class VoiceFeedback {
    var overlay by mutableStateOf<VoiceOverlayState?>(null)
    var toast by mutableStateOf<String?>(null)
}
internal val LocalVoiceFeedback = staticCompositionLocalOf<VoiceFeedback?> { null }

/** Full-window drawing avoids a navigation-inset gap. No pointer handler: the input retains its gesture. */
@Composable
internal fun VoiceFeedbackHost(content: @Composable () -> Unit) {
    val feedback = remember { VoiceFeedback() }
    CompositionLocalProvider(LocalVoiceFeedback provides feedback) {
        Box(Modifier.fillMaxSize()) {
            content()
            feedback.overlay?.let { VoiceRecordingOverlay(it, Modifier.align(Alignment.BottomCenter)) }
            VoiceToast(feedback.toast)
        }
    }
    LaunchedEffect(feedback.toast) {
        if (feedback.toast != null) { kotlinx.coroutines.delay(3500); feedback.toast = null }
    }
}

@Composable
internal expect fun VoiceToast(message: String?)

@Composable
private fun VoiceRecordingOverlay(state: VoiceOverlayState, modifier: Modifier) {
    val primary = AndexPrimary
    val background = AndexBackground
    val foreground = themeForeground(primary)
    Box(modifier.fillMaxWidth().height(190.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(0f to Color.Transparent, .55f to background.copy(alpha = .92f), 1f to background))
            val center = Offset(size.width / 2, size.height)
            val radius = size.width * .72f
            scale(1f, size.height / radius, pivot = center) {
                drawCircle(Brush.radialGradient(
                    0f to primary, .55f to primary.copy(alpha = .98f),
                    .78f to primary.copy(alpha = .72f), 1f to Color.Transparent,
                    center = center, radius = radius,
                ), radius, center)
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.phase == VoicePhase.Listening && !state.preview.isNullOrBlank() &&
                !state.preview.startsWith("正在聆听")) {
                Text(state.preview, color = foreground, style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(when {
                state.cancelArmed -> "松开取消"
                state.phase == VoicePhase.Listening -> "松开转为文字，上滑取消"
                else -> "正在识别完整语句…"
            }, color = foreground, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Canvas(Modifier.fillMaxWidth(.78f).height(30.dp)) {
                val spacing = size.width / state.levels.size
                state.levels.forEachIndexed { index, level ->
                    val x = spacing * (index + .5f)
                    val halfHeight = 2.dp.toPx() + level * (size.height / 2 - 2.dp.toPx())
                    val edge = 1f - kotlin.math.abs(index - (state.levels.size - 1) / 2f) / (state.levels.size / 2f)
                    drawLine(foreground.copy(alpha = .3f + .7f * edge),
                        Offset(x, size.height / 2 - halfHeight), Offset(x, size.height / 2 + halfHeight),
                        strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }
    }
}
