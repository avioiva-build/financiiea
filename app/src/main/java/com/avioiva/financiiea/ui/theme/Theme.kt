package com.avioiva.financiiea.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.avioiva.financiiea.AppContainer
import com.avioiva.financiiea.data.SettingsRepository

// ---- Brand palette (from the FINANCIIEA logo: navy, teal, gold) ----
val NavyDeep = Color(0xFF0A1428)
val NavyBackground = Color(0xFF0B1626)
val NavySurface = Color(0xFF101F35)
val NavySurfaceHigh = Color(0xFF16263F)
val NavyContainer = Color(0xFF1B2C47)
val NavyOutline = Color(0xFF33456A)
val OnNavy = Color(0xFFE8EEF7)
val OnNavyDim = Color(0xFFA8B8CF)
val BrandTeal = Color(0xFF14B8A6)
val BrandGold = Color(0xFFD4A017)
val IncomeGreen = Color(0xFF4ADE80)
val ExpenseRed = Color(0xFFF87171)

/** Preset swatches offered in Settings → Theme Colour. */
val presetColors = listOf(
    Color(0xFF14B8A6), // Teal (brand default)
    Color(0xFF1E3A8A), // Royal Navy
    Color(0xFFD4A017), // Gold
    Color(0xFF22C55E), // Green
    Color(0xFF3B82F6), // Blue
    Color(0xFF8B5CF6), // Purple
    Color(0xFFF43F5E), // Rose
    Color(0xFFF97316), // Orange
    Color(0xFF06B6D4), // Cyan
    Color(0xFFEC4899)  // Pink
)

/** Builds the app's dark-navy Material 3 scheme around the user's chosen accent. */
fun brandScheme(accent: Color): androidx.compose.material3.ColorScheme {
    val onAccent = if (accent.luminanceCompat() > 0.55f) NavyDeep else Color.White
    return darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accent.copy(alpha = 0.30f).compositeOver(NavyContainer),
        onPrimaryContainer = lerp(accent, Color.White, 0.55f),
        secondary = lerp(accent, BrandTeal, 0.55f),
        onSecondary = onAccent,
        secondaryContainer = lerp(accent, BrandTeal, 0.55f).copy(alpha = 0.28f).compositeOver(NavyContainer),
        onSecondaryContainer = lerp(lerp(accent, BrandTeal, 0.55f), Color.White, 0.55f),
        tertiary = BrandGold,
        onTertiary = NavyDeep,
        tertiaryContainer = BrandGold.copy(alpha = 0.25f).compositeOver(NavyContainer),
        onTertiaryContainer = lerp(BrandGold, Color.White, 0.5f),
        background = NavyBackground,
        onBackground = OnNavy,
        surface = NavySurface,
        onSurface = OnNavy,
        surfaceVariant = NavySurfaceHigh,
        onSurfaceVariant = OnNavyDim,
        surfaceTint = Color.Transparent,
        inverseSurface = OnNavy,
        inverseOnSurface = NavyDeep,
        outline = NavyOutline,
        outlineVariant = NavyOutline.copy(alpha = 0.55f),
        error = ExpenseRed,
        onError = Color.White,
        errorContainer = ExpenseRed.copy(alpha = 0.22f).compositeOver(NavyContainer),
        onErrorContainer = lerp(ExpenseRed, Color.White, 0.55f),
        scrim = NavyDeep
    )
}

private fun Color.luminanceCompat(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue

private val AppTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun FinanciieaTheme(content: @Composable () -> Unit) {
    val accentLong by AppContainer.settings.accentColor.collectAsState(
        initial = SettingsRepository.DEFAULT_ACCENT
    )
    MaterialTheme(
        colorScheme = brandScheme(Color(accentLong)),
        typography = AppTypography,
        content = content
    )
}
