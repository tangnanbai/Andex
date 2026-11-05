package com.android.andex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.android.andex.ui.theme.AndexBackground
import com.android.andex.ui.theme.AndexSurface
import com.android.andex.ui.theme.AndexOutline
import com.android.andex.ui.theme.AndexOnPrimary
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val LocalGlassState = compositionLocalOf<HazeState?> { null }
private val LocalGlassContrast = compositionLocalOf { false }

/** The app layer supplies popup backdrops; the lower chat layer supplies inline controls.
 * Keeping the source below its consumers prevents sampling a control into its own backdrop.
 */
@Composable
fun AndexGlassHost(increasedContrast: Boolean = false, content: @Composable () -> Unit) {
    val state = rememberHazeState()
    CompositionLocalProvider(
        LocalGlassState provides state,
        LocalGlassContrast provides increasedContrast,
    ) {
        Box(Modifier.fillMaxSize().hazeSource(state, zIndex = 0f, key = "app")) {
            content()
        }
    }
}

/** Record only the message viewport, never the full conversation history or a bitmap copy. */
fun Modifier.andexGlassBackdrop(): Modifier = composed {
    val state = LocalGlassState.current
    if (state == null) this else hazeSource(state, zIndex = -1f, key = "chat")
}

/** Shared frosted material. Content remains sharp; only the backdrop passes through Haze.
 * Tint is drawn on the surface itself so cross-window popups do not tint the entire host.
 */
fun Modifier.andexGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    popup: Boolean = false,
    tint: Color? = null,
    emphasized: Boolean = false,
): Modifier = composed {
    val state = LocalGlassState.current
    val contrast = LocalGlassContrast.current
    val dark = AndexBackground.luminance() <= 0.179f
    // Transparent is a request for the theme surface, not transparent black with a new alpha.
    val color = tint?.takeIf { it.alpha > 0f } ?: AndexSurface
    val blur = state != null && state.blurEnabled && !contrast
    val opacity = when {
        !blur -> 0.97f
        emphasized -> 0.94f
        popup -> if (dark) 0.76f else 0.72f
        else -> if (dark) 0.64f else 0.58f
    }
    val effect = if (blur) Modifier.hazeEffect(
        state = state,
        style = HazeStyle(
            backgroundColor = AndexBackground,
            tints = emptyList(),
            blurRadius = if (popup) 24.dp else 18.dp,
            noiseFactor = 0.025f,
            fallbackTint = HazeTint(Color.Transparent),
        ),
    ) {
        inputScale = HazeInputScale.Auto
    } else Modifier
    this.clip(shape)
        .then(effect)
        .background(color.copy(alpha = opacity))
        .border(
            0.6.dp,
            (if (emphasized) AndexOnPrimary.copy(alpha = 0.40f) else AndexOutline),
            shape,
        )
}
