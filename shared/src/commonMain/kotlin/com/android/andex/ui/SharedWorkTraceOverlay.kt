package com.android.andex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.android.andex.ui.theme.*
import kotlinx.coroutines.launch

data class WorkTraceStep(val id: String, val label: String, val detail: String,
                                  val kind: String, val time: Long = 0, val running: Boolean = false)

/** The same full-screen directional drawer as the conversation sidebar. */
@Composable
fun SharedWorkTraceOverlay(backHandler: @Composable (Boolean, () -> Unit) -> Unit = { _, _ -> },
    leftDrawerActive: Boolean = false,
    onRightGestureActive: (Boolean) -> Unit = {},
    stepsProvider: @Composable () -> List<WorkTraceStep>, content: @Composable () -> Unit) {
    val state = rememberSharedSlidingDrawerState()
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { state.close() } }
    val notifyGesture by rememberUpdatedState(onRightGestureActive)
    val rightActive = state.isOpen || state.targetValue == DrawerValue.Open
    SideEffect { notifyGesture(rightActive) }
    DisposableEffect(Unit) { onDispose { notifyGesture(false) } }
    backHandler(rightActive, close)
    SharedSlidingDrawer(drawerState = state, right = true,
        gesturesEnabled = !leftDrawerActive, scrimColor = AndexScrim,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxHeight().width(322.dp),
                drawerContainerColor = AndexSidebarBackground,
                drawerShape = RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp)) {
                WorkTracePanel(if (rightActive) stepsProvider() else emptyList())
            }
        }, content = content)
}

@Composable
private fun WorkTracePanel(steps: List<WorkTraceStep>) {
                var query by remember { mutableStateOf("") }
                var searchExpanded by remember { mutableStateOf(false) }
                val clipboard = LocalClipboardManager.current
                val filtered by produceState(steps, steps, query) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        steps.filter { query.isBlank() || it.label.contains(query, true) || it.detail.contains(query, true) }
                    }
                }
                Column(Modifier.navigationBarsPadding().padding(bottom = 18.dp)) {
                    Column(Modifier.fillMaxWidth().background(sharedDrawerOverlayBodyGradient(AndexSidebarBackground))
                        .padding(horizontal = 16.dp).padding(top = 18.dp).statusBarsPadding()) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("工作轨迹", Modifier.padding(start = 6.dp), color = AndexOnSurface,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal))
                            Row {
                                HeaderCircleButton(icon = LucideIcons.Search, contentDescription = "搜索轨迹", onClick = {
                                    searchExpanded = !searchExpanded; if (!searchExpanded) query = ""
                                }, size = 46.dp, containerColor = androidx.compose.ui.graphics.Color.Transparent, showHalo = false)
                            }
                        }
                        androidx.compose.animation.AnimatedVisibility(searchExpanded || query.isNotBlank()) {
                            Column { Spacer(Modifier.height(16.dp)); SharedDrawerSearchField(query) { query = it } }
                        }
                        Spacer(Modifier.height(if (searchExpanded || query.isNotBlank()) 10.dp else 12.dp))
                    }
                    Column(Modifier.padding(horizontal = 16.dp).weight(1f)) {
                    Text("${steps.size} 步 · 点击展开详情", Modifier.padding(vertical = 10.dp), style = MaterialTheme.typography.labelMedium)
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        listOf("输入", "回复", "工具").forEach { kind ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(kind, Modifier.width(34.dp), style = MaterialTheme.typography.labelSmall)
                                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    steps.takeLast(80).forEach { step ->
                                        Box(Modifier.weight(1f).height(8.dp).background(
                                            if (step.kind == kind) when (kind) {
                                                "工具" -> MaterialTheme.colorScheme.tertiary
                                                "输入" -> MaterialTheme.colorScheme.secondary
                                                else -> MaterialTheme.colorScheme.primary
                                            } else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)))
                                    }
                                }
                            }
                        }
                    }
                    if (filtered.isEmpty()) Text("暂无记录", Modifier.padding(16.dp))
                    LazyColumn(Modifier.weight(1f)) {
                        items(filtered, key = { it.id }) { step ->
                            var expanded by remember(step.id) { mutableStateOf(false) }
                            Column(Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(when(step.kind) { "工具" -> "🔧"; "输入" -> "👤"; "状态" -> "◉"; else -> "✦" })
                                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                        Text(step.label.ifBlank { step.kind }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        val firstTime = steps.firstOrNull { it.time > 0 }?.time ?: 0L
                                        val time = if (step.time > 0 && firstTime > 0) "+${((step.time - firstTime).coerceAtLeast(0L)) / 1000}s" else ""
                                        Text("${step.kind} $time${if (step.running) " · 运行中" else ""}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(if (expanded) "⌃" else "⌄")
                                }
                                if (expanded) {
                                    TextButton(onClick = { clipboard.setText(AnnotatedString(step.detail)) }) { Text("复制详情") }
                                    SelectionContainer {
                                        Text(step.detail.ifBlank { step.label },
                                            Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()).padding(10.dp),
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                    }
                }
}
