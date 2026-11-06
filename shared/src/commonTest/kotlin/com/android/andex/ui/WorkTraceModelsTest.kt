package com.android.andex.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkTraceModelsTest {
    @Test fun newestRowAccountsForPreviouslyExpandedHistory() {
        val tasks = groupWorkTraceSteps(listOf(
            WorkTraceStep("u1", "旧任务", "", "输入"),
            WorkTraceStep("t1", "命令", "pwd", "工具"),
            WorkTraceStep("u2", "新任务", "", "输入"),
            WorkTraceStep("t2", "搜索", "query", "工具"),
        ))
        assertEquals(3, workTraceLatestRowIndex(tasks) { it.id == "u2" })
        assertEquals(5, workTraceLatestRowIndex(tasks) { true })
        assertEquals("pwd", workTraceStepPreview(tasks.first().steps.last()))
        assertEquals("echo ok", workTraceStepPreview(WorkTraceStep("x", "命令", "输出", "工具", command = "echo ok\n")))
    }
    @Test
    fun requestsAreSeparateGroupsWithTheOriginalInputAsTitle() {
        val title = "搜索 Qwen 新闻并整理标题与链接".repeat(30)
        val steps = listOf(
            WorkTraceStep("u1", title, title, "输入"),
            WorkTraceStep("t1", "搜索", "结果", "工具"),
            WorkTraceStep("a1", "回复", "正文", "回复"),
            WorkTraceStep("u2", "继续整理", "继续整理", "输入"),
            WorkTraceStep("t2", "保存", "文件", "工具", running = true),
        )
        val tasks = groupWorkTraceSteps(steps)
        assertEquals(listOf("u1", "u2"), tasks.map { it.id })
        assertEquals(title, tasks.first().title)
        assertEquals(listOf("u1", "t1", "a1"), tasks.first().steps.map { it.id })
        assertEquals(listOf("u2", "t2"), tasks.last().steps.map { it.id })
        assertFalse(tasks.first().running)
        assertTrue(tasks.last().running)
    }

    @Test
    fun liveUpdatesKeepGroupIdentityAndDoNotCreateExtraTasks() {
        val input = WorkTraceStep("u", "执行命令", "执行命令", "输入")
        val running = WorkTraceStep("tool", "运行", "", "工具", running = true)
        val before = groupWorkTraceSteps(listOf(input, running)).single()
        val after = groupWorkTraceSteps(listOf(input, running.copy(running = false, result = "done"))).single()
        assertEquals(before.id, after.id)
        assertEquals(before.steps.map { it.id }, after.steps.map { it.id })
        assertFalse(after.running)
    }

    @Test
    fun groupsCanBeExpandedTogetherAndExplicitCollapseSurvivesNewData() {
        assertFalse(workTraceTaskExpanded("old", "latest", emptyList(), emptyList()))
        assertTrue(workTraceTaskExpanded("latest", "latest", emptyList(), emptyList()))
        assertTrue(workTraceTaskExpanded("old", "latest", listOf("old"), emptyList()))
        assertTrue(workTraceTaskExpanded("latest", "latest", listOf("old"), emptyList()))
        assertFalse(workTraceTaskExpanded("latest", "latest", listOf("old"), listOf("latest")))
        assertTrue(workTraceTaskExpanded("old", "new", listOf("old"), listOf("latest")))
        assertFalse(workTraceTaskExpanded("latest", "new", listOf("old"), listOf("latest")))
    }

    @Test
    fun matchingGroupsCanStillBeCollapsedDuringSearch() {
        assertTrue(workTraceTaskExpanded("old", "latest", emptyList(), emptyList(), searching = true))
        assertFalse(workTraceTaskExpanded("old", "latest", emptyList(), listOf("old"), searching = true))
        assertTrue(workTraceTaskExpanded("old", "latest", listOf("old"), emptyList(), searching = true))
    }

    @Test
    fun emptyAndOrphanHistoryDoNotDropRecords() {
        assertTrue(groupWorkTraceSteps(emptyList()).isEmpty())
        val orphan = WorkTraceStep("tool", "命令", "输出", "工具")
        val input = WorkTraceStep("u", "下一步", "下一步", "输入")
        val tasks = groupWorkTraceSteps(listOf(orphan, input))
        assertEquals(2, tasks.size)
        assertEquals("执行记录", tasks.first().title)
        assertEquals(orphan, tasks.first().steps.single())
        assertEquals("下一步", tasks.last().title)
    }
}
