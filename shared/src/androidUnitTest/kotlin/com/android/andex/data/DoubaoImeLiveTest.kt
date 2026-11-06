package com.android.andex.data

import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.Assert.*

/** Opt-in real network test. Credentials and sample stay outside the repository. */
class DoubaoImeLiveTest {
    @Test fun streamsShortChinesePcmAndWaitsForFinalSentence() = runBlocking {
        val directory = System.getenv("ANDEX_ASR_SMOKE_DIR")
        assumeTrue(!directory.isNullOrBlank())
        val root = File(directory!!)
        val raw = Json.parseToJsonElement(File(root, "private/credentials.json").readText()).jsonObject
        var credentials = DoubaoCredentials(deviceId = raw.getValue("device_id").jsonPrimitive.content,
            cdid = raw.getValue("cdid").jsonPrimitive.content, token = raw.getValue("token").jsonPrimitive.content)
        var writes = 0
        val client = DoubaoImeAsr(object : DoubaoCredentialStore {
            override suspend fun read() = credentials
            override suspend fun write(value: DoubaoCredentials) { credentials = value; writes++ }
        })
        val started = System.nanoTime()
        val pcm = File(root, "zh.pcm").readBytes()
        var partials = 0
        try {
            val cancellationChecked = System.getenv("ANDEX_ASR_CANCEL_SMOKE") == "1"
            if (cancellationChecked) {
                val recording = CompletableDeferred<Unit>()
                val first = launch {
                    client.transcribe(flow {
                        recording.complete(Unit) // StartSession is acknowledged before PCM collection.
                        emit(pcm.copyOfRange(0, 3200))
                        awaitCancellation()
                    })
                }
                withTimeout(20_000) { recording.await() }
                first.cancelAndJoin()
                assertTrue(first.isCancelled)
            }
            val result = client.transcribe(flow {
                for (offset in pcm.indices step 3200) {
                    emit(pcm.copyOfRange(offset, minOf(offset + 3200, pcm.size))); delay(100)
                }
            }) { partials++ }
            assertTrue(result.isNotBlank())
            assertTrue(partials > 1)
            assertEquals(if (cancellationChecked) 2 else 1, writes) // Same cached identity, no registration or token rotation.
            File(root, "native-result.json").writeText(buildJsonObject {
                put("text", result); put("elapsed_ms", (System.nanoTime() - started) / 1_000_000)
                put("partial_events", partials); put("cache_reused", true)
                put("cancel_then_repeat", cancellationChecked)
            }.toString())
        } finally { client.close() }
    }
}
