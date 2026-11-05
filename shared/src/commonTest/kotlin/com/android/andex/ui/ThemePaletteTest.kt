package com.android.andex.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.android.andex.ui.theme.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemePaletteTest {
    @Test fun referencePairsAndReversedPairsAreExact() {
        assertEquals(8, ThemeAccentOptions.size)
        assertEquals(8, ThemeAccentOptions.map { it.id }.toSet().size)
        for (i in listOf(0, 2, 4)) {
            assertEquals(ThemeAccentOptions[i].primary, ThemeAccentOptions[i + 1].secondary)
            assertEquals(ThemeAccentOptions[i].secondary, ThemeAccentOptions[i + 1].primary)
        }
        assertEquals(Color(0xFFF35B83), ThemeAccentOptions[0].primary)
        assertEquals(Color(0xFF8AEED1), ThemeAccentOptions[0].secondary)
        assertEquals(Color(0xFFFACBC6), ThemeAccentOptions[2].primary)
        assertEquals(Color(0xFF465C47), ThemeAccentOptions[2].secondary)
        assertEquals(Color(0xFF615EDE), ThemeAccentOptions[4].primary)
        assertEquals(Color(0xFFFF7BBF), ThemeAccentOptions[4].secondary)
    }

    @Test fun defaultsFollowAppearanceAndOldAccentsFallBackSafely() {
        for (stored in listOf("auto", "purple", "blue", "", "future-theme")) {
            assertEquals("pink-white", resolvedThemeAccent(stored, false).id)
            assertEquals("pink-black", resolvedThemeAccent(stored, true).id)
        }
        assertEquals(Color(0xFFFF7CA2), andexPalette(false).primary)
        assertEquals(Color.White, andexPalette(false).background)
        assertEquals(Color.Black, andexPalette(true).background)
        assertEquals("mint-pink", resolvedThemeAccent("mint-pink", true).id)
    }

    @Test fun bothColorsApplyAndLabelsHaveReadableContrast() {
        for (choice in ThemeAccentOptions) for (dark in listOf(false, true)) for (contrast in listOf(false, true)) {
            val palette = andexPalette(dark, increasedContrast = contrast, accent = choice.id)
            assertEquals(choice.primary, palette.primary)
            assertEquals(choice.secondary, palette.background)
            assertEquals(choice.primary, palette.secondary)
            for ((fill, label) in listOf(
                palette.primary to palette.onPrimary,
                palette.secondary to palette.onSecondary,
                palette.primaryContainer to palette.onPrimaryContainer,
                palette.secondaryContainer to palette.onSecondaryContainer,
                palette.background to palette.onSurface,
                palette.surface to palette.onSurface,
                palette.surfaceHigh to palette.onSurface,
                palette.background to palette.onSurfaceVariant,
            )) {
                val a = fill.luminance()
                val b = label.luminance()
                assertTrue((maxOf(a, b) + .05f) / (minOf(a, b) + .05f) >= 4.5f,
                    "${choice.id} dark=$dark fill=$fill label=$label")
            }
            val material = andexMaterialColors(palette)
            assertEquals(choice.secondary, material.background)
            assertEquals(palette.surface, material.surface)
            assertEquals(palette.onSurface, material.primary)
            assertEquals(palette.onSurface, material.onBackground)
        }
    }

    @Test fun independentColorsRemainReadableIncludingMatchingBlackAndWhite() {
        fun ratio(a: Color, b: Color): Float {
            val x = a.luminance(); val y = b.luminance()
            return (maxOf(x, y) + .05f) / (minOf(x, y) + .05f)
        }
        for (accent in ThemeColorOptions) for (background in ThemeColorOptions) for (dark in listOf(false, true)) {
            val palette = andexPalette(dark, accent = accent.id, backgroundColor = background.id)
            assertEquals(accent.color, palette.primary)
            assertEquals(background.color, palette.background)
            val material = andexMaterialColors(palette)
            for (surface in listOf(palette.background, palette.surface, palette.surfaceHigh, palette.surfaceHigher, palette.surfaceVariant)) {
                assertTrue(ratio(surface, palette.onSurface) >= 4.5f, "${accent.id}/${background.id} text")
                assertTrue(ratio(surface, material.primary) >= 4.5f, "${accent.id}/${background.id} button text")
            }
            assertTrue(ratio(material.primary, material.onPrimary) >= 4.5f)
            assertTrue(ratio(palette.primary, palette.onPrimary) >= 4.5f)
            assertTrue(ratio(palette.background, palette.onSurfaceVariant) >= 4.5f)
        }
    }

    @Test fun choosingOneColorDoesNotChangeTheOtherAndOldPairsStillResolve() {
        for (pair in ThemeAccentOptions) {
            assertEquals(pair.primary, resolvedThemeColor(pair.id, false).color)
            assertEquals(pair.secondary, resolvedBackgroundColor("auto", pair.id, false).color)
        }
        assertEquals(Color.Black, andexPalette(false, accent = "white", backgroundColor = "black").background)
        assertEquals(Color.White, andexPalette(true, accent = "black", backgroundColor = "white").background)
    }
}
