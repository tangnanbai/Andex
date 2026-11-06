package com.android.andex.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.andex.platform.LocalReduceMotion
import com.android.andex.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Uses the same drawer and directional gestures as the conversation sidebar. */
@Composable
fun SharedWorkTraceOverlay(
    backHandler: @Composable (Boolean, () -> Unit) -> Unit = { _, _ -> },
    leftDrawerActive: Boolean = false,
    onRightGestureActive: (Boolean) -> Unit = {},
    stepsProvider: @Composable () -> List<WorkTraceStep>,
    content: @Composable () -> Unit,
) {
    val state = rememberSharedSlidingDrawerState()
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { state.close() } }
    val notifyGesture by rememberUpdatedState(onRightGestureActive)
    val rightActive = state.isOpen || state.targetValue == DrawerValue.Open
    SideEffect { notifyGesture(rightActive) }
    DisposableEffect(Unit) { onDispose { notifyGesture(false) } }
    backHandler(rightActive, close)
    SharedSlidingDrawer(
        drawerState = state,
        right = true,
        gesturesEnabled = !leftDrawerActive,
        scrimColor = AndexScrim,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight().width(322.dp),
                drawerContainerColor = AndexSidebarBackground,
                drawerShape = RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp),
            ) {
                WorkTracePanel(if (rightActive) stepsProvider() else emptyList())
            }
        },
        content = content,
    )
}

@Composable
private fun WorkTracePanel(steps: List<WorkTraceStep>) {
    var query by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    // Kept outside lazy rows: filtering, drawer closing and live updates do not reset disclosure.
    var expandedTasks by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var collapsedTasks by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var expandedSteps by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val tasks by produceState(emptyList<WorkTraceTask>(), steps) {
        value = withContext(Dispatchers.Default) { groupWorkTraceSteps(steps) }
    }
    val filtered by produceState(tasks, tasks, query) {
        value = withContext(Dispatchers.Default) {
            if (query.isBlank()) tasks else tasks.mapNotNull { task ->
                if (task.title.contains(query, true)) task else {
                    val matches = task.steps.filter {
                        it.label.contains(query, true) || it.detail.contains(query, true)
                    }
                    task.copy(steps = matches).takeIf { matches.isNotEmpty() }
                }
            }
        }
    }
    val latestTaskId = tasks.lastOrNull()?.id
    val taskStartTimes = remember(tasks) {
        tasks.associate { task -> task.id to (task.steps.firstOrNull { it.time > 0 }?.time ?: 0L) }
    }
    Column(Modifier.navigationBarsPadding().padding(bottom = 18.dp)) {
        Column(
            Modifier.fillMaxWidth()
                .background(sharedDrawerOverlayBodyGradient(AndexSidebarBackground))
                .padding(horizontal = 16.dp).padding(top = 18.dp).statusBarsPadding(),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "工作轨迹", Modifier.padding(start = 6.dp),
                    color = AndexOnSurface,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
                )
                HeaderCircleButton(
                    icon = LucideIcons.Search, contentDescription = "搜索轨迹",
                    onClick = { searchExpanded = !searchExpanded; if (!searchExpanded) query = "" },
                    size = 46.dp, containerColor = Color.Transparent, showHalo = false,
                )
            }
            AnimatedVisibility(searchExpanded || query.isNotBlank()) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    SharedDrawerSearchField(query) { query = it }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Column(Modifier.padding(horizontal = 20.dp).weight(1f)) {
            Text(
                "${tasks.size} 个任务 · ${steps.size} 步",
                Modifier.padding(vertical = 10.dp),
                style = MaterialTheme.typography.labelMedium, color = AndexOnSurfaceVariant,
            )
            WorkTraceOverview(steps)
            if (filtered.isEmpty()) {
                Text(
                    if (query.isBlank()) "暂无记录" else "没有匹配的记录",
                    Modifier.padding(vertical = 20.dp), color = AndexOnSurfaceVariant,
                )
            }
            LazyColumn(Modifier.weight(1f)) {
                filtered.forEach { task ->
                    val expanded = workTraceTaskExpanded(
                        task.id, latestTaskId, expandedTasks, collapsedTasks, searching = query.isNotBlank(),
                    )
                    item(key = "task:${task.id}", contentType = "task") {
                        WorkTraceTaskHeader(task, expanded) {
                            if (expanded) {
                                expandedTasks = expandedTasks - task.id
                                collapsedTasks = (collapsedTasks + task.id).distinct()
                            } else {
                                collapsedTasks = collapsedTasks - task.id
                                expandedTasks = (expandedTasks + task.id).distinct()
                            }
                        }
                    }
                    if (expanded) {
                        itemsIndexed(task.steps, key = { _, step -> "step:${step.id}" }, contentType = { _, _ -> "step" }) { index, step ->
                            WorkTraceTimelineRow(
                                step = step,
                                firstTime = taskStartTimes[task.id] ?: 0,
                                isLast = index == task.steps.lastIndex,
                                expanded = step.id in expandedSteps,
                                onToggle = {
                                    expandedSteps = if (step.id in expandedSteps) expandedSteps - step.id
                                    else expandedSteps + step.id
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkTraceOverview(steps: List<WorkTraceStep>) {
    val recent = remember(steps) { steps.takeLast(80) }
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf("输入", "回复", "工具").forEach { kind ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(kind, Modifier.width(34.dp), style = MaterialTheme.typography.labelSmall, color = AndexOnSurfaceVariant)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    recent.forEach { step ->
                        Box(
                            Modifier.weight(1f).height(5.dp).background(
                                if (step.kind == kind) AndexOnSurface.copy(alpha = 0.8f)
                                else AndexOutlineSoft.copy(alpha = 0.25f),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkTraceTaskHeader(task: WorkTraceTask, expanded: Boolean, onToggle: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth()
                .semantics { stateDescription = if (expanded) "已展开" else "已折叠" }
                .clickable(onClickLabel = if (expanded) "折叠任务" else "展开任务", onClick = onToggle)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                null, Modifier.size(20.dp), tint = AndexOnSurfaceVariant,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    task.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    color = AndexOnSurface,
                )
                Text(
                    "${task.steps.size} 步${if (task.running) " · 运行中" else ""}",
                    style = MaterialTheme.typography.labelSmall, color = AndexOnSurfaceVariant,
                )
            }
        }
        if (!expanded) HorizontalDivider(color = AndexOutlineSoft.copy(alpha = 0.45f))
    }
}

@Composable
private fun WorkTraceTimelineRow(
    step: WorkTraceStep, firstTime: Long, isLast: Boolean, expanded: Boolean, onToggle: () -> Unit,
) {
    val duration = if (LocalReduceMotion.current) 0 else 240
    val lineColor = AndexOnSurfaceVariant.copy(alpha = 0.16f)
    Row(
        Modifier.fillMaxWidth().drawBehind {
            if (!isLast && size.height > 36.dp.toPx()) {
                drawLine(
                    color = lineColor,
                    start = Offset(11.dp.toPx(), 30.dp.toPx()),
                    end = Offset(11.dp.toPx(), size.height - 6.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.width(22.dp), contentAlignment = Alignment.TopCenter) {
            val icon = when {
                step.kind == "输入" -> Icons.Rounded.PersonOutline
                step.toolName.equals("bash", true) -> Icons.Rounded.Terminal
                step.toolName.lowercase() in setOf("fetch_web_url", "read_webpage", "tavily_search", "web_search") -> Icons.Rounded.Language
                step.kind == "工具" -> Icons.Rounded.Build
                else -> null
            }
            if (icon != null) Icon(icon, null, Modifier.padding(top = 9.dp).size(20.dp), tint = AndexOnSurfaceVariant)
            else Box(Modifier.padding(top = 15.dp).size(8.dp).clip(CircleShape).background(AndexOnSurfaceVariant))
        }
        Column(Modifier.weight(1f).padding(bottom = if (isLast) 16.dp else 22.dp)) {
            Column(
                Modifier.fillMaxWidth()
                    .semantics { stateDescription = if (expanded) "已展开" else "已折叠" }
                    .clickable(onClickLabel = if (expanded) "收起详情" else "展开详情", onClick = onToggle)
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    step.label.ifBlank { step.kind }, color = AndexOnSurface,
                    style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                val elapsed = if (step.time > 0 && firstTime > 0) " · +${(step.time - firstTime).coerceAtLeast(0) / 1000} 秒" else ""
                Text(
                    "${step.kind}$elapsed${if (step.running) " · 运行中" else ""}",
                    style = MaterialTheme.typography.labelSmall, color = AndexOnSurfaceVariant,
                )
            }
            AnimatedVisibility(
                expanded,
                enter = expandVertically(tween(duration)) + fadeIn(tween(duration)),
                exit = shrinkVertically(tween(duration)) + fadeOut(tween(duration)),
            ) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (step.kind == "工具" && step.command != null) {
                        WorkTraceDetailBlock("命令", step.command, code = true)
                        WorkTraceDetailBlock(
                            "结果", step.result?.takeIf { it.isNotBlank() }
                                ?: if (step.running) "运行中，等待输出…" else "无输出", code = true,
                        )
                    } else {
                        WorkTraceDetailBlock(if (step.kind == "记录") "原文" else "内容", step.detail.ifBlank { step.label }, code = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkTraceDetailBlock(label: String, text: String, code: Boolean) {
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = AndexOnSurfaceVariant)
            TextButton(onClick = { clipboard.setText(AnnotatedString(text)) }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Icon(LucideIcons.Copy, null, Modifier.size(13.dp), tint = AndexOnSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("复制", style = MaterialTheme.typography.labelSmall, color = AndexOnSurfaceVariant)
            }
        }
        SelectionContainer {
            Text(
                text,
                Modifier.fillMaxWidth().heightIn(max = 220.dp)
                    .then(if (code) Modifier.clip(RoundedCornerShape(16.dp)).background(AndexSurfaceHigh) else Modifier)
                    .verticalScroll(rememberScrollState())
                    .padding(if (code) 12.dp else 0.dp),
                style = if (code) MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    else MaterialTheme.typography.bodyMedium,
                color = AndexContentText,
            )
        }
    }
}
