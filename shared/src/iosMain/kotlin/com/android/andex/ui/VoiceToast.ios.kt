package com.android.andex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.andex.ui.theme.*

@Composable
internal actual fun VoiceToast(message: String?) {
    if (!message.isNullOrBlank()) Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.BottomCenter) {
        Text(message, Modifier.padding(24.dp).widthIn(max = 320.dp)
            .background(AndexSurfaceHigh, RoundedCornerShape(24.dp)).padding(horizontal = 20.dp, vertical = 12.dp),
            color = AndexOnSurface, style = MaterialTheme.typography.bodySmall)
    }
}
