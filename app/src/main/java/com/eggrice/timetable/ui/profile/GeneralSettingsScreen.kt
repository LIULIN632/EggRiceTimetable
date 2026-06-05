package com.eggrice.timetable.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.di.AppContainer
import com.eggrice.timetable.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    darkMode: String,
    vibrationMode: Int,
    colorTheme: String,
    borderStyle: Int,
    onBack: () -> Unit,
    onDarkMode: () -> Unit,
    onVibrationMode: () -> Unit,
    onColorTheme: () -> Unit,
    onFeatureToggles: () -> Unit,
    onImportToCalendar: () -> Unit,
    onBorderStyle: (Int) -> Unit
) {
    val colors = LocalEggRiceColors.current
    val modeLabel = when (darkMode) { "dark" -> "深色模式"; "system" -> "跟随系统"; else -> "浅色模式" }
    val vibrationLabels = listOf("关闭", "轻柔", "适中", "强力")
    val vibrationLabel = vibrationLabels.getOrElse(vibrationMode) { "轻柔" }
    val themeLabel = ThemeType.fromKey(colorTheme).label

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通用设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surfaceCard)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.surfaceAlt)
        ) {
            SettingsMenuItem(
                icon = Icons.Outlined.DarkMode,
                title = "深色/浅色模式",
                subtitle = modeLabel,
                onClick = onDarkMode
            )
            SettingsMenuItem(
                icon = Icons.Outlined.Palette,
                title = "配色主题",
                subtitle = themeLabel,
                onClick = onColorTheme
            )
            // 方格边框设置（实线/虚线/无）
            Surface(
                color = colors.surfaceCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceHighlight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.GridOn,
                            null,
                            tint = colors.accentMain,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        "方格边框",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("无" to 0, "实线" to 1, "虚线" to 2).forEach { (label, style) ->
                            FilterChip(
                                selected = borderStyle == style,
                                onClick = { onBorderStyle(style) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accentMain,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = colors.borderDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 70.dp))
            SettingsMenuItem(
                icon = Icons.Outlined.Vibration,
                title = "震动反馈",
                subtitle = vibrationLabel,
                onClick = onVibrationMode
            )
            SettingsMenuItem(
                icon = Icons.Outlined.ToggleOn,
                title = "功能开关",
                subtitle = "百宝箱、提醒、小组件、更新",
                onClick = onFeatureToggles
            )
            SettingsMenuItem(
                icon = Icons.Outlined.CalendarMonth,
                title = "导入到日程",
                subtitle = "将本学期课表导入系统日历",
                onClick = onImportToCalendar
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureToggleScreen(
    container: AppContainer,
    showTreasureBox: Boolean,
    showWidget: Boolean,
    reminderEnabled: Boolean,
    autoUpdate: Boolean,
    onBack: () -> Unit
) {
    val colors = LocalEggRiceColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("功能开关", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surfaceCard)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.surfaceAlt)
        ) {
            Text(
                "所有非核心功能默认关闭，按需开启",
                fontSize = 12.sp,
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceCard,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Column {
                    FeatureToggleItem("百宝箱功能", "学习资源、今天吃什么等", showTreasureBox) { container.toggleTreasureBox() }
                    HorizontalDivider(color = colors.borderDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                    FeatureToggleItem("课程提醒功能", "上课前推送通知提醒", reminderEnabled) { container.toggleReminder() }
                    HorizontalDivider(color = colors.borderDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                    FeatureToggleItem("桌面小组件", "桌面快捷查看课表", showWidget) { container.toggleWidget() }
                    HorizontalDivider(color = colors.borderDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                    FeatureToggleItem("自动检查更新", "启动时自动检查新版本", autoUpdate) { container.toggleAutoUpdate() }
                }
            }
        }
    }
}

@Composable
private fun FeatureToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val isDark = LocalDarkMode.current
    val colors = LocalEggRiceColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            Text(subtitle, fontSize = 12.sp, color = colors.textTertiary)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accentMain,
                uncheckedThumbColor = colors.surfaceCard,
                uncheckedTrackColor = colors.borderDivider
            )
        )
    }
}
