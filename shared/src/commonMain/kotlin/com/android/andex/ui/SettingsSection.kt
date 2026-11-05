package com.android.andex.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.android.andex.ui.theme.AndexOutlineSoft

/** Flat settings rows share the page canvas and a single theme-colored lower rule. */
fun Modifier.andexSettingsRule(): Modifier = composed {
    val color = AndexOutlineSoft
    drawBehind {
        val stroke = 0.5.dp.toPx()
        val y = size.height - stroke / 2
        drawLine(color, Offset(16.dp.toPx(), y), Offset((size.width - 16.dp.toPx()).coerceAtLeast(0f), y), stroke)
    }
}
