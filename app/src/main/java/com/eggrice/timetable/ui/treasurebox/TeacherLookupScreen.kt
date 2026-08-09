package com.eggrice.timetable.ui.treasurebox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.data.entity.TeacherEntity
import com.eggrice.timetable.data.repository.CourseRepository
import com.eggrice.timetable.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherLookupScreen(
    onBack: () -> Unit,
    repository: CourseRepository,
    schemeId: Long
) {
    val colors = LocalEggRiceColors.current
    val scope = rememberCoroutineScope()
    val teachers by repository.getAllTeachers().collectAsState(initial = emptyList())
    val distinctNames by repository.getDistinctTeacherNames(schemeId).collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTeacher by remember { mutableStateOf<TeacherEntity?>(null) }
    var confirmingDelete by remember { mutableStateOf<String?>(null) }

    // Auto-sync teachers from course names on first load
    var hasAutoSynced by remember { mutableStateOf(false) }
    LaunchedEffect(distinctNames, hasAutoSynced) {
        if (!hasAutoSynced && distinctNames.isNotEmpty()) {
            val existingNames = teachers.map { it.name }.toSet()
            val toInsert = distinctNames.filter { it !in existingNames }
            if (toInsert.isNotEmpty()) {
                scope.launch {
                    toInsert.forEach { name ->
                        repository.insertTeacher(TeacherEntity(name = name.trim()))
                    }
                }
            }
            hasAutoSynced = true
        }
    }

    val filteredTeachers = remember(searchQuery, teachers) {
        if (searchQuery.isBlank()) teachers
        else teachers.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("查询老师办公室", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surfaceCard)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = accentColor()
            ) {
                Icon(Icons.Filled.Add, "添加老师", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.surfaceAlt)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索老师姓名...", color = colors.textTertiary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor(),
                    cursorColor = accentColor()
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredTeachers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (teachers.isEmpty()) "暂无教师信息，点击右下角 + 添加"
                        else "未找到匹配的老师",
                        color = colors.textTertiary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTeachers, key = { it.name }) { teacher ->
                        TeacherCard(
                            teacher = teacher,
                            onEdit = { editingTeacher = teacher },
                            onDelete = { confirmingDelete = teacher.name }
                        )
                    }
                }
            }
        }
    }

    // Add/Edit dialog
    if (showAddDialog || editingTeacher != null) {
        TeacherEditDialog(
            initial = editingTeacher,
            onDismiss = { showAddDialog = false; editingTeacher = null },
            onSave = { teacher ->
                scope.launch {
                    repository.insertTeacher(teacher)
                }
                showAddDialog = false
                editingTeacher = null
            }
        )
    }

    // Delete confirm dialog
    if (confirmingDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = null },
            title = { Text("删除老师信息") },
            text = { Text("确定要删除「${confirmingDelete}」的信息吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            confirmingDelete?.let { name ->
                                repository.deleteTeacher(name)
                            }
                        }
                        confirmingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) {
                    Text("删除", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun TeacherCard(
    teacher: TeacherEntity,
    onEdit: (TeacherEntity) -> Unit,
    onDelete: (TeacherEntity) -> Unit
) {
    val colors = LocalEggRiceColors.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        teacher.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    if (teacher.title.isNotEmpty()) {
                        Text(
                            teacher.title,
                            fontSize = 13.sp,
                            color = accentColor(),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Row {
                    IconButton(onClick = { onEdit(teacher) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Edit, "编辑", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onDelete(teacher) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Delete, "删除", tint = Color(0xFFE57373), modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (teacher.office.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                InfoRow(Icons.Outlined.WorkOutline, "办公室", teacher.office, colors)
            }

            if (expanded) {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))

                if (teacher.officeHours.isNotEmpty()) {
                    InfoRow(Icons.Outlined.WatchLater, "办公时间", teacher.officeHours, colors)
                    Spacer(Modifier.height(6.dp))
                }
                if (teacher.phone.isNotEmpty()) {
                    InfoRow(Icons.Outlined.MailOutline, "联系电话", teacher.phone, colors)
                    Spacer(Modifier.height(6.dp))
                }
                if (teacher.lastUpdated > 0) {
                    Text(
                        "更新于 ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(teacher.lastUpdated))}",
                        fontSize = 11.sp,
                        color = colors.textTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, colors: EggRiceColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = colors.textTertiary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = colors.textTertiary)
        Spacer(Modifier.width(8.dp))
        Text(value, fontSize = 13.sp, color = colors.textPrimary)
    }
}

@Composable
private fun TeacherEditDialog(
    initial: TeacherEntity?,
    onDismiss: () -> Unit,
    onSave: (TeacherEntity) -> Unit
) {
    val colors = LocalEggRiceColors.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var office by remember { mutableStateOf(initial?.office ?: "") }
    var officeHours by remember { mutableStateOf(initial?.officeHours ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial != null) "编辑老师信息" else "添加老师",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("姓名") }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor(), cursorColor = accentColor()))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("职称") }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor(), cursorColor = accentColor()))
                OutlinedTextField(value = office, onValueChange = { office = it }, label = { Text("办公室") }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor(), cursorColor = accentColor()))
                OutlinedTextField(value = officeHours, onValueChange = { officeHours = it }, label = { Text("办公时间") }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor(), cursorColor = accentColor()))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("联系电话") }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor(), cursorColor = accentColor()))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(TeacherEntity(
                            name = name.trim(),
                            office = office.trim(),
                            officeHours = officeHours.trim(),
                            phone = phone.trim(),
                            title = title.trim()
                        ))
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor())
            ) { Text("保存", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
