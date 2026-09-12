package com.example.ui.language

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

@Composable
fun LanguageProvider(language: AppLanguage, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val configuration = LocalConfiguration.current
    val localized = remember(base, configuration, language) {
        ContextThemeWrapper(base, 0).apply {
            applyOverrideConfiguration(Configuration(configuration).apply {
                setLocale(Locale(language.code))
                setLayoutDirection(Locale(language.code))
            })
        }
    }
    CompositionLocalProvider(LocalAppLanguage provides language, LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
