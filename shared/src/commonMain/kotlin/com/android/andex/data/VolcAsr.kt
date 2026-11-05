package com.android.andex.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid

@Serializable
data class VolcAsrSettings(
    val apiKey: String = "",
    val appId: String = "",
    val accessKey: String = "",
    val resourceId: String = "volc.bigasr.auc_turbo",
    val endpoint: String = VolcAsrClient.Endpoint,
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank() || (appId.isNotBlank() && accessKey.isNotBlank())
}

fun serializeVolcAsr(settings: VolcAsrSettings): String = Json.encodeToString(settings)
fun parseVolcAsr(value: String): VolcAsrSettings =
    runCatching { Json { ignoreUnknownKeys = true }.decodeFromString<VolcAsrSettings>(value) }
        .getOrDefault(VolcAsrSettings())

/** Volcano's synchronous short audio API; no audio is sent before the user releases the hold. */
class VolcAsrClient(engine: HttpClientEngine? = null) {
    private val client = if (engine == null) HttpClient { timeouts() } else HttpClient(engine) { timeouts() }

    suspend fun transcribe(settings: VolcAsrSettings, wav: ByteArray): String {
        require(settings.isConfigured) { "请先在通用设置中配置火山 ASR。" }
        require(wav.size > 44) { "录音太短，请按住后说话。" }
        val response = upload(settings, wav)
        val body = response.bodyAsText()
        val code = response.headers["X-Api-Status-Code"]
        if (code == "20000003") error("没有识别到语音，请重试。")
        check(response.status.isSuccess() && code == "20000000") {
            // Never surface request headers or response bodies that might contain credentials.
            "火山 ASR 识别失败（HTTP ${response.status.value}，状态 ${code ?: "未知"}）。请检查密钥和语音资源权限。"
        }
        val text = Json.parseToJsonElement(body).jsonObject["result"]?.jsonObject
            ?.get("text")?.jsonPrimitive?.content.orEmpty().trim()
        check(text.isNotEmpty()) { "没有识别到语音，请重试。" }
        return text
    }

    /** Send a short, valid silent WAV to verify the endpoint, credentials and resource access. */
    suspend fun testConnection(settings: VolcAsrSettings): String {
        require(settings.isConfigured) { "请填写完整的 ASR 凭据。" }
        val response = upload(settings, silentProbeWav())
        val code = response.headers["X-Api-Status-Code"]
        response.bodyAsText()
        check(response.status.isSuccess() && code in setOf("20000000", "20000003")) {
            "检测失败（HTTP ${response.status.value}，状态 ${code ?: "未知"}）。请检查地址、密钥和语音资源权限。"
        }
        return "连接正常 · 鉴权和语音资源可用"
    }

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    private suspend fun upload(settings: VolcAsrSettings, wav: ByteArray): HttpResponse {
        val endpoint = settings.endpoint.trim().ifBlank { Endpoint }
        require(runCatching { Url(endpoint).protocol == URLProtocol.HTTPS }.getOrDefault(false)) { "服务地址需使用 HTTPS。" }
        return client.post(endpoint) {
            contentType(ContentType.Application.Json)
            if (settings.apiKey.isNotBlank()) header("X-Api-Key", settings.apiKey.trim())
            else {
                header("X-Api-App-Key", settings.appId.trim())
                header("X-Api-Access-Key", settings.accessKey.trim())
            }
            header("X-Api-Resource-Id", settings.resourceId.trim().ifBlank { "volc.bigasr.auc_turbo" })
            header("X-Api-Request-Id", Uuid.random().toString())
            header("X-Api-Sequence", "-1")
            setBody(buildJsonObject {
                putJsonObject("user") { put("uid", "andex-voice") }
                putJsonObject("audio") { put("data", Base64.encode(wav)) }
                putJsonObject("request") { put("model_name", "bigmodel"); put("enable_punc", true) }
            }.toString())
        }
    }

    private fun silentProbeWav(): ByteArray {
        val bytes = ByteArray(44 + 8000)
        fun text(offset: Int, value: String) { value.encodeToByteArray().copyInto(bytes, offset) }
        fun number(offset: Int, value: Int, count: Int) { repeat(count) { bytes[offset + it] = (value ushr (8 * it)).toByte() } }
        text(0, "RIFF"); number(4, bytes.size - 8, 4); text(8, "WAVEfmt ")
        number(16, 16, 4); number(20, 1, 2); number(22, 1, 2); number(24, 16000, 4)
        number(28, 32000, 4); number(32, 2, 2); number(34, 16, 2); text(36, "data"); number(40, 8000, 4)
        return bytes
    }

    fun close() = client.close()
    companion object {
        const val Endpoint = "https://openspeech.bytedance.com/api/v3/auc/bigmodel/recognize/flash"
        private fun io.ktor.client.HttpClientConfig<*>.timeouts() {
            install(HttpTimeout) { requestTimeoutMillis = 60_000; connectTimeoutMillis = 15_000; socketTimeoutMillis = 60_000 }
        }
    }
}
