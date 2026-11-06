package com.android.andex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.andex.ui.theme.*

@Composable
internal actual fun VoiceToast(message: String?) {
    if (!message.isNullOrBlank()) androidx.compose.ui.window.Popup(
        popupPositionProvider = remember { object : androidx.compose.ui.window.PopupPositionProvider {
            override fun calculatePosition(anchorBounds: androidx.compose.ui.unit.IntRect,
                windowSize: androidx.compose.ui.unit.IntSize, layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                popupContentSize: androidx.compose.ui.unit.IntSize) = androidx.compose.ui.unit.IntOffset(
                    (windowSize.width - popupContentSize.width) / 2,
                    (windowSize.height * 2 / 3 - popupContentSize.height / 2).coerceAtLeast(0))
        } },
        properties = androidx.compose.ui.window.PopupProperties(focusable = false),
    ) {
        Text(message, Modifier.padding(horizontal = 24.dp).widthIn(max = 320.dp)
            .background(AndexSurfaceHigh, RoundedCornerShape(24.dp)).padding(horizontal = 20.dp, vertical = 12.dp),
            color = AndexOnSurface, style = MaterialTheme.typography.bodySmall)
    }
}
