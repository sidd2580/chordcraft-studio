package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MidnightColorScheme = darkColorScheme(
    primary = AmberNeon,
    onPrimary = Color(0xFF1E1200),
    primaryContainer = Color(0xFF3E2800),
    onPrimaryContainer = AmberGlow,
    secondary = CyanNeon,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF80F1FF),
    tertiary = Color(0xFFFF5252),
    background = SlateDark,
    onBackground = Color(0xFFEDEBF2),
    surface = SlateSurface,
    onSurface = Color(0xFFEDEBF2),
    surfaceVariant = SlateCard,
    onSurfaceVariant = Color(0xFFB0B3C6),
    outline = SlateBorder
)

private val OledBlackColorScheme = darkColorScheme(
    primary = AmberPure,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF261A00),
    onPrimaryContainer = AmberPure,
    secondary = Color(0xFF00E5FF),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF002930),
    onSecondaryContainer = Color(0xFF80F1FF),
    tertiary = Color(0xFFFF5252),
    background = PureBlack,
    onBackground = Color.White,
    surface = OledSurface,
    onSurface = Color.White,
    surfaceVariant = OledCard,
    onSurfaceVariant = Color(0xFFCCCCCC),
    outline = OledBorder
)

private val DaylightPaperColorScheme = lightColorScheme(
    primary = AmberPaper,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF92400E),
    secondary = BluePaper,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E40AF),
    tertiary = Color(0xFFDC2626),
    background = PaperBackground,
    onBackground = PaperText,
    surface = PaperSurface,
    onSurface = PaperText,
    surfaceVariant = PaperCard,
    onSurfaceVariant = PaperSubtext,
    outline = Color(0xFFD1D5DB)
)

@Composable
fun ChordCraftTheme(
    themeName: String = "MIDNIGHT",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "OLED_BLACK" -> OledBlackColorScheme
        "DAYLIGHT_PAPER" -> DaylightPaperColorScheme
        else -> MidnightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
