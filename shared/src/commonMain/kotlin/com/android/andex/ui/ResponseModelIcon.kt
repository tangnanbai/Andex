package com.android.andex.ui

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import com.android.andex.data.ResponseModelIdentity
import com.android.andex.data.brandId
import com.android.andex.ui.theme.AndexOnSurfaceVariant
import org.jetbrains.compose.resources.painterResource

@Composable
fun ResponseModelIcon(
    model: ResponseModelIdentity,
    modifier: Modifier = Modifier,
    contentDescription: String = model.description,
) {
    val brand = model.brandId()
    if (brand.isBlank()) {
        Icon(Icons.Rounded.SmartToy, contentDescription, modifier, tint = AndexOnSurfaceVariant)
    } else {
        Image(
            painter = painterResource(providerBrandIconRes(brand)),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            // Monochrome marks remain readable against every configured background.
            colorFilter = if (brand in setOf("openai", "openai-codex", "anthropic", "openrouter", "xai"))
                ColorFilter.tint(AndexOnSurfaceVariant) else null,
        )
    }
}
