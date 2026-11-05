package com.android.andex.phonecontrol

import org.junit.Assert.*
import org.junit.Test

class AccessibilityNoticePolicyTest {
    @Test fun enabledServiceGetsAFullReconnectWindow() {
        val policy = AccessibilityNoticePolicy()
        for (time in 0L..59_000L step 1000L) assertNull(policy.sample(time, true, false, true))
        assertEquals(AccessibilityNotice.ConnectionUnavailable, policy.sample(60_000, true, false, true))
        assertNull(policy.sample(61_000, true, false, true))
    }

    @Test fun disabledPermissionRequiresStableConfirmationAndOnlyNotifiesOnce() {
        val policy = AccessibilityNoticePolicy()
        assertNull(policy.sample(0, false, false, true))
        assertNull(policy.sample(7_999, false, false, true))
        assertEquals(AccessibilityNotice.PermissionDisabled, policy.sample(8_000, false, false, true))
        assertNull(policy.sample(20_000, false, false, true))
    }

    @Test fun reconnectAndForegroundResumeDoNotReuseOldFailureSamples() {
        val policy = AccessibilityNoticePolicy()
        assertNull(policy.sample(0, false, false, true))
        policy.resume()
        assertNull(policy.sample(20_000, false, false, true))
        assertNull(policy.sample(27_000, true, false, true))
        assertNull(policy.sample(35_000, true, true, true))
        assertNull(policy.sample(99_000, true, false, true))
        assertNull(policy.sample(100_000, null, false, true))
        assertNull(policy.sample(200_000, false, false, false))
    }
}
