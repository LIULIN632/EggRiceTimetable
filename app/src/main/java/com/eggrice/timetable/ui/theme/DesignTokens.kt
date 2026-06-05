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

// ═══ 海盐蓝（默认）═══
val SeaSaltBlueLight = EggRiceColors(
    surfaceBase = Color(0xFFFAFAFA),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF5F5F5),
    surfaceHighlight = Color(0xFFE3F2FD),
    textPrimary = Color(0xFF212121),
    textSecondary = Color(0xFF424242),
    textTertiary = Color(0xFF757575),
    borderDivider = Color(0xFFEEEEEE),
    danger = Color(0xFFE57373),
    accentMain = Color(0xFF6B95CF)
)

val SeaSaltBlueDark = EggRiceColors(
    surfaceBase = Color(0xFF1A1C1E),
    surfaceCard = Color(0xFF2A2C2E),
    surfaceAlt = Color(0xFF212325),
    surfaceHighlight = Color(0xFF1C2538),
    textPrimary = Color(0xFFE8E8E8),
    textSecondary = Color(0xFF9E9E9E),
    textTertiary = Color(0xFF757575),
    borderDivider = Color(0xFF303234),
    danger = Color(0xFFEF9A9A),
    accentMain = Color(0xFF8AB4F8)
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

// ═══ 炒饭黄 ═══
val FriedRiceYellowLight = EggRiceColors(
    surfaceBase = Color(0xFFFAFAFA),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF5F5F5),
    surfaceHighlight = Color(0xFFFFF5D6),
    textPrimary = Color(0xFF212121),
    textSecondary = Color(0xFF424242),
    textTertiary = Color(0xFF757575),
    borderDivider = Color(0xFFEEEEEE),
    danger = Color(0xFFE57373),
    accentMain = Color(0xFFF6C84C)
)

val FriedRiceYellowDark = EggRiceColors(
    surfaceBase = Color(0xFF1A1C1E),
    surfaceCard = Color(0xFF2A2C2E),
    surfaceAlt = Color(0xFF212325),
    surfaceHighlight = Color(0xFF3A3028),
    textPrimary = Color(0xFFE8E8E8),
    textSecondary = Color(0xFF9E9E9E),
    textTertiary = Color(0xFF757575),
    borderDivider = Color(0xFF303234),
    danger = Color(0xFFEF9A9A),
    accentMain = Color(0xFFFFD95A)
)

// ═══ CompositionLocal 环境注入 ═══
val LocalEggRiceColors = staticCompositionLocalOf { SeaSaltBlueLight }
