package com.android.andex.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Stable
class SharedSlidingDrawerState {
    var progress by mutableFloatStateOf(0f)
        private set
    var currentValue by mutableStateOf(DrawerValue.Closed)
        private set
    var targetValue by mutableStateOf(DrawerValue.Closed)
        private set
    val isOpen get() = progress > 0f
    private var animation: Job? = null

    fun dragBy(delta: Float) {
        animation?.cancel()
        progress = (progress + delta).coerceIn(0f, 1f)
    }
    suspend fun open() = settle(DrawerValue.Open)
    suspend fun close() = settle(DrawerValue.Closed)
    suspend fun settle(value: DrawerValue) {
        val runningJob = kotlinx.coroutines.currentCoroutineContext()[Job]
        animation?.cancel()
        animation = runningJob
        targetValue = value
        try {
            animate(progress, if (value == DrawerValue.Open) 1f else 0f,
                animationSpec = spring(dampingRatio = 1f, stiffness = 400f)) { fraction, _ -> progress = fraction.coerceIn(0f, 1f) }
            currentValue = value
        } finally { if (animation === runningJob) animation = null }
    }
}

@Composable
fun rememberSharedSlidingDrawerState() = remember { SharedSlidingDrawerState() }

val LocalConversationImeEnabled = staticCompositionLocalOf { true }
val LocalDrawerVisible = staticCompositionLocalOf { true }

/** Both sides share the exact same drag, release animation, sheet placement and scrim. */
@Composable
fun SharedSlidingDrawer(
    drawerState: SharedSlidingDrawerState,
    right: Boolean = false,
    gesturesEnabled: Boolean = true,
    scrimColor: Color,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val enabled by rememberUpdatedState(gesturesEnabled)
    val density = LocalDensity.current
    var sheetWidth by remember(density) { mutableFloatStateOf(with(density) { 322.dp.toPx() }) }
    val sign = if (right) -1f else 1f
    val drawerActive = drawerState.isOpen || drawerState.targetValue == DrawerValue.Open
    val allowContentIme = LocalConversationImeEnabled.current && !drawerActive
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(drawerActive) {
        focus.clearFocus(force = true)
        keyboard?.hide()
    }
    Box(Modifier.fillMaxSize().pointerInput(drawerState, right) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val available = enabled
            val wasOpen = drawerState.progress > 0f
            var lastPosition = down.position
            var claimed = false
            var rejected = !available
            var lastDelta = 0f
            var travel = 0f
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                val displacement = pointer.position - down.position
                if (!claimed && !rejected && (abs(displacement.x) > viewConfiguration.touchSlop || abs(displacement.y) > viewConfiguration.touchSlop)) {
                    if (abs(displacement.y) >= abs(displacement.x) || (!wasOpen && displacement.x * sign <= 0f) || pointer.isConsumed) rejected = true
                    else claimed = true
                }
                if (claimed) {
                    val delta = pointer.position.x - lastPosition.x
                    if (abs(delta) > 0f) lastDelta = delta * sign
                    drawerState.dragBy(delta * sign / sheetWidth)
                    // Include the movement before touch slop was crossed. Otherwise
                    // the advertised short release distance becomes longer in practice.
                    travel = displacement.x * sign
                    pointer.consume()
                }
                lastPosition = pointer.position
            } while (event.changes.any { it.pressed })
            if (claimed) {
                val open = when {
                    abs(travel) >= 28.dp.toPx() -> travel > 0
                    abs(lastDelta) > 2.dp.toPx() -> lastDelta > 0
                    else -> drawerState.progress >= .5f
                }
                scope.launch { drawerState.settle(if (open) DrawerValue.Open else DrawerValue.Closed) }
            }
        }
    }) {
        CompositionLocalProvider(LocalConversationImeEnabled provides allowContentIme) {
            Box(Modifier.fillMaxSize().then(
                if (allowContentIme) Modifier else Modifier.consumeWindowInsets(WindowInsets.ime),
            )) { content() }
        }
        if (drawerState.progress > 0f) {
            Box(Modifier.fillMaxSize().background(scrimColor.copy(alpha = scrimColor.alpha * drawerState.progress))
                .semantics { contentDescription = "关闭导航菜单" }
                .clickable { scope.launch { drawerState.close() } })
        }
        Box(Modifier.align(if (right) AbsoluteAlignment.TopRight else AbsoluteAlignment.TopLeft)
            .offset { IntOffset(((1f - drawerState.progress) * sheetWidth * -sign).roundToInt(), 0) }
            .onSizeChanged { if (it.width > 0) sheetWidth = it.width.toFloat() }) {
            CompositionLocalProvider(LocalDrawerVisible provides drawerActive) {
                drawerContent()
            }
        }
    }
}
