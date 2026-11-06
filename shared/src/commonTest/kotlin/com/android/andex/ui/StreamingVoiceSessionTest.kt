@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.android.andex.ui

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

class StreamingVoiceSessionTest {
    private class Recorder : VoiceRecorder {
        var allowed = true
        var stops = 0
        var cancelled = 0
        var feed: ((ByteArray) -> Unit)? = null
        override fun start(onPcm: ((ByteArray) -> Unit)?): Boolean { feed = onPcm; return allowed }
        override suspend fun stop(): ByteArray { stops++; return ByteArray(6400) }
        override fun cancel() { cancelled++ }
    }
    @Test fun uploadsDuringHoldAndOnlyCommitsOneFinalSentenceAfterRelease() = runTest {
        val recorder = Recorder()
        val result = mutableListOf<String>()
        var uploaded = 0
        var preview = ""
        val finalReady = CompletableDeferred<Unit>()
        val voice = StreamingVoiceSession(this, recorder, { pcm, interim ->
            pcm.collect { uploaded += it.size; interim("中间修订") }
            finalReady.await()
            "这是完整句子。"
        }, { _, text -> preview = text.orEmpty() }, result::add)
        assertTrue(voice.start())
        recorder.feed!!(ByteArray(3200))
        runCurrent()
        assertEquals(3200, uploaded)
        assertEquals("中间修订", preview)
        assertTrue(result.isEmpty())
        voice.finish()
        runCurrent()
        assertEquals(1, recorder.stops)
        assertEquals(VoicePhase.Finishing, voice.phase)
        assertTrue(result.isEmpty())
        finalReady.complete(Unit); runCurrent()
        assertEquals(listOf("这是完整句子。"), result)
        assertEquals(VoicePhase.Idle, voice.phase)
    }
    @Test fun cancelDiscardsLateCallbacksAndAllowsAnotherRecording() = runTest {
        val recorder = Recorder()
        val result = mutableListOf<String>()
        val late = CompletableDeferred<Unit>()
        var calls = 0
        val voice = StreamingVoiceSession(this, recorder, { _, interim ->
            calls++
            if (calls == 1) withContext(NonCancellable) { late.await(); interim("迟到内容") }
            else awaitCancellation()
            "旧结果"
        }, { _, _ -> }, result::add)
        voice.start(); runCurrent()
        voice.cancel()
        voice.start(); runCurrent()
        assertFalse(voice.start())
        assertEquals(1, calls)
        late.complete(Unit); runCurrent()
        assertEquals(2, calls)
        assertTrue(result.isEmpty())
        assertEquals(VoicePhase.Listening, voice.phase)
        voice.cancel()
    }
    @Test fun networkFailureIsRetainedAndReleaseCannotReplaceItWithRecordingError() = runTest {
        val recorder = Recorder()
        var notice: String? = null
        val voice = StreamingVoiceSession(this, recorder, { _, _ -> error("认证失败") },
            { _, text -> notice = text }, { fail("Must not fill a failed transcript") })
        voice.start(); runCurrent()
        assertEquals(VoicePhase.Idle, voice.phase)
        voice.finish(); runCurrent()
        assertEquals("认证失败", notice)
        assertEquals(0, recorder.stops)
        assertTrue(recorder.cancelled > 0)
    }
    @Test fun deniedPermissionDoesNotStartNetwork() = runTest {
        val recorder = Recorder().apply { allowed = false }
        val voice = StreamingVoiceSession(this, recorder, { _, _ -> fail("No network before microphone permission") },
            { _, _ -> }, { fail("No result") })
        assertFalse(voice.start())
        runCurrent()
        assertEquals(VoicePhase.Idle, voice.phase)
    }
}
