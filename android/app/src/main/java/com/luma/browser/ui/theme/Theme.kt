package com.luma.browser.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LumaLightColors = lightColorScheme(
    primary = Color(0xFF5B5BD6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E0FF),
    onPrimaryContainer = Color(0xFF171452),
    secondary = Color(0xFF5C5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1E0F9),
    onSecondaryContainer = Color(0xFF191A2C),
    tertiary = Color(0xFF7A5368),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E8),
    onTertiaryContainer = Color(0xFF2F1120),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE4E1EC),
    onSurfaceVariant = Color(0xFF47464F),
    outline = Color(0xFF787680),
    outlineVariant = Color(0xFFC8C5D0),
    surfaceDim = Color(0xFFDCD8E3),
    surfaceBright = Color(0xFFFCF8FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F2FB),
    surfaceContainer = Color(0xFFF0ECF4),
    surfaceContainerHigh = Color(0xFFEAE6EF),
    surfaceContainerHighest = Color(0xFFE5E1E9),
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF3EFF7),
    inversePrimary = Color(0xFFC1C0FF),
)

private val LumaDarkColors = darkColorScheme(
    primary = Color(0xFFC1C0FF),
    onPrimary = Color(0xFF2A2A76),
    primaryContainer = Color(0xFF3E3E96),
    onPrimaryContainer = Color(0xFFE2E0FF),
    secondary = Color(0xFFC5C4DD),
    onSecondary = Color(0xFF2E2F42),
    secondaryContainer = Color(0xFF444559),
    onSecondaryContainer = Color(0xFFE1E0F9),
    tertiary = Color(0xFFEBB8CF),
    onTertiary = Color(0xFF472536),
    tertiaryContainer = Color(0xFF613B4E),
    onTertiaryContainer = Color(0xFFFFD8E8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE5E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE5E1E9),
    surfaceVariant = Color(0xFF47464F),
    onSurfaceVariant = Color(0xFFC8C5D0),
    outline = Color(0xFF928F9A),
    outlineVariant = Color(0xFF47464F),
    surfaceDim = Color(0xFF131318),
    surfaceBright = Color(0xFF39383F),
    surfaceContainerLowest = Color(0xFF0E0E13),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF292930),
    surfaceContainerHighest = Color(0xFF34343B),
    inverseSurface = Color(0xFFE5E1E9),
    inverseOnSurface = Color(0xFF303036),
    inversePrimary = Color(0xFF5B5BD6),
)

private val LumaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val LumaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 54.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1.6).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.2).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
)

@Composable
fun LumaTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) LumaDarkColors else LumaLightColors,
        shapes = LumaShapes,
        typography = LumaTypography,
        content = content,
    )
}
