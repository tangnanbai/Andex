package com.android.andex.data

import org.junit.Assert.*
import org.junit.Test

class ContextUsageTest {
    @Test fun readsPiLastRequestIncludingCacheInsteadOfTurnTotals() {
        assertEquals(135L, lastRequestContextTokens("""{"piAssistantMessage":{"usage":{"input":100,"output":5,"cacheRead":20,"cacheWrite":10}},"usage":{"input_tokens":9000,"request_count":8}}"""))
    }
    @Test fun readsLegacySingleRequestPayload() {
        assertEquals(125L, lastRequestContextTokens("""{"usage":{"input_tokens":100,"output_tokens":5,"cached_input_tokens":20}}"""))
    }
    @Test fun doesNotUseAggregatedAgentUsageAsContextSize() {
        assertNull(lastRequestContextTokens("""{"usage":{"input_tokens":9000,"request_count":8}}"""))
        assertNull(lastRequestContextTokens("invalid"))
    }
}
