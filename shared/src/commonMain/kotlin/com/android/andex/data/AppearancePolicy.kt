package com.android.andex.data

/** Black is dark mode; the stored canvas remembers the last non-black choice. */
fun AppSettings.withAppearanceColors(accent: String, background: String): AppSettings = copy(
    themeAccent = accent,
    themeBackground = if (background == "black") themeBackground.takeUnless { it == "black" } ?: "white" else background,
    themeMode = if (background == "black") AppThemeMode.Dark else AppThemeMode.Light,
)
