package com.android.andex.ui

data class WorkTraceStep(
    val id: String,
    val label: String,
    val detail: String,
    val kind: String,
    val time: Long = 0,
    val running: Boolean = false,
    val toolName: String = "",
    val command: String? = null,
    val result: String? = null,
)

data class WorkTraceTask(
    val id: String,
    val title: String,
    val steps: List<WorkTraceStep>,
) {
    val running: Boolean get() = steps.any { it.running }
}

internal fun workTraceStepPreview(step: WorkTraceStep): String =
    (step.command?.takeIf { it.isNotBlank() } ?: step.detail)
        .lineSequence().map(String::trim).firstOrNull { it.isNotBlank() }.orEmpty().take(240)

/** User messages delimit requests; subsequent replies and tools stay with that request. */
internal fun groupWorkTraceSteps(steps: List<WorkTraceStep>): List<WorkTraceTask> {
    val groups = mutableListOf<WorkTraceTask>()
    var current = mutableListOf<WorkTraceStep>()
    fun flush() {
        if (current.isEmpty()) return
        val first = current.first()
        groups += WorkTraceTask(
            id = first.id,
            title = if (first.kind == "输入") first.label else "执行记录",
            steps = current.toList(),
        )
        current = mutableListOf()
    }
    steps.forEach { step ->
        if (step.kind == "输入") flush()
        current += step
    }
    flush()
    return groups
}

internal fun workTraceTaskExpanded(
    taskId: String,
    latestTaskId: String?,
    expandedIds: List<String>,
    collapsedIds: List<String>,
    searching: Boolean = false,
): Boolean = taskId in expandedIds ||
    (taskId !in collapsedIds && (searching || taskId == latestTaskId))

fun workTraceToolLabel(name: String, running: Boolean): String = when (name.lowercase()) {
    "bash" -> if (running) "正在执行 bash 命令" else "已执行 bash 命令"
    "fetch_web_url", "read_webpage" -> if (running) "正在读取网页" else "已读取网页"
    "tavily_search", "web_search" -> if (running) "正在搜索网页" else "已搜索网页"
    "phone_control" -> if (running) "正在操作手机" else "已调用手机控制"
    else -> if (running) "$name · 运行中" else name
}
internal fun workTraceLatestRowIndex(tasks: List<WorkTraceTask>, expanded: (WorkTraceTask) -> Boolean): Int {
    if (tasks.isEmpty()) return 0
    return (tasks.sumOf { 1 + if (expanded(it)) it.steps.size else 0 } - 1).coerceAtLeast(0)
}
