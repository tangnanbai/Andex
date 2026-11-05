package com.android.andex.data

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class VolcAsrTest {
    private val audio = ByteArray(6444) { (it % 127).toByte() }

    @Test fun newKeyUploadsAudioAndReturnsTranscript() = runTest {
        var called = false
        val engine = MockEngine { request ->
            called = true
            assertEquals(VolcAsrClient.Endpoint, request.url.toString())
            assertEquals("test-key", request.headers["X-Api-Key"])
            assertNull(request.headers["X-Api-App-Key"])
            assertEquals("volc.bigasr.auc_turbo", request.headers["X-Api-Resource-Id"])
            assertEquals("-1", request.headers["X-Api-Sequence"])
            assertTrue(request.headers["X-Api-Request-Id"].orEmpty().isNotBlank())
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertTrue(body["audio"]!!.jsonObject["data"]!!.jsonPrimitive.content.length > audio.size)
            assertEquals("bigmodel", body["request"]!!.jsonObject["model_name"]!!.jsonPrimitive.content)
            respond("""{"result":{"text":"打开 IT 之家。"}}""", HttpStatusCode.OK,
                headersOf("X-Api-Status-Code", "20000000"))
        }
        val client = VolcAsrClient(engine)
        try { assertEquals("打开 IT 之家。", client.transcribe(VolcAsrSettings(apiKey = "test-key"), audio)) }
        finally { client.close() }
        assertTrue(called)
    }

    @Test fun legacyCredentialsUseSeparateHeaders() = runTest {
        val client = VolcAsrClient(MockEngine { request ->
            assertNull(request.headers["X-Api-Key"])
            assertEquals("app", request.headers["X-Api-App-Key"])
            assertEquals("token", request.headers["X-Api-Access-Key"])
            respond("""{"result":{"text":"你好"}}""", HttpStatusCode.OK, headersOf("X-Api-Status-Code", "20000000"))
        })
        try { assertEquals("你好", client.transcribe(VolcAsrSettings(appId = "app", accessKey = "token"), audio)) }
        finally { client.close() }
    }

    @Test fun failedResponsesDoNotLeakSecrets() = runTest {
        val client = VolcAsrClient(MockEngine {
            respond("""{"error":"sensitive-token"}""", HttpStatusCode.Unauthorized,
                headersOf("X-Api-Status-Code", "45000001"))
        })
        try {
            val error = assertFailsWith<IllegalStateException> {
                client.transcribe(VolcAsrSettings(apiKey = "sensitive-token"), audio)
            }
            assertTrue(error.message.orEmpty().contains("401"))
            assertFalse(error.message.orEmpty().contains("sensitive-token"))
        } finally { client.close() }
    }

    @Test fun silenceReturnsHelpfulError() = runTest {
        val client = VolcAsrClient(MockEngine {
            respond("{}", HttpStatusCode.OK, headersOf("X-Api-Status-Code", "20000003"))
        })
        try {
            assertTrue(assertFailsWith<IllegalStateException> {
                client.transcribe(VolcAsrSettings(apiKey = "key"), audio)
            }.message.orEmpty().contains("没有识别到语音"))
        } finally { client.close() }
    }

    @Test fun incompleteConfigurationNeverSendsAudio() = runTest {
        val client = VolcAsrClient(MockEngine { error("Must not access network") })
        try {
            assertFailsWith<IllegalArgumentException> { client.transcribe(VolcAsrSettings(appId = "app"), audio) }
            assertFailsWith<IllegalArgumentException> { client.transcribe(VolcAsrSettings(apiKey = "key"), ByteArray(44)) }
        } finally { client.close() }
    }

    @Test fun settingsRoundTripPreservesAppearanceAndVoiceAndReadsOlderData() {
        val settings = AppSettings(themeAccent = "mint", themeBackground = "moss", voiceInput = VolcAsrSettings(apiKey = "key"))
        assertEquals(settings, parseAppSettings(serializeAppSettings(settings)))
        assertEquals("auto", parseAppSettings("{}").themeAccent)
        assertEquals("auto", parseAppSettings("{}").themeBackground)
        assertFalse(parseAppSettings("{}").voiceInput.isConfigured)
    }

    @Test fun probeAcceptsSilenceAndUsesConfiguredEndpoint() = runTest {
        val client = VolcAsrClient(MockEngine { request ->
            assertEquals("https://example.test/asr", request.url.toString())
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            val encoded = body["audio"]!!.jsonObject["data"]!!.jsonPrimitive.content
            assertTrue(encoded.startsWith("UklGR")) // Valid RIFF/WAVE rather than arbitrary data.
            respond("{}", HttpStatusCode.OK, headersOf("X-Api-Status-Code", "20000003"))
        })
        try { assertTrue(client.testConnection(VolcAsrSettings(apiKey = "key", endpoint = "https://example.test/asr")).contains("连接正常")) }
        finally { client.close() }
    }

    @Test fun probeRejectsAuthenticationFailureWithoutExposingCredentials() = runTest {
        val client = VolcAsrClient(MockEngine {
            respond("secret-key", HttpStatusCode.Unauthorized, headersOf("X-Api-Status-Code", "45000001"))
        })
        try {
            val failure = assertFailsWith<IllegalStateException> { client.testConnection(VolcAsrSettings(apiKey = "secret-key")) }
            assertTrue(failure.message.orEmpty().contains("401"))
            assertFalse(failure.message.orEmpty().contains("secret-key"))
            assertFailsWith<IllegalArgumentException> { client.testConnection(VolcAsrSettings(apiKey = "secret-key", endpoint = "http://example.test")) }
        } finally { client.close() }
    }
}
