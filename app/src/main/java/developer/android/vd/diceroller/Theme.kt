package developer.android.vd.diceroller

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Color system
val ColorPrimary = Color(0xFF6366F1)
val ColorPrimaryDark = Color(0xFF4F46E5)
val ColorAccent = Color(0xFFEC4899)
val TextPrimary = Color(0xFF1F2937)
val TextSecondary = Color(0xFF6B7280)

// Premium colors
val PremiumPurpleStart = Color(0xFF6366F1)
val PremiumPurpleEnd = Color(0xFF8B5CF6)
val PremiumGoldStart = Color(0xFFFFD700)
val PremiumGoldEnd = Color(0xFFFFA500)
val PremiumGoldCenter = Color(0xFFFFC800)
val GreenSoft = Color(0xFFE1F5FE)

// Dark/Light text
val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFF94A3B8)

// Fonts
val BebasFontFamily = FontFamily(Font(R.font.bebas))
val AladinFontFamily = FontFamily(Font(R.font.aladin))
val KabelFontFamily = FontFamily(Font(R.font.kabel))

private val LightColorScheme = lightColorScheme(
    primary = ColorPrimary,
    onPrimary = Color.White,
    secondary = ColorAccent,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary
)

private val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        letterSpacing = 0.02.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    )
)

@Composable
fun DiceRollerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
