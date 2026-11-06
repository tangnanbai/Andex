package com.android.andex.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.sp
import com.android.andex.data.AppLanguage
import com.android.andex.ui.theme.DarkAndexPalette
import com.android.andex.ui.theme.DarkHighContrastAndexPalette
import com.android.andex.ui.theme.LightAndexPalette
import com.android.andex.ui.theme.LightHighContrastAndexPalette
import com.android.andex.ui.theme.updateAndexPalette
import com.android.andex.data.AppThemeMode
import com.android.andex.platform.LocalReduceMotion
import com.android.andex.platform.rememberPlatformAccessibilityPreferences

private fun andexLightColors(palette: com.android.andex.ui.theme.AndexPalette) = lightColorScheme(
    primary = palette.primary,
    onPrimary = palette.onPrimary,
    primaryContainer = palette.primaryContainer,
    onPrimaryContainer = palette.onPrimaryContainer,
    secondary = palette.secondary,
    onSecondary = palette.onSecondary,
    secondaryContainer = palette.secondaryContainer,
    onSecondaryContainer = palette.onSecondaryContainer,
    background = palette.background,
    surface = palette.surface,
    surfaceVariant = palette.surfaceVariant,
    surfaceContainerHighest = palette.surfaceVariant,
    onSurface = palette.onSurface,
    onSurfaceVariant = palette.onSurfaceVariant,
    tertiary = palette.tertiary,
    error = palette.error,
    outline = palette.outline,
)

private fun andexDarkColors(palette: com.android.andex.ui.theme.AndexPalette) = darkColorScheme(
    primary = palette.primary,
    onPrimary = palette.onPrimary,
    primaryContainer = palette.primaryContainer,
    onPrimaryContainer = palette.onPrimaryContainer,
    secondary = palette.secondary,
    onSecondary = palette.onSecondary,
    secondaryContainer = palette.secondaryContainer,
    onSecondaryContainer = palette.onSecondaryContainer,
    background = palette.background,
    surface = palette.surface,
    surfaceVariant = palette.surfaceVariant,
    surfaceContainerHighest = palette.surfaceVariant,
    onSurface = palette.onSurface,
    onSurfaceVariant = palette.onSurfaceVariant,
    tertiary = palette.tertiary,
    error = palette.error,
    outline = palette.outline,
)

private val lightColors = andexLightColors(LightAndexPalette)
private val lightHighContrastColors = andexLightColors(LightHighContrastAndexPalette)
private val darkHighContrastColors = andexDarkColors(DarkHighContrastAndexPalette)

private val darkColors = andexDarkColors(DarkAndexPalette)

private val typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.9).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 29.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 31.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 25.sp,
    ),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)

@Composable
internal fun SharedAndexTheme(
    themeMode: AppThemeMode = AppThemeMode.System,
    themeAccent: String = "auto",
    themeBackground: String = "auto",
    language: AppLanguage = AppLanguage.English,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.System -> isSystemInDarkTheme()
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    }
    val accessibility = rememberPlatformAccessibilityPreferences()
    updateAndexPalette(darkTheme, accessibility.increasedContrast, themeAccent, themeBackground)
    val controller = androidx.compose.ui.uikit.LocalUIViewController.current
    val canvas = com.android.andex.ui.theme.AndexBackground
    SideEffect {
        val background = platform.UIKit.UIColor(red = canvas.red.toDouble(), green = canvas.green.toDouble(),
            blue = canvas.blue.toDouble(), alpha = 1.0)
        controller.view.backgroundColor = background
        var parent = controller.parentViewController
        while (parent != null) {
            parent.view.backgroundColor = background
            parent = parent.parentViewController
        }
        controller.view.window?.backgroundColor = background
    }
    val layoutDirection = if (language == AppLanguage.Persian) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
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
            content = { AndexGlassHost(accessibility.increasedContrast, content) },
        )
    }
}
