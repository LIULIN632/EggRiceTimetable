package com.eggrice.timetable.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.data.entity.CourseEntity
import com.eggrice.timetable.di.AppContainer
import com.eggrice.timetable.ui.theme.*

@Composable
internal fun AppearanceDialog(
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
    colorTheme: String,
    onDismiss: () -> Unit
) {
    val savedCornerRadius by container.cornerRadius.collectAsState()
    val colors = LocalEggRiceColors.current
    val isDark = LocalDarkMode.current

    var previewTeacher by remember { mutableStateOf(showTeacher) }
    var previewRoom by remember { mutableStateOf(showRoom) }
    var previewCampus by remember { mutableStateOf(showCampus) }
    var previewSlotTime by remember { mutableStateOf(showSlotTime) }
    var previewBorderStyle by remember { mutableIntStateOf(borderStyle) }
    var previewCentered by remember(textCentered) { mutableStateOf(textCentered) }
    var previewHeight by remember { mutableIntStateOf(gridHeight) }
    var previewRadius by remember { mutableIntStateOf(savedCornerRadius) }
    var previewOpacity by remember { mutableStateOf(gridOpacity) }
    var previewTextSize by remember { mutableIntStateOf(gridTextSize) }
    var previewNonCurrentWeek by remember { mutableStateOf(showNonCurrentWeek) }
    var previewOddEven by remember { mutableStateOf(showOddEven) }
    var previewColorTheme by remember { mutableStateOf(colorTheme) }

    val sampleCourse = remember {
        CourseEntity(name = "高等数学", teacher = "张教授", room = "明伦校区 综合教学楼 101", dayOfWeek = 1, startSlot = 1, endSlot = 2, colorIndex = 0)
    }

    fun commitAll() {
        if (previewTeacher != showTeacher) container.toggleShowTeacher()
        if (previewRoom != showRoom) container.toggleShowRoom()
        if (previewCampus != showCampus) container.toggleShowCampus()
        if (previewSlotTime != showSlotTime) container.toggleShowSlotTime()
        if (previewBorderStyle != borderStyle) container.setBorderStyle(previewBorderStyle)
        if (previewCentered != textCentered) container.setTextCentered(previewCentered)
        if (previewNonCurrentWeek != showNonCurrentWeek) container.toggleShowNonCurrentWeek()
        if (previewOddEven != showOddEven) container.toggleShowOddEven()
        if (previewColorTheme != colorTheme) container.setColorTheme(previewColorTheme)
        container.setGridHeight(previewHeight)
        container.setCornerRadius(previewRadius)
        container.setGridOpacity(previewOpacity)
        container.setGridTextSize(previewTextSize)
    }

    fun resetDefaults() {
        previewTeacher = true; previewRoom = true; previewCampus = false; previewSlotTime = false
        previewBorderStyle = 0; previewCentered = false; previewHeight = 72; previewRadius = 12
        previewOpacity = 1f; previewTextSize = 13; previewNonCurrentWeek = true
        previewOddEven = true; previewColorTheme = "default"
    }

    AlertDialog(
        onDismissRequest = { commitAll(); onDismiss() },
        title = { Text("个性化配置", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // ═══ Preview card ═══
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("实时预览", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textTertiary)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    val previewInfoParts = mutableListOf<String>()
                                    if (previewTeacher) previewInfoParts.add(sampleCourse.teacher)
                                    if (previewRoom) {
                                        val room = sampleCourse.room
                                        val displayRoom = if (!previewCampus) {
                                            val campusIdx = room.indexOf("校区")
                                            if (campusIdx > 0) room.substring(campusIdx + 2).trimStart(' ', '·', '-') else room
                                        } else room
                                        if (displayRoom.isNotEmpty()) previewInfoParts.add(displayRoom)
                                    }
                                    if (previewInfoParts.isNotEmpty()) {
                                        Text(previewInfoParts.joinToString(" · "), fontSize = (previewTextSize - 2).sp, color = courseTextColor(sampleCourse.colorIndex, isDark).copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
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
                                        .background(courseBgColor(4, isDark).copy(alpha = 0.18f))
                                        .drawBehind { drawRoundRect(color = courseBorderColor(4, isDark), cornerRadius = CornerRadius(previewRadius.dp.toPx()), style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f), 0f))) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("非本周 · 虚线幽灵", fontSize = (previewTextSize - 1).sp, color = courseTextColor(4, isDark).copy(alpha = 0.5f), fontWeight = FontWeight.Medium, maxLines = 1)
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
                        AppSwitchRow("显示教师", "在课程卡片上显示教师姓名", previewTeacher) { previewTeacher = it }
                        HDiv()
                        AppSwitchRow("显示教室", "在课程卡片上显示教学楼和教室", previewRoom) { previewRoom = it }
                        HDiv()
                        AppSwitchRow("显示校区", "导入课程时附带的校区信息", previewCampus) { previewCampus = it }
                        HDiv()
                        AppSwitchRow("显示节次时间", "左侧节次栏显示具体时间", previewSlotTime) { previewSlotTime = it }
                        HDiv()
                        AppSwitchRow("非本周课程", "非本周课程半透明显示", previewNonCurrentWeek) { previewNonCurrentWeek = it }
                        HDiv()
                        AppSwitchRow("文字居中", "课程文字居中对齐", previewCentered) { previewCentered = it }
                        HDiv()
                        AppSwitchRow("单双周标识", "在课程卡片上显示单双周", previewOddEven) { previewOddEven = it }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ═══ Color theme ═══
                Text("配色主题", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = colors.surfaceCard) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("海盐蓝" to "default", "马卡龙蓝" to "macaron_blue", "马卡龙粉" to "macaron_pink", "抹茶绿" to "matcha", "樱花粉" to "sakura", "紫藤紫" to "wisteria", "蛋炒饭" to "fried_rice").forEach { (label, theme) ->
                            FilterChip(selected = previewColorTheme == theme, onClick = { previewColorTheme = theme }, label = { Text(label, fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.accentMain, selectedLabelColor = Color.White))
                        }
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

                Spacer(Modifier.height(20.dp))

                // ═══ Sliders ═══
                Text("参数调整", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = colors.surfaceCard) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        AppSliderRow("格子高度", "${previewHeight}dp", previewHeight.toFloat(), 56f..96f) { previewHeight = it.toInt() }
                        AppSliderRow("卡片圆角", "${previewRadius}dp", previewRadius.toFloat(), 0f..16f) { previewRadius = it.toInt() }
                        AppSliderRow("不透明度", "${(previewOpacity * 100).toInt()}%", previewOpacity, 0.5f..1.0f) { previewOpacity = it }
                        AppSliderRow("文字大小", "${previewTextSize}sp", previewTextSize.toFloat(), 10f..16f) { previewTextSize = it.toInt() }
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
                Spacer(Modifier.height(8.dp))
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { commitAll(); onDismiss() }) { Text("完成", fontWeight = FontWeight.Bold, color = colors.accentMain) } }
    )
}

@Composable
private fun AppSwitchRow(label: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
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
private fun AppSliderRow(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    val colors = LocalEggRiceColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.width(72.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = 0, colors = SliderDefaults.colors(thumbColor = colors.accentMain, activeTrackColor = colors.accentMain, inactiveTrackColor = colors.borderDivider), modifier = Modifier.weight(1f))
        Text(valueLabel, fontSize = 12.sp, color = colors.textTertiary, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun HDiv() {
    val colors = LocalEggRiceColors.current
    HorizontalDivider(color = colors.borderDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
}
