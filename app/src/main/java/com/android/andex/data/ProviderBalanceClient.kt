package com.android.andex.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Only use the selected provider's documented balance endpoint; failures remain hidden. */
object ProviderBalanceClient {
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .callTimeout(10, TimeUnit.SECONDS).build()

    suspend fun fetch(option: ProviderModelOption): String? = withContext(Dispatchers.IO) {
        runCatching {
            val base = option.baseUrl.toHttpUrlOrNull() ?: return@runCatching null
            if (base.scheme != "https" || base.host != "api.deepseek.com" ||
                base.username.isNotEmpty() || base.password.isNotEmpty() || option.apiKey.isBlank()) return@runCatching null
            val url = base.newBuilder().encodedPath("/user/balance").query(null).fragment(null).build()
            client.newCall(Request.Builder().url(url)
                .header("Authorization", "Bearer ${option.apiKey}").build()).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val json = JSONObject(response.body?.string() ?: return@use null)
                val balances = json.optJSONArray("balance_infos") ?: return@use null
                (0 until balances.length()).mapNotNull { index ->
                    val item = balances.optJSONObject(index) ?: return@mapNotNull null
                    val amount = item.optString("total_balance")
                    val currency = item.optString("currency")
                    if (amount.toBigDecimalOrNull() == null || currency !in listOf("CNY", "USD")) null
                    else "${if (currency == "CNY") "¥" else "$"}$amount"
                }.joinToString(" / ").takeIf(String::isNotBlank)
            }
        }.getOrNull()
    }
}
