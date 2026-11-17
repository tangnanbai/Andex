package com.android.andex.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrawerGestureHotZoneTest {
    @Test fun shortContentDragsDoNotOpenEitherDrawer() {
        assertFalse(drawerGesturePastOpeningDistance(12f, 28f, wasOpen = false))
        assertFalse(drawerGesturePastOpeningDistance(-27f, 28f, wasOpen = false))
        assertTrue(drawerGesturePastOpeningDistance(28f, 28f, wasOpen = false))
        assertTrue(drawerGesturePastOpeningDistance(-28f, 28f, wasOpen = false))
        assertTrue(drawerGesturePastOpeningDistance(-12f, 28f, wasOpen = true))
    }

    @Test fun centerThirdLeavesHorizontalContentGesturesUntouched() {
        for (x in listOf(101f, 150f, 199f)) {
            assertFalse(drawerGestureStartsInHotZone(x, 300f, right = false))
            assertFalse(drawerGestureStartsInHotZone(x, 300f, right = true))
        }
    }

    @Test fun eachSideOnlyOwnsItsThirdInPortraitAndLandscape() {
        for (width in listOf(300f, 1000f)) {
            assertTrue(drawerGestureStartsInHotZone(width / 6, width, right = false))
            assertFalse(drawerGestureStartsInHotZone(width / 6, width, right = true))
            assertTrue(drawerGestureStartsInHotZone(width * 5 / 6, width, right = true))
            assertFalse(drawerGestureStartsInHotZone(width * 5 / 6, width, right = false))
        }
        assertFalse(drawerGestureStartsInHotZone(0f, 0f, right = false))
    }
}
