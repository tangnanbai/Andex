package com.android.andex.data

/** Decode common Markdown encodings without silently corrupting imported notes. */
fun decodeMemoryMarkdown(bytes: ByteArray): String {
    val littleEndian = bytes.size >= 2 && bytes[0] == 0xff.toByte() && bytes[1] == 0xfe.toByte()
    val bigEndian = bytes.size >= 2 && bytes[0] == 0xfe.toByte() && bytes[1] == 0xff.toByte()
    if (!littleEndian && !bigEndian) return try {
        bytes.decodeToString(throwOnInvalidSequence = true).removePrefix("\uFEFF")
    } catch (_: CharacterCodingException) {
        error("文件编码无法识别，请保存为 UTF-8 或带 BOM 的 UTF-16 Markdown 后导入。")
    }
    require(bytes.size % 2 == 0) { "UTF-16 文件不完整，请重新导出后导入。" }
    return buildString((bytes.size - 2) / 2) {
        for (index in 2 until bytes.size step 2) {
            val first = bytes[index].toInt() and 255
            val second = bytes[index + 1].toInt() and 255
            append((if (littleEndian) first or (second shl 8) else (first shl 8) or second).toChar())
        }
    }
}
