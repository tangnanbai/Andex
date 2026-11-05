package com.android.andex.phonecontrol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

class PhoneControlValidationTest {
    @Test fun acceptsAnExplicitIpv4Transport() {
        assertEquals("127.0.0.1:37123", validateAdbEndpoint(" 127.0.0.1:37123 "))
        assertEquals("192.168.1.5:65535", validateAdbEndpoint("192.168.1.5:65535"))
    }

    @Test fun rejectsInvalidOrInjectedEndpoints() {
        listOf("", "host:123", "127.0.0.1", "256.1.1.1:123", "1.1.1.1:0", "1.1.1.1:65536",
            "127.0.0.1:123;id", "127.0.0.1:123\nid", "[::1]:123",
        ).forEach { assertTrue("Accepted $it", runCatching { validateAdbEndpoint(it) }.isFailure) }
    }

    @Test fun quotesShellArgumentsWithoutInterpolation() {
        assertEquals("'a'\"'\"'b'", shellQuote("a'b"))
        assertEquals("'\$(id); `id`'", shellQuote("\$(id); `id`"))
        assertEquals("''", shellQuote(""))
    }

    @Test fun mapsNormalizedCoordinatesToTheLastValidPixel() {
        assertEquals(0, normalizedPixel(0.0, 1080))
        assertEquals(1079, normalizedPixel(1000.0, 1080))
        assertEquals(539, normalizedPixel(500.0, 1080))
        assertEquals(0, normalizedPixel(1000.0, 1))
    }

    @Test fun rejectsOutOfBoundsAndNonFiniteCoordinates() {
        listOf(-1.0, 1000.1, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertTrue(runCatching { normalizedPixel(it, 1080) }.isFailure)
        }
        assertTrue(runCatching { normalizedPixel(0.0, 0) }.isFailure)
    }

    @Test fun snapshotBoundsAreNormalizedAndClampedToTheSameScreenSpace() {
        assertEquals(349, normalizedBound(502, 1440))
        assertEquals(172, normalizedBound(551, 3200))
        assertEquals(958, normalizedBound(1380, 1440))
        assertEquals(1000, normalizedBound(3200, 3200))
        assertEquals(0, normalizedBound(-10, 1440))
        assertEquals(1000, normalizedBound(4000, 3200))
    }

    @Test fun swipeAliasesAndPixelCoordinatesWorkWithoutManualConversion() {
        val aliases = listOf(
            listOf("start_x", "start_y", "end_x", "end_y"),
            listOf("fromX", "fromY", "toX", "toY"),
        )
        aliases.forEach { names ->
            val args = JSONObject().put("action", "swipe")
            names.zip(listOf(500, 800, 500, 200)).forEach { (name, value) -> args.put(name, value) }
            val parsed = canonicalPhoneArguments(args)
            assertEquals(800, parsed.getInt("y1"))
            assertEquals(200, parsed.getInt("y2"))
            assertEquals(2559, phoneCoordinate(parsed, "y1", 3200))
        }
        val pixels = canonicalPhoneArguments(JSONObject(
            """{"action":"tap","coordinate_space":"pixels","x":502,"y":551}"""))
        assertEquals(502, phoneCoordinate(pixels, "x", 1440))
        assertEquals(551, phoneCoordinate(pixels, "y", 3200))
    }

    @Test fun wrongParameterNamesReportMissingNamesBeforeValues() {
        val error = runCatching { canonicalPhoneArguments(JSONObject(
            """{"action":"swipe","foo_x":500,"foo_y":800}""")) }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("Missing coordinate 'x1'"))
        assertTrue(error?.message.orEmpty().contains("x1, y1, x2, y2"))
        val invalid = runCatching { canonicalPhoneArguments(JSONObject(
            """{"action":"tap","x":1500,"y":500}""")) }.exceptionOrNull()
        assertTrue(invalid?.message.orEmpty().contains("'x'=1500"))
        assertTrue(runCatching { canonicalPhoneArguments(JSONObject(
            """{"action":"tap","x":"500","y":500}""")) }.isFailure)
        val pixels = canonicalPhoneArguments(JSONObject(
            """{"action":"tap","coordinate_space":"pixels","x":1440,"y":551}"""))
        assertTrue(runCatching { phoneCoordinate(pixels, "x", 1440) }.isFailure)
    }
}
