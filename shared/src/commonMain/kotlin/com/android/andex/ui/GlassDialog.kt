package com.android.andex.ui

import androidx.compose.material3.AlertDialog as MaterialAlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.android.andex.ui.theme.AndexOnSurface
import com.android.andex.ui.theme.AndexOnSurfaceVariant
import com.android.andex.ui.theme.AndexSurface

/** Keep Material dialog behavior, sizing and accessibility while sharing the popup material. */
@Composable
fun AndexGlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AndexSurface,
    iconContentColor: Color = AndexOnSurfaceVariant,
    titleContentColor: Color = AndexOnSurface,
    textContentColor: Color = AndexOnSurfaceVariant,
    tonalElevation: Dp = 0.dp,
    properties: DialogProperties = DialogProperties(),
) {
    MaterialAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.andexGlass(shape, popup = true, tint = containerColor),
        dismissButton = dismissButton, icon = icon, title = title, text = text, shape = shape,
        containerColor = Color.Transparent,
        iconContentColor = iconContentColor, titleContentColor = titleContentColor,
        textContentColor = textContentColor, tonalElevation = tonalElevation, properties = properties,
    )
}
