package com.android.andex.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.android.andex.data.AgentRole
import com.android.andex.ui.theme.*

val LocalExpandedRoleName = staticCompositionLocalOf { false }

/** Shared by model, balance and role controls so height, type and shadow stay identical. */
@Composable
fun ConversationCapsule(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
                        horizontalPadding: androidx.compose.ui.unit.Dp = 17.dp,
                        content: @Composable RowScope.() -> Unit) {
    Box(modifier.height(38.dp), propagateMinConstraints = true) {
        Box(Modifier.matchParentSize().offset(y = 4.dp)
            .blur(14.dp, BlurredEdgeTreatment.Unbounded).clip(RoundedCornerShape(999.dp))
            .background(Color(0x14000000)))
        Row(Modifier.height(38.dp).andexGlass(RoundedCornerShape(999.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = horizontalPadding), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
fun RoleCapsule(roles: List<AgentRole>, selectedId: String, defaultId: String, modifier: Modifier = Modifier,
                avatar: @Composable (String, Modifier) -> Unit, onSelected: (String) -> Unit) {
    val role = roles.firstOrNull { it.id == selectedId } ?: roles.firstOrNull { it.id == defaultId }
    val choices = if (roles.isEmpty()) listOf(AgentRole("", "默认助手")) else roles
    var expanded by remember { mutableStateOf(false) }
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = expanded
    var height by remember { mutableIntStateOf(0) }
    var availableWidth by remember { mutableStateOf(242.dp) }
    val density = LocalDensity.current
    val desiredWidth = conversationModelMenuWidth(choices.map { it.name }).coerceAtLeast(200.dp)
    val showName = LocalExpandedRoleName.current
    Box(modifier) {
        ConversationCapsule((if (showName) Modifier.widthIn(max = 210.dp) else Modifier.size(38.dp)).semantics {
            contentDescription = "切换角色：${role?.name ?: "默认助手"}"
        }.onGloballyPositioned {
            height = it.size.height
            val bounds = it.boundsInWindow()
            availableWidth = with(density) { (bounds.right - 20.dp.toPx()).toDp() }.coerceAtLeast(1.dp)
        }, onClick = { expanded = true }, horizontalPadding = if (showName) 8.dp else 4.dp) {
            Box(Modifier.size(30.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                avatar(role?.avatar ?: "🤖", Modifier.fillMaxSize())
            }
            if (showName) {
                Spacer(Modifier.width(8.dp))
                Text(role?.name ?: "默认助手", Modifier.widthIn(max = 154.dp).padding(end = 4.dp),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
                    color = AndexOnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (visibility.currentState || visibility.targetState) Popup(
            alignment = Alignment.TopEnd,
            offset = IntOffset(0, height + with(density) { 10.dp.roundToPx() }),
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
        ) {
            AnimatedVisibility(visibleState = visibility,
                enter = fadeIn() + scaleIn(initialScale = 0.96f, transformOrigin = TransformOrigin(0.5f, 0f)) +
                    slideInVertically(initialOffsetY = { -it / 10 }),
                exit = fadeOut() + scaleOut(targetScale = 0.98f, transformOrigin = TransformOrigin(0.5f, 0f)) +
                    slideOutVertically(targetOffsetY = { -it / 12 })) {
                Column(Modifier.width(desiredWidth.coerceAtMost(availableWidth))
                    .shadow(14.dp, RoundedCornerShape(22.dp), ambientColor = AndexScrim, spotColor = AndexScrim)
                    .andexGlass(RoundedCornerShape(22.dp), popup = true).padding(vertical = 5.dp)
                    .heightIn(max = 312.dp).verticalScroll(rememberScrollState())) {
                    choices.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 1.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(if (item.id == role?.id) AndexOnSurface.copy(alpha = 0.06f) else Color.Transparent)
                            .clickable { if (roles.isNotEmpty()) onSelected(item.id); expanded = false }.padding(horizontal = 9.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Box(Modifier.size(26.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                                avatar(item.avatar, Modifier.fillMaxSize())
                            }
                            Text(item.name, Modifier.weight(1f), color = AndexOnSurface,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                                maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            if (item.id == (role?.id ?: "")) Icon(Icons.Rounded.Check, null, tint = AndexOnSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (roles.isEmpty()) Text("在设置 › 角色定义中添加角色",
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = AndexOnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
