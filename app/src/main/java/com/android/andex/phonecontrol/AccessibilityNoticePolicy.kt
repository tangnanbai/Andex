package com.android.andex.phonecontrol

internal enum class AccessibilityNotice { PermissionDisabled, ConnectionUnavailable }

/** Require a continuous observation window; foreground transitions start a new grace period. */
internal class AccessibilityNoticePolicy {
    private var missingSince: Long? = null
    private var previousEnabled: Boolean? = null
    private var notified: AccessibilityNotice? = null

    fun resume() {
        missingSince = null
        previousEnabled = null
    }

    fun sample(now: Long, enabled: Boolean?, connected: Boolean, monitoring: Boolean): AccessibilityNotice? {
        if (connected || !monitoring || enabled == null) {
            missingSince = null
            notified = null
            previousEnabled = enabled
            return null
        }
        if (enabled != previousEnabled) {
            missingSince = null
            notified = null
            previousEnabled = enabled
        }
        val since = missingSince ?: now.also { missingSince = it }
        val notice = if (enabled) AccessibilityNotice.ConnectionUnavailable else AccessibilityNotice.PermissionDisabled
        val grace = if (enabled) 60_000L else 8_000L
        return if (now - since >= grace && notified != notice) notice.also { notified = it } else null
    }
}
