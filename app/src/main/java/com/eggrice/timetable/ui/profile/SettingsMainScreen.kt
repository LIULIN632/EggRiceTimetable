package com.eggrice.timetable.ui.profile

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.TimetableApplication
import com.eggrice.timetable.di.AppContainer
import com.eggrice.timetable.ui.theme.*

// ═══════════════════════════════════════════
//  Settings Main — 2nd-level settings page
// ═══════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onTimeSlotManagement: () -> Unit,
    onAppearance: () -> Unit,
    onSemesterSettings: () -> Unit,
    onReminderTime: () -> Unit,
    onVibrationMode: () -> Unit = {}
) {
    val isDark = LocalDarkMode.current
    val reminderEnabled by container.reminderEnabled.collectAsState()
    val reminderMinutes by container.reminderMinutes.collectAsState()
    val autoUpdate by container.autoUpdate.collectAsState()
    val vibrationMode by container.vibrationMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("课表设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDark) DarkSurfaceCard else SurfaceCard)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDark) DarkSurfaceAlt else SurfaceAlt)
        ) {
            // ── Sub-item 1: 时间段管理 ──
            SettingsMenuItem(
                icon = Icons.Outlined.Schedule,
                title = "时间段管理",
                subtitle = "设置每节课时间和休息时长",
                onClick = onTimeSlotManagement
            )

            // ── Sub-item 2: 个性化配置 ──
            SettingsMenuItem(
                icon = Icons.Outlined.Palette,
                title = "个性化配置",
                subtitle = "课表外观、实时预览、恢复默认",
                onClick = onAppearance
            )

            // ── Sub-item 3: 学期设置 ──
            SettingsMenuItem(
                icon = Icons.Outlined.CalendarMonth,
                title = "学期设置",
                subtitle = "学期开始日期、自动计算当前周",
                onClick = onSemesterSettings
            )

            // ── Sub-item 4: 提醒设置 ──
            SettingsSwitchItem(
                icon = Icons.Outlined.Notifications,
                title = "上课提醒总开关",
                subtitle = "课前自动发送上课提醒通知",
                checked = reminderEnabled,
                onToggle = { container.toggleReminder() }
            )

            SettingsMenuItem(
                icon = Icons.Outlined.Timer,
                title = "提前提醒时间",
                subtitle = "${reminderMinutes}分钟",
                onClick = onReminderTime
            )

            SettingsSwitchItem(
                icon = Icons.Outlined.Refresh,
                title = "课表自动更新",
                subtitle = "自动从教务系统同步最新课表",
                checked = autoUpdate,
                onToggle = { container.toggleAutoUpdate() }
            )

            val vibrationLabels = listOf("关闭", "轻柔", "适中", "强力")
            SettingsMenuItem(
                icon = Icons.Outlined.Vibration,
                title = "震动模式",
                subtitle = vibrationLabels.getOrElse(vibrationMode) { "轻柔" },
                onClick = onVibrationMode
            )

        }
    }
}

@Composable
internal fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val isDark = LocalDarkMode.current

    Surface(
        color = if (isDark) DarkSurfaceCard else SurfaceCard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) DarkAccentSoft else accentSoftColor()),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor(), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (isDark) DarkTextPrimary else TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = if (isDark) DarkTextTertiary else TextTertiary)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = if (isDark) BorderDark else IconTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    HorizontalDivider(color = if (isDark) DarkDivider else Divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 70.dp))
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val isDark = LocalDarkMode.current

    Surface(
        color = if (isDark) DarkSurfaceCard else SurfaceCard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) DarkAccentSoft else accentSoftColor()),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor(), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (isDark) DarkTextPrimary else TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = if (isDark) DarkTextTertiary else TextTertiary)
            }
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor(),
                    uncheckedThumbColor = if (isDark) BorderDark else Color.White,
                    uncheckedTrackColor = if (isDark) BorderDark else BorderLight
                )
            )
        }
    }
    HorizontalDivider(color = if (isDark) DarkDivider else Divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 70.dp))
}

// ═══════════════════════════════════════════
//  Semester Settings Page
// ═══════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SemesterSettingsPage(
    container: AppContainer,
    onBack: () -> Unit
) {
    val semesterStart by container.semesterStart.collectAsState()
    val semesterWeeks by container.semesterWeeks.collectAsState()
    var editingWeeks by remember { mutableStateOf(semesterWeeks) }
    val context = LocalContext.current

    // Parse saved date into individual parts
    val savedParts = remember(semesterStart) {
        semesterStart.split("-").mapNotNull { it.toIntOrNull() }
    }
    val now = java.time.LocalDate.now()
    var year by remember { mutableIntStateOf(savedParts.getOrElse(0) { now.year }) }
    var month by remember { mutableIntStateOf(savedParts.getOrElse(1) { 2 }) }
    var day by remember { mutableIntStateOf(savedParts.getOrElse(2) { 1 }) }

    val isDark = LocalDarkMode.current
    val editingStart = "$year-${String.format("%02d", month)}-${String.format("%02d", day)}"
    val autoWeek = remember(year, month, day, editingWeeks) {
        try {
            val start = java.time.LocalDate.of(year, month, day)
            val today = java.time.LocalDate.now()
            val days = java.time.temporal.ChronoUnit.DAYS.between(start, today)
            ((days / 7L).toInt() + 1).coerceIn(1, editingWeeks)
        } catch (_: Exception) { container.autoCurrentWeek() }
    }

    // Track which control last changed to avoid feedback loops
    var lastChangeSource by remember { mutableStateOf("init") }

    // Quick week selector state — initialized from autoWeek
    var quickWeek by remember { mutableIntStateOf(autoWeek) }

    // When date fields change (from manual date picker), sync quickWeek
    LaunchedEffect(year, month, day) {
        if (lastChangeSource != "week") {
            quickWeek = autoWeek
        }
        lastChangeSource = "date"
    }

    // Reverse-calculate semester start date from week number
    fun applyWeekSelection(week: Int) {
        if (week < 1 || week > editingWeeks) return
        lastChangeSource = "week"
        val today = java.time.LocalDate.now()
        // Semester typically starts on a Monday — find Monday of the current week
        val mondayOfThisWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        // Week 1 starts at semesterStart; current week = week N means N-1 weeks have passed
        val newStart = mondayOfThisWeek.minusWeeks((week - 1).toLong())
        year = newStart.year
        month = newStart.monthValue
        day = newStart.dayOfMonth
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学期设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        container.setSemesterStart(editingStart)
                        container.setSemesterWeeks(editingWeeks)
                        Toast.makeText(context, "学期设置已保存", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("保存", color = accentColor(), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDark) DarkSurfaceCard else SurfaceCard)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDark) DarkSurfaceAlt else SurfaceAlt)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Auto-calculated week display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("当前自动计算周次", fontSize = 13.sp, color = if (isDark) DarkTextTertiary else TextTertiary)
                    Text(
                        "第 $autoWeek 周",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor()
                    )
                }
            }

            // Quick week selector — reverse-calculates semester start
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "快捷设置当前周次",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) DarkTextPrimary else TextPrimary
                    )
                    Text(
                        "选择后将自动推算学期开始日期（周一）",
                        fontSize = 11.sp,
                        color = if (isDark) DarkTextTertiary else TextTertiary
                    )
                    Spacer(Modifier.height(12.dp))
                    WheelPicker(
                        value = quickWeek,
                        onValueChange = { applyWeekSelection(it) },
                        range = 1..editingWeeks,
                        formatLabel = { "第${it}周" },
                        modifier = Modifier.fillMaxWidth(0.5f)
                    )
                }
            }

            // Semester start date — 3 wheel pickers
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "学期开始日期",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) DarkTextPrimary else TextPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WheelPicker(
                            value = year,
                            onValueChange = { year = it },
                            range = (now.year - 3)..(now.year + 3),
                            formatLabel = { "${it}年" },
                            modifier = Modifier.weight(1f)
                        )
                        WheelPicker(
                            value = month,
                            onValueChange = { month = it },
                            range = 1..12,
                            formatLabel = { "${it}月" },
                            modifier = Modifier.weight(1f)
                        )
                        WheelPicker(
                            value = day,
                            onValueChange = { day = it },
                            range = 1..31,
                            formatLabel = { "${it}日" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Semester total weeks — wheel picker
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "学期总周数",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) DarkTextPrimary else TextPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    WheelPicker(
                        value = editingWeeks,
                        onValueChange = { editingWeeks = it },
                        range = 1..30,
                        formatLabel = { "${it}周" },
                        modifier = Modifier.fillMaxWidth(0.5f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
//  Display Settings Page
// ═══════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsPage(
    container: AppContainer,
    onBack: () -> Unit
) {
    val showTeacher by container.showTeacher.collectAsState()
    val showRoom by container.showRoom.collectAsState()
    val showSlotTime by container.showSlotTime.collectAsState()
    val showOddEven by container.showOddEven.collectAsState()
    val isDark = LocalDarkMode.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("显示设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDark) DarkSurfaceCard else SurfaceCard)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDark) DarkSurfaceAlt else SurfaceAlt)
        ) {
            DisplaySwitchItem("显示教师", "在课程卡片上显示教师姓名", showTeacher) { container.toggleShowTeacher() }
            DisplaySwitchItem("显示教室", "在课程卡片上显示教室位置", showRoom) { container.toggleShowRoom() }
            DisplaySwitchItem("显示节次时间", "在左侧节次栏显示具体时间", showSlotTime) { container.toggleShowSlotTime() }
            DisplaySwitchItem("单双周课程显示", "单双周课程标注「单周」「双周」", showOddEven) { container.toggleShowOddEven() }
        }
    }
}

@Composable
private fun DisplaySwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val isDark = LocalDarkMode.current

    Surface(
        color = if (isDark) DarkSurfaceCard else SurfaceCard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (isDark) DarkTextPrimary else TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = if (isDark) DarkTextTertiary else TextTertiary)
            }
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor(),
                    uncheckedThumbColor = if (isDark) BorderDark else Color.White,
                    uncheckedTrackColor = if (isDark) BorderDark else BorderLight
                )
            )
        }
    }
    HorizontalDivider(color = if (isDark) DarkDivider else Divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
}

// ═══════════════════════════════════════════
//  Wheel Picker — scroll wheel selector
// ═══════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    formatLabel: (Int) -> String = { it.toString() },
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkMode.current
    val items = range.toList()
    val itemHeight = 44.dp
    val visibleCount = 3
    val centerFraction = visibleCount / 2f  // 1.5 — visual center of 3-item viewport

    val startIdx = items.indexOf(value).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIdx)
    val density = LocalDensity.current
    val itemHeightPx: Float = with(density) { itemHeight.toPx() }

    // Derive selected value from scroll position
    val selected by remember {
        derivedStateOf {
            val centerOffset = listState.firstVisibleItemScrollOffset + (centerFraction * itemHeightPx).toInt()
            val idx = listState.firstVisibleItemIndex + (centerOffset / itemHeightPx.toInt())
            items.getOrElse(idx - 1) { items.last() }
        }
    }

    LaunchedEffect(selected) {
        if (selected != value) onValueChange(selected)
    }

    Box(
        modifier = modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center
    ) {
        // Center highlight bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    if (isDark) Color.White.copy(alpha = 0.05f)
                    else accentColor().copy(alpha = 0.08f),
                    RoundedCornerShape(8.dp)
                )
        )

        // Fade edges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Top gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                if (isDark) DarkSurfaceCard else SurfaceCard,
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = itemHeightPx * 1.5f
                        )
                    )
                    // Bottom gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                if (isDark) DarkSurfaceCard else SurfaceCard
                            ),
                            startY = size.height - itemHeightPx * 1.5f,
                            endY = size.height
                        )
                    )
                }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            flingBehavior = rememberSnapFlingBehavior(listState)
        ) {
            // Top spacer for centering
            item { Spacer(modifier = Modifier.height(itemHeight)) }
            items(items.size) { i ->
                val item = items[i]
                val isSelected = item == selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatLabel(item),
                        fontSize = if (isSelected) 17.sp else 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isSelected -> accentColor()
                            isDark -> DarkTextTertiary
                            else -> TextTertiary
                        }
                    )
                }
            }
            // Bottom spacer for centering
            item { Spacer(modifier = Modifier.height(itemHeight)) }
        }
    }
}
