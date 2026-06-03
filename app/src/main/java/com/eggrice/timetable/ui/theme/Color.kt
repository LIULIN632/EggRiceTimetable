package com.eggrice.timetable.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ═══ 海盐蓝主色 (椒盐音乐风格) ═══
val Accent = Color(0xFF6B95CF)
val AccentLight = Color(0xFF8AB4F8)
val AccentSoft = Color(0xFFE3F2FD)

// ═══ 抹茶绿 ═══
val AccentGreen = Color(0xFF7CB342)
val AccentGreenLight = Color(0xFFAED581)
val AccentGreenSoft = Color(0xFFF1F8E9)

// ═══ 樱花粉 ═══
val PinkAccent = Color(0xFFF48FB1)
val PinkAccentLight = Color(0xFFF8BBD0)
val PinkSoft = Color(0xFFFCE4EC)

// ═══ 紫藤紫 ═══
val PurpleAccent = Color(0xFF9575CD)
val PurpleAccentLight = Color(0xFFB39DDB)
val PurpleSoft = Color(0xFFEDE7F6)

// ═══ 炒饭黄 ═══
val FriedAccent = Color(0xFFF6C84C)
val FriedAccentLight = Color(0xFFFFD95A)
val FriedAccentSoft = Color(0xFFFFF5D6)
val DarkFriedAccentSoft = Color(0xFF3A3028)

// ═══ Theme-aware accent providers ═══

@Composable
fun accentColor(): Color = when (LocalThemeType.current) {
    ThemeType.SEA_BLUE -> Color(0xFF6B95CF)
    ThemeType.MATCHA_GREEN -> AccentGreen
    ThemeType.CHERRY_PINK -> PinkAccent
    ThemeType.WISTERIA_PURPLE -> PurpleAccent
    ThemeType.FRIED_RICE_YELLOW -> FriedAccent
}

@Composable
fun accentLightColor(): Color = when (LocalThemeType.current) {
    ThemeType.SEA_BLUE -> Color(0xFF8AB4F8)
    ThemeType.MATCHA_GREEN -> AccentGreenLight
    ThemeType.CHERRY_PINK -> PinkAccentLight
    ThemeType.WISTERIA_PURPLE -> PurpleAccentLight
    ThemeType.FRIED_RICE_YELLOW -> FriedAccentLight
}

@Composable
fun accentSoftColor(): Color = when (LocalThemeType.current) {
    ThemeType.SEA_BLUE -> Color(0xFFE3F2FD)
    ThemeType.MATCHA_GREEN -> AccentGreenSoft
    ThemeType.CHERRY_PINK -> PinkSoft
    ThemeType.WISTERIA_PURPLE -> PurpleSoft
    ThemeType.FRIED_RICE_YELLOW -> FriedAccentSoft
}

// ═══ 辅助暖色 ═══
val OrangeAccent = Color(0xFFF7B787)
val OrangeSoft = Color(0xFFFFF4EB)

// ═══ Light theme neutral ═══
val Surface = Color(0xFFFAFAFA)
val SurfaceCard = Color(0xFFFFFFFF)
val SurfaceAlt = Color(0xFFF5F5F5)
val CardBorder = Color(0xFFE0E0E0)
val Divider = Color(0xFFEEEEEE)
val TodayBg = Color(0xFFE8F0FE)

val TextPrimary = Color(0xFF212121)
val TextSecondary = Color(0xFF424242)
val TextTertiary = Color(0xFF757575)

val StatusBar = Color(0xFFFAFAFA)

// ═══ Dark theme neutral ═══
val DarkSurface = Color(0xFF1A1C1E)
val DarkSurfaceAlt = Color(0xFF212325)
val DarkSurfaceCard = Color(0xFF2A2C2E)
val DarkCardBorder = Color(0xFF383A3C)
val DarkDivider = Color(0xFF303234)
val DarkTodayBg = Color(0xFF1E2838)
val DarkAccentSoft = Color(0xFF1C2538)

val DarkTextPrimary = Color(0xFFE8E8E8)
val DarkTextSecondary = Color(0xFF9E9E9E)
val DarkTextTertiary = Color(0xFF757575)

val DarkStatusBar = Color(0xFF1A1C1E)

// ═══ Semantic colors ═══
val DangerColor = Color(0xFFE57373)
val DangerColorDark = Color(0xFFEF9A9A)
val SuccessGreen = Color(0xFF4CAF50)
val BorderLight = Color(0xFFE0E0E0)
val BorderDark = Color(0xFF555555)
val IconTertiary = Color(0xFFBDBDBD)
val IconTertiaryDark = Color(0xFF555555)

// ═══ Composable theme-aware color providers ═══

@Composable
fun themeSurface(darkMode: String): Color =
    if (darkMode == "dark") DarkSurface else Surface

@Composable
fun themeSurfaceAlt(darkMode: String): Color =
    if (darkMode == "dark") DarkSurfaceAlt else SurfaceAlt

@Composable
fun themeCardBg(darkMode: String): Color =
    if (darkMode == "dark") DarkSurfaceCard else SurfaceCard

@Composable
fun themeCardBorder(darkMode: String): Color =
    if (darkMode == "dark") DarkCardBorder else CardBorder

@Composable
fun themeDivider(darkMode: String): Color =
    if (darkMode == "dark") DarkDivider else Divider

@Composable
fun themeTodayBg(darkMode: String): Color =
    if (darkMode == "dark") DarkTodayBg else TodayBg

@Composable
fun themeAccentSoft(darkMode: String): Color =
    if (darkMode == "dark") DarkAccentSoft else AccentSoft

@Composable
fun themeTextPrimary(darkMode: String): Color =
    if (darkMode == "dark") DarkTextPrimary else TextPrimary

@Composable
fun themeTextSecondary(darkMode: String): Color =
    if (darkMode == "dark") DarkTextSecondary else TextSecondary

@Composable
fun themeTextTertiary(darkMode: String): Color =
    if (darkMode == "dark") DarkTextTertiary else TextTertiary

@Composable
fun themeWhite(darkMode: String): Color =
    if (darkMode == "dark") DarkSurfaceCard else Color.White

@Composable
fun themeStatusBar(darkMode: String): Color =
    if (darkMode == "dark") DarkStatusBar else StatusBar

// ═══ Low-saturation macaron course card colors (light) ═══
val CourseColors = listOf(
    Color(0xFFFFE8E5),  // soft rose
    Color(0xFFDEE8FF),  // soft lavender blue
    Color(0xFFD8F0E2),  // soft sage
    Color(0xFFEDE2F8),  // soft lilac
    Color(0xFFFFEAD6),  // soft peach
    Color(0xFFFFDEE0),  // soft pink
    Color(0xFFD6EDF2),  // soft sky
    Color(0xFFD0EBD8),  // soft mint
    Color(0xFFFFF0D6),  // soft cream
    Color(0xFFE2E2F6),  // soft periwinkle
    Color(0xFFFFD8D6),  // soft coral
    Color(0xFFD2F0D2),  // soft lime
    Color(0xFFFFF8D6),  // soft butter
    Color(0xFFF0DCF6),  // soft orchid
    Color(0xFFD6E6FF)   // soft baby blue
)

// ═══ Low-saturation macaron course card colors (dark) ═══
val CourseColorsDark = listOf(
    Color(0xFF5C3A38), Color(0xFF38405C),
    Color(0xFF385040), Color(0xFF483A50),
    Color(0xFF5C4438), Color(0xFF5C3838),
    Color(0xFF384A5C), Color(0xFF384A40),
    Color(0xFF5C4A38), Color(0xFF42425C),
    Color(0xFF5C383A), Color(0xFF384A38),
    Color(0xFF5C5038), Color(0xFF423A50),
    Color(0xFF384048)
)

// ═══ Course card text colors (readable on light backgrounds) ═══
val CourseTextColors = listOf(
    Color(0xFF8B5A5E), Color(0xFF4A5C82),
    Color(0xFF4A6C56), Color(0xFF584A78),
    Color(0xFF826448), Color(0xFF824E50),
    Color(0xFF486A80), Color(0xFF486A5C),
    Color(0xFF826448), Color(0xFF4E4E7A),
    Color(0xFF824A4C), Color(0xFF3E6A3E),
    Color(0xFF826E40), Color(0xFF6A4680),
    Color(0xFF3E6280)
)

// ═══ Course card text colors (dark mode) ═══
val CourseTextColorsDark = listOf(
    Color(0xFFFFD0CE), Color(0xFFD0DDFF),
    Color(0xFFD0EED8), Color(0xFFDDD0FF),
    Color(0xFFFFE0D0), Color(0xFFFFD0D0),
    Color(0xFFD0ECFF), Color(0xFFD0EED8),
    Color(0xFFFFEED0), Color(0xFFDDDDFF),
    Color(0xFFFFD0D0), Color(0xFFD0FFD0),
    Color(0xFFFFF6D0), Color(0xFFEED0FF),
    Color(0xFFD0E8FF)
)

fun courseBgColor(colorIndex: Int, isDark: Boolean): Color {
    val colors = if (isDark) CourseColorsDark else CourseColors
    return colors[colorIndex % colors.size]
}

fun courseTextColor(colorIndex: Int, isDark: Boolean): Color {
    val colors = if (isDark) CourseTextColorsDark else CourseTextColors
    return colors[colorIndex % colors.size]
}

// ═══ Grid background color palette ═══
// -1 = follow theme (no custom color)
val GridBgPalette = listOf(
    Color(0xFFFAFAFA) to Color(0xFF1A1C1E),  // 极简白
    Color(0xFFFFFDF7) to Color(0xFF1E1C18),  // 奶油白
    Color(0xFFF5F0E8) to Color(0xFF1F1C18),  // 暖米色
    Color(0xFFEEF4FA) to Color(0xFF181C22),  // 淡蓝灰
    Color(0xFFF0F4EE) to Color(0xFF181C1A),  // 淡绿灰
    Color(0xFFFDF2F4) to Color(0xFF1F1A1C),  // 淡粉灰
    Color(0xFFF4F0F8) to Color(0xFF1C1A20),  // 淡紫灰
    Color(0xFFFFF8EB) to Color(0xFF1F1C16),  // 暖蛋黄
    Color(0xFFF0F0F0) to Color(0xFF161616),  // 中性灰
)

fun courseBorderColor(colorIndex: Int, isDark: Boolean): Color {
    val bg = courseBgColor(colorIndex, isDark)
    return bg.copy(
        red = (bg.red * 0.85f).coerceIn(0f, 1f),
        green = (bg.green * 0.85f).coerceIn(0f, 1f),
        blue = (bg.blue * 0.85f).coerceIn(0f, 1f)
    )
}
