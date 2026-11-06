package com.android.andex.data

data class ModelMentionQuery(val start: Int, val end: Int, val query: String)

fun modelMentionQuery(text: String, cursor: Int): ModelMentionQuery? {
    if (cursor !in 0..text.length) return null
    val before = text.take(cursor)
    val start = before.lastIndexOf('@')
    if (start < 0) return null
    if (start > 0 && ((before[start - 1].code < 128 && before[start - 1].isLetterOrDigit()) || before[start - 1] in "._-")) return null
    val query = before.substring(start + 1)
    if (query.any { it.isWhitespace() || it in "@「」" }) return null
    return ModelMentionQuery(start, cursor, query)
}

fun ProviderModelOption.mentionToken(): String = "@「$providerName / $modelId」"

private val mentionPattern = Regex("@「[^」\\r\\n]+」")

/** Resolve against enabled configs, never silently route an unknown mention elsewhere. */
fun resolveModelMention(text: String, options: List<ProviderModelOption>): ProviderModelOption? {
    val tokens = mentionPattern.findAll(text).map { it.value }.distinct().toList()
    if (tokens.isEmpty()) return null
    require(tokens.size == 1) { "每条消息请选择一个模型；需要多个模型时请分别发送。" }
    val matches = options.filter { it.mentionToken() == tokens.single() }
    require(matches.size == 1) { "指定模型不可用或渠道名称重复，请重新通过 @ 选择模型。" }
    return matches.single()
}

fun stripModelMentions(text: String): String = mentionPattern.replace(text, "").trim()

fun isImageGenerationModel(modelId: String): Boolean {
    val name = modelId.lowercase()
    return name.contains("gpt-image") || name.contains("dall-e") ||
        name.contains("nano-banana") || name.contains("nanobanana") ||
        (name.contains("gemini") && name.contains("image")) || name.startsWith("imagen-")
}
