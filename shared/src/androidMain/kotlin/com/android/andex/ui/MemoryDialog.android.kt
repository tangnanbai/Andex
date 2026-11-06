package com.android.andex.ui

import androidx.compose.ui.window.DialogProperties

internal actual fun memoryDialogProperties() = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
)
