package com.android.andex.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex

/** Shared by the settings probe and composer, including newly created clients. */
internal class DoubaoSessionGuard(private val now: () -> Long = ::platformCurrentTimeMillis) {
    private val mutex = Mutex()
    private var refusedUntil = 0L

    suspend fun <T> run(block: suspend () -> T): T {
        check(mutex.tryLock()) { "已有语音请求正在处理，请等待结束后再录音。" }
        try {
            val remaining = refusedUntil - now()
            check(remaining <= 0) { "语音服务暂时拒绝访问，请在 ${(remaining + 999) / 1000} 秒后重试。" }
            return block()
        } finally {
            mutex.unlock()
        }
    }

    /** Called inside run; never retries or changes the registered device. */
    fun refuse() { refusedUntil = now() + 60_000 }
}

internal val sharedDoubaoSessionGuard = DoubaoSessionGuard()

/** Cleanup must still run when the recording's coroutine is already cancelled. */
internal suspend fun releaseDoubaoSession(
    finishAndClose: suspend () -> Unit,
    abort: () -> Unit,
) = withContext(NonCancellable) {
    try {
        withTimeoutOrNull(1_500) {
            try { finishAndClose() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Force termination below even after a failed close. */ }
        }
    } finally {
        abort()
    }
}

internal fun doubaoFailureMessage(stage: String, response: DoubaoImeProtocol.Response, identity: DoubaoCredentials): String {
    var detail = response.message
    for (secret in listOf(identity.token, identity.deviceId, identity.cdid, identity.openudid, identity.clientudid)) {
        if (secret.isNotEmpty()) detail = detail.replace(secret, "[REDACTED]")
    }
    detail = SharedDiagnosticRedactor.sanitizeString(detail.replace(Regex("[\\r\\n\\t]+"), " "), 240)
    val reason = when {
        detail.contains("concurrency", ignoreCase = true) ->
            "服务端并发会话额度已占用，请等待上一会话释放后重试。"
        detail.contains("quota", ignoreCase = true) || detail.contains("rate limit", ignoreCase = true) ->
            "服务端限制了本次请求，请稍后重试。"
        else -> "服务端拒绝了本次请求，请稍后重试。"
    }
    return "语音 $stage 失败（${response.code}）：$reason" +
        if (detail.isBlank()) "" else "\n$detail"
}
