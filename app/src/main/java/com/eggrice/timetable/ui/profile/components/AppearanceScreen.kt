package com.eggrice.timetable.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.data.entity.CourseEntity
import com.eggrice.timetable.di.AppContainer
import com.eggrice.timetable.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceScreen(
    container: AppContainer,
    showTeacher: Boolean,
    showRoom: Boolean,
    showCampus: Boolean,
    showSlotTime: Boolean,
    borderStyle: Int,
    textCentered: Boolean,
    gridHeight: Int,
    gridOpacity: Float,
    gridTextSize: Int,
    showNonCurrentWeek: Boolean,
    showOddEven: Boolean,
    gridBgColor: Int,
    otherWeekAlpha: Float,
    wallpaperUri: String,
    onBack: () -> Unit
) {
    val savedCornerRadius by container.cornerRadius.collectAsState()
    val colors = LocalEggRiceColors.current
    val isDark = LocalDarkMode.current

    var previewTeacher by remember { mutableStateOf(showTeacher) }
    var previewRoom by remember { mutableStateOf(showRoom) }
    var previewCampus by remember { mutableStateOf(showCampus) }
    var previewSlotTime by remember { mutableStateOf(showSlotTime) }
    var previewBorderStyle by remember { mutableIntStateOf(borderStyle) }
    var previewCentered by remember { mutableStateOf(textCentered) }
    var previewHeight by remember { mutableIntStateOf(gridHeight) }
    var previewRadius by remember { mutableIntStateOf(savedCornerRadius) }
    var previewOpacity by remember { mutableStateOf(gridOpacity) }
    var previewTextSize by remember { mutableIntStateOf(gridTextSize) }
    var previewNonCurrentWeek by remember { mutableStateOf(showNonCurrentWeek) }
    var previewOddEven by remember { mutableStateOf(showOddEven) }
    var previewBgColor by remember { mutableIntStateOf(gridBgColor) }
    var previewOtherWeekAlpha by remember { mutableStateOf(otherWeekAlpha) }
    var previewWallpaper by remember { mutableStateOf(wallpaperUri) }

    val sampleCourse = remember {
        CourseEntity(name = "高等数学", teacher = "张教授", room = "明伦校区 综合教学楼 101", dayOfWeek = 1, startSlot = 1, endSlot = 2, colorIndex = 0)
    }

    fun commitAll() {
        if (previewTeacher != showTeacher) container.toggleShowTeacher()
        if (previewRoom != showRoom) container.toggleShowRoom()
        if (previewCampus != showCampus) container.toggleShowCampus()
        if (previewSlotTime != showSlotTime) container.toggleShowSlotTime()
        if (previewBorderStyle != borderStyle) container.setBorderStyle(previewBorderStyle)
        if (previewCentered != textCentered) container.toggleTextCentered()
        if (previewNonCurrentWeek != showNonCurrentWeek) container.toggleShowNonCurrentWeek()
        if (previewOddEven != showOddEven) container.toggleShowOddEven()
        container.setGridHeight(previewHeight)
        container.setCornerRadius(previewRadius)
        container.setGridOpacity(previewOpacity)
        container.setGridTextSize(previewTextSize)
        if (previewBgColor != gridBgColor) container.setGridBgColor(previewBgColor)
        container.setOtherWeekAlpha(previewOtherWeekAlpha)
        if (previewWallpaper != wallpaperUri) container.setWallpaperUri(previewWallpaper)
    }

    fun resetDefaults() {
        previewTeacher = true; previewRoom = true; previewCampus = false; previewSlotTime = false
        previewBorderStyle = 0; previewCentered = false; previewHeight = 64; previewRadius = 4
        previewOpacity = 1f; previewTextSize = 12; previewNonCurrentWeek = true
        previewOddEven = true; previewBgColor = -1; previewOtherWeekAlpha = 0.50f; previewWallpaper = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个性化配置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { commitAll(); onBack() }) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ═══ Preview card ═══
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("实时预览", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textTertiary)
                    Spacer(Modifier.height(6.dp))
                    val previewRowBg = if (previewBgColor >= 0 && previewBgColor < GridBgPalette.size) {
                        val (lightBg, darkBg) = GridBgPalette[previewBgColor]
                        if (isDark) darkBg else lightBg
                    } else Color.Unspecified
                    Row(modifier = Modifier.fillMaxWidth().then(if (previewRowBg != Color.Unspecified) Modifier.background(previewRowBg, RoundedCornerShape(8.dp)) else Modifier).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(20.dp)) {
                            Text("一", fontSize = 10.sp, color = colors.textTertiary)
                            Spacer(Modifier.height(2.dp))
                            Text(if (previewSlotTime) "08:00" else "1", fontSize = 9.sp, color = colors.textTertiary)
                            Spacer(Modifier.height(2.dp))
                            Text(if (previewSlotTime) "08:45" else "2", fontSize = 9.sp, color = colors.textTertiary)
                        }
                        Box(
                            modifier = Modifier.weight(1f).height(previewHeight.dp)
                                .clip(RoundedCornerShape(previewRadius.dp))
                                .background(courseBgColor(sampleCourse.colorIndex, isDark).copy(alpha = previewOpacity))
                                .then(when (previewBorderStyle) {
                                    1 -> Modifier.drawBehind { drawRoundRect(color = courseBorderColor(sampleCourse.colorIndex, isDark), cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(width = 1.dp.toPx())) }
                                    2 -> Modifier.drawBehind { drawRoundRect(color = courseBorderColor(sampleCourse.colorIndex, isDark), cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f))) }
                                    else -> Modifier
                                }),
                            contentAlignment = if (previewCentered) Alignment.Center else Alignment.TopStart
                        ) {
                            Column(modifier = Modifier.padding(4.dp), horizontalAlignment = if (previewCentered) Alignment.CenterHorizontally else Alignment.Start) {
                                Text(sampleCourse.name, fontSize = previewTextSize.sp, fontWeight = FontWeight.Bold, color = courseTextColor(sampleCourse.colorIndex, isDark), maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                if (previewTeacher) {
                                    Text(sampleCourse.teacher, fontSize = (previewTextSize - 2).sp, color = courseTextColor(sampleCourse.colorIndex, isDark).copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                }
                                if (previewRoom) {
                                    val room = sampleCourse.room
                                    val displayRoom = if (!previewCampus) {
                                        val campusIdx = room.indexOf("校区")
                                        if (campusIdx > 0) room.substring(campusIdx + 2).trimStart(' ', '·', '-') else room
                                    } else room
                                    if (displayRoom.isNotEmpty()) {
                                        Text(displayRoom, fontSize = (previewTextSize - 2).sp, color = courseTextColor(sampleCourse.colorIndex, isDark).copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                    }
                                }
                            }
                        }
                    }
                    if (previewNonCurrentWeek) {
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(20.dp))
                            Box(
                                modifier = Modifier.weight(1f).height((previewHeight * 0.65f).dp.coerceAtLeast(32.dp))
                                    .clip(RoundedCornerShape(previewRadius.dp))
                                    .background(courseBgColor(4, isDark).copy(alpha = previewOtherWeekAlpha))
                                    .drawBehind { drawRoundRect(color = courseBorderColor(4, isDark), cornerRadius = CornerRadius(previewRadius.dp.toPx()), style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f), 0f))) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("非本周 · 虚线幽灵", fontSize = (previewTextSize - 1).sp, color = courseTextColor(4, isDark).copy(alpha = (previewOtherWeekAlpha * 2.78f).coerceIn(0.3f, 1f)), fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ═══ Display settings ═══
            Text("显示设置", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = colors.surfaceCard) {
                Column {
                    AppSettingSwitchRow("显示教师", "在课程卡片上显示教师姓名", previewTeacher) { previewTeacher = it }
                    SettingDiv()
                    AppSettingSwitchRow("显示教室", "在课程卡片上显示教学楼和教室", previewRoom) { previewRoom = it }
                    SettingDiv()
                    AppSettingSwitchRow("显示校区", "导入课程时附带的校区信息", previewCampus) { previewCampus = it }
                    SettingDiv()
                    AppSettingSwitchRow("显示节次时间", "左侧节次栏显示具体时间", previewSlotTime) { previewSlotTime = it }
                    SettingDiv()
                    AppSettingSwitchRow("非本周课程", "非本周课程半透明显示", previewNonCurrentWeek) { previewNonCurrentWeek = it }
                    SettingDiv()
                    AppSettingSwitchRow("文字居中", "课程文字居中对齐", previewCentered) { previewCentered = it }
                    SettingDiv()
                    AppSettingSwitchRow("单双周标识", "在课程卡片上显示单双周", previewOddEven) { previewOddEven = it }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ═══ Border style ═══
            Text("边框样式", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = colors.surfaceCard) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("无" to 0, "实线" to 1, "虚线" to 2).forEach { (label, style) ->
                        FilterChip(selected = previewBorderStyle == style, onClick = { previewBorderStyle = style }, label = { Text(label, fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.accentMain, selectedLabelColor = Color.White))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ═══ Grid background color palette ═══
            Text("课程表背景", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = colors.surfaceCard) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        FilterChip(
                            selected = previewBgColor == -1,
                            onClick = { previewBgColor = -1 },
                            label = { Text("默认", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.accentMain, selectedLabelColor = Color.White)
                        )
                    }
                    val paletteColors = GridBgPalette
                    val rows = paletteColors.chunked(5)
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEachIndexed { _, pair ->
                                val idx = paletteColors.indexOf(pair)
                                val (lightColor, darkColor) = pair
                                val displayColor = if (isDark) darkColor else lightColor
                                val selectedStroke = colors.accentMain
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(displayColor)
                                        .then(
                                            if (previewBgColor == idx)
                                                Modifier.drawBehind {
                                                    drawCircle(color = selectedStroke, radius = size.minDimension / 2, style = Stroke(width = 3.dp.toPx()))
                                                }
                                            else Modifier
                                        )
                                        .clickable { previewBgColor = idx },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (previewBgColor == idx) {
                                        Text("✓", color = if (isDark) Color.White else Color(0xFF424242), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ═══ Wallpaper picker ═══
            Text("页面壁纸", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = colors.surfaceCard) {
                val context = LocalContext.current
                val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    if (uri != null) {
                        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        previewWallpaper = uri.toString()
                    }
                }
                var wallpaperBitmap by remember(previewWallpaper) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
                LaunchedEffect(previewWallpaper) {
                    if (previewWallpaper.isNotEmpty()) {
                        withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val uri = android.net.Uri.parse(previewWallpaper)
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    BitmapFactory.decodeStream(stream)?.let { bitmap ->
                                        wallpaperBitmap = bitmap.asImageBitmap()
                                    }
                                }
                            } catch (_: Exception) { }
                        }
                    } else {
                        wallpaperBitmap = null
                    }
                }

                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (previewWallpaper.isNotEmpty() && wallpaperBitmap != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp))) {
                            Image(bitmap = wallpaperBitmap!!, contentDescription = "壁纸预览", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { imagePicker.launch("image/*") }, shape = RoundedCornerShape(8.dp)) {
                                Text("更换", fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = { previewWallpaper = ""; wallpaperBitmap = null }, shape = RoundedCornerShape(8.dp)) {
                                Text("移除", fontSize = 12.sp, color = colors.danger)
                            }
                        }
                    } else {
                        Icon(Icons.Outlined.Image, null, tint = colors.textTertiary.copy(alpha = 0.3f), modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("从相册选择图片作为课表壁纸", fontSize = 11.sp, color = colors.textTertiary)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { imagePicker.launch("image/*") }, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("选择图片", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ═══ Sliders ═══
            Text("参数调整", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = colors.surfaceCard) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    AppSettingSliderRow("格子高度", "${previewHeight}dp", previewHeight.toFloat(), 56f..96f) { previewHeight = it.toInt() }
                    AppSettingSliderRow("卡片圆角", "${previewRadius}dp", previewRadius.toFloat(), 0f..16f) { previewRadius = it.toInt() }
                    AppSettingSliderRow("不透明度", "${(previewOpacity * 100).toInt()}%", previewOpacity, 0.5f..1.0f) { previewOpacity = it }
                    AppSettingSliderRow("文字大小", "${previewTextSize}sp", previewTextSize.toFloat(), 10f..16f) { previewTextSize = it.toInt() }
                    AppSettingSliderRow("非本周透明度", "${(previewOtherWeekAlpha * 100).toInt()}%", previewOtherWeekAlpha, 0.05f..0.5f) { previewOtherWeekAlpha = it }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { resetDefaults() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.danger),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(colors.danger.copy(alpha = 0.5f)))
            ) { Text("恢复默认设置", fontSize = 14.sp) }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AppSettingSwitchRow(label: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = LocalEggRiceColors.current
    val isDark = LocalDarkMode.current
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            Text(subtitle, fontSize = 11.sp, color = colors.textTertiary)
        }
        Switch(checked = checked, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.accentMain, uncheckedThumbColor = colors.surfaceCard, uncheckedTrackColor = colors.borderDivider))
    }
}

@Composable
private fun AppSettingSliderRow(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    val colors = LocalEggRiceColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.width(72.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = 0, colors = SliderDefaults.colors(thumbColor = colors.accentMain, activeTrackColor = colors.accentMain, inactiveTrackColor = colors.borderDivider), modifier = Modifier.weight(1f))
        Text(valueLabel, fontSize = 12.sp, color = colors.textTertiary, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun SettingDiv() {
    val colors = LocalEggRiceColors.current
    HorizontalDivider(color = colors.borderDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
}

// ── MVI — AppearanceUiState + AppearanceIntent ──
data class AppearanceUiState(
    val showTeacher: Boolean = true,
    val showRoom: Boolean = true,
    val showCampus: Boolean = false,
    val showSlotTime: Boolean = false,
    val borderStyle: Int = 0,
    val textCentered: Boolean = false,
    val gridHeight: Int = 64,
    val cornerRadius: Int = 4,
    val gridOpacity: Float = 1.0f,
    val gridTextSize: Int = 12,
    val showNonCurrentWeek: Boolean = true,
    val showOddEven: Boolean = true,
    val gridBgColor: Int = -1,
    val otherWeekAlpha: Float = 0.50f,
    val wallpaperUri: String = ""
)

sealed interface AppearanceIntent {
    data class SetTeacher(val value: Boolean) : AppearanceIntent
    data class SetRoom(val value: Boolean) : AppearanceIntent
    data class SetCampus(val value: Boolean) : AppearanceIntent
    data class SetSlotTime(val value: Boolean) : AppearanceIntent
    data class SetBorderStyle(val style: Int) : AppearanceIntent
    data class SetTextCentered(val value: Boolean) : AppearanceIntent
    data class SetGridHeight(val height: Int) : AppearanceIntent
    data class SetCornerRadius(val radius: Int) : AppearanceIntent
    data class SetOpacity(val opacity: Float) : AppearanceIntent
    data class SetTextSize(val size: Int) : AppearanceIntent
    data class SetNonCurrentWeek(val value: Boolean) : AppearanceIntent
    data class SetOddEven(val value: Boolean) : AppearanceIntent
    data class SetBgColor(val index: Int) : AppearanceIntent
    data class SetOtherWeekAlpha(val alpha: Float) : AppearanceIntent
    data class SetWallpaper(val uri: String) : AppearanceIntent
    data object ResetDefaults : AppearanceIntent
}
