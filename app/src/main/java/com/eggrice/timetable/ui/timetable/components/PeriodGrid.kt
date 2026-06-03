package com.eggrice.timetable.ui.timetable.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.eggrice.timetable.data.entity.CourseEntity
import com.eggrice.timetable.data.entity.TimeSlotEntity
import com.eggrice.timetable.ui.theme.*
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun PeriodGrid(
    timeSlots: List<TimeSlotEntity>,
    courses: List<CourseEntity>,
    currentWeek: Int,
    isCurrentWeek: Boolean = false,
    showTeacher: Boolean,
    showRoom: Boolean,
    showCampus: Boolean = false,
    showSlotTime: Boolean,
    showDashedBorder: Boolean,
    textCentered: Boolean,
    gridHeight: Int,
    cornerRadius: Int,
    gridOpacity: Float,
    gridTextSize: Int,
    showOddEven: Boolean,
    borderStyle: Int = 0,
    nonCurrentCourses: List<CourseEntity> = emptyList(),
    showNonCurrentWeek: Boolean = true,
    vibrationMode: Int = 1,
    gridBgColor: Int = -1,
    otherWeekAlpha: Float = 0.18f,
    homeworkCourseNames: Set<String> = emptySet(),
    onCourseClick: (CourseEntity) -> Unit,
    onEmptyCellClick: (Int, Int) -> Unit,
    onCourseMoved: (CourseEntity, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = java.time.LocalDate.now()
    val todayDay = today.dayOfWeek.value
    val now = LocalTime.now()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val isDark = LocalDarkMode.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val vibrator = remember { context.getSystemService(android.os.Vibrator::class.java) }

    fun triggerVibration() {
        if (vibrationMode == 0 || vibrator == null) return
        val duration = when (vibrationMode) {
            1 -> 15L; 2 -> 35L; 3 -> 60L
            else -> return
        }
        try {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: SecurityException) { }
    }
    val safeGridHeight = gridHeight.coerceAtLeast(1)

    val currentPeriod = remember(now, timeSlots) {
        if (!isCurrentWeek) -1
        else timeSlots.indexOfFirst {
            try {
                val start = LocalTime.parse(it.startTime)
                val end = LocalTime.parse(it.endTime)
                now in start..end
            } catch (_: Exception) { false }
        }.let { if (it >= 0) it + 1 else -1 }
    }

    // Drag state
    var draggedCourse by remember { mutableStateOf<CourseEntity?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragTargetDay by remember { mutableStateOf(0) }
    var dragTargetSlot by remember { mutableStateOf(0) }
    var gridPixelWidth by remember { mutableStateOf(0f) }
    var sidebarWidthPx by remember { mutableStateOf(0f) }

    val allDisplayCourses = remember(courses, nonCurrentCourses, showNonCurrentWeek) {
        if (showNonCurrentWeek) {
            val currentIds = courses.map { it.id }.toSet()
            courses + nonCurrentCourses.filter { it.id !in currentIds }
        } else {
            courses
        }
    }

    val currentCourseIds = remember(courses) { courses.map { it.id }.toSet() }
    val nonCurrentIds = remember(allDisplayCourses, currentCourseIds) {
        allDisplayCourses.map { it.id }.toSet() - currentCourseIds
    }

    val cellW = remember(gridPixelWidth, sidebarWidthPx) {
        val w = if (gridPixelWidth > 0f) (gridPixelWidth - sidebarWidthPx) / 7f else 0f
        if (w > 0f) w else 0f
    }
    val cellH = with(density) { safeGridHeight.toFloat().dp.roundToPx().toFloat().coerceAtLeast(1f) }

    val totalGridHeight = timeSlots.size * safeGridHeight
    val cellWDp = with(density) { cellW.toDp() }
    val sidebarDp = with(density) { sidebarWidthPx.toDp() }

    // Split and group separately: non-current rendered behind, current on top
    val currentByCell = remember(allDisplayCourses, nonCurrentIds) {
        allDisplayCourses.filter { it.id !in nonCurrentIds }
            .groupBy { it.dayOfWeek to it.startSlot }
    }
    val nonCurrentByCell = remember(allDisplayCourses, nonCurrentIds) {
        allDisplayCourses.filter { it.id in nonCurrentIds }
            .groupBy { it.dayOfWeek to it.startSlot }
    }

    Box(
        modifier = modifier.fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { size -> gridPixelWidth = size.width.toFloat() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .verticalScroll(scrollState, enabled = draggedCourse == null)
        ) {
            // Single tall Box containing grid backgrounds + absolutely-positioned course cards
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((totalGridHeight).dp)
            ) {
                // ── Layer 1: Grid backgrounds ──
                timeSlots.forEachIndexed { slotIdx, slot ->
                    val slotNum = slotIdx + 1
                    val isCurrentPeriod = slotNum == currentPeriod
                    val rowYDp = (slotIdx * safeGridHeight).dp

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(safeGridHeight.dp)
                            .offset(y = rowYDp)
                    ) {
                        // Period sidebar
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .fillMaxHeight()
                                .onSizeChanged { sidebarWidthPx = it.width.toFloat() }
                                .background(
                                    if (isCurrentPeriod) accentColor().copy(alpha = if (isDark) 0.18f else 0.12f)
                                    else if (isDark) DarkSurfaceCard
                                    else SurfaceCard
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = slot.slot.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrentPeriod) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isCurrentPeriod) accentColor()
                                        else if (isDark) DarkTextSecondary
                                        else TextSecondary
                                )
                                if (showSlotTime) {
                                    Text(
                                        text = slot.startTime,
                                        fontSize = 10.sp,
                                        color = if (isDark) DarkTextTertiary else TextTertiary
                                    )
                                }
                            }
                        }

                        // 7 day cells — background + border + highlights only
                        for (day in 1..7) {
                            val isToday = isCurrentWeek && day == todayDay
                            val isDragTarget = draggedCourse != null && dragTargetDay == day && dragTargetSlot == slotNum

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 1.dp)
                                    .then(
                                        when (borderStyle) {
                                            1 -> Modifier.drawBehind {
                                                drawRoundRect(color = if (isDark) BorderDark else BorderLight, cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(width = 1.dp.toPx()))
                                            }
                                            2 -> Modifier.drawBehind {
                                                drawRoundRect(color = if (isDark) BorderDark else BorderLight, cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)))
                                            }
                                            else -> Modifier
                                        }
                                    )
                                    .then(
                                        if (isDragTarget) {
                                            val dragAccent = accentColor()
                                            Modifier.drawBehind {
                                                drawRoundRect(color = dragAccent.copy(alpha = 0.35f), cornerRadius = CornerRadius(4.dp.toPx()), size = size)
                                                drawRoundRect(color = dragAccent, cornerRadius = CornerRadius(4.dp.toPx()), size = size, style = Stroke(width = 2.dp.toPx()))
                                            }
                                        }
                                        else Modifier
                                    )
                                    .then(
                                        if (isCurrentPeriod)
                                            Modifier.background(accentColor().copy(alpha = if (isDark) 0.10f else 0.08f), RoundedCornerShape(4.dp))
                                        else Modifier
                                    )
                                    .then(
                                        if (gridBgColor >= 0 && gridBgColor < GridBgPalette.size) {
                                            val (lightBg, darkBg) = GridBgPalette[gridBgColor]
                                            Modifier.background(if (isDark) darkBg else lightBg, RoundedCornerShape(4.dp))
                                        } else Modifier
                                    )
                                    .clickable { triggerVibration(); onEmptyCellClick(day, slotNum) }
                            )
                        }
                    }
                }

                // ── Layer 2a: Non-current week courses (behind, dashed ghost style) ──
                if (sidebarWidthPx > 0f && cellW > 0f) {
                    nonCurrentByCell.forEach { (cell, coursesAtCell) ->
                        val (day, startSlot) = cell
                        val overlapCount = coursesAtCell.size
                        val cellPad = 1.dp
                        val gap = if (overlapCount > 1) 1.dp else 0.dp
                        val usableWidth = cellWDp - cellPad * 2
                        val groupWidth = usableWidth * 0.65f
                        val groupXOffset = (usableWidth - groupWidth) / 2
                        val cardWidth = if (overlapCount > 1)
                            (groupWidth - gap * (overlapCount - 1)) / overlapCount
                        else groupWidth
                        val cornerDp = cornerRadius.dp

                        coursesAtCell.forEachIndexed { idx, course ->
                            val isDragging = draggedCourse?.id == course.id
                            if (isDragging) return@forEachIndexed

                            val spanSlots = (course.endSlot - course.startSlot + 1).coerceAtLeast(1)
                            val cardHeightDp = spanSlots * safeGridHeight
                            val bgColor = courseBgColor(course.colorIndex, isDark)
                            val textColor = courseTextColor(course.colorIndex, isDark)
                            val xDp = sidebarDp + cellPad + cellWDp * (day - 1) + groupXOffset + (cardWidth + gap) * idx
                            val yDp = safeGridHeight.dp * (startSlot - 1)

                            Box(
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(cardHeightDp.dp)
                                    .offset(x = xDp, y = yDp)
                                    .clip(RoundedCornerShape(cornerDp))
                                    .background(bgColor.copy(alpha = otherWeekAlpha))
                                    .drawBehind {
                                        drawRoundRect(
                                            color = courseBorderColor(course.colorIndex, isDark),
                                            cornerRadius = CornerRadius(cornerDp.toPx()),
                                            style = Stroke(
                                                width = 1.5.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f), 0f)
                                            )
                                        )
                                    }
                                    .clickable {
                                        if (draggedCourse == null) {
                                            triggerVibration()
                                            onCourseClick(course)
                                        }
                                    }
                                    .padding(horizontal = if (overlapCount > 1) 3.dp else 5.dp, vertical = 2.dp),
                                contentAlignment = if (textCentered) Alignment.Center else Alignment.TopStart
                            ) {
                                CourseCardContent(course, textColor, true, showTeacher, showRoom, showCampus, showOddEven, textCentered, gridTextSize, course.name in homeworkCourseNames, otherWeekAlpha)
                            }
                        }
                    }

                    // ── Layer 2b: Current week courses (on top, solid style with drag) ──
                    currentByCell.forEach { (cell, coursesAtCell) ->
                        val (day, startSlot) = cell
                        val overlapCount = coursesAtCell.size
                        val cellPad = 1.dp
                        val gap = if (overlapCount > 1) 1.dp else 0.dp
                        val usableWidth = cellWDp - cellPad * 2
                        val cardWidth = if (overlapCount > 1)
                            (usableWidth - gap * (overlapCount - 1)) / overlapCount
                        else usableWidth

                        coursesAtCell.forEachIndexed { idx, course ->
                            val spanSlots = (course.endSlot - course.startSlot + 1).coerceAtLeast(1)
                            val cardHeightDp = spanSlots * safeGridHeight
                            val bgColor = courseBgColor(course.colorIndex, isDark)
                            val textColor = courseTextColor(course.colorIndex, isDark)
                            val xDp = sidebarDp + cellPad + cellWDp * (day - 1) + (cardWidth + gap) * idx
                            val yDp = safeGridHeight.dp * (startSlot - 1)

                            Box(
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(cardHeightDp.dp)
                                    .offset(x = xDp, y = yDp)
                                    .zIndex(10f)
                                    .shadow(2.dp, RoundedCornerShape(cornerRadius.dp))
                                    .clip(RoundedCornerShape(cornerRadius.dp))
                                    .background(bgColor.copy(alpha = gridOpacity))
                                    .drawBehind {
                                        if (borderStyle > 0) {
                                            drawRoundRect(
                                                color = courseBorderColor(course.colorIndex, isDark),
                                                cornerRadius = CornerRadius(cornerRadius.dp.toPx()),
                                                style = Stroke(
                                                    width = 1.dp.toPx(),
                                                    pathEffect = if (borderStyle == 2) PathEffect.dashPathEffect(floatArrayOf(6f, 3f), 0f) else null
                                                )
                                            )
                                        }
                                    }
                                    .pointerInput(course.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                triggerVibration()
                                                draggedCourse = course
                                                dragOffset = Offset.Zero
                                                dragTargetDay = course.dayOfWeek
                                                dragTargetSlot = course.startSlot
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount
                                                val c = draggedCourse ?: return@detectDragGesturesAfterLongPress
                                                if (cellW > 0f && cellH > 0f) {
                                                    dragTargetDay = (c.dayOfWeek + (dragOffset.x / cellW).roundToInt()).coerceIn(1, 7)
                                                    dragTargetSlot = (c.startSlot + (dragOffset.y / cellH).roundToInt()).coerceIn(1, timeSlots.size.coerceAtLeast(1))
                                                }
                                            },
                                            onDragEnd = {
                                                val c = draggedCourse
                                                draggedCourse = null
                                                if (c != null) {
                                                    val td = dragTargetDay.coerceIn(1, 7)
                                                    val ts = dragTargetSlot.coerceIn(1, timeSlots.size.coerceAtLeast(1))
                                                    dragOffset = Offset.Zero
                                                    dragTargetDay = 0; dragTargetSlot = 0
                                                    if (td != c.dayOfWeek || ts != c.startSlot) {
                                                        onCourseMoved(c, td, ts)
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                draggedCourse = null
                                                dragOffset = Offset.Zero
                                                dragTargetDay = 0; dragTargetSlot = 0
                                            }
                                        )
                                    }
                                    .clickable {
                                        if (draggedCourse == null) {
                                            triggerVibration()
                                            onCourseClick(course)
                                        }
                                    }
                                    .padding(horizontal = if (overlapCount > 1) 4.dp else 6.dp, vertical = 3.dp),
                                contentAlignment = if (textCentered) Alignment.Center else Alignment.TopStart
                            ) {
                                CourseCardContent(course, textColor, false, showTeacher, showRoom, showCampus, showOddEven, textCentered, gridTextSize, course.name in homeworkCourseNames)
                            }
                        }
                    }
                }
            }
        }

        // Floating card during drag (rendered outside scroll in outer Box)
        draggedCourse?.let { course ->
            if (cellW > 0f && cellH > 0f) {
                val spanSlots = (course.endSlot - course.startSlot + 1).coerceAtLeast(1)
                val floatBg = courseBgColor(course.colorIndex, isDark)
                val floatText = courseTextColor(course.colorIndex, isDark)
                val cellPadPx = with(density) { 1.dp.toPx() }
                val xPx = (cellPadPx + sidebarWidthPx + (course.dayOfWeek - 1) * cellW + dragOffset.x).roundToInt()
                val yPx = ((course.startSlot - 1) * cellH + dragOffset.y - scrollState.value).roundToInt()

                Box(
                    modifier = Modifier
                        .width(with(density) { cellW.toDp() - 2.dp })
                        .height((spanSlots * safeGridHeight).dp)
                        .offset { IntOffset(xPx, yPx) }
                        .zIndex(200f)
                        .shadow(16.dp, RoundedCornerShape(cornerRadius.dp))
                        .clip(RoundedCornerShape(cornerRadius.dp))
                        .background(floatBg.copy(alpha = gridOpacity))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = if (textCentered) Alignment.Center else Alignment.TopStart
                ) {
                    CourseCardContent(course, floatText, false, showTeacher, showRoom, showCampus, showOddEven, textCentered, gridTextSize, course.name in homeworkCourseNames)
                }
            }
        }
    }
}

@Composable
private fun CourseCardContent(
    course: CourseEntity,
    textColor: Color,
    isNonCurrent: Boolean,
    showTeacher: Boolean,
    showRoom: Boolean,
    showCampus: Boolean,
    showOddEven: Boolean,
    textCentered: Boolean,
    gridTextSize: Int,
    hasHomework: Boolean = false,
    otherWeekAlpha: Float = 0.18f
) {
    val nameAlpha = if (isNonCurrent) (otherWeekAlpha * 2.78f).coerceIn(0.3f, 1f) else 1f
    val infoAlpha = if (isNonCurrent) (otherWeekAlpha * 1.67f).coerceIn(0.2f, 0.8f) else 0.6f

    val infoParts = mutableListOf<String>()
    if (isNonCurrent) infoParts.add("非本周")
    if (showTeacher && course.teacher.isNotEmpty()) infoParts.add(course.teacher)
    if (showRoom && course.room.isNotEmpty()) {
        val room = course.room
        val displayRoom = if (!showCampus) {
            val campusIdx = room.indexOf("校区")
            if (campusIdx > 0) room.substring(campusIdx + 2).trimStart(' ', '·', '-')
            else room
        } else room
        if (displayRoom.isNotEmpty()) infoParts.add(displayRoom)
    }
    if (showOddEven && course.weekType != "all") {
        infoParts.add(if (course.weekType == "odd") "单周" else "双周")
    }
    val infoLine = infoParts.joinToString(" · ")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (textCentered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = course.name,
                color = textColor.copy(alpha = nameAlpha),
                fontSize = gridTextSize.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Clip,
                softWrap = true,
                textAlign = if (textCentered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
            if (hasHomework) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = "有作业",
                    tint = Color(0xFFFFB800),
                    modifier = Modifier.size((gridTextSize + 2).dp)
                )
            }
        }
        if (infoLine.isNotEmpty()) {
            Text(
                text = infoLine,
                color = textColor.copy(alpha = infoAlpha),
                fontSize = (gridTextSize - 2).sp,
                maxLines = 2,
                overflow = TextOverflow.Clip,
                softWrap = true,
                textAlign = if (textCentered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
