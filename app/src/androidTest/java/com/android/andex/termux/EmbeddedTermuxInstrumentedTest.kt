package com.android.andex.termux

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmbeddedTermuxInstrumentedTest {
    @Test fun bootstrapRunsBashAndNativeBatteryWithoutExternalTermux() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runtime = EmbeddedTermuxRuntime(context)
        try {
            val setup = runtime.initialize()
            assertTrue(setup.detail, setup.isReady)
            val result = JSONObject(runtime.execute("printf 'andex-embedded'; printf 'err' >&2"))
            assertTrue(result.toString(), result.getBoolean("ok"))
            assertEquals("andex-embedded", result.getString("stdout"))
            assertEquals("err", result.getString("stderr"))
            val battery = JSONObject(runtime.execute(
                "\$HOME/.local/bin/termux-battery-status",
            ))
            assertTrue(battery.toString(), battery.getBoolean("ok"))
            assertTrue(JSONObject(battery.getString("stdout")).has("percentage"))
        } finally {
            runtime.stop()
        }
    }

    @Test fun detachedChildDoesNotDelayDispatchCompletion() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runtime = EmbeddedTermuxRuntime(context)
        try {
            assertTrue(runtime.initialize().isReady)
            val result = JSONObject(runtime.execute(
                "sleep 5 </dev/null >/dev/null 2>&1 & printf 'launched'",
                timeoutMillis = 2000,
            ))
            assertTrue(result.toString(), result.getBoolean("ok"))
            assertEquals("launched", result.getString("stdout"))
        } finally {
            runtime.stop()
        }
    }

    @Test fun commandCanRoundTripAFullBase64FileChunk() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runtime = EmbeddedTermuxRuntime(context)
        val path = "${TermuxContract.HomeDirectory}/.andex-test-${UUID.randomUUID()}"
        try {
            assertTrue(runtime.initialize().isReady)
            val result = JSONObject(runtime.execute(
                "head -c 65536 /dev/zero > '$path'; base64 '$path' | tr -d '\\n'; rm -f '$path'",
            ))
            assertTrue(result.toString(), result.getBoolean("ok"))
            assertEquals(65536, java.util.Base64.getDecoder().decode(result.getString("stdout")).size)
        } finally {
            runtime.stop()
        }
    }
}
