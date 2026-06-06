package com.eggrice.timetable.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ═══ 核心颜色令牌 ═══
@Immutable
data class EggRiceColors(
    val surfaceBase: Color,        // 页面大背景
    val surfaceCard: Color,        // 卡片/顶栏背景
    val surfaceAlt: Color,         // 次级背景
    val surfaceHighlight: Color,   // 今日高亮 / accent 柔和背景
    val textPrimary: Color,        // 主标题文字
    val textSecondary: Color,      // 辅助说明文字
    val textTertiary: Color,       // 弱化文字/图标
    val borderDivider: Color,      // 分割线/卡片边框
    val danger: Color,             // 危险操作（红）
    val accentMain: Color          // 动态主色
)

// ═══ 海盐蓝（默认 V2 — PRD colors）═══
val SeaSaltBlueLight = EggRiceColors(
    surfaceBase = Color(0xFFF8FAFC),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF5F5F5),
    surfaceHighlight = Color(0xFFEAF4FF),
    textPrimary = Color(0xFF1F2937),
    textSecondary = Color(0xFF8B95A7),
    textTertiary = Color(0xFF9CA3AF),
    borderDivider = Color(0xFFEEEEEE),
    danger = Color(0xFFE57373),
    accentMain = Color(0xFF4D8DFF)
)

val SeaSaltBlueDark = EggRiceColors(
    surfaceBase = Color(0xFF121212),
    surfaceCard = Color(0xFF1E1E1E),
    surfaceAlt = Color(0xFF1A1A1A),
    surfaceHighlight = Color(0xFF1A2A4A),
    textPrimary = Color(0xFFE8E8E8),
    textSecondary = Color(0xFF9E9E9E),
    textTertiary = Color(0xFF757575),
    borderDivider = Color(0xFF2B2B2B),
    danger = Color(0xFFEF9A9A),
    accentMain = Color(0xFF6AA8FF)
)

// ═══ 马卡龙蓝 (Figma: cool lavender-white BG #F7F7FA, cobalt accent #5594E8) ═══
val MacaronBlueLight = EggRiceColors(
    surfaceBase = Color(0xFFF7F7FA),
    surfaceCard = Color(0xFFFEFEFE),
    surfaceAlt = Color(0xFFF1F1F5),
    surfaceHighlight = Color(0xFFE8F0FB),
    textPrimary = Color(0xFF131313),
    textSecondary = Color(0xFF6B6B6B),
    textTertiary = Color(0xFFB0B0B0),
    borderDivider = Color(0xFFDEDEDE),
    danger = Color(0xFFE57373),
    accentMain = MacaronBlueAccent
)

val MacaronBlueDark = EggRiceColors(
    surfaceBase = Color(0xFF1A1C1E),
    surfaceCard = Color(0xFF2A2C2E),
    surfaceAlt = Color(0xFF212325),
    surfaceHighlight = Color(0xFF1C3048),
    textPrimary = Color(0xFFE8E8E8),
    textSecondary = Color(0xFF9E9E9E),
    textTertiary = Color(0xFF757575),
    borderDivider = Color(0xFF303234),
    danger = Color(0xFFEF9A9A),
    accentMain = Color(0xFF8AB4F8)
)

// ═══ 马卡龙粉 (Figma: pink-tinted white BG #FEF8F9, coral accent #F97C9E) ═══
val MacaronPinkLight = EggRiceColors(
    surfaceBase = Color(0xFFFEF8F9),
    surfaceCard = Color(0xFFFEFEFE),
    surfaceAlt = Color(0xFFFEF3F6),
    surfaceHighlight = Color(0xFFFDECEF),
    textPrimary = Color(0xFF1C1918),
    textSecondary = Color(0xFF6B6B6B),
    textTertiary = Color(0xFFB0B0B0),
    borderDivider = Color(0xFFDADAD9),
    danger = Color(0xFFE57373),
    accentMain = MacaronPinkAccent
)

val MacaronPinkDark = EggRiceColors(
    surfaceBase = Color(0xFF1A1C1E),
    surfaceCard = Color(0xFF2A2C2E),
    surfaceAlt = Color(0xFF282225),
    surfaceHighlight = Color(0xFF3D2836),
    textPrimary = Color(0xFFE8E8E8),
    textSecondary = Color(0xFF9E9E9E),
    textTertiary = Color(0xFF757575),
    borderDivider = Color(0xFF303234),
    danger = Color(0xFFEF9A9A),
    accentMain = Color(0xFFFFB3BA)
)

// ═══ 抹茶绿 ═══
val MatchaGreenLight = EggRiceColors(
    surfaceBase = Color(0xFFFAFAFA),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF5F5F5),
    surfaceHighlight = Color(0xFFF1F8E9),
    textPrimary = Color(0xFF212121),
    textSecondary = Color(0xFF424242),
    textTertiary = Color(0xFF757575),
    borderDivider = Color(0xFFEEEEEE),
    danger = Color(0xFFE57373),
    accentMain = Color(0xFF7CB342)
)

val MatchaGreenDark = EggRiceColors(
    surfaceBase = Color(0xFF1A1C1E),
    surfaceCard = Color(0xFF2A2C2E),
    surfaceAlt = Color(0xFF212325),
    surfaceHighlight = Color(0xFF1E3A1A),
    textPrimary = Color(0xFFE8E8E8),
    textSecondary = Color(0xFF9E9E9E),
    textTertiary = Color(0xFF757575),
    borderDivider = Color(0xFF303234),
    danger = Color(0xFFEF9A9A),
    accentMain = Color(0xFFAED581)
)

// ═══ 樱花粉 ═══
val CherryPinkLight = EggRiceColors(
    surfaceBase = Color(0xFFFAFAFA),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF5F5F5),
    surfaceHighlight = Color(0xFFFCE4EC),
    textPrimary = Color(0xFF212121),
    textSecondary = Color(0xFF424242),
    textTertiary = Color(0xFF757575),
    borderDivider = Color(0xFFEEEEEE),
    danger = Color(0xFFE57373),
    accentMain = Color(0xFFF48FB1)
)

val CherryPinkDark = EggRiceColors(
    surfaceBase = Color(0xFF1A1C1E),
    surfaceCard = Color(0xFF2A2C2E),
    surfaceAlt = Color(0xFF212325),
    surfaceHighlight = Color(0xFF4A2A3A),
    textPrimary = Color(0xFFE8E8E8),
    textSecondary = Color(0xFF9E9E9E),
    textTertiary = Color(0xFF757575),
    borderDivider = Color(0xFF303234),
    danger = Color(0xFFEF9A9A),
    accentMain = Color(0xFFF8BBD0)
)

// ═══ 紫藤紫 ═══
val WisteriaPurpleLight = EggRiceColors(
    surfaceBase = Color(0xFFFAFAFA),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF5F5F5),
    surfaceHighlight = Color(0xFFEDE7F6),
    textPrimary = Color(0xFF212121),
    textSecondary = Color(0xFF424242),
    textTertiary = Color(0xFF757575),
    borderDivider = Color(0xFFEEEEEE),
    danger = Color(0xFFE57373),
    accentMain = Color(0xFF9575CD)
)

val WisteriaPurpleDark = EggRiceColors(
    surfaceBase = Color(0xFF1A1C1E),
    surfaceCard = Color(0xFF2A2C2E),
    surfaceAlt = Color(0xFF212325),
    surfaceHighlight = Color(0xFF3A2A5A),
    textPrimary = Color(0xFFE8E8E8),
    textSecondary = Color(0xFF9E9E9E),
    textTertiary = Color(0xFF757575),
    borderDivider = Color(0xFF303234),
    danger = Color(0xFFEF9A9A),
    accentMain = Color(0xFFB39DDB)
)

// ═══ 炒饭黄 (V2 蛋炒饭 — 金黄琥珀 · 温暖治愈) ═══
val FriedRiceYellowLight = EggRiceColors(
    surfaceBase = Color(0xFFFFFDF5),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFFFF8E8),
    surfaceHighlight = Color(0xFFFFF0D0),
    textPrimary = Color(0xFF2D2010),
    textSecondary = Color(0xFF8B7A60),
    textTertiary = Color(0xFFA89880),
    borderDivider = Color(0xFFF0E5D0),
    danger = Color(0xFFE57373),
    accentMain = Color(0xFFF0A030)
)

val FriedRiceYellowDark = EggRiceColors(
    surfaceBase = Color(0xFF1A1814),
    surfaceCard = Color(0xFF24211C),
    surfaceAlt = Color(0xFF1F1D18),
    surfaceHighlight = Color(0xFF3A3020),
    textPrimary = Color(0xFFE8E4D8),
    textSecondary = Color(0xFF9E9888),
    textTertiary = Color(0xFF757060),
    borderDivider = Color(0xFF2B2820),
    danger = Color(0xFFEF9A9A),
    accentMain = Color(0xFFF5B840)
)

// ═══ CompositionLocal 环境注入 ═══
val LocalEggRiceColors = staticCompositionLocalOf { SeaSaltBlueLight }
