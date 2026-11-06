package com.android.andex.ui

import com.android.andex.ui.AndexToast as Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun VoiceToast(message: String?) {
    val context = LocalContext.current.applicationContext
    DisposableEffect(message) {
        val toast = message?.takeIf { it.isNotBlank() }?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).also { it.show() } }
        onDispose { toast?.cancel() }
    }
}
