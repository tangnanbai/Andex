package com.android.andex.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.round

/** Credentials are sent only to the configured, supported HTTPS origin. */
class SharedProviderBalanceClient(engine: HttpClientEngine? = null) {
    private val client = if (engine == null) HttpClient { configure() } else HttpClient(engine) { configure() }

    suspend fun fetch(option: ProviderModelOption): String? = try {
        val base = Url(option.baseUrl)
        if (base.protocol != URLProtocol.HTTPS || base.user != null || base.password != null || option.apiKey.isBlank()) null
        else when (base.host.lowercase()) {
            "api.deepseek.com" -> {
                val response = getJson("https://api.deepseek.com/user/balance", option.apiKey)
                (response?.get("balance_infos") as? JsonArray)?.mapNotNull { element ->
                    val item = element as? JsonObject ?: return@mapNotNull null
                    val amount = item.number("total_balance") ?: return@mapNotNull null
                    val symbol = when (item.string("currency")) { "CNY" -> "¥"; "USD" -> "$"; else -> return@mapNotNull null }
                    "$symbol${formatBalance(amount)}"
                }?.joinToString(" / ")?.takeIf { it.isNotBlank() }
            }
            "v2api.top" -> fetchV2Api(option.apiKey)
            else -> null
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private suspend fun fetchV2Api(key: String): String? {
        val status = getJson("https://v2api.top/api/status")?.get("data") as? JsonObject
        val display = status.string("quota_display_type").uppercase()
        val symbol = when (display) {
            "USD" -> "$"
            "CNY" -> "¥"
            "CUSTOM" -> status.string("custom_currency_symbol")
            else -> ""
        }
        val token = getJson("https://v2api.top/api/usage/token/", key)?.get("data") as? JsonObject
        if (token != null) {
            val account = token["user_quota"] as? JsonObject
            val unlimited = (token["unlimited_quota"] as? JsonPrimitive)?.booleanOrNull == true
            val available = account.number("total_available")
                ?: token.number("total_available").takeUnless { unlimited }
            val units = status.number("quota_per_unit")
            if (available != null && units != null && units > 0 && symbol.isNotBlank()) {
                val exchange = when (display) {
                    "CNY" -> status.number("usd_exchange_rate")
                    "CUSTOM" -> status.number("custom_currency_exchange_rate")
                    else -> 1.0
                }
                if (exchange != null && exchange > 0) return "$symbol${formatBalance(available / units * exchange)}"
            }
        }
        if (symbol.isBlank()) return null
        val subscription = getJson("https://v2api.top/v1/dashboard/billing/subscription", key) ?: return null
        val usage = getJson("https://v2api.top/v1/dashboard/billing/usage", key) ?: return null
        val granted = subscription.number("hard_limit_usd") ?: return null
        val usedCents = usage.number("total_usage") ?: return null
        if (granted >= 100_000_000) return null
        return "$symbol${formatBalance(granted - usedCents / 100)}"
    }

    private suspend fun getJson(url: String, key: String? = null): JsonObject? {
        val response = client.get(url) { key?.let { header("Authorization", "Bearer $it") } }
        if (!response.status.isSuccess()) return null
        val json = runCatching { Json.parseToJsonElement(response.bodyAsText()) as? JsonObject }.getOrNull()
        if (json?.get("error") != null || (json?.get("success") as? JsonPrimitive)?.booleanOrNull == false) return null
        return json
    }

    fun close() = client.close()
}

private fun io.ktor.client.HttpClientConfig<*>.configure() {
    followRedirects = false
    install(HttpTimeout) { requestTimeoutMillis = 8_000; connectTimeoutMillis = 5_000 }
}

private fun JsonObject?.string(key: String): String = (this?.get(key) as? JsonPrimitive)?.content.orEmpty()
private fun JsonObject?.number(key: String): Double? =
    (this?.get(key) as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() }
private fun formatBalance(value: Double): String {
    val rounded = round(value * 10_000) / 10_000
    val result = rounded.toString()
    return if ('.' in result) result.trimEnd('0').trimEnd('.') else result
}
