package com.android.andex.ui

import kotlin.test.*

class ContextUsageDetailsTest {
    @Test fun referenceUsesGroupedCountsAndRoundedPercent() {
        val usage = ContextUsageDetails(489_320, 1_000_000, "DeepSeek")
        assertEquals("489,320 / 1,000,000 (49%)", usage.label)
        assertEquals(.48932f, usage.fraction)
    }
    @Test fun absentOrInvalidUsageDoesNotInventProgress() {
        assertNull(ContextUsageDetails().fraction)
        assertNull(ContextUsageDetails(10, 0).fraction)
        assertNull(ContextUsageDetails(-1, 100).fraction)
        assertEquals(1f, ContextUsageDetails(150, 100).fraction)
        assertEquals("0", contextTokenLabel(0))
    }
}
