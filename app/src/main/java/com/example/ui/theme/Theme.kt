package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.AppStrings
import com.example.core.stringsFor
import com.example.data.prefs.ThemeMode
import com.example.editor.CodeSyntaxPalette

@Immutable
data class GlassTokens(
    val isDark: Boolean,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val orbPrimary: Color,
    val orbSecondary: Color,
    val glassCard: Color,
    val glassCardElevated: Color,
    val glassBorderStart: Color,
    val glassBorderEnd: Color,
    val editorBackground: Color,
    val editorGutter: Color
)

val LocalStrings = staticCompositionLocalOf<AppStrings> { stringsFor(AppLanguage.ENGLISH) }
val LocalCodePalette = staticCompositionLocalOf<CodeSyntaxPalette> { DarkSyntaxPalette }
val LocalGlassTokens = staticCompositionLocalOf<GlassTokens> {
    GlassTokens(
        isDark = true,
        backgroundTop = ObsidianNight,
        backgroundBottom = DeepSapphire,
        orbPrimary = ElectricCyan.copy(alpha = 0.22f),
        orbSecondary = AmethystViolet.copy(alpha = 0.20f),
        glassCard = GlassDarkSurface,
        glassCardElevated = GlassDarkSurfaceElevated,
        glassBorderStart = GlassDarkBorderTop,
        glassBorderEnd = GlassDarkBorderBottom,
        editorBackground = Color(0xFF080F1E),
        editorGutter = Color(0xFF0B1427)
    )
}

private val StudioDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF042135),
    primaryContainer = Color(0xFF0E3A5C),
    onPrimaryContainer = Color(0xFFD0F2FF),
    secondary = AmethystViolet,
    onSecondary = Color(0xFF1F1047),
    secondaryContainer = Color(0xFF2D1D5E),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = EmeraldMint,
    onTertiary = Color(0xFF022C22),
    background = ObsidianNight,
    onBackground = Color(0xFFF1F5F9),
    surface = DeepSapphire,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF16223C),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0x3394A3B8),
    error = CoralRose,
    onError = Color(0xFF450A17)
)

private val StudioLightColorScheme = lightColorScheme(
    primary = CobaltBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EEFF),
    onPrimaryContainer = Color(0xFF002741),
    secondary = DeepIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF24005A),
    tertiary = TealDeep,
    onTertiary = Color.White,
    background = FrostMist,
    onBackground = InkSlate,
    surface = CloudCanvas,
    onSurface = InkSlate,
    surfaceVariant = Color(0xFFE2ECF7),
    onSurfaceVariant = MutedSlate,
    outline = Color(0x33475569),
    error = Color(0xFFE11D48),
    onError = Color.White
)

private val StudioShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MobileCodeStudioTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    language: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (isDark) StudioDarkColorScheme else StudioLightColorScheme
    val codePalette = if (isDark) DarkSyntaxPalette else LightSyntaxPalette
    val glassTokens = if (isDark) {
        GlassTokens(
            isDark = true,
            backgroundTop = ObsidianNight,
            backgroundBottom = Color(0xFF0A1429),
            orbPrimary = ElectricCyan.copy(alpha = 0.22f),
            orbSecondary = AmethystViolet.copy(alpha = 0.18f),
            glassCard = GlassDarkSurface,
            glassCardElevated = GlassDarkSurfaceElevated,
            glassBorderStart = GlassDarkBorderTop,
            glassBorderEnd = GlassDarkBorderBottom,
            editorBackground = Color(0xFF080F1E),
            editorGutter = Color(0xFF0B1427)
        )
    } else {
        GlassTokens(
            isDark = false,
            backgroundTop = FrostMist,
            backgroundBottom = Color(0xFFE2ECF9),
            orbPrimary = CobaltBlue.copy(alpha = 0.16f),
            orbSecondary = DeepIndigo.copy(alpha = 0.12f),
            glassCard = GlassLightSurface,
            glassCardElevated = GlassLightSurfaceElevated,
            glassBorderStart = GlassLightBorderTop,
            glassBorderEnd = GlassLightBorderBottom,
            editorBackground = Color(0xFFFDFEFF),
            editorGutter = Color(0xFFF1F5FA)
        )
    }

    val layoutDirection = if (language == AppLanguage.PERSIAN) LayoutDirection.Rtl else LayoutDirection.Ltr
    val strings = stringsFor(language)

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalCodePalette provides codePalette,
        LocalGlassTokens provides glassTokens,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = appTypographyFor(language),
            shapes = StudioShapes,
            content = content
        )
    }
}
