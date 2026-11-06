package com.android.andex.data

import com.android.andex.ui.theme.andexPalette
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceAndShowcaseTest {
    @Test fun lightCanvasSurvivesDarkModeAndRestart() {
        val pink = AppSettings().withAppearanceColors("moss", "pink")
        val dark = pink.withAppearanceColors("moss", "black")
        assertEquals("pink", dark.themeBackground)
        assertEquals(AppThemeMode.Dark, dark.themeMode)
        assertEquals(Color.Black, andexPalette(true, accent = dark.themeAccent, backgroundColor = dark.themeBackground).background)
        val restored = parseAppSettings(serializeAppSettings(dark)).copy(themeMode = AppThemeMode.Light)
        assertEquals(pink.themeBackground, restored.themeBackground)
        assertEquals(andexPalette(false, accent = "moss", backgroundColor = "pink").background,
            andexPalette(false, accent = restored.themeAccent, backgroundColor = restored.themeBackground).background)
    }

    @Test fun sharedDemoDictionaryRestoresValuesWithoutChangingTypes() {
        val catalog = Json.parseToJsonElement("""{"number":42,"flag":true,"null":null,"items":["__ANDEX_SHOWCASE_STRING_0__","plain"]}""")
        val dictionary = Json.parseToJsonElement("""{"__ANDEX_SHOWCASE_STRING_0__":"full original data"}""").jsonObject
        assertEquals(Json.parseToJsonElement("""{"number":42,"flag":true,"null":null,"items":["full original data","plain"]}"""),
            restoreShowcaseStrings(catalog, dictionary))
    }
}
