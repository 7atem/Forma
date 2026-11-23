package com.formacoach.app

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

internal val FormaColors = darkColorScheme(
    primary = Color(0xFFC8F57B),
    onPrimary = Color(0xFF102412),
    primaryContainer = Color(0xFF233D29),
    onPrimaryContainer = Color(0xFFE3FAC6),
    secondary = Color(0xFFFFBE7B),
    onSecondary = Color(0xFF2D1905),
    // Material maps tonal buttons and selected chips to secondaryContainer, so it stays on-brand.
    // Amber is reserved for explicit accents (protein, records, warm-ups) via accentContainer.
    secondaryContainer = Color(0xFF2A4733),
    onSecondaryContainer = Color(0xFFE3FAC6),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF06281C),
    tertiaryContainer = Color(0xFF153B2E),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = Color(0xFF0B1713),
    onBackground = Color(0xFFF3F8F1),
    surface = Color(0xFF13251F),
    onSurface = Color(0xFFF3F8F1),
    surfaceVariant = Color(0xFF1E372E),
    onSurfaceVariant = Color(0xFFB3C7BA),
    surfaceContainerLowest = Color(0xFF08110E),
    surfaceContainerLow = Color(0xFF101F1A),
    surfaceContainer = Color(0xFF142721),
    surfaceContainerHigh = Color(0xFF1B332B),
    surfaceContainerHighest = Color(0xFF234036),
    outline = Color(0xFF567363),
    outlineVariant = Color(0xFF264338),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0907),
    errorContainer = Color(0xFF5C1814),
    onErrorContainer = Color(0xFFFFDAD6),
    // Snackbars use the inverse roles; without these Material falls back to its default purple.
    inverseSurface = Color(0xFFE3EEE6),
    inverseOnSurface = Color(0xFF142721),
    inversePrimary = Color(0xFF2E6B24),
)

internal val FormaLightColors = lightColorScheme(
    primary = Color(0xFF2E6B24),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9F5C8),
    onPrimaryContainer = Color(0xFF0E2909),
    secondary = Color(0xFFB35B12),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5EBCB),
    onSecondaryContainer = Color(0xFF12301A),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF042F2E),
    background = Color(0xFFF3F7F3),
    onBackground = Color(0xFF112018),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF112018),
    surfaceVariant = Color(0xFFE3ECE5),
    onSurfaceVariant = Color(0xFF42594B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7FAF7),
    surfaceContainer = Color(0xFFEDF3EE),
    surfaceContainerHigh = Color(0xFFE4EDE6),
    surfaceContainerHighest = Color(0xFFDAE6DD),
    outline = Color(0xFF708778),
    outlineVariant = Color(0xFFCAD8CE),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF1B332B),
    inverseOnSurface = Color(0xFFEAF3EC),
    inversePrimary = Color(0xFFC8F57B),
)

/** Amber container for achievement, record and warm-up accents; pair with colorScheme.secondary text. */
internal val ColorScheme.accentContainer: Color
    get() = if (background.luminance() < 0.5f) Color(0xFF3E2D1B) else Color(0xFFFFE4C7)

internal val FormaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

/** Generous line heights for Arabic cursive legibility and tabular numerals for gym-floor metrics. */
internal val FormaTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
    headlineMedium = TextStyle(fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
    headlineSmall = TextStyle(fontSize = 21.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 19.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
)

internal enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    EN("en"),
    AR("ar"),
}

internal object AppLocaleHelper {
    private const val PREFS = "forma_ui_prefs"
    private const val KEY = "app_language"

    fun current(context: Context): AppLanguage {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
        return AppLanguage.entries.firstOrNull { it.tag == saved } ?: AppLanguage.SYSTEM
    }

    fun apply(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language.tag).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(LocaleManager::class.java)
            manager?.applicationLocales = if (language == AppLanguage.SYSTEM) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(language.tag)
            }
        } else {
            (context as? Activity)?.recreate()
        }
    }

    fun wrapContext(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val saved = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
        if (saved.isBlank()) return base
        val locale = Locale.forLanguageTag(saved)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }
}
