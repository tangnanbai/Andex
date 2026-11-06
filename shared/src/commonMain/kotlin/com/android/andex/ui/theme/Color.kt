package com.android.andex.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

data class AndexPalette(
    val background: Color,
    val backgroundGradientTop: Color,
    val settingsBackground: Color,
    val sidebarBackground: Color,
    val sidebarControl: Color,
    val settingsIcon: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val surfaceHigher: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val outlineSoft: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val error: Color,
    val messageBubble: Color,
    val scrim: Color,
)

val LightAndexPalette = AndexPalette(
    background = Color(0xFFFFFFFF),
    backgroundGradientTop = Color(0xFFFFFFFF),
    settingsBackground = Color(0xFFF2F2F7),
    sidebarBackground = Color(0xFFF9F9F9),
    sidebarControl = Color(0xFFF3F3F2),
    settingsIcon = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFF3F3F2),
    surfaceHigher = Color(0xFFECECEC),
    surfaceVariant = Color(0xFFE5E5E5),
    outline = Color(0xFFD9D9D9),
    outlineSoft = Color(0xFFE7E7E7),
    onSurface = Color(0xFF202123),
    onSurfaceVariant = Color(0xFF6B6B6B),
    primary = Color(0xFFAD7BF9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF1E5FF),
    onPrimaryContainer = Color(0xFF4D2F8E),
    secondary = Color(0xFF4A7B6B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDF1E8),
    onSecondaryContainer = Color(0xFF1E4A3B),
    tertiary = Color(0xFF9A7DF8),
    error = Color(0xFFB43E3E),
    messageBubble = Color(0xFFF0E3FF),
    scrim = Color(0x22000000),
)

val DarkAndexPalette = AndexPalette(
    background = Color.Black,
    backgroundGradientTop = Color.Black,
    settingsBackground = Color.Black,
    sidebarBackground = Color.Black,
    sidebarControl = Color(0xFF24282D),
    settingsIcon = Color(0xFFF3F1EC),
    surface = Color(0xFF1C1F23),
    surfaceHigh = Color(0xFF24282D),
    surfaceHigher = Color(0xFF2C3036),
    surfaceVariant = Color(0xFF343941),
    outline = Color(0xFF4A5059),
    outlineSoft = Color(0xFF3D424A),
    onSurface = Color(0xFFF3F1EC),
    onSurfaceVariant = Color(0xFFB9B4AA),
    primary = Color(0xFFC0AEFF),
    onPrimary = Color(0xFF251448),
    primaryContainer = Color(0xFF3A275F),
    onPrimaryContainer = Color(0xFFF0E9FF),
    secondary = Color(0xFF89C8AF),
    onSecondary = Color(0xFF143126),
    secondaryContainer = Color(0xFF24483A),
    onSecondaryContainer = Color(0xFFDDF6EA),
    tertiary = Color(0xFFD1C2FF),
    error = Color(0xFFFF8E8E),
    messageBubble = Color(0xFF32264A),
    scrim = Color(0x66000000),
)

val LightHighContrastAndexPalette = LightAndexPalette.copy(
    outline = Color(0xFFA39E95),
    outlineSoft = Color(0xFFBBB6AC),
    onSurface = Color(0xFF111214),
    onSurfaceVariant = Color(0xFF514D46),
    primary = Color(0xFF6545D6),
    primaryContainer = Color(0xFFE7D5FF),
    onPrimaryContainer = Color(0xFF35186F),
    error = Color(0xFFB43E3E),
)

val DarkHighContrastAndexPalette = DarkAndexPalette.copy(
    outline = Color(0xFF777F89),
    outlineSoft = Color(0xFF656C75),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFD2CCC1),
    primary = Color(0xFFD5C8FF),
    primaryContainer = Color(0xFF4C3378),
    onPrimaryContainer = Color(0xFFFFFFFF),
    error = Color(0xFFFFB0B0),
)

private var currentPalette by mutableStateOf(LightAndexPalette)

/** Colors transcribed from the supplied reference artwork (not compressed screen pixels). */
data class ThemeAccentOption(val id: String, val label: String, val primary: Color, val secondary: Color)
val ThemeAccentOptions = listOf(
    ThemeAccentOption("pink-mint", "桃绯 · 薄荷", Color(0xFFF35B83), Color(0xFF8AEED1)),
    ThemeAccentOption("mint-pink", "薄荷 · 桃绯", Color(0xFF8AEED1), Color(0xFFF35B83)),
    ThemeAccentOption("peach-moss", "裸桃 · 苔青", Color(0xFFFACBC6), Color(0xFF465C47)),
    ThemeAccentOption("moss-peach", "苔青 · 裸桃", Color(0xFF465C47), Color(0xFFFACBC6)),
    ThemeAccentOption("purple-sakura", "雾堇 · 樱粉", Color(0xFF615EDE), Color(0xFFFF7BBF)),
    ThemeAccentOption("sakura-purple", "樱粉 · 雾堇", Color(0xFFFF7BBF), Color(0xFF615EDE)),
    ThemeAccentOption("pink-black", "胭脂 · 墨黑", Color(0xFFFF7CA2), Color.Black),
    ThemeAccentOption("pink-white", "胭脂 · 纯白", Color(0xFFFF7CA2), Color.White),
)

data class ThemeColorOption(val id: String, val label: String, val color: Color)
val ThemeColorOptions = listOf(
    ThemeColorOption("rose", "桃绯", Color(0xFFF35B83)),
    ThemeColorOption("mint", "薄荷", Color(0xFF8AEED1)),
    ThemeColorOption("peach", "裸桃", Color(0xFFFACBC6)),
    ThemeColorOption("moss", "苔青", Color(0xFF465C47)),
    ThemeColorOption("purple", "雾堇", Color(0xFF615EDE)),
    ThemeColorOption("sakura", "樱粉", Color(0xFFFF7BBF)),
    ThemeColorOption("pink", "胭脂", Color(0xFFFF7CA2)),
    ThemeColorOption("black", "黑色", Color.Black),
    ThemeColorOption("white", "白色", Color.White),
)

fun resolvedThemeColor(accent: String, darkTheme: Boolean): ThemeColorOption {
    ThemeColorOptions.firstOrNull { it.id == accent }?.let { return it }
    val legacy = resolvedThemeAccent(accent, darkTheme)
    return ThemeColorOptions.first { it.color == legacy.primary }
}

/** Preserve installed two-color themes until either independent choice is changed. */
fun resolvedBackgroundColor(background: String, accent: String, darkTheme: Boolean): ThemeColorOption {
    if (darkTheme) return ThemeColorOptions.first { it.id == "black" }
    ThemeColorOptions.firstOrNull { it.id == background && it.id != "black" }?.let { return it }
    val legacy = ThemeAccentOptions.firstOrNull { it.id == accent }
    val color = legacy?.secondary?.takeUnless { it == Color.Black } ?: Color.White
    return ThemeColorOptions.first { it.color == color }
}

fun resolvedThemeAccent(accent: String, darkTheme: Boolean): ThemeAccentOption =
    ThemeAccentOptions.firstOrNull { it.id == accent }
        ?: ThemeAccentOptions.first { it.id == if (darkTheme) "pink-black" else "pink-white" }

/** Choose a readable label without changing the reference fill color. */
fun themeForeground(fill: Color): Color =
    if (fill.luminance() > 0.179f) Color.Black else Color.White

private fun contrastRatio(a: Color, b: Color): Float =
    (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)

private fun readableSecondaryText(background: Color, foreground: Color, increasedContrast: Boolean, surfaces: List<Color>): Color {
    var amount = if (increasedContrast) 0.96f else 0.72f
    while (true) {
        val text = androidx.compose.ui.graphics.lerp(background, foreground, amount)
        if ((surfaces.all { contrastRatio(it, text) >= 4.5f }) || amount >= 1f) return text
        amount = (amount + 0.04f).coerceAtMost(1f)
    }
}

/** Retain the accent hue in UI text/icons, adjusting its tone only for legibility. */
private fun themeInk(primary: Color, background: Color, surfaces: List<Color>): Color {
    val foreground = themeForeground(background)
    var amount = 0f
    while (true) {
        val ink = androidx.compose.ui.graphics.lerp(primary, foreground, amount)
        if ((contrastRatio(background, ink) >= 5.8f && surfaces.all { contrastRatio(it, ink) >= 4.5f }) || amount >= 1f) return ink
        amount = (amount + .025f).coerceAtMost(1f)
    }
}

fun andexPalette(darkTheme: Boolean, increasedContrast: Boolean = false, accent: String = "auto", backgroundColor: String = "auto"): AndexPalette {
    val base = when {
        darkTheme && increasedContrast -> DarkHighContrastAndexPalette
        darkTheme -> DarkAndexPalette
        increasedContrast -> LightHighContrastAndexPalette
        else -> LightAndexPalette
    }
    val choice = resolvedThemeColor(accent, darkTheme)
    // The second reference color is the canvas, not another action/accent color.
    val background = resolvedBackgroundColor(backgroundColor, accent, darkTheme).color
    val foreground = themeForeground(background)
    val darkCanvas = foreground == Color.White
    val lift = when {
        background == Color.Black -> Color.White
        background == Color.White -> Color.Black
        darkCanvas -> Color.Black
        else -> Color.White
    }
    fun surface(amount: Float) = androidx.compose.ui.graphics.lerp(background, lift, amount)
    val surfaces = listOf(background, surface(0.025f), surface(0.065f), surface(0.10f), surface(0.14f))
    val ink = if (increasedContrast) foreground else themeInk(choice.color, background, surfaces)
    val container = androidx.compose.ui.graphics.lerp(background, choice.color, 0.20f)
    val surface = surface(if (darkCanvas) 0.065f else 0.025f)
    return base.copy(
        background = background, backgroundGradientTop = background,
        settingsBackground = background, sidebarBackground = surface(0.025f),
        sidebarControl = surface(0.065f), settingsIcon = ink,
        surface = surface, surfaceHigh = surface(0.06f),
        surfaceHigher = surface(0.10f), surfaceVariant = surface(0.14f),
        onSurface = ink,
        onSurfaceVariant = readableSecondaryText(background, ink, increasedContrast, surfaces),
        outline = androidx.compose.ui.graphics.lerp(background, ink, if (increasedContrast) 0.85f else 0.72f),
        outlineSoft = androidx.compose.ui.graphics.lerp(background, ink, if (increasedContrast) 0.60f else 0.28f),
        primary = choice.color, onPrimary = themeForeground(choice.color),
        primaryContainer = container, onPrimaryContainer = themeForeground(container),
        secondary = choice.color, onSecondary = themeForeground(choice.color),
        secondaryContainer = container, onSecondaryContainer = themeForeground(container),
        tertiary = choice.color, messageBubble = container,
        error = if (darkCanvas) Color(0xFFFFB4AB) else Color(0xFF9B1C20),
        scrim = Color.Black.copy(alpha = if (darkCanvas) 0.40f else 0.18f),
    )
}

/** Supply every Material role from the same palette so native controls update with the canvas. */
fun andexMaterialColors(palette: AndexPalette): androidx.compose.material3.ColorScheme {
    val base = if (palette.background.luminance() < 0.179f) {
        androidx.compose.material3.darkColorScheme()
    } else androidx.compose.material3.lightColorScheme()
    return base.copy(
        primary = palette.onSurface, onPrimary = themeForeground(palette.onSurface),
        primaryContainer = palette.primaryContainer, onPrimaryContainer = palette.onPrimaryContainer,
        secondary = palette.onSurface, onSecondary = themeForeground(palette.onSurface),
        secondaryContainer = palette.secondaryContainer, onSecondaryContainer = palette.onSecondaryContainer,
        tertiary = palette.onSurface, onTertiary = themeForeground(palette.onSurface),
        tertiaryContainer = palette.primaryContainer, onTertiaryContainer = palette.onPrimaryContainer,
        background = palette.background, onBackground = palette.onSurface,
        surface = palette.surface, onSurface = palette.onSurface, surfaceTint = palette.primary,
        surfaceVariant = palette.surfaceVariant, onSurfaceVariant = palette.onSurfaceVariant,
        surfaceDim = palette.background, surfaceBright = palette.surfaceHigh,
        surfaceContainerLowest = palette.background, surfaceContainerLow = palette.surface,
        surfaceContainer = palette.surfaceHigh, surfaceContainerHigh = palette.surfaceHigher,
        surfaceContainerHighest = palette.surfaceVariant,
        outline = palette.outline, outlineVariant = palette.outlineSoft,
        error = palette.error, scrim = Color.Black,
    )
}

fun updateAndexPalette(darkTheme: Boolean, increasedContrast: Boolean = false, accent: String = "auto", backgroundColor: String = "auto") {
    val palette = andexPalette(darkTheme, increasedContrast, accent, backgroundColor)
    if (currentPalette != palette) currentPalette = palette
}

val AndexBackground: Color
    get() = currentPalette.background

val AndexBackgroundGradientTop: Color
    get() = currentPalette.backgroundGradientTop

val AndexSettingsBackground: Color
    get() = currentPalette.settingsBackground

val AndexSidebarBackground: Color
    get() = currentPalette.sidebarBackground

val AndexSidebarControl: Color
    get() = currentPalette.sidebarControl

val AndexSettingsIcon: Color
    get() = currentPalette.settingsIcon

val AndexSurface: Color
    get() = currentPalette.surface

val AndexSurfaceHigh: Color
    get() = currentPalette.surfaceHigh

val AndexSurfaceHigher: Color
    get() = currentPalette.surfaceHigher

val AndexSurfaceVariant: Color
    get() = currentPalette.surfaceVariant

val AndexOutline: Color
    get() = currentPalette.outline

val AndexOutlineSoft: Color
    get() = currentPalette.outlineSoft

val AndexOnSurface: Color
    get() = currentPalette.onSurface

/** Long-form AI content stays neutral rather than tinting a whole page of text. */
val AndexContentText: Color
    get() = themeForeground(currentPalette.background)

val AndexOnSurfaceVariant: Color
    get() = currentPalette.onSurfaceVariant

val AndexPrimary: Color
    get() = currentPalette.primary

val AndexOnPrimary: Color
    get() = currentPalette.onPrimary

val AndexPrimaryContainer: Color
    get() = currentPalette.primaryContainer

val AndexOnPrimaryContainer: Color
    get() = currentPalette.onPrimaryContainer

val AndexSecondary: Color
    get() = currentPalette.secondary

val AndexOnSecondary: Color
    get() = currentPalette.onSecondary

val AndexSecondaryContainer: Color
    get() = currentPalette.secondaryContainer

val AndexOnSecondaryContainer: Color
    get() = currentPalette.onSecondaryContainer

val AndexTertiary: Color
    get() = currentPalette.tertiary

val AndexError: Color
    get() = currentPalette.error

val AndexMessageBubble: Color
    get() = currentPalette.messageBubble

val AndexScrim: Color
    get() = currentPalette.scrim
