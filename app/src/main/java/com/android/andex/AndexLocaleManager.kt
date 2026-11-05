package com.android.andex

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.android.andex.data.AppLanguage
import com.android.andex.data.appLanguageForTag
import com.android.andex.data.defaultAppLanguage

object AndexLocaleManager {
    fun apply(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.languageTag),
        )
    }

    fun applyIfChanged(language: AppLanguage) {
        if (currentApplicationLanguage() == language) return
        apply(language)
    }

    fun currentApplicationLanguage(): AppLanguage? =
        AppCompatDelegate.getApplicationLocales().get(0)
            ?.toLanguageTag()
            ?.let(::appLanguageForTag)

    fun currentLanguage(): AppLanguage =
        currentApplicationLanguage() ?: defaultAppLanguage()
}
