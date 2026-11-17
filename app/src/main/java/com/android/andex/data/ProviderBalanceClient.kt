package com.android.andex.data

object ProviderBalanceClient {
    private val client = SharedProviderBalanceClient()
    suspend fun fetch(option: ProviderModelOption): String? = client.fetch(option)
}
