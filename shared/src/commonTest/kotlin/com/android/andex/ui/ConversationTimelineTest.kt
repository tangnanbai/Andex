package com.android.andex.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ConversationTimelineTest {
    @Test
    fun compactWindowKeepsFiveTurnSequenceAnchoredLikeMinimap() {
        assertEquals(listOf(0, 1, 2, 3), compactTimelineIndices(total = 5, currentIndex = 0))
        assertEquals(listOf(0, 1, 2, 3), compactTimelineIndices(total = 5, currentIndex = 1))
        assertEquals(listOf(1, 2, 3, 4), compactTimelineIndices(total = 5, currentIndex = 2))
        assertEquals(listOf(1, 2, 3, 4), compactTimelineIndices(total = 5, currentIndex = 3))
        assertEquals(listOf(1, 2, 3, 4), compactTimelineIndices(total = 5, currentIndex = 4))
    }

    @Test
    fun compactWindowAdvancesOneSlotAtATimeForLongConversations() {
        assertEquals(8, compactTimelineWindowStart(total = 18, currentIndex = 9))
        assertEquals(9, compactTimelineWindowStart(total = 18, currentIndex = 10))
        assertEquals(10, compactTimelineWindowStart(total = 18, currentIndex = 11))
        assertEquals(14, compactTimelineWindowStart(total = 18, currentIndex = 17))
    }

    @Test
    fun navigationPositionMapsWholeRailToWholeConversation() {
        val args = arrayOf(100f, 10f, 3f)
        assertEquals(0, timelineIndexForPosition(0f, args[0], 5, args[1], args[2]))
        assertEquals(2, timelineIndexForPosition(50f, args[0], 5, args[1], args[2]))
        assertEquals(4, timelineIndexForPosition(100f, args[0], 5, args[1], args[2]))
    }

    @Test fun overshootingBottomSelectsLastTurnAndStillCommits() {
        assertEquals(49, timelineIndexForPosition(700f, 500f, 50, 10f, 3f))
        assertTrue(timelineGestureCommits(false, 90f, 8f, 40f, 58f, 28f))
        assertEquals(0, timelineIndexForPosition(-100f, 500f, 50, 10f, 3f))
        assertFalse(timelineGestureCommits(false, 90f, 8f, -60f, 58f, 28f))
        assertFalse(timelineGestureCommits(true, 90f, 8f, 40f, 58f, 28f))
        assertFalse(timelineGestureCommits(false, 2f, 8f, 40f, 58f, 28f))
    }
}
