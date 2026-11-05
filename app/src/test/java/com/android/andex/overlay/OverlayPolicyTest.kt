package com.android.andex.overlay

import org.junit.Assert.*
import org.junit.Test

class OverlayPolicyTest {
    @Test fun statusLabelsExplainTheCurrentWork() {
        assertEquals("完成✅", OverlayPolicy.label("completed"))
        assertEquals("错误❌", OverlayPolicy.label("failed"))
        assertEquals("搜索🔎", OverlayPolicy.label("tool", "andex_web_search"))
        assertEquals("运行命令⚙️", OverlayPolicy.label("tool", "bash"))
        assertEquals("手机操作📱", OverlayPolicy.label("tool", "phone_control"))
        assertEquals("等待确认⏸", OverlayPolicy.label("waiting", "bash"))
    }
    @Test fun hiddenUnlessBackgroundTaskIsAvailable() {
        assertEquals(OverlayMode.HIDDEN, OverlayPolicy.mode(false, false, setOf("a"), emptySet(), true))
        assertEquals(OverlayMode.HIDDEN, OverlayPolicy.mode(true, true, setOf("a"), emptySet(), true))
        assertEquals(OverlayMode.HIDDEN, OverlayPolicy.mode(true, false, emptySet(), emptySet(), false))
        assertEquals(OverlayMode.PILL, OverlayPolicy.mode(true, false, setOf("a"), emptySet(), false))
        assertEquals(OverlayMode.BUBBLE, OverlayPolicy.mode(true, false, setOf("a"), emptySet(), true))
    }
    @Test fun dismissingOneRunDoesNotHideOtherRuns() {
        assertEquals(OverlayMode.HIDDEN, OverlayPolicy.mode(true, false, setOf("a:1"), setOf("a:1"), false))
        assertEquals(OverlayMode.PILL, OverlayPolicy.mode(true, false, setOf("a:2"), setOf("a:1"), false))
        assertEquals(OverlayMode.PILL, OverlayPolicy.mode(true, false, setOf("a", "b"), setOf("a"), false))
    }
    @Test fun positionCannotLeaveScreenEvenAfterRotation() {
        assertEquals(0, OverlayPolicy.clamp(-10, 1080, 60))
        assertEquals(1020, OverlayPolicy.clamp(3000, 1080, 60))
        assertEquals(0, OverlayPolicy.clamp(100, 100, 280))
    }
    @Test fun unknownTaskLengthDoesNotInventPercentage() {
        assertNull(OverlayPolicy.progress(4, null))
        assertNull(OverlayPolicy.progress(4, 0))
        assertEquals(50, OverlayPolicy.progress(2, 4))
        assertEquals(100, OverlayPolicy.progress(Int.MAX_VALUE, 10))
    }
}
