package com.eggrice.timetable.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.TimetableApplication
import com.eggrice.timetable.data.entity.CourseEntity
import com.eggrice.timetable.data.entity.SchemeEntity
import com.eggrice.timetable.ui.import_.ImportScreen
import com.eggrice.timetable.ui.import_.WebImportScreen
import com.eggrice.timetable.ui.profile.components.*
import com.eggrice.timetable.ui.treasurebox.TreasureBoxScreen
import com.eggrice.timetable.ui.timetable.components.SchemeManagerDialog
import com.eggrice.timetable.ui.theme.*
import com.eggrice.timetable.util.CalendarExportUtil
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipInputStream

@Composable
fun ProfileScreen(onSubPageChange: (Boolean) -> Unit = {}, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TimetableApplication
    val container = app.appContainer
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }

    // ── Dialog states ──
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showPetDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showCornerRadiusDialog by remember { mutableStateOf(false) }
    var showColorThemeDialog by remember { mutableStateOf(false) }
    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showReminderTimeDialog by remember { mutableStateOf(false) }
    var showVibrationModeDialog by remember { mutableStateOf(false) }
    var showShareCodeDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showSchoolRequestDialog by remember { mutableStateOf(false) }
    var showShareExportDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }
    var showJwImportSub by remember { mutableStateOf(false) }
    var showFileImportSub by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showGeneralSettings by remember { mutableStateOf(false) }
    var showFeatureToggles by remember { mutableStateOf(false) }
    var showImportScreen by remember { mutableStateOf(false) }
    var showWebImportScreen by remember { mutableStateOf(false) }
    var freeImportMode by remember { mutableStateOf(false) }
    var showTreasureBoxDialog by remember { mutableStateOf(false) }
    var showSchemeManager by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }

    // ── Settings sub-page states ──
    var showSettingsMain by remember { mutableStateOf(false) }
    var showTimeSlotManagement by remember { mutableStateOf(false) }
    var showSemesterSettings by remember { mutableStateOf(false) }

    // Sub-page visibility tracking — hide bottom nav + handle system back
    // Includes dialog states so system back can dismiss dialogs properly
    val hasSubPage = showImportScreen || showWebImportScreen || showTreasureBoxDialog
            || showSettingsMain || showTimeSlotManagement || showSemesterSettings || showChangelog
            || showFeatureToggles || showGeneralSettings || showAppearanceDialog
            || showImportMenu || showJwImportSub || showFileImportSub || showShareMenu

    LaunchedEffect(hasSubPage) { onSubPageChange(hasSubPage) }

    BackHandler(enabled = hasSubPage) {
        when {
            // Sub-pages → go back to parent dialog/menu
            showImportScreen -> { showImportScreen = false; showImportMenu = true }
            showWebImportScreen -> { showWebImportScreen = false; freeImportMode = false; showImportMenu = true }
            showTreasureBoxDialog -> showTreasureBoxDialog = false
            // Settings sub-pages
            showSettingsMain -> showSettingsMain = false
            showTimeSlotManagement -> showTimeSlotManagement = false
            showSemesterSettings -> showSemesterSettings = false
            showChangelog -> showChangelog = false
            showFeatureToggles -> showFeatureToggles = false
            showGeneralSettings -> showGeneralSettings = false
            showAppearanceDialog -> showAppearanceDialog = false
            // Sub-dialogs → back to ImportMenu
            showJwImportSub -> { showJwImportSub = false; showImportMenu = true }
            showFileImportSub -> { showFileImportSub = false; showImportMenu = true }
            // Top-level dialogs → dismiss
            showImportMenu -> showImportMenu = false
            showShareMenu -> showShareMenu = false
        }
    }

    if (showImportScreen) {
        ImportScreen(onBack = { showImportScreen = false; showImportMenu = true })
        return
    }
    if (showWebImportScreen) {
        WebImportScreen(
            onBack = { showWebImportScreen = false; freeImportMode = false; showImportMenu = true },
            freeMode = freeImportMode
        )
        return
    }
    if (showTreasureBoxDialog) {
        TreasureBoxScreen(
            onBack = { showTreasureBoxDialog = false },
            onImportCourse = { courseName, teacher, room, day, startSlot, endSlot ->
                val schemeId = container.activeSchemeId.value
                val course = com.eggrice.timetable.data.entity.CourseEntity(
                    name = courseName, teacher = teacher, room = room,
                    dayOfWeek = day, startSlot = startSlot, endSlot = endSlot,
                    schemeId = schemeId,
                    colorIndex = (day * 3 + startSlot) % 15
                )
                scope.launch {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        app.repository.insert(course)
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, "$courseName 已导入课表！", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            repository = app.repository,
            schemeId = container.activeSchemeId.value
        )
        return
    }

    // ── Settings sub-pages ──
    if (showSettingsMain) {
        SettingsMainScreen(
            container = container,
            onBack = { showSettingsMain = false },
            onTimeSlotManagement = { showSettingsMain = false; showTimeSlotManagement = true },
            onAppearance = { showSettingsMain = false; showAppearanceDialog = true },
            onSemesterSettings = { showSettingsMain = false; showSemesterSettings = true },
            onReminderTime = { showSettingsMain = false; showReminderTimeDialog = true },
            onVibrationMode = { showSettingsMain = false; showVibrationModeDialog = true }
        )
        return
    }
    if (showTimeSlotManagement) {
        TimeSlotManagementScreen(onBack = { showTimeSlotManagement = false })
        return
    }
    if (showSemesterSettings) {
        SemesterSettingsPage(container = container, onBack = { showSemesterSettings = false })
        return
    }
    if (showChangelog) {
        ChangelogScreen(onBack = { showChangelog = false })
        return
    }
    val nickname by container.nickname.collectAsState()
    val school by container.school.collectAsState()
    val showTeacher by container.showTeacher.collectAsState()
    val showRoom by container.showRoom.collectAsState()
    val showCampus by container.showCampus.collectAsState()
    val showSlotTime by container.showSlotTime.collectAsState()
    val cornerRadius by container.cornerRadius.collectAsState()
    val colorTheme by container.colorTheme.collectAsState()
    val darkMode by container.darkMode.collectAsState()
    val reminderEnabled by container.reminderEnabled.collectAsState()
    val reminderMinutes by container.reminderMinutes.collectAsState()
    val vibrationMode by container.vibrationMode.collectAsState()
    val showOddEven by container.showOddEven.collectAsState()
    val autoUpdate by container.autoUpdate.collectAsState()
    val showDashedBorder by container.showDashedBorder.collectAsState()
    val textCentered by container.textCentered.collectAsState()
    val gridHeight by container.gridHeight.collectAsState()
    val gridOpacity by container.gridOpacity.collectAsState()
    val gridTextSize by container.gridTextSize.collectAsState()
    val borderStyle by container.borderStyle.collectAsState()
    val petIndex by container.petIndex.collectAsState()
    val petName by container.petName.collectAsState()
    val showNonCurrentWeek by container.showNonCurrentWeek.collectAsState()
    val gridBgColor by container.gridBgColor.collectAsState()
    val otherWeekAlpha by container.otherWeekAlpha.collectAsState()
    val wallpaperUri by container.wallpaperUri.collectAsState()
    val showTreasureBox by container.showTreasureBox.collectAsState()
    val showWidget by container.showWidget.collectAsState()
    val allSchemes by app.repository.allSchemes.collectAsState(emptyList())
    val activeSchemeId by container.activeSchemeId.collectAsState()

    val colors = LocalEggRiceColors.current

    // ── File pickers ──
    val excelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { importFromUri(context, app, uri, "excel") } }

    val htmlLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { importFromUri(context, app, uri, "html") } }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { importFromUri(context, app, uri, "backup") } }

    val icsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) {
        Toast.makeText(context, "ICS导入即将推出，敬请期待", Toast.LENGTH_SHORT).show()
    }

    val csvImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { importFromUri(context, app, uri, "excel") } }

    val backupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val courses = app.repository.allCourses.firstOrNull() ?: emptyList()
                        val backup = CourseBackup(
                            exportTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                            courses = courses
                        )
                        val json = gson.toJson(backup)
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        }
                    } catch (_: Exception) {}
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "备份已导出", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val courses = app.repository.allCourses.firstOrNull() ?: emptyList()
                        val csv = buildString {
                            appendLine("课程名称,学分,教师,教室,星期,开始节次,结束节次,周类型,周数")
                            courses.forEach { c ->
                                appendLine("${c.name},${c.credits},${c.teacher},${c.room},${c.dayOfWeek},${c.startSlot},${c.endSlot},${c.weekType},${c.weeks}")
                            }
                        }
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(csv.toByteArray(Charsets.UTF_8))
                        }
                    } catch (_: Exception) {}
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "CSV已导出", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val crashExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val logFile = app.crashHandler.getLatestCrashLog()
                        if (logFile != null && logFile.exists()) {
                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                logFile.inputStream().use { inp -> inp.copyTo(out) }
                            }
                        }
                    } catch (_: Exception) {}
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "崩溃日志已导出", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── Calendar permission launcher ──
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            exportToCalendar(app, container, scope, context)
        } else {
            Toast.makeText(context, "需要日历权限才能导入课表", Toast.LENGTH_SHORT).show()
        }
    }

    val onImportToCalendar: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
            == PackageManager.PERMISSION_GRANTED) {
            exportToCalendar(app, container, scope, context)
        } else {
            calendarPermissionLauncher.launch(Manifest.permission.WRITE_CALENDAR)
        }
    }

    // ── Settings sub-pages (need state, render before main content) ──
    if (showGeneralSettings) {
        GeneralSettingsScreen(
            darkMode = darkMode,
            vibrationMode = vibrationMode,
            colorTheme = colorTheme,
            borderStyle = borderStyle,
            onBack = { showGeneralSettings = false },
            onDarkMode = { showGeneralSettings = false; showDarkModeDialog = true },
            onVibrationMode = { showGeneralSettings = false; showVibrationModeDialog = true },
            onColorTheme = { showGeneralSettings = false; showColorThemeDialog = true },
            onFeatureToggles = { showGeneralSettings = false; showFeatureToggles = true },
            onImportToCalendar = onImportToCalendar,
            onBorderStyle = { container.setBorderStyle(it) }
        )
        return
    }
    if (showFeatureToggles) {
        FeatureToggleScreen(
            container = container,
            showTreasureBox = showTreasureBox,
            showWidget = showWidget,
            reminderEnabled = reminderEnabled,
            autoUpdate = autoUpdate,
            onBack = { showFeatureToggles = false }
        )
        return
    }
    if (showAppearanceDialog) {
        AppearanceScreen(
            container = container,
            showTeacher = showTeacher,
            showRoom = showRoom,
            showCampus = showCampus,
            showSlotTime = showSlotTime,
            borderStyle = borderStyle,
            textCentered = textCentered,
            gridHeight = gridHeight,
            gridOpacity = gridOpacity,
            gridTextSize = gridTextSize,
            showNonCurrentWeek = showNonCurrentWeek,
            showOddEven = showOddEven,
            gridBgColor = gridBgColor,
            otherWeekAlpha = otherWeekAlpha,
            wallpaperUri = wallpaperUri,
            onBack = { showAppearanceDialog = false }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ━━━━ User profile area ━━━━
            item {
                UserProfileArea(
                    nickname = nickname,
                    school = school,
                    onEditNickname = { showNicknameDialog = true }
                )
            }

            item { SpacerH(6) }

            // ━━━━ Pet area ━━━━
            item {
                PetArea(
                    petIndex = petIndex,
                    petName = petName,
                    onClick = { showPetDialog = true }
                )
            }

            item { SpacerH(12) }

            // ━━━━ Row 1: 导入 + 分享 ━━━━
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DualButton(
                        text = "导入",
                        icon = Icons.Outlined.FileDownload,
                        modifier = Modifier.weight(1f),
                        onClick = { showImportMenu = true },
                        containerColor = PinkSoft,
                        contentColor = PinkAccent
                    )
                    DualButton(
                        text = "分享",
                        icon = Icons.Outlined.Share,
                        modifier = Modifier.weight(1f),
                        onClick = { showShareMenu = true }
                    )
                }
            }

            item { SpacerH(12) }

            // ━━━━ Row 2: 课表设置 + 通用设置 ━━━━
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DualButton(
                        text = "课表设置",
                        icon = Icons.Outlined.GridView,
                        modifier = Modifier.weight(1f),
                        onClick = { showSettingsMain = true }
                    )
                    DualButton(
                        text = "通用设置",
                        icon = Icons.Outlined.Settings,
                        modifier = Modifier.weight(1f),
                        onClick = { showGeneralSettings = true },
                        containerColor = PinkSoft,
                        contentColor = PinkAccent
                    )
                }
            }

            item { SpacerH(16) }

            // ━━━━ Bottom area ━━━━
            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Divider) }
            item {
                ArrowRow("课表管理", "管理你的多张课表") {
                    showSchemeManager = true
                }
            }
            if (showTreasureBox) {
                item {
                    ArrowRow("百宝箱", "学习资源 · 今天吃什么") {
                        showTreasureBoxDialog = true
                    }
                }
            }
            item {
                ArrowRow("清理浏览器缓存", "清理教务登录缓存和Cookie") {
                    val wv = android.webkit.WebView(context)
                    wv.clearCache(true)
                    wv.destroy()
                    android.webkit.CookieManager.getInstance().removeAllCookies(null)
                    Toast.makeText(context, "浏览器缓存已清理", Toast.LENGTH_SHORT).show()
                }
            }
            item {
                ArrowRow("导出崩溃日志") {
                    val hasLog = app.crashHandler.getLatestCrashLog() != null
                    if (hasLog) {
                        crashExportLauncher.launch("crash_log_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt")
                    } else {
                        Toast.makeText(context, "暂无崩溃日志", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            if (autoUpdate) {
                item {
                    ArrowRow("检查更新") { showUpdateDialog = true }
                }
            }
            item {
                ArrowRow("更新日志", "版本更新记录") { showChangelog = true }
            }
            item {
                ArrowRow("关于我们") { showAboutDialog = true }
            }
            item {
                ArrowRow("清空所有课表数据") { showClearDialog = true }
            }
        }
    }

    // ── Dialogs ──
    if (showNicknameDialog) {
        EditTextDialog(
            title = "修改昵称",
            currentValue = nickname,
            hint = "请输入昵称",
            maxLength = 12,
            onConfirm = { container.setNickname(it.ifBlank { "同学" }) },
            onDismiss = { showNicknameDialog = false }
        )
    }

    if (showPetDialog) {
        PetManageDialog(
            petIndex = petIndex,
            petName = petName,
            onSelectPet = { container.setPetIndex(it) },
            onRenamePet = { name -> container.setPetName(name) },
            onDismiss = { showPetDialog = false }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清空") },
            text = { Text("确定要删除所有课程数据吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            app.repository.deleteAll()
                            withContext(Dispatchers.Main) {
                                showClearDialog = false
                                Toast.makeText(context, "已清空所有课程", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("确认清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }

    if (showCornerRadiusDialog) {
        OptionsDialog(
            title = "课程卡片圆角",
            options = listOf("0dp" to 0, "4dp" to 4, "8dp" to 8, "12dp" to 12, "16dp" to 16),
            selected = cornerRadius,
            onSelect = { container.setCornerRadius(it) },
            onDismiss = { showCornerRadiusDialog = false }
        )
    }

    if (showColorThemeDialog) {
        OptionsDialog(
            title = "课程配色主题",
            options = listOf("海盐蓝" to "default", "抹茶绿" to "matcha", "樱花粉" to "sakura", "紫藤紫" to "wisteria", "蛋炒饭" to "fried_rice"),
            selected = colorTheme,
            onSelect = { container.setColorTheme(it) },
            onDismiss = { showColorThemeDialog = false }
        )
    }

    if (showDarkModeDialog) {
        OptionsDialog(
            title = "深色/浅色模式",
            options = listOf("浅色模式" to "light", "深色模式" to "dark", "跟随系统" to "system"),
            selected = darkMode,
            onSelect = { mode ->
                container.setDarkMode(mode)
                when (mode) {
                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            },
            onDismiss = { showDarkModeDialog = false }
        )
    }

    if (showReminderTimeDialog) {
        OptionsDialog(
            title = "提前提醒时间",
            options = listOf("5分钟" to 5, "10分钟" to 10, "15分钟" to 15, "30分钟" to 30),
            selected = reminderMinutes,
            onSelect = { container.setReminderMinutes(it) },
            onDismiss = { showReminderTimeDialog = false }
        )
    }

    if (showVibrationModeDialog) {
        OptionsDialog(
            title = "震动模式",
            options = listOf("关闭" to 0, "轻柔" to 1, "适中" to 2, "强力" to 3),
            selected = vibrationMode,
            onSelect = { container.setVibrationMode(it) },
            onDismiss = { showVibrationModeDialog = false }
        )
    }

    if (showShareCodeDialog) {
        ShareCodeDialog(
            gson = gson,
            app = app,
            onDismiss = { showShareCodeDialog = false }
        )
    }

    if (showFaqDialog) {
        FaqDialog(onDismiss = { showFaqDialog = false })
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            context = context,
            onDismiss = { showFeedbackDialog = false }
        )
    }

    if (showUpdateDialog) {
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
        } catch (_: Exception) { "未知" }
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("检查更新") },
            text = {
                Text("当前版本：v$versionName\n\n蛋炒饭课程表\n纯净无广告 · 开源课表\n\n已是最新版本")
            },
            confirmButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("确定") }
            }
        )
    }

    if (showSchoolRequestDialog) {
        SchoolRequestDialog(
            context = context,
            onDismiss = { showSchoolRequestDialog = false }
        )
    }

    if (showShareExportDialog) {
        ShareExportDialog(
            gson = gson,
            app = app,
            onDismiss = { showShareExportDialog = false }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            context = context,
            onDismiss = { showAboutDialog = false }
        )
    }

    if (showImportMenu) {
        ImportMenuDialog(
            onDismiss = { showImportMenu = false },
            onJwImport = { showImportMenu = false; showJwImportSub = true },
            onShareCodeImport = { showImportMenu = false; showShareCodeDialog = true },
            onFileImport = { showImportMenu = false; showFileImportSub = true },
            onBackupRestore = { showImportMenu = false; backupLauncher.launch(arrayOf("application/json")) },
            onSchoolRequest = { showImportMenu = false; showSchoolRequestDialog = true }
        )
    }

    if (showJwImportSub) {
        JwImportSubDialog(
            onDismiss = { showJwImportSub = false; showImportMenu = true },
            onWebImport = { showJwImportSub = false; freeImportMode = false; showWebImportScreen = true },
            onNativeImport = { showJwImportSub = false; showImportScreen = true },
            onFreeImport = { showJwImportSub = false; freeImportMode = true; showWebImportScreen = true }
        )
    }

    if (showFileImportSub) {
        FileImportSubDialog(
            onDismiss = { showFileImportSub = false; showImportMenu = true },
            onExcel = { showFileImportSub = false; excelLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) },
            onHtml = { showFileImportSub = false; htmlLauncher.launch(arrayOf("text/html")) },
            onCsv = { showFileImportSub = false; csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values")) },
            onIcs = { showFileImportSub = false; icsLauncher.launch(arrayOf("text/calendar", "application/ics")) }
        )
    }

    if (showShareMenu) {
        ShareMenuDialog(
            onDismiss = { showShareMenu = false },
            onGenerateCode = { showShareMenu = false; showShareExportDialog = true },
            onExportBackup = {
                showShareMenu = false
                backupExportLauncher.launch("课表备份_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.json")
            },
            onExportCsv = {
                showShareMenu = false
                csvExportLauncher.launch("课表_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.csv")
            }
        )
    }

    if (showSchemeManager) {
        SchemeManagerDialog(
            schemes = allSchemes,
            activeSchemeId = activeSchemeId,
            onSwitchScheme = { scheme ->
                container.setActiveScheme(scheme.id, scheme.name)
                showSchemeManager = false
            },
            onCreateScheme = { name ->
                scope.launch {
                    val id = app.repository.createScheme(name)
                    container.setActiveScheme(id, name)
                }
            },
            onRenameScheme = { scheme, newName ->
                scope.launch {
                    app.repository.updateScheme(scheme.copy(name = newName))
                    if (scheme.id == container.activeSchemeId.value) {
                        container.setActiveScheme(scheme.id, newName)
                    }
                }
            },
            onDeleteScheme = { scheme ->
                scope.launch {
                    app.repository.deleteByScheme(scheme.id)
                    app.repository.deleteScheme(scheme.id)
                    if (scheme.id == container.activeSchemeId.value) {
                        val def = app.repository.getSchemeById(0L)
                            ?: SchemeEntity(id = 0, name = "默认课表")
                        container.setActiveScheme(def.id, def.name)
                    }
                }
            },
            onDismiss = { showSchemeManager = false }
        )
    }
}

// ═══════════════════════════════════════════
//  Import helpers
// ═══════════════════════════════════════════

private fun importFromUri(
    context: android.content.Context,
    app: TimetableApplication,
    uri: Uri,
    type: String
) {
    val gson = Gson()
    val scope = kotlinx.coroutines.MainScope()
    scope.launch {
        withContext(Dispatchers.IO) {
            try {
                val courses: List<CourseEntity> = when (type) {
                    "excel" -> parseExcel(context, uri)
                    "html" -> parseHtml(context, uri)
                    "backup" -> parseBackup(context, uri, gson)
                    else -> emptyList()
                }

                if (courses.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "未识别到有效课程数据，请检查文件格式", Toast.LENGTH_LONG).show()
                    }
                    return@withContext
                }

                app.repository.deleteAll()
                courses.forEach { app.repository.insert(it) }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "成功导入 ${courses.size} 门课程", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导入失败：${e.message ?: "文件格式不正确"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

private fun parseExcel(context: android.content.Context, uri: Uri): List<CourseEntity> {
    val courses = mutableListOf<CourseEntity>()

    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            // Try xlsx (ZIP containing XML)
            val zip = ZipInputStream(inputStream)
            var sharedStringsXml = ""
            var sheetXml = ""

            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == "xl/sharedStrings.xml" -> {
                        sharedStringsXml = zip.bufferedReader().readText()
                    }
                    entry.name == "xl/worksheets/sheet1.xml" -> {
                        sheetXml = zip.bufferedReader().readText()
                    }
                }
                entry = zip.nextEntry
            }

            if (sheetXml.isBlank()) {
                return emptyList()
            }

            // Parse shared strings
            val strings = Regex("<t[^>]*>([^<]*)</t>").findAll(sharedStringsXml)
                .map { it.groupValues[1] }.toList()

            // Parse sheet rows (skip header)
            val rows = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
                .findAll(sheetXml).toList()

            rows.drop(1).forEach { row ->
                val cells = Regex("<c[^>]*><v>(\\d+)</v></c>").findAll(row.value)
                    .map { it.groupValues[1].toIntOrNull() }
                    .toList()

                if (cells.size >= 6) {
                    val nameIdx = cells.getOrNull(0) ?: return@forEach
                    val teacherIdx = cells.getOrNull(1) ?: return@forEach
                    val roomIdx = cells.getOrNull(2) ?: return@forEach
                    val day = (cells.getOrNull(3) ?: return@forEach)
                    val startSlot = (cells.getOrNull(4) ?: return@forEach)
                    val endSlot = (cells.getOrNull(5) ?: return@forEach)
                    val weeksStr = if (cells.size > 6) cells[6].toString() else ""

                    courses.add(CourseEntity(
                        name = strings.getOrElse(nameIdx) { "" },
                        teacher = strings.getOrElse(teacherIdx) { "" },
                        room = strings.getOrElse(roomIdx) { "" },
                        dayOfWeek = day,
                        startSlot = startSlot,
                        endSlot = endSlot,
                        weeks = weeksStr
                    ))
                }
            }
        }
    } catch (_: Exception) {
        return emptyList()
    }

    return courses
}

private fun parseHtml(context: android.content.Context, uri: Uri): List<CourseEntity> {
    val courses = mutableListOf<CourseEntity>()

    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val html = inputStream.bufferedReader().readText()
            // Look for table rows with course data
            val trRegex = Regex("<tr[^>]*>(.*?)</tr>", RegexOption.DOT_MATCHES_ALL)
            val tdRegex = Regex("<td[^>]*>(.*?)</td>", RegexOption.DOT_MATCHES_ALL)
            val stripTags = Regex("<[^>]*>")

            val rows = trRegex.findAll(html).toList()
            rows.drop(1).forEach { tr ->
                val cells = tdRegex.findAll(tr.value)
                    .map { it.groupValues[1].replace(stripTags, "").trim() }
                    .filter { it.isNotBlank() }
                    .toList()

                if (cells.size >= 6) {
                    val day = cells[3].toIntOrNull() ?: return@forEach
                    val startSlot = cells[4].toIntOrNull() ?: return@forEach
                    val endSlot = cells[5].toIntOrNull() ?: return@forEach

                    courses.add(CourseEntity(
                        name = cells[0],
                        teacher = cells.getOrElse(1) { "" },
                        room = cells.getOrElse(2) { "" },
                        dayOfWeek = day,
                        startSlot = startSlot,
                        endSlot = endSlot,
                        weeks = cells.getOrElse(6) { "" }
                    ))
                }
            }
        }
    } catch (_: Exception) {
        return emptyList()
    }

    return courses
}

private fun parseBackup(context: android.content.Context, uri: Uri, gson: Gson): List<CourseEntity> {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val json = inputStream.bufferedReader().readText()
            val backup = gson.fromJson(json, CourseBackup::class.java)
            return backup.courses
        }
    } catch (_: Exception) {
        return emptyList()
    }
    return emptyList()
}

private fun exportToCalendar(
    app: TimetableApplication,
    container: com.eggrice.timetable.di.AppContainer,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    scope.launch {
        val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val courses = app.repository.getCoursesByScheme(container.activeSchemeId.value).firstOrNull() ?: emptyList()
                if (courses.isEmpty()) {
                    "暂无课程可导入"
                } else {
                    val startStr = container.semesterStart.value
                    if (startStr.isBlank() || !startStr.contains("-")) {
                        "请先在设置中配置学期开始日期"
                    } else {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val startDate = sdf.parse(startStr) ?: return@withContext "日期解析失败"
                        val weeks = container.semesterWeeks.value
                        val cal = java.util.Calendar.getInstance().apply {
                            time = startDate
                            add(java.util.Calendar.DAY_OF_MONTH, weeks * 7)
                        }
                        val endDate = cal.timeInMillis
                        val count = CalendarExportUtil.exportToCalendar(
                            context, courses, startDate.time, endDate
                        )
                        "已导入 $count 门课程到系统日历"
                    }
                }
            } catch (e: Exception) {
                "导入失败: ${e.message}"
            }
        }
        withContext(kotlinx.coroutines.Dispatchers.Main) {
            android.widget.Toast.makeText(context, result, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
