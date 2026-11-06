@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.android.andex.data

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

class DoubaoSessionGuardTest {
    @Test fun concurrentEntrypointsCannotOpenTwoSessionsAndCancellationReleasesTheGate() = runTest {
        val guard = DoubaoSessionGuard()
        val first = launch { guard.run { awaitCancellation() } }
        runCurrent()
        val failure = assertFailsWith<IllegalStateException> { guard.run { fail("Cannot open another connection") } }
        assertContains(failure.message.orEmpty(), "已有语音请求")
        first.cancelAndJoin()
        assertEquals("second", guard.run { "second" })
    }

    @Test fun refusalCooldownSurvivesTheNextCallerWithoutAnAutomaticRetry() = runTest {
        var now = 1000L
        val guard = DoubaoSessionGuard { now }
        guard.run { guard.refuse() }
        val failure = assertFailsWith<IllegalStateException> { guard.run { fail("Cannot retry during cooldown") } }
        assertContains(failure.message.orEmpty(), "60 秒")
        now += 60_000
        assertTrue(guard.run { true })
    }

    @Test fun cancellingTheOwnerStillFinishesAndClosesTheSocket() = runTest {
        val events = mutableListOf<String>()
        val owner = launch {
            try { awaitCancellation() }
            finally {
                releaseDoubaoSession({
                    events += "finish"
                    delay(30)
                    events += "close"
                }, { events += "abort" })
            }
        }
        runCurrent()
        owner.cancelAndJoin()
        assertEquals(listOf("finish", "close", "abort"), events)
    }

    @Test fun stalledCloseIsBoundedAndAlwaysForcesTermination() = runTest {
        var aborted = false
        releaseDoubaoSession({ awaitCancellation() }, { aborted = true })
        assertTrue(aborted)
        assertEquals(1500L, testScheduler.currentTime)
    }

    @Test fun serverReasonPreservesStageAndCodeWhileRedactingCredentials() {
        val identity = DoubaoCredentials(deviceId = "private-device", token = "private-token")
        val reason = doubaoFailureMessage("StartSession",
            DoubaoImeProtocol.Response("SessionFailed", 40200011, null,
                "concurrency quota exceeded private-device token=private-token"), identity)
        assertContains(reason, "StartSession")
        assertContains(reason, "40200011")
        assertContains(reason, "并发会话")
        assertFalse(reason.contains("private-device"))
        assertFalse(reason.contains("private-token"))
    }
}
