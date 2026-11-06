package com.android.andex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import com.android.andex.R
import com.android.andex.data.AppLanguage
import com.android.andex.data.AppThemeMode
import com.android.andex.platform.LocalReduceMotion
import com.android.andex.platform.rememberPlatformAccessibilityPreferences

private val LightHighContrastAndexColors = lightColorScheme(
    primary = LightHighContrastAndexPalette.primary,
    onPrimary = LightHighContrastAndexPalette.onPrimary,
    primaryContainer = LightHighContrastAndexPalette.primaryContainer,
    onPrimaryContainer = LightHighContrastAndexPalette.onPrimaryContainer,
    secondary = LightHighContrastAndexPalette.secondary,
    onSecondary = LightHighContrastAndexPalette.onSecondary,
    secondaryContainer = LightHighContrastAndexPalette.secondaryContainer,
    onSecondaryContainer = LightHighContrastAndexPalette.onSecondaryContainer,
    background = LightHighContrastAndexPalette.background,
    surface = LightHighContrastAndexPalette.surface,
    surfaceVariant = LightHighContrastAndexPalette.surfaceVariant,
    surfaceContainerHighest = LightHighContrastAndexPalette.surfaceVariant,
    onSurface = LightHighContrastAndexPalette.onSurface,
    onSurfaceVariant = LightHighContrastAndexPalette.onSurfaceVariant,
    tertiary = LightHighContrastAndexPalette.tertiary,
    error = LightHighContrastAndexPalette.error,
    outline = LightHighContrastAndexPalette.outline,
)

private val DarkHighContrastAndexColors = darkColorScheme(
    primary = DarkHighContrastAndexPalette.primary,
    onPrimary = DarkHighContrastAndexPalette.onPrimary,
    primaryContainer = DarkHighContrastAndexPalette.primaryContainer,
    onPrimaryContainer = DarkHighContrastAndexPalette.onPrimaryContainer,
    secondary = DarkHighContrastAndexPalette.secondary,
    onSecondary = DarkHighContrastAndexPalette.onSecondary,
    secondaryContainer = DarkHighContrastAndexPalette.secondaryContainer,
    onSecondaryContainer = DarkHighContrastAndexPalette.onSecondaryContainer,
    background = DarkHighContrastAndexPalette.background,
    surface = DarkHighContrastAndexPalette.surface,
    surfaceVariant = DarkHighContrastAndexPalette.surfaceVariant,
    surfaceContainerHighest = DarkHighContrastAndexPalette.surfaceVariant,
    onSurface = DarkHighContrastAndexPalette.onSurface,
    onSurfaceVariant = DarkHighContrastAndexPalette.onSurfaceVariant,
    tertiary = DarkHighContrastAndexPalette.tertiary,
    error = DarkHighContrastAndexPalette.error,
    outline = DarkHighContrastAndexPalette.outline,
)

private val LightAndexColors = lightColorScheme(
    primary = LightAndexPalette.primary,
    onPrimary = LightAndexPalette.onPrimary,
    primaryContainer = LightAndexPalette.primaryContainer,
    onPrimaryContainer = LightAndexPalette.onPrimaryContainer,
    secondary = LightAndexPalette.secondary,
    onSecondary = LightAndexPalette.onSecondary,
    secondaryContainer = LightAndexPalette.secondaryContainer,
    onSecondaryContainer = LightAndexPalette.onSecondaryContainer,
    background = LightAndexPalette.background,
    surface = LightAndexPalette.surface,
    surfaceVariant = LightAndexPalette.surfaceVariant,
    surfaceContainerHighest = LightAndexPalette.surfaceVariant,
    onSurface = LightAndexPalette.onSurface,
    onSurfaceVariant = LightAndexPalette.onSurfaceVariant,
    tertiary = LightAndexPalette.tertiary,
    error = LightAndexPalette.error,
    outline = LightAndexPalette.outline,
)

private val DarkAndexColors = darkColorScheme(
    primary = DarkAndexPalette.primary,
    onPrimary = DarkAndexPalette.onPrimary,
    primaryContainer = DarkAndexPalette.primaryContainer,
    onPrimaryContainer = DarkAndexPalette.onPrimaryContainer,
    secondary = DarkAndexPalette.secondary,
    onSecondary = DarkAndexPalette.onSecondary,
    secondaryContainer = DarkAndexPalette.secondaryContainer,
    onSecondaryContainer = DarkAndexPalette.onSecondaryContainer,
    background = DarkAndexPalette.background,
    surface = DarkAndexPalette.surface,
    surfaceVariant = DarkAndexPalette.surfaceVariant,
    surfaceContainerHighest = DarkAndexPalette.surfaceVariant,
    onSurface = DarkAndexPalette.onSurface,
    onSurfaceVariant = DarkAndexPalette.onSurfaceVariant,
    tertiary = DarkAndexPalette.tertiary,
    error = DarkAndexPalette.error,
    outline = DarkAndexPalette.outline,
)

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private fun getAndexTypography(fontFamily: FontFamily) = Typography(
    headlineLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.9).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 29.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 31.sp
    ),
    titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 25.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 28.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
)

@Composable
fun AndexTheme(
    themeMode: AppThemeMode = AppThemeMode.System,
    themeAccent: String = "auto",
    themeBackground: String = "auto",
    language: AppLanguage = AppLanguage.English,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.System -> isSystemInDarkTheme()
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    }
    val accessibility = rememberPlatformAccessibilityPreferences()
    val view = LocalView.current
    updateAndexPalette(darkTheme, accessibility.increasedContrast, themeAccent, themeBackground)
    SideEffect {
        if (!view.isInEditMode) {
            var context = view.context
            while (context is android.content.ContextWrapper && context !is android.app.Activity) {
                val next = context.baseContext
                if (next === context) break
                context = next
            }
            (context as? android.app.Activity)?.let { activity ->
                val color = AndexBackground.toArgb()
                activity.window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(color))
                activity.window.decorView.setBackgroundColor(color)
                val preferences = activity.getSharedPreferences("window_appearance", android.content.Context.MODE_PRIVATE)
                if (preferences.getInt("background", 0) != color) preferences.edit().putInt("background", color).apply()
                androidx.core.view.WindowCompat.getInsetsController(activity.window, view).apply {
                    isAppearanceLightStatusBars = themeForeground(AndexBackground) == androidx.compose.ui.graphics.Color.Black
                    isAppearanceLightNavigationBars = themeForeground(AndexBackground) == androidx.compose.ui.graphics.Color.Black
                }
            }
        }
    }
    val currentFontFamily = if (language == AppLanguage.Persian) {
        VazirmatnFontFamily
    } else {
        FontFamily.SansSerif
    }
    val layoutDirection = if (language == AppLanguage.Persian) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    val typography = remember(currentFontFamily) {
        getAndexTypography(currentFontFamily)
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalReduceMotion provides accessibility.reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = com.android.andex.ui.theme.andexMaterialColors(
                com.android.andex.ui.theme.andexPalette(darkTheme, accessibility.increasedContrast, themeAccent, themeBackground),
            ),
            typography = typography,
            content = {
                com.android.andex.ui.AndexGlassHost(accessibility.increasedContrast, content)
            }
        )
    }
}
