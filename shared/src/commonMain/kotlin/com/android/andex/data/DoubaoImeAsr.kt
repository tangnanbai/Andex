@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.android.andex.data

import androidx.compose.runtime.Composable
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.uuid.Uuid

@Serializable
data class DoubaoCredentials(val deviceId: String = "", val cdid: String = "",
    val openudid: String = "", val clientudid: String = "", val token: String = "")

interface DoubaoCredentialStore {
    suspend fun read(): DoubaoCredentials?
    suspend fun write(value: DoubaoCredentials)
}

interface OpusPacketEncoder {
    /** Exactly 320 signed little-endian samples: mono, 16kHz, 20ms. */
    fun encode(pcm: ByteArray): ByteArray
    fun close()
}
expect fun createOpusPacketEncoder(): OpusPacketEncoder
@Composable expect fun rememberDoubaoImeAsr(): DoubaoImeAsr

private val registrationMutex = Mutex()

/** No server relay. One private device identity is reused; refusals never rotate it. */
class DoubaoImeAsr(private val store: DoubaoCredentialStore, private val http: HttpClient = HttpClient {
    install(WebSockets)
    install(HttpTimeout) { connectTimeoutMillis = 15_000; requestTimeoutMillis = 20_000; socketTimeoutMillis = 20_000 }
}) {
    private suspend fun credentials(): DoubaoCredentials = registrationMutex.withLock {
        var value = store.read() ?: DoubaoCredentials(cdid = Uuid.random().toString(),
            openudid = Uuid.random().toString().replace("-", "").take(16), clientudid = Uuid.random().toString())
        // Save the generated identity before any request, including interrupted bootstrap.
        store.write(value)
        if (value.deviceId.isBlank()) {
            val header = buildJsonObject {
                put("device_id", 0); put("install_id", 0)
                appValues.forEach { (key, item) -> put(key, item) }
                deviceValues.forEach { (key, item) -> put(key, item) }
                put("aid", 401734); put("version_code", 100102018); put("manifest_version_code", 100102018)
                put("update_version_code", 100102018); put("timezone", 8); put("tz_offset", 28800)
                put("not_request_sender", 0); put("openudid", value.openudid); put("clientudid", value.clientudid); put("cdid", value.cdid)
            }
            val reply = post("https://log.snssdk.com/service/2/device_register/", value, true,
                buildJsonObject { put("magic_tag", "ss_app_log"); put("header", header); put("_gen_time", platformCurrentTimeMillis()) }.toString())
            val id = reply["device_id_str"]?.jsonPrimitive?.contentOrNull
                ?: reply["device_id"]?.jsonPrimitive?.contentOrNull
            check(!id.isNullOrBlank() && id != "0") { sharedDoubaoSessionGuard.refuse(); "语音设备注册被拒绝，未重复注册。" }
            value = value.copy(deviceId = id)
            store.write(value)
        }
        if (value.token.isBlank()) {
            val reply = post("https://is.snssdk.com/service/settings/v3/", value, false, "body=null")
            val token = reply["data"]?.jsonObject?.get("settings")?.jsonObject
                ?.get("asr_config")?.jsonObject?.get("app_key")?.jsonPrimitive?.contentOrNull
            check(!token.isNullOrBlank()) { "语音认证未返回凭据，设备身份已保留。" }
            value = value.copy(token = token)
            store.write(value)
        }
        value
    }

    private suspend fun post(endpoint: String, identity: DoubaoCredentials, register: Boolean, body: String): JsonObject {
        val response = http.post(endpoint) {
            header(HttpHeaders.UserAgent, UserAgent)
            url {
                parameters.appendAll(Parameters.build {
                    append("device_platform", "android"); append("os", "android"); append("ssmix", "a")
                    append("_rticket", platformCurrentTimeMillis().toString()); append("cdid", identity.cdid)
                    for (key in listOf("channel", "aid", "app_name", "version_code", "version_name"))
                        append(key, appValues.getValue(key).jsonPrimitive.content)
                    if (register) for (key in listOf("resolution", "dpi", "device_type", "device_brand", "language", "os_api", "os_version"))
                        append(key, deviceValues.getValue(key).jsonPrimitive.content)
                    if (register) {
                        append("manifest_version_code", "100102018"); append("update_version_code", "100102018"); append("ac", "wifi")
                    } else append("device_id", identity.deviceId)
                })
            }
            if (register) contentType(ContentType.Application.Json) else {
                contentType(ContentType.Application.FormUrlEncoded)
                header("x-ss-stub", "46C03B52742B3F2615A3ABDF1636B754")
            }
            setBody(body)
        }
        if (!response.status.isSuccess()) {
            if (response.status.value in listOf(401, 403, 429)) sharedDoubaoSessionGuard.refuse()
            error("语音${if (register) "注册" else "认证"}失败（HTTP ${response.status.value}），未自动重试。")
        }
        val raw = response.bodyAsText()
        check(raw.length <= 1_048_576) { "语音认证响应过大。" }
        return try { Json.parseToJsonElement(raw).jsonObject }
        catch (_: Exception) { error("语音认证响应格式错误。") }
    }

    @Throws(Exception::class)
    suspend fun transcribe(pcm: Flow<ByteArray>, onInterim: (String) -> Unit = {}): String = sharedDoubaoSessionGuard.run {
        val identity = credentials()
        try {
            withTimeout(85_000) {
                val session = http.webSocketSession {
                    url("$Endpoint?aid=401734&device_id=${identity.deviceId}")
                    header(HttpHeaders.UserAgent, UserAgent)
                    header("proto-version", "v2")
                    header("x-custom-keepalive", "true")
                }
                val id = Uuid.random().toString()
                var sessionStarted = false
                val finishSent = CompletableDeferred<Unit>()
                try { coroutineScope { with(session) {
                    suspend fun receive(stage: String, expected: String? = null): DoubaoImeProtocol.Response = withTimeout(15_000) {
                        while (true) {
                            val frame = incoming.receiveCatching().getOrNull() ?: error("语音连接提前断开。")
                            if (frame is Frame.Binary) {
                                val bytes = frame.readBytes()
                                check(bytes.size <= 1_048_576) { "语音响应过大。" }
                                val response = DoubaoImeProtocol.response(bytes)
                                if (response.type in listOf("TaskFailed", "SessionFailed") ||
                                    response.code !in listOf(0L, 20000000L)) {
                                    if (response.code.toString().startsWith("401") || response.code.toString().startsWith("403")) {
                                        store.write(identity.copy(token = ""))
                                    }
                                    sharedDoubaoSessionGuard.refuse()
                                    val message = doubaoFailureMessage(stage, response, identity)
                                    runCatching {
                                        SharedDiagnosticLogger.event("voice", "asr_refused", level = "warning",
                                            details = mapOf("stage" to stage, "code" to response.code, "reason" to message))
                                    }
                                    error(message)
                                }
                                if (expected != null && response.type != expected) continue
                                return@withTimeout response
                            }
                        }
                        @Suppress("UNREACHABLE_CODE") error("语音连接中断。")
                    }
                    send(Frame.Binary(true, DoubaoImeProtocol.request(id, "StartTask", identity.token)))
                    receive("StartTask", "TaskStarted")
                    send(Frame.Binary(true, DoubaoImeProtocol.request(id, "StartSession", identity.token, DoubaoImeProtocol.session(identity.deviceId))))
                    receive("StartSession", "SessionStarted")
                    sessionStarted = true
                    val transcript = DoubaoTranscript()
                    val finishedSending = CompletableDeferred<Unit>()
                    val sender = launch(Dispatchers.Default) {
                        val encoder = createOpusPacketEncoder()
                        try {
                            val buffer = ByteArray(640)
                            var used = 0; var frameIndex = 0
                            val timestamp = platformCurrentTimeMillis()
                            suspend fun packet(last: Boolean) {
                                send(Frame.Binary(true, DoubaoImeProtocol.request(id, "TaskRequest",
                                    payload = """{"extra":{},"timestamp_ms":${timestamp + frameIndex * 20L}}""",
                                    audio = encoder.encode(buffer).also { check(it.isNotEmpty()) { "Opus 音频编码失败。" } },
                                    state = if (last) 9 else if (frameIndex == 0) 1 else 3)))
                                frameIndex++
                            }
                            pcm.collect { chunk ->
                                var at = 0
                                while (at < chunk.size) {
                                    val count = minOf(640 - used, chunk.size - at)
                                    chunk.copyInto(buffer, used, at, at + count); used += count; at += count
                                    if (used == 640) { packet(false); used = 0 }
                                }
                            }
                            buffer.fill(0, used, 640)
                            if (frameIndex == 0 && used == 0) error("没有录到语音，请重试。")
                            packet(true)
                            finishedSending.complete(Unit)
                            send(Frame.Binary(true, DoubaoImeProtocol.request(id, "FinishSession", identity.token)))
                            finishSent.complete(Unit)
                        } finally { encoder.close() }
                    }
                    try {
                        while (true) {
                            val response = receive("Recognition")
                            transcript.accept(response.json)?.let(onInterim)
                            if (response.type == "SessionFinished") {
                                check(finishedSending.isCompleted) { "语音服务提前结束，请重试。" }
                                check(transcript.hasFinal && transcript.finalText.isNotBlank()) { "没有识别到完整语音，请重试。" }
                                return@with transcript.finalText
                            }
                        }
                        @Suppress("UNREACHABLE_CODE") ""
                    } finally { sender.cancelAndJoin() }
                } } } finally {
                    releaseDoubaoSession(finishAndClose = {
                        if (sessionStarted && !finishSent.isCompleted) {
                            session.send(Frame.Binary(true, DoubaoImeProtocol.request(id, "FinishSession", identity.token)))
                        }
                        session.close()
                        session.coroutineContext[Job]?.join()
                    }, abort = { session.cancel() })
                }
            }
        } catch (_: TimeoutCancellationException) { error("语音识别超时，请重试。") }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: IllegalStateException) { throw error }
        catch (_: Exception) { error("语音网络连接失败或超时，请重试。") }
    }

    /** Protocol reachability only; this is deliberately not reported as successful recognition. */
    @Throws(Exception::class)
    suspend fun probe(): String {
        val text = transcribe(kotlinx.coroutines.flow.flow { emit(ByteArray(16000)) })
        return text
    }
    fun close() = http.close()

    companion object {
        const val Endpoint = "wss://frontier-audio-ime-ws.doubao.com/ocean/api/v1/ws"
        private const val UserAgent = "com.bytedance.android.doubaoime/100102018 (Linux; U; Android 16; en_US; Pixel 7 Pro; Build/BP2A.250605.031.A2; Cronet/TTNetVersion:94cf429a 2025-11-17 QuicVersion:1f89f732 2025-05-08)"
        private val appValues = buildJsonObject {
            put("aid", 401734); put("app_name", "oime"); put("version_code", 100102018); put("version_name", "1.1.2")
            put("manifest_version_code", 100102018); put("update_version_code", 100102018)
            put("channel", "official"); put("package", "com.bytedance.android.doubaoime")
        }
        private val deviceValues = buildJsonObject {
            put("device_platform", "android"); put("os", "android"); put("os_api", "34"); put("os_version", "16")
            put("device_type", "Pixel 7 Pro"); put("device_brand", "google"); put("device_model", "Pixel 7 Pro")
            put("resolution", "1080*2400"); put("dpi", "420"); put("language", "zh")
            put("timezone", 8); put("access", "wifi"); put("rom", "UP1A.231005.007"); put("rom_version", "UP1A.231005.007")
            put("region", "CN"); put("tz_name", "Asia/Shanghai"); put("tz_offset", 28800); put("sim_region", "cn")
            put("carrier_region", "cn"); put("cpu_abi", "arm64-v8a"); put("build_serial", "unknown")
            put("not_request_sender", 0); put("sig_hash", ""); put("google_aid", ""); put("mc", ""); put("serial_number", "")
        }
    }
}
