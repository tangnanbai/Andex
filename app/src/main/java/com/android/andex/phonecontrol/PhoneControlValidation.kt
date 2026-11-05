package com.android.andex.phonecontrol

import org.json.JSONObject

internal fun shellQuote(value: String): String = "'" + value.replace("'", "'\"'\"'") + "'"

internal fun validateAdbEndpoint(value: String): String {
    val endpoint = value.trim()
    val match = Regex("""^(\d{1,3}(?:\.\d{1,3}){3}):(\d{1,5})$""").matchEntire(endpoint)
        ?: error("Use an IPv4 address and port, for example 127.0.0.1:37123.")
    require(match.groupValues[1].split('.').all { it.toInt() in 0..255 }) { "Invalid IPv4 address." }
    require(match.groupValues[2].toInt() in 1..65535) { "Port must be between 1 and 65535." }
    return endpoint
}

internal fun normalizedPixel(value: Double, dimension: Int): Int {
    require(value.isFinite() && value in 0.0..1000.0) { "Coordinates must be in 0..1000." }
    require(dimension > 0) { "Display dimensions are unavailable." }
    return (value / 1000.0 * (dimension - 1)).toInt()
}

internal fun normalizedBound(pixel: Int, dimension: Int): Int {
    require(dimension > 0) { "Display dimensions are unavailable." }
    return kotlin.math.round(pixel.toDouble() / dimension * 1000).toInt().coerceIn(0, 1000)
}

/** Accept historical spellings at the transport boundary, then validate canonical names. */
internal fun canonicalPhoneArguments(source: JSONObject): JSONObject {
    val args = JSONObject(source.toString())
    if (args.optString("action") == "swipe") {
        mapOf(
            "x1" to listOf("start_x", "fromX", "startX"),
            "y1" to listOf("start_y", "fromY", "startY"),
            "x2" to listOf("end_x", "toX", "endX"),
            "y2" to listOf("end_y", "toY", "endY"),
        ).forEach { (key, aliases) ->
            if (!args.has(key)) aliases.firstOrNull(args::has)?.let { args.put(key, args.get(it)) }
        }
    }
    val coordinates = when (args.optString("action")) {
        "tap" -> listOf("x", "y")
        "swipe" -> listOf("x1", "y1", "x2", "y2")
        else -> emptyList()
    }
    val space = args.optString("coordinate_space", "normalized_0_1000")
    require(space in listOf("normalized_0_1000", "pixels")) {
        "coordinate_space must be normalized_0_1000 or pixels."
    }
    coordinates.forEach { key ->
        require(args.has(key) && !args.isNull(key)) {
            "Missing coordinate '$key' for ${args.optString("action")}; required: ${coordinates.joinToString()}."
        }
        require(args.get(key) is Number) { "Coordinate '$key' must be a number." }
        val value = args.getDouble(key)
        require(value.isFinite() && value >= 0 && (space == "pixels" || value <= 1000)) {
            "Coordinate '$key'=$value is outside ${if (space == "pixels") "the pixel range" else "0..1000"}."
        }
    }
    return args
}

internal fun phoneCoordinate(args: JSONObject, key: String, dimension: Int): Int {
    if (args.optString("coordinate_space") != "pixels") return normalizedPixel(args.getDouble(key), dimension)
    val value = args.getDouble(key)
    require(dimension > 0 && value in 0.0..(dimension - 1).toDouble()) {
        "Pixel coordinate '$key'=$value must be in 0..${dimension - 1} for the current display."
    }
    return value.toInt()
}
