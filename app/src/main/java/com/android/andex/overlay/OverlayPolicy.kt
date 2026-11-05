package com.android.andex.overlay

object OverlayNavigation {
    const val SESSION_EXTRA = "andex_session_id"
    const val OPEN_TASK = "com.android.andex.OPEN_TASK"
}

enum class OverlayMode { HIDDEN, PILL, BUBBLE }

/** Pure policy shared by tests and the window controller. */
object OverlayPolicy {
    const val RESULT_VISIBLE_MILLIS = 8_000L
    fun label(state: String, tool: String = ""): String = when (state) {
        "completed" -> "完成✅"
        "failed" -> "错误❌"
        "waiting" -> "等待确认⏸"
        "cancelled" -> "已停止⏹"
        else -> when (tool) {
            "andex_web_search", "web_search" -> "搜索🔎"
            "andex_web_page", "web_fetch" -> "浏览网页🌐"
            "bash", "shell", "termux_bash", "exec" -> "运行命令⚙️"
            "read", "read_file", "ls" -> "读取文件📄"
            "grep", "find" -> "查找文件🔎"
            "write", "edit", "apply_patch" -> "修改文件✏️"
            "phone_control", "agent_mode", "agent_display" -> "手机操作📱"
            "browser" -> "浏览网页🌐"
            "andex_github" -> "访问 GitHub🐙"
            "andex_archive" -> "处理压缩包📦"
            else -> "运行中⏳"
        }
    }
    fun mode(connected: Boolean, foreground: Boolean, taskIds: Set<String>,
             dismissed: Set<String>, expanded: Boolean): OverlayMode = when {
        !connected || foreground || taskIds.isEmpty() || taskIds.all { it in dismissed } -> OverlayMode.HIDDEN
        expanded -> OverlayMode.BUBBLE
        else -> OverlayMode.PILL
    }

    fun clamp(position: Int, screen: Int, window: Int): Int =
        position.coerceIn(0, (screen - window).coerceAtLeast(0))

    fun progress(completed: Int?, total: Int?): Int? =
        if (completed == null || total == null || total <= 0) null
        else ((completed.toLong().coerceIn(0, total.toLong()) * 100) / total).toInt()
}
