package com.eggrice.timetable.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══ 五套主题枚举 ═══
enum class ThemeType(val key: String, val label: String) {
    SEA_BLUE("default", "海盐蓝"),
    MATCHA_GREEN("matcha", "抹茶绿"),
    CHERRY_PINK("sakura", "樱花粉"),
    WISTERIA_PURPLE("wisteria", "紫藤紫"),
    FRIED_RICE_YELLOW("fried_rice", "蛋炒饭");

    companion object {
        fun fromKey(key: String) = entries.find { it.key == key } ?: SEA_BLUE
    }
}

val LocalDarkMode = staticCompositionLocalOf { false }
val LocalThemeType = staticCompositionLocalOf { ThemeType.SEA_BLUE }

// ═══ Material3 颜色方案 ═══

private val SeaBlueLightScheme = lightColorScheme(
    primary = Color(0xFF6B95CF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF1A365D),
    secondary = Color(0xFF8AB4F8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F0FE),
    onSecondaryContainer = Color(0xFF1A365D),
    tertiary = PinkAccent,
    onTertiary = Color.White,
    tertiaryContainer = PinkSoft,
    onTertiaryContainer = Color(0xFF4A1A2E),
    surface = SurfaceCard,
    onSurface = TextSecondary,
    background = Surface,
    onBackground = TextPrimary,
    surfaceVariant = SurfaceAlt,
    onSurfaceVariant = TextTertiary,
    outline = CardBorder,
    outlineVariant = Divider,
    error = DangerColor,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFF4A1A1A)
)

private val MatchaGreenLightScheme = SeaBlueLightScheme.copy(
    primary = Color(0xFF7CB342),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF1F8E9),
    onPrimaryContainer = Color(0xFF1A3A0A),
    secondary = Color(0xFFAED581),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F8E9),
    onSecondaryContainer = Color(0xFF1A3A0A)
)

private val CherryPinkLightScheme = SeaBlueLightScheme.copy(
    primary = Color(0xFFF48FB1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE4EC),
    onPrimaryContainer = Color(0xFF4A1A2E),
    secondary = Color(0xFFF8BBD0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE4EC),
    onSecondaryContainer = Color(0xFF4A1A2E)
)

private val WisteriaPurpleLightScheme = SeaBlueLightScheme.copy(
    primary = Color(0xFF9575CD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7F6),
    onPrimaryContainer = Color(0xFF2A1A4E),
    secondary = Color(0xFFB39DDB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7F6),
    onSecondaryContainer = Color(0xFF2A1A4E)
)

private val FriedRiceLightScheme = SeaBlueLightScheme.copy(
    primary = FriedAccent,
    onPrimary = Color(0xFF3A2B1F),
    primaryContainer = FriedAccentSoft,
    onPrimaryContainer = Color(0xFF3A2B1F),
    secondary = Color(0xFF73D9A5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4F5E5),
    onSecondaryContainer = Color(0xFF1A3A28)
)

private val SeaBlueDarkScheme = darkColorScheme(
    primary = AccentLight,
    onPrimary = Color(0xFF1A2E4A),
    primaryContainer = DarkAccentSoft,
    onPrimaryContainer = Color(0xFFD0DDFF),
    secondary = AccentGreenLight,
    onSecondary = Color(0xFF1A2E20),
    secondaryContainer = Color(0xFF1E3A2A),
    onSecondaryContainer = Color(0xFFD0EED8),
    tertiary = PinkAccentLight,
    onTertiary = Color(0xFF3A1A28),
    tertiaryContainer = Color(0xFF4A2A38),
    onTertiaryContainer = Color(0xFFFFD0D8),
    surface = DarkSurfaceCard,
    onSurface = DarkTextSecondary,
    background = DarkSurface,
    onBackground = DarkTextPrimary,
    surfaceVariant = DarkSurfaceAlt,
    onSurfaceVariant = DarkTextTertiary,
    outline = DarkCardBorder,
    outlineVariant = DarkDivider,
    error = DangerColorDark,
    onError = Color(0xFF2E1A1A),
    errorContainer = Color(0xFF4A2A2A),
    onErrorContainer = Color(0xFFFFCDD2)
)

private val MatchaGreenDarkScheme = SeaBlueDarkScheme.copy(
    primary = AccentGreenLight,
    onPrimary = Color(0xFF1A3A0A),
    primaryContainer = Color(0xFF1E3A1A),
    onPrimaryContainer = Color(0xFFD0EED8),
    secondary = AccentGreen,
    onSecondary = Color(0xFF1A3A0A),
    secondaryContainer = Color(0xFF2A4A28),
    onSecondaryContainer = Color(0xFFD0EED8)
)

private val CherryPinkDarkScheme = SeaBlueDarkScheme.copy(
    primary = PinkAccentLight,
    onPrimary = Color(0xFF4A1A2E),
    primaryContainer = Color(0xFF4A2A3A),
    onPrimaryContainer = Color(0xFFFFD0D8),
    secondary = PinkAccent,
    onSecondary = Color(0xFF4A1A2E),
    secondaryContainer = Color(0xFF4A2838),
    onSecondaryContainer = Color(0xFFFFD0D8)
)

private val WisteriaPurpleDarkScheme = SeaBlueDarkScheme.copy(
    primary = PurpleAccentLight,
    onPrimary = Color(0xFF2A1A4E),
    primaryContainer = Color(0xFF3A2A5A),
    onPrimaryContainer = Color(0xFFDDD0FF),
    secondary = PurpleAccent,
    onSecondary = Color(0xFF2A1A4E),
    secondaryContainer = Color(0xFF3A284A),
    onSecondaryContainer = Color(0xFFEED0FF)
)

private val FriedRiceDarkScheme = SeaBlueDarkScheme.copy(
    primary = FriedAccentLight,
    onPrimary = Color(0xFF3A2B1F),
    primaryContainer = DarkFriedAccentSoft,
    onPrimaryContainer = Color(0xFFFFF5D6),
    secondary = Color(0xFF5CBF8A),
    onSecondary = Color(0xFF1A3A28),
    secondaryContainer = Color(0xFF2A4A38),
    onSecondaryContainer = Color(0xFFD4F5E5)
)

// ═══ 椒盐音乐同款无衬线极简字体 ═══
private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp
    )
)

// ═══ 全柔和圆角 ═══
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// ═══ 主题入口 ═══
@Composable
fun EggRiceTheme(
    themeType: ThemeType = ThemeType.SEA_BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val lightScheme = when (themeType) {
        ThemeType.SEA_BLUE -> SeaBlueLightScheme
        ThemeType.MATCHA_GREEN -> MatchaGreenLightScheme
        ThemeType.CHERRY_PINK -> CherryPinkLightScheme
        ThemeType.WISTERIA_PURPLE -> WisteriaPurpleLightScheme
        ThemeType.FRIED_RICE_YELLOW -> FriedRiceLightScheme
    }

    val darkScheme = when (themeType) {
        ThemeType.SEA_BLUE -> SeaBlueDarkScheme
        ThemeType.MATCHA_GREEN -> MatchaGreenDarkScheme
        ThemeType.CHERRY_PINK -> CherryPinkDarkScheme
        ThemeType.WISTERIA_PURPLE -> WisteriaPurpleDarkScheme
        ThemeType.FRIED_RICE_YELLOW -> FriedRiceDarkScheme
    }

    val colorScheme = if (darkTheme) darkScheme else lightScheme

    CompositionLocalProvider(
        LocalDarkMode provides darkTheme,
        LocalThemeType provides themeType
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

/** Convenience: resolve active accent color from current theme */
@Composable
fun currentAccent(): Color = MaterialTheme.colorScheme.primary

@Composable
fun currentAccentSoft(): Color = MaterialTheme.colorScheme.primaryContainer
