package com.eggrice.timetable.ui.timetable.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
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
import com.eggrice.timetable.ui.timetable.MergedCourseBlock
import com.eggrice.timetable.ui.timetable.mergeCourses
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    textCentered: Boolean,
    gridHeightProvider: () -> Int,
    cornerRadius: Int,
    gridOpacityProvider: () -> Float,
    gridTextSize: Int,
    showOddEven: Boolean,
    borderStyle: Int = 0,
    nonCurrentCourses: List<CourseEntity> = emptyList(),
    showNonCurrentWeek: Boolean = true,
    vibrationMode: Int = 1,
    gridBgColor: Int = -1,
    otherWeekAlpha: Float = 0.50f,
    verticalLayout: Boolean = true,
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
    val colors = LocalEggRiceColors.current
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
    val safeGridHeight = gridHeightProvider().coerceAtLeast(1)
    val gridOpacity = gridOpacityProvider()

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
    var viewportHeightPx by remember { mutableStateOf(0f) }

    // Optimistic position overrides — applied immediately on drag end,
    // cleared when the underlying course list syncs from DB.
    var optimisticMoves by remember { mutableStateOf(mapOf<Long, Triple<Int, Int, Int>>()) }
    // Clear optimistic moves when courses list changes (DB sync complete)
    val coursesKey = remember(courses) { courses.hashCode() }
    LaunchedEffect(coursesKey) {
        if (optimisticMoves.isNotEmpty()) optimisticMoves = emptyMap()
    }

    // P0 冲突块状态：底部弹窗 + 块内当前显示课程（默认主课程）
    var conflictSheet by remember { mutableStateOf<MergedCourseBlock?>(null) }
    var conflictActiveIds by remember { mutableStateOf(mapOf<String, Long>()) }

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
    // Apply optimistic moves so dragged courses reflect their new position instantly
    val currentCoursesResolved = remember(allDisplayCourses, nonCurrentIds, optimisticMoves) {
        allDisplayCourses.filter { it.id !in nonCurrentIds }.map { course ->
            val om = optimisticMoves[course.id]
            if (om != null) course.copy(dayOfWeek = om.first, startSlot = om.second, endSlot = om.third)
            else course
        }
    }
    val nonCurrentResolved = remember(allDisplayCourses, nonCurrentIds, optimisticMoves) {
        allDisplayCourses.filter { it.id in nonCurrentIds }.map { course ->
            val om = optimisticMoves[course.id]
            if (om != null) course.copy(dayOfWeek = om.first, startSlot = om.second, endSlot = om.third)
            else course
        }
    }
    val nonCurrentByCell = remember(nonCurrentResolved) {
        nonCurrentResolved.groupBy { it.dayOfWeek to it.startSlot }
    }

    // Drag target span — how many cells the dragged course occupies
    val dragSpanSlots = draggedCourse?.let { (it.endSlot - it.startSlot + 1).coerceAtLeast(1) } ?: 0

    Box(
        modifier = modifier.fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { size -> gridPixelWidth = size.width.toFloat() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .onSizeChanged { viewportHeightPx = it.height.toFloat() }
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
                                    if (isCurrentPeriod) colors.accentMain.copy(alpha = if (isDark) 0.18f else 0.12f)
                                    else colors.surfaceCard
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = slot.slot.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isCurrentPeriod) colors.accentMain
                                        else colors.textSecondary
                                )
                                if (showSlotTime) {
                                    Text(
                                        text = slot.startTime,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = colors.textTertiary
                                    )
                                }
                            }
                        }

                        // 7 day cells — background + border + highlights only
                        for (day in 1..7) {
                            val isToday = isCurrentWeek && day == todayDay
                            val isDragTarget = draggedCourse != null && dragTargetDay == day
                                && slotNum in dragTargetSlot until (dragTargetSlot + dragSpanSlots)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 0.5.dp)
                                    .drawBehind {
                                        drawRoundRect(
                                            color = Color(0xFFE9ECEF),
                                            cornerRadius = CornerRadius(2.dp.toPx()),
                                            style = Stroke(width = 0.5.dp.toPx())
                                        )
                                    }
                                    .then(
                                        if (isDragTarget) {
                                            val dragAccent = colors.accentMain
                                            Modifier.drawBehind {
                                                drawRoundRect(color = dragAccent.copy(alpha = 0.35f), cornerRadius = CornerRadius(4.dp.toPx()), size = size)
                                                drawRoundRect(color = dragAccent, cornerRadius = CornerRadius(4.dp.toPx()), size = size, style = Stroke(width = 2.dp.toPx()))
                                            }
                                        }
                                        else Modifier
                                    )
                                    .then(
                                        if (gridBgColor >= 0 && gridBgColor < GridBgPalette.size) {
                                            val (lightBg, darkBg) = GridBgPalette[gridBgColor]
                                            Modifier.background(if (isDark) darkBg else lightBg, RoundedCornerShape(4.dp))
                                        } else Modifier
                                    )
                                    .then(
                                        if (borderStyle > 0) {
                                            val cellBorderColor = if (isDark)
                                                colors.borderDivider.copy(red = (colors.borderDivider.red * 1.8f).coerceIn(0f, 1f), green = (colors.borderDivider.green * 1.8f).coerceIn(0f, 1f), blue = (colors.borderDivider.blue * 1.8f).coerceIn(0f, 1f))
                                            else
                                                colors.borderDivider.copy(red = (colors.borderDivider.red * 0.7f).coerceIn(0f, 1f), green = (colors.borderDivider.green * 0.7f).coerceIn(0f, 1f), blue = (colors.borderDivider.blue * 0.7f).coerceIn(0f, 1f))
                                            Modifier.drawBehind {
                                                drawRoundRect(
                                                    color = cellBorderColor,
                                                    cornerRadius = CornerRadius(4.dp.toPx()),
                                                    style = Stroke(
                                                        width = 1.dp.toPx(),
                                                        pathEffect = if (borderStyle == 2) PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f) else null
                                                    )
                                                )
                                            }
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
                                    .background(bgColor.copy(alpha = if (isDragging) otherWeekAlpha * 0.3f else otherWeekAlpha))
                                    .then(
                                        if (!isDragging) Modifier.drawBehind {
                                            drawRoundRect(
                                                color = courseBorderColor(course.colorIndex, isDark),
                                                cornerRadius = CornerRadius(cornerDp.toPx()),
                                                style = Stroke(
                                                    width = 1.5.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f), 0f)
                                                )
                                            )
                                        } else Modifier
                                    )
                                    .clickable {
                                        if (draggedCourse == null) {
                                            triggerVibration()
                                            onCourseClick(course)
                                        }
                                    }
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                contentAlignment = if (textCentered) Alignment.Center else Alignment.TopStart
                            ) {
                                CourseCardContent(course, textColor, true, showTeacher, showRoom, showCampus, showOddEven, textCentered, gridTextSize, course.name in homeworkCourseNames, if (isDragging) 0.15f else otherWeekAlpha, verticalLayout)
                            }
                        }
                    }

                    // ── Layer 2b: Current week courses (merged blocks, on top, solid style with drag) ──
                    val currentBlocks = remember(currentCoursesResolved, currentWeek) {
                        mergeCourses(currentCoursesResolved, currentWeek).groupBy { it.day }
                    }
                    val cellPad = 1.dp
                    val usableWidth = cellWDp - cellPad * 2

                    currentBlocks.forEach { (day, blocks) ->
                        blocks.forEach { block ->
                            val blockKey = blockKeyOf(block)
                            // 默认显示：用户选择的课程 → 第一个本周活跃课 → 块内第一门
                            val mainCourse = block.courses.firstOrNull { it.course.id == conflictActiveIds[blockKey] }?.course
                                ?: block.courses.firstOrNull { it.isActive }?.course
                                ?: block.courses.firstOrNull()?.course
                                ?: return@forEach
                            val isDragging = block.courses.any { it.course.id == draggedCourse?.id }
                            val isConflict = block.isConflict

                            val spanSlots = (block.end - block.start).coerceAtLeast(0.3f)
                            val cardHeightDp = spanSlots * safeGridHeight
                            val bgColor = courseBgColor(mainCourse.colorIndex, isDark)
                            val textColor = courseTextColor(mainCourse.colorIndex, isDark)
                            val xDp = sidebarDp + cellPad + cellWDp * (day - 1)
                            val yDp = safeGridHeight.dp * (block.start - 1)

                            Box(
                                modifier = Modifier
                                    .width(usableWidth)
                                    .height(cardHeightDp.dp)
                                    .offset(x = xDp, y = yDp)
                                    .zIndex(10f)
                                    .then(
                                        if (isDragging) Modifier
                                        else Modifier
                                            .shadow(2.dp, RoundedCornerShape(cornerRadius.dp))
                                    )
                                    .clip(RoundedCornerShape(cornerRadius.dp))
                                    .background(bgColor.copy(alpha = if (isDragging) gridOpacity * 0.2f else gridOpacity))
                                    .then(
                                        if (!isDragging && borderStyle > 0) Modifier.drawBehind {
                                            drawRoundRect(
                                                color = courseBorderColor(mainCourse.colorIndex, isDark),
                                                cornerRadius = CornerRadius(cornerRadius.dp.toPx()),
                                                style = Stroke(
                                                    width = 1.dp.toPx(),
                                                    pathEffect = if (borderStyle == 2) PathEffect.dashPathEffect(floatArrayOf(6f, 3f), 0f) else null
                                                )
                                            )
                                        } else Modifier
                                    )
                                    .then(
                                        if (isConflict) {
                                            // 冲突块第一版：禁用拖动（拖动作用于块内哪门课是状态泥潭），
                                            // 点击进弹窗切换，长按提示先解决冲突
                                            Modifier.combinedClickable(
                                                onClick = {
                                                    if (draggedCourse == null) {
                                                        triggerVibration()
                                                        conflictSheet = block
                                                    }
                                                },
                                                onLongClick = {
                                                    android.widget.Toast.makeText(context, "课程重叠，点击角标选择要显示的课程", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            Modifier
                                                .pointerInput(block.courses.map { it.course.id }) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            triggerVibration()
                                                            draggedCourse = mainCourse
                                                            dragOffset = Offset.Zero
                                                            dragTargetDay = mainCourse.dayOfWeek
                                                            dragTargetSlot = mainCourse.startSlot
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffset += dragAmount
                                                            val c = draggedCourse ?: return@detectDragGesturesAfterLongPress
                                                            if (cellW > 0f && cellH > 0f) {
                                                                dragTargetDay = (c.dayOfWeek + (dragOffset.x / cellW).roundToInt()).coerceIn(1, 7)
                                                                val span = (c.endSlot - c.startSlot).coerceAtLeast(0)
                                                                val maxSlot = (timeSlots.size - span).coerceAtLeast(1)
                                                                dragTargetSlot = (c.startSlot + (dragOffset.y / cellH).roundToInt()).coerceIn(1, maxSlot)
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            val c = draggedCourse
                                                            draggedCourse = null
                                                            if (c != null) {
                                                                val span = (c.endSlot - c.startSlot).coerceAtLeast(0)
                                                                val maxSlot = (timeSlots.size - span).coerceAtLeast(1)
                                                                val td = dragTargetDay.coerceIn(1, 7)
                                                                val ts = dragTargetSlot.coerceIn(1, maxSlot)
                                                                dragOffset = Offset.Zero
                                                                dragTargetDay = 0; dragTargetSlot = 0
                                                                if (td != c.dayOfWeek || ts != c.startSlot) {
                                                                    optimisticMoves = optimisticMoves + (c.id to Triple(td, ts, c.endSlot - c.startSlot + ts))
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
                                                        onCourseClick(mainCourse)
                                                    }
                                                }
                                        }
                                    )
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                contentAlignment = if (textCentered) Alignment.Center else Alignment.TopStart
                            ) {
                                CourseCardContent(mainCourse, textColor, false, showTeacher, showRoom, showCampus, showOddEven, textCentered, gridTextSize, mainCourse.name in homeworkCourseNames, if (isDragging) 0.5f else 1f, verticalLayout)
                                // 冲突角标：右上角重叠课程数（仅真正的重叠冲突，相邻同名合并不加角标）
                                if (isConflict) {
                                    ConflictBadge(
                                        count = block.courses.count { it.isActive }.coerceAtLeast(1),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 2.dp, end = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating card during drag (rendered outside scroll in outer Box)
        val liftScale by animateFloatAsState(
            targetValue = if (draggedCourse != null) 1.06f else 1f,
            animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
            label = "dragLift"
        )
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
                        .scale(liftScale)
                        .shadow(if (liftScale > 1.02f) 20.dp else 10.dp, RoundedCornerShape(cornerRadius.dp))
                        .clip(RoundedCornerShape(cornerRadius.dp))
                        .background(floatBg.copy(alpha = gridOpacity))
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    contentAlignment = if (textCentered) Alignment.Center else Alignment.TopStart
                ) {
                    CourseCardContent(course, floatText, false, showTeacher, showRoom, showCampus, showOddEven, textCentered, gridTextSize, course.name in homeworkCourseNames, otherWeekAlpha = 1f, verticalLayout = verticalLayout)
                }
            }
        }

        // Autoscroll while dragging near edges
        LaunchedEffect(draggedCourse) {
            val course = draggedCourse ?: return@LaunchedEffect
            while (draggedCourse != null) {
                val scrollPx = scrollState.value.toFloat()
                val courseStartYPx = (course.startSlot - 1) * cellH
                val dragYInView = courseStartYPx + dragOffset.y - scrollPx
                val edgePx = viewportHeightPx * 0.15f
                val speed = when {
                    dragYInView < edgePx && scrollPx > 0f ->
                        ((dragYInView - edgePx) / edgePx * 12f).coerceIn(-12f, 0f)
                    scrollPx < scrollState.maxValue && dragYInView > viewportHeightPx - edgePx ->
                        ((dragYInView - (viewportHeightPx - edgePx)) / edgePx * 12f).coerceIn(0f, 12f)
                    else -> 0f
                }
                if (speed != 0f) {
                    scrollState.scrollTo((scrollPx + speed).roundToInt().coerceIn(0, scrollState.maxValue))
                }
                kotlinx.coroutines.delay(16L)
            }
        }

        // 冲突课程选择底部弹窗
        conflictSheet?.let { block ->
            ConflictCoursesSheet(
                block = block,
                activeId = conflictActiveIds[blockKeyOf(block)],
                onSelect = { id ->
                    conflictActiveIds = conflictActiveIds + (blockKeyOf(block) to id)
                    conflictSheet = null
                },
                onOpenCourse = { course ->
                    conflictSheet = null
                    onCourseClick(course)
                },
                onDismiss = { conflictSheet = null }
            )
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
    otherWeekAlpha: Float = 0.50f,
    verticalLayout: Boolean = true
) {
    val nameAlpha = if (isNonCurrent) (otherWeekAlpha * 2.78f).coerceIn(0.3f, 1f) else 1f
    val infoAlpha = if (isNonCurrent) (otherWeekAlpha * 1.67f).coerceIn(0.2f, 0.8f) else 0.6f

    val displayRoom = remember(course.room, showCampus) {
        val room = course.room
        if (showCampus) room
        else {
            val campusIdx = room.indexOf("校区")
            if (campusIdx > 0) room.substring(campusIdx + 2).trimStart(' ', '·', '-') else room
        }
    }

    if (!verticalLayout) {
        // ── Horizontal compact layout (original style): name + teacher/room, single lines ──
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
                    lineHeight = (gridTextSize + 2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    textAlign = if (textCentered) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                if (hasHomework) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "有作业",
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (showTeacher && course.teacher.isNotEmpty()) {
                Text(
                    text = course.teacher,
                    color = textColor.copy(alpha = infoAlpha),
                    fontSize = (gridTextSize - 2).sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = (gridTextSize + 1).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    textAlign = if (textCentered) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (showRoom && displayRoom.isNotEmpty()) {
                Text(
                    text = displayRoom,
                    color = textColor.copy(alpha = infoAlpha),
                    fontSize = (gridTextSize - 2).sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = (gridTextSize + 1).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    textAlign = if (textCentered) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (showOddEven && course.weekType != "all") {
                Text(
                    text = if (course.weekType == "odd") "单周" else "双周",
                    color = textColor.copy(alpha = infoAlpha),
                    fontSize = (gridTextSize - 2).sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = (gridTextSize + 1).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    softWrap = false,
                    textAlign = if (textCentered) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

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
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
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
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (isNonCurrent) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "非本周",
                color = textColor.copy(alpha = infoAlpha),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
                textAlign = if (textCentered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (showTeacher && course.teacher.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = course.teacher,
                color = textColor.copy(alpha = infoAlpha),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                textAlign = if (textCentered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (showRoom && displayRoom.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = displayRoom,
                color = textColor.copy(alpha = infoAlpha),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                textAlign = if (textCentered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (showOddEven && course.weekType != "all") {
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (course.weekType == "odd") "单周" else "双周",
                color = textColor.copy(alpha = infoAlpha),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
                textAlign = if (textCentered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── P0 冲突块：块 key + 角标 + 底部选择弹窗 ──

/** 冲突块的稳定 key（day + Float 逻辑坐标），用于记住用户当前选择显示的课程 */
private fun blockKeyOf(block: MergedCourseBlock): String = "${block.day}:${block.start}:${block.end}"

/** 冲突角标：右上角重叠课程数 */
@Composable
private fun ConflictBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCCE57373))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = "×$count",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/** 冲突课程选择底部弹窗：列出块内全部课程，点击切换块内显示，可进详情 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConflictCoursesSheet(
    block: MergedCourseBlock,
    activeId: Long?,
    onSelect: (Long) -> Unit,
    onOpenCourse: (CourseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = "课程重叠 · ${block.courses.size} 门",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        Text(
            text = "点击切换课表中显示的课程",
            fontSize = 12.sp,
            color = colors.textTertiary,
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
        )
        block.courses.forEachIndexed { index, bc ->
            val course = bc.course
            val isActive = course.id == activeId || (activeId == null && index == 0)
            HorizontalDivider(color = colors.borderDivider, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(course.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(courseBgColor(course.colorIndex, LocalDarkMode.current))
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = course.name,
                            fontSize = 14.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = colors.textPrimary
                        )
                        if (!bc.isActive) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "非本周",
                                fontSize = 10.sp,
                                color = colors.textTertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append("第").append(course.startSlot).append("-").append(course.endSlot).append("节")
                            if (course.teacher.isNotBlank()) append(" · ").append(course.teacher)
                            if (course.room.isNotBlank()) append(" · ").append(course.room)
                            if (!bc.isActive && bc.visibleRange == null) append(" · 被本周课程覆盖")
                        },
                        fontSize = 12.sp,
                        color = colors.textTertiary
                    )
                }
                if (isActive) {
                    Text(
                        text = "显示中",
                        fontSize = 12.sp,
                        color = accentColor(),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
                TextButton(onClick = { onOpenCourse(course) }) {
                    Text("详情", fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
