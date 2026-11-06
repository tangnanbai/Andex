package com.android.andex.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.andex.platform.LocalReduceMotion
import com.android.andex.ui.theme.AndexOnSurface

/** Search replaces its header in place and expands from the trailing search icon. */
@Composable
internal fun DrawerSearchHeader(
    title: String,
    expanded: Boolean,
    query: String,
    onExpandedChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val duration = if (LocalReduceMotion.current) 0 else 220
    AnimatedContent(
        targetState = expanded,
        modifier = Modifier.fillMaxWidth().height(38.dp),
        transitionSpec = {
            (fadeIn(tween(duration)) + slideInHorizontally(tween(duration)) { if (targetState) it / 3 else -it / 5 })
                .togetherWith(fadeOut(tween(duration)) + slideOutHorizontally(tween(duration)) { if (targetState) -it / 5 else it / 3 })
                .using(SizeTransform(clip = true))
        },
        label = "drawer-inline-search",
    ) { searching ->
        if (searching) {
            SharedDrawerSearchField(query, onQueryChange, onClose = { onExpandedChange(false) })
        } else Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title, Modifier.weight(1f),
                color = AndexOnSurface,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderCircleButton(
                    icon = LucideIcons.Search, contentDescription = "搜索",
                    onClick = { onExpandedChange(true) }, size = 38.dp, iconSize = 19.dp,
                    containerColor = Color.Transparent, showHalo = false,
                )
                actions()
            }
        }
    }
}
