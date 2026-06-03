package com.eggrice.timetable.ui.timetable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.TimetableApplication
import com.eggrice.timetable.data.entity.CourseEntity
import com.eggrice.timetable.data.entity.HomeworkEntity
import androidx.lifecycle.viewmodel.compose.viewModel

import com.eggrice.timetable.ui.timetable.components.PeriodGrid
import com.eggrice.timetable.ui.timetable.components.WeekHeader
import com.eggrice.timetable.ui.timetable.components.HomeworkListDialog
import com.eggrice.timetable.ui.timetable.components.AddHomeworkDialog
import com.eggrice.timetable.ui.profile.SemesterSettingsPage
import com.eggrice.timetable.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TimetableScreen(onSubPageChange: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as TimetableApplication
    val container = app.appContainer
    val viewModel: TimetableViewModel = viewModel(
        factory = TimetableViewModel.Factory(app.repository, container)
    )
    val courses by viewModel.allCourses.collectAsState()
    val timeSlots by viewModel.allTimeSlots.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val editingCourse by viewModel.editingCourse.collectAsState()
    val showEditor by viewModel.showEditor.collectAsState()
    var showDeleteRangeDialog by remember { mutableStateOf(false) }
    var pendingDeleteCourse by remember { mutableStateOf<CourseEntity?>(null) }

    val nonCurrentWeekCourses by viewModel.nonCurrentWeekCourses.collectAsState()
    val filteredCourses by viewModel.filteredCourses.collectAsState()

    // Appearance settings
    val showTeacher by container.showTeacher.collectAsState()
    val showRoom by container.showRoom.collectAsState()
    val showCampus by container.showCampus.collectAsState()
    val showSlotTime by container.showSlotTime.collectAsState()
    val showDashedBorder by container.showDashedBorder.collectAsState()
    val textCentered by container.textCentered.collectAsState()
    val gridHeight by container.gridHeight.collectAsState()
    val cornerRadius by container.cornerRadius.collectAsState()
    val gridOpacity by container.gridOpacity.collectAsState()
    val gridTextSize by container.gridTextSize.collectAsState()
    val showOddEven by container.showOddEven.collectAsState()
    val borderStyle by container.borderStyle.collectAsState()
    val showNonCurrentWeek by container.showNonCurrentWeek.collectAsState()
    val vibrationMode by container.vibrationMode.collectAsState()
    val gridBgColor by container.gridBgColor.collectAsState()
    val otherWeekAlpha by container.otherWeekAlpha.collectAsState()

    val today = LocalDate.now()
    val todayDay = today.dayOfWeek.value
    val semesterStart = container.semesterStart.collectAsState().value
    val autoWeek = container.autoCurrentWeek()
    val isCurrentWeek = currentWeek == autoWeek

    val startOfWeek = if (semesterStart.isNotBlank()) {
        try {
            val parts = semesterStart.split("-")
            LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                .plusWeeks((currentWeek - 1).toLong())
        } catch (_: Exception) { today.plusDays((-(todayDay - 1) + (currentWeek - 1) * 7).toLong()) }
    } else {
        today.plusDays((-(todayDay - 1) + (currentWeek - 1) * 7).toLong())
    }
    val endOfWeek = startOfWeek.plusDays(6)
    val fmt = DateTimeFormatter.ofPattern("M/d")

    // Homework state
    val schemeId by container.activeSchemeId.collectAsState()
    val allHomework by app.repository.getAllHomework().collectAsState(initial = emptyList())
    val filteredHomework = remember(allHomework, schemeId) {
        allHomework.filter { it.schemeId == schemeId || it.schemeId == 0L }
    }
    var showHomeworkList by remember { mutableStateOf(false) }
    var showAddHomework by remember { mutableStateOf(false) }
    var showSemesterSettings by remember { mutableStateOf(false) }

    // Hide bottom nav + handle system back when semester settings is open
    LaunchedEffect(showSemesterSettings) { onSubPageChange(showSemesterSettings) }
    BackHandler(enabled = showSemesterSettings) { showSemesterSettings = false }

    // Active homework course names (non-completed) — for asterisk marks on cards
    val homeworkCourseNames by app.repository.getActiveHomeworkCourseNames(schemeId)
        .collectAsState(initial = emptyList())

    // Unique course names from current timetable — for homework course dropdown
    val timetableCourseNames = remember(courses) {
        courses.map { it.name }.distinct().sorted()
    }

    LaunchedEffect(Unit) {
        if (timeSlots.isEmpty()) {
            (context.applicationContext as TimetableApplication).repository.initTimeSlots()
        }
        viewModel.goToToday()
    }

    val isDark = LocalDarkMode.current
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = if (isDark) DarkSurface else Surface,
        floatingActionButton = {
            // FAB press animation per MD spec
            var fabPressed by remember { mutableStateOf(false) }
            val fabScale by animateFloatAsState(
                targetValue = if (fabPressed) 0.92f else 1f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!isCurrentWeek) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            fabPressed = true
                            viewModel.goToToday()
                            fabPressed = false
                        },
                        icon = { Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(20.dp)) },
                        text = { Text("回本周", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                        containerColor = accentColor(),
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 12.dp
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(40.dp).scale(fabScale)
                    )
                    Spacer(Modifier.height(12.dp))
                }
                FloatingActionButton(
                    onClick = {
                        fabPressed = true
                        viewModel.openAddEditor()
                        fabPressed = false
                    },
                    containerColor = accentColor(),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    ),
                    shape = CircleShape,
                    modifier = Modifier.scale(fabScale)
                ) {
                    Icon(Icons.Default.Add, "添加课程", modifier = Modifier.size(24.dp))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Top bar: compact ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) DarkSurfaceCard else SurfaceCard)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val weekInteractionSource = remember { MutableInteractionSource() }
                val isWeekPressed by weekInteractionSource.collectIsPressedAsState()
                val weekScale by animateFloatAsState(
                    targetValue = if (isWeekPressed) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f)
                )
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = weekInteractionSource,
                            indication = null
                        ) { showSemesterSettings = true }
                        .scale(weekScale)
                        .padding(vertical = 1.dp, horizontal = 4.dp)
                ) {
                    Text(
                        "第${currentWeek}周",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) DarkTextPrimary else TextPrimary
                    )
                    Text(
                        "${startOfWeek.format(fmt)} - ${endOfWeek.format(fmt)}",
                        fontSize = 9.sp,
                        color = if (isDark) DarkTextTertiary else TextTertiary
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.prevWeek() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ChevronLeft, "上一周", tint = accentColor(), modifier = Modifier.size(18.dp))
                    }
                    Button(
                        onClick = { viewModel.goToToday() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCurrentWeek) accentSoftColor() else accentColor(),
                            contentColor = if (isCurrentWeek) accentColor() else Color.White
                        )
                    ) { Text("今天", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    IconButton(onClick = { viewModel.nextWeek() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ChevronRight, "下一周", tint = accentColor(), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ── Week date header ──
            WeekHeader(
                currentWeek = currentWeek,
                isCurrentWeek = isCurrentWeek,
                semesterStart = semesterStart,
                onHomeworkClick = { showHomeworkList = true }
            )

            // Subtle separator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (isDark) DarkDivider else Divider)
            )

            // ── Course grid ──
            PeriodGrid(
                timeSlots = timeSlots,
                courses = filteredCourses,
                currentWeek = currentWeek,
                isCurrentWeek = isCurrentWeek,
                showTeacher = showTeacher,
                showRoom = showRoom,
                showCampus = showCampus,
                showSlotTime = showSlotTime,
                showDashedBorder = showDashedBorder,
                textCentered = textCentered,
                gridHeight = gridHeight,
                cornerRadius = cornerRadius,
                gridOpacity = gridOpacity,
                gridTextSize = gridTextSize,
                showOddEven = showOddEven,
                borderStyle = borderStyle,
                nonCurrentCourses = nonCurrentWeekCourses,
                showNonCurrentWeek = showNonCurrentWeek,
                vibrationMode = vibrationMode,
                gridBgColor = gridBgColor,
                otherWeekAlpha = otherWeekAlpha,
                homeworkCourseNames = homeworkCourseNames.toSet(),
                onCourseClick = { viewModel.openEditEditor(it) },
                onEmptyCellClick = { day, slot -> viewModel.openAddEditor(day, slot) },
                onCourseMoved = { course, newDay, newSlot -> viewModel.updateCoursePosition(course, newDay, newSlot) },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }

    if (showEditor && editingCourse != null) {
        CourseEditorDialog(
            course = editingCourse!!,
            currentWeek = currentWeek,
            onDismiss = { viewModel.closeEditor() },
            onSave = { viewModel.saveCourse(it); viewModel.closeEditor() },
            onDelete = {
                pendingDeleteCourse = it
                viewModel.closeEditor()
                showDeleteRangeDialog = true
            }
        )
    }

    if (showDeleteRangeDialog && pendingDeleteCourse != null) {
        DeleteRangeDialog(
            onDismiss = {
                showDeleteRangeDialog = false
                pendingDeleteCourse = null
            },
            onConfirm = { range ->
                pendingDeleteCourse?.let { viewModel.deleteCourseRange(it, range) }
                showDeleteRangeDialog = false
                pendingDeleteCourse = null
            }
        )
    }

    if (showHomeworkList) {
        HomeworkListDialog(
            homework = filteredHomework,
            onDismiss = { showHomeworkList = false },
            onToggleComplete = { hw ->
                scope.launch {
                    app.repository.setHomeworkCompleted(hw.id, !hw.completed)
                }
            },
            onDelete = { hw ->
                scope.launch {
                    app.repository.deleteHomework(hw.id)
                }
            },
            onAdd = {
                showHomeworkList = false
                showAddHomework = true
            }
        )
    }

    if (showAddHomework) {
        AddHomeworkDialog(
            onDismiss = { showAddHomework = false },
            existingCourses = timetableCourseNames,
            onSave = { courseName, content, dueDate ->
                val hw = HomeworkEntity(
                    courseName = courseName,
                    content = content,
                    dueDate = dueDate,
                    schemeId = schemeId
                )
                scope.launch {
                    app.repository.insertHomework(hw)
                }
                showAddHomework = false
            }
        )
    }

    if (showSemesterSettings) {
        SemesterSettingsPage(
            container = container,
            onBack = { showSemesterSettings = false }
        )
    }

}

// ═══════════════════════════════════════════
//  Course Editor Dialog
// ═══════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditorDialog(
    course: CourseEntity,
    currentWeek: Int,
    onDismiss: () -> Unit,
    onSave: (CourseEntity) -> Unit,
    onDelete: (CourseEntity) -> Unit
) {
    var name by remember { mutableStateOf(course.name) }
    var credits by remember { mutableStateOf(course.credits) }
    var teacher by remember { mutableStateOf(course.teacher) }
    var room by remember { mutableStateOf(course.room) }
    var dayOfWeek by remember { mutableStateOf(course.dayOfWeek) }
    var startSlot by remember { mutableStateOf(course.startSlot) }
    var endSlot by remember { mutableStateOf(course.endSlot) }
    var weekType by remember { mutableStateOf(course.weekType) }
    var weeks by remember { mutableStateOf(course.weeks) }
    var colorIndex by remember { mutableStateOf(course.colorIndex) }

    val isEditing = course.id != 0L
    val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    // Parse weeks string into range
    val parsedWeeks = remember(weeks) {
        val nums = weeks.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
        if (nums.isNotEmpty()) nums.first() to nums.last() else 1 to 16
    }
    var startWeek by remember { mutableIntStateOf(parsedWeeks.first) }
    var endWeek by remember { mutableIntStateOf(parsedWeeks.second) }

    // Original parsed weeks for dirty detection
    val origParsedWeeks = remember(course.id) {
        val nums = course.weeks.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
        if (nums.isNotEmpty()) nums.first() to nums.last() else 1 to 16
    }

    // Dirty state detection
    val hasChanges = name != course.name ||
        credits != course.credits ||
        teacher != course.teacher ||
        room != course.room ||
        dayOfWeek != course.dayOfWeek ||
        startSlot != course.startSlot ||
        endSlot != course.endSlot ||
        weekType != course.weekType ||
        colorIndex != course.colorIndex ||
        startWeek != origParsedWeeks.first ||
        endWeek != origParsedWeeks.second

    var showExitConfirm by remember { mutableStateOf(false) }

    // Initialize weekType from existing course data
    LaunchedEffect(course.id) {
        if (course.id != 0L) {
            // Only set once per dialog open
            weekType = course.weekType
        }
    }

    val showWeekRange = weekType == "all" || weekType == "odd" || weekType == "even"

    AlertDialog(
        onDismissRequest = { if (hasChanges) showExitConfirm = true else onDismiss() },
        title = { Text(if (isEditing) "编辑课程" else "添加课程", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("课程名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = credits, onValueChange = { credits = it }, label = { Text("学分") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = teacher, onValueChange = { teacher = it }, label = { Text("教师") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("教室/地点") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var dayExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = dayExpanded, onExpandedChange = { dayExpanded = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = weekdays[dayOfWeek - 1], onValueChange = {}, readOnly = true, label = { Text("星期") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true))
                        ExposedDropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                            weekdays.forEachIndexed { i, d -> DropdownMenuItem(text = { Text(d) }, onClick = { dayOfWeek = i + 1; dayExpanded = false }) }
                        }
                    }
                    var startExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = startExpanded, onExpandedChange = { startExpanded = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = "第${startSlot}节", onValueChange = {}, readOnly = true, label = { Text("开始节次") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true))
                        ExposedDropdownMenu(expanded = startExpanded, onDismissRequest = { startExpanded = false }) {
                            (1..12).forEach { s -> DropdownMenuItem(text = { Text("第${s}节") }, onClick = { startSlot = s; if (endSlot < s) endSlot = s; startExpanded = false }) }
                        }
                    }
                    var endExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = endExpanded, onExpandedChange = { endExpanded = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = "第${endSlot}节", onValueChange = {}, readOnly = true, label = { Text("结束节次") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true))
                        ExposedDropdownMenu(expanded = endExpanded, onDismissRequest = { endExpanded = false }) {
                            (startSlot..12).forEach { s -> DropdownMenuItem(text = { Text("第${s}节") }, onClick = { endSlot = s; endExpanded = false }) }
                        }
                    }
                }

                // ── Week type selector ──
                Text("周次类型", fontSize = 13.sp, color = TextTertiary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = weekType == "all",
                        onClick = { weekType = "all" },
                        label = { Text("全周", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = weekType == "thisWeek",
                        onClick = { weekType = "thisWeek" },
                        label = { Text("仅本周", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = weekType == "odd",
                        onClick = { weekType = "odd" },
                        label = { Text("单周", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = weekType == "even",
                        onClick = { weekType = "even" },
                        label = { Text("双周", fontSize = 12.sp) }
                    )
                }

                // ── Week range (shown for all/odd/even, hidden for thisWeek) ──
                if (showWeekRange) {
                    Text("课程周数范围", fontSize = 13.sp, color = TextTertiary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var sExp by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = sExp,
                            onExpandedChange = { sExp = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "第${startWeek}周开始",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("开始周") },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            )
                            ExposedDropdownMenu(expanded = sExp, onDismissRequest = { sExp = false }) {
                                (1..25).forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text("第${w}周") },
                                        onClick = { startWeek = w; if (endWeek < w) endWeek = w; sExp = false }
                                    )
                                }
                            }
                        }
                        var eExp by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = eExp,
                            onExpandedChange = { eExp = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "第${endWeek}周结束",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("结束周") },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            )
                            ExposedDropdownMenu(expanded = eExp, onDismissRequest = { eExp = false }) {
                                (startWeek..30).forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text("第${w}周") },
                                        onClick = { endWeek = w; eExp = false }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "共${endWeek - startWeek + 1}周：${startWeek}-${endWeek}周",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else {
                    // For "仅本周"
                    Text(
                        text = "仅第 ${currentWeek} 周显示",
                        fontSize = 12.sp,
                        color = accentColor(),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CourseColors.forEachIndexed { i, c ->
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(c).clickable { colorIndex = i }, contentAlignment = Alignment.Center) {
                            if (i == colorIndex) Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color.White))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val generatedWeeks = when (weekType) {
                        "thisWeek" -> currentWeek.toString()
                        else -> (startWeek..endWeek).joinToString(",") { it.toString() }
                    }
                    val finalWeekType = when (weekType) {
                        "thisWeek" -> "all"  // "仅本周" stored with weeks = single week, type = all
                        else -> weekType
                    }
                    onSave(course.copy(
                        name = name, credits = credits, teacher = teacher, room = room,
                        dayOfWeek = dayOfWeek, startSlot = startSlot, endSlot = endSlot,
                        weekType = finalWeekType, weeks = generatedWeeks, colorIndex = colorIndex
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor())
            ) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (isEditing) TextButton(onClick = { onDelete(course) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = { if (hasChanges) showExitConfirm = true else onDismiss() }) { Text("取消") }
            }
        }
    )

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("放弃更改？", fontWeight = FontWeight.Bold) },
            text = { Text("你有未保存的修改，退出后将丢失所有更改。") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("继续编辑") }
            }
        )
    }
}

// ═══════════════════════════════════════════
//  Delete Range Dialog
// ═══════════════════════════════════════════

@Composable
fun DeleteRangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (DeleteRange) -> Unit
) {
    val options = listOf(
        DeleteRange.ALL_BY_NAME to "删除全部该课程",
        DeleteRange.SAME_TIME_SLOT to "删除该时间段所有课",
        DeleteRange.THIS_INSTANCE to "仅删除本次课"
    )
    var selected by remember { mutableStateOf(DeleteRange.ALL_BY_NAME) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                "请选择删除范围",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                options.forEach { (range, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selected = range }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == range,
                            onClick = { selected = range },
                            colors = RadioButtonDefaults.colors(selectedColor = accentColor())
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            label,
                            fontSize = 14.sp,
                            color = if (selected == range) TextPrimary else TextSecondary,
                            fontWeight = if (selected == range) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("确认删除", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}
