package com.android.andex.data

private val HttpHeaderNamePattern = Regex("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$")

val AndexLlmUserAgent: String
    get() = platformDefaultLlmUserAgent()

fun normalizeLlmUserAgent(value: String?): String =
    value?.trim().orEmpty().ifBlank { AndexLlmUserAgent }

fun List<LlmCustomHeader>.normalizedLlmHeaders(): List<LlmCustomHeader> =
    map { header -> LlmCustomHeader(header.name.trim(), header.value) }
        .filter { header ->
            header.name.isNotBlank() &&
                !header.name.equals("User-Agent", ignoreCase = true) &&
                HttpHeaderNamePattern.matches(header.name)
        }
        .distinctBy { header -> header.name.lowercase() }
