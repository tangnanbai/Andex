package com.android.andex.data

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import kotlin.test.*
import kotlinx.coroutines.test.runTest

class SharedProviderBalanceClientTest {
    private fun option(url: String = "https://v2api.top/v1") = LlmProviderConfig(
        id = "test", providerId = "proxy", name = "测试", piProviderId = "openai-compatible",
        modelId = "gpt-image-2", baseUrl = url, apiKey = "private-test-key",
    ).let { listOf(it).availableModelOptions().single() }

    @Test fun newApiQuotaUsesSiteUnitsAndDoesNotSendCredentialsToPublicStatus() = runTest {
        val paths = mutableListOf<String>()
        val client = SharedProviderBalanceClient(MockEngine { request ->
            paths += request.url.encodedPath
            when (request.url.encodedPath) {
                "/api/status" -> {
                    assertNull(request.headers[HttpHeaders.Authorization])
                    respond("""{"data":{"quota_per_unit":500000,"quota_display_type":"USD"}}""")
                }
                "/api/usage/token/" -> {
                    assertEquals("Bearer private-test-key", request.headers[HttpHeaders.Authorization])
                    respond("""{"data":{"total_available":1250000,"unlimited_quota":false}}""")
                }
                else -> error("Unexpected request")
            }
        })
        try {
            assertEquals("$2.5", client.fetch(option()))
            assertEquals(listOf("/api/status", "/api/usage/token/"), paths)
        } finally { client.close() }
    }

    @Test fun accountQuotaAndCurrencyConversionTakePrecedenceOverTokenLimit() = runTest {
        val client = SharedProviderBalanceClient(MockEngine { request ->
            respond(if (request.url.encodedPath == "/api/status")
                """{"data":{"quota_per_unit":500000,"quota_display_type":"CNY","usd_exchange_rate":7.3}}"""
            else """{"data":{"total_available":0,"unlimited_quota":true,"user_quota":{"total_available":1000000}}}""")
        })
        try { assertEquals("¥14.6", client.fetch(option())) } finally { client.close() }
    }

    @Test fun billingFallbackSubtractsCentsRatherThanShowingGrantedQuotaAsBalance() = runTest {
        val client = SharedProviderBalanceClient(MockEngine { request ->
            respond(when (request.url.encodedPath) {
                "/api/status" -> """{"data":{"quota_display_type":"USD"}}"""
                "/api/usage/token/" -> """{"success":false,"message":"unsupported"}"""
                "/v1/dashboard/billing/subscription" -> """{"hard_limit_usd":10}"""
                "/v1/dashboard/billing/usage" -> """{"total_usage":250}"""
                else -> error("Unexpected request")
            })
        })
        try { assertEquals("$7.5", client.fetch(option())) } finally { client.close() }
    }

    @Test fun failuresMissingDataAndUnlimitedSentinelsRemainHidden() = runTest {
        for (body in listOf("""{"error":{"message":"invalid token"}}""", """{"data":{}}""", "not json")) {
            val client = SharedProviderBalanceClient(MockEngine { respond(body) })
            try { assertNull(client.fetch(option())) } finally { client.close() }
        }
    }

    @Test fun unknownInsecureAndCredentialBearingOriginsMakeNoRequest() = runTest {
        val client = SharedProviderBalanceClient(MockEngine { error("Must not send a key") })
        try {
            for (url in listOf("https://example.test/v1", "http://v2api.top/v1", "https://user:pass@v2api.top/v1"))
                assertNull(client.fetch(option(url)))
        } finally { client.close() }
    }

    @Test fun existingDeepSeekBalanceRemainsSupported() = runTest {
        val client = SharedProviderBalanceClient(MockEngine { request ->
            assertEquals("https://api.deepseek.com/user/balance", request.url.toString())
            respond("""{"balance_infos":[{"currency":"CNY","total_balance":"12.3400"}]}""")
        })
        try { assertEquals("¥12.34", client.fetch(option("https://api.deepseek.com/v1"))) } finally { client.close() }
    }
}
