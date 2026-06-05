package com.eggrice.timetable.ui.treasurebox

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.data.entity.TaskEntity
import com.eggrice.timetable.data.repository.CourseRepository
import com.eggrice.timetable.ui.theme.*
import kotlinx.coroutines.launch

private val PRESET_TASKS = listOf(
    "英语四六级",
    "计算机二级",
    "考驾照",
    "拿奖学金",
    "入党",
    "考研",
    "找实习",
    "参加竞赛",
    "发表论文",
    "社团活动",
    "志愿服务"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskChecklistScreen(
    repository: CourseRepository,
    schemeId: Long,
    onBack: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    val scope = rememberCoroutineScope()

    val tasks by repository.getTasksByScheme(schemeId).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    // Initialize preset tasks on first load
    LaunchedEffect(Unit) {
        if (tasks.isEmpty()) {
            PRESET_TASKS.forEachIndexed { idx, name ->
                repository.insertTask(TaskEntity(name = name, sortOrder = idx, schemeId = schemeId))
            }
        }
    }

    val completedCount = tasks.count { it.completed }
    val totalCount = tasks.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("大学任务清单", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "添加任务", tint = colors.accentMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surfaceCard
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.surfaceAlt)
        ) {
            // ── Progress card ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "任务进度",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                        Text(
                            "$completedCount / $totalCount",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentMain
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = colors.accentMain,
                        trackColor = colors.surfaceAlt,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        if (progress >= 1f) "全部完成，太棒了！🎉"
                        else if (progress >= 0.5f) "已经过半，继续加油！"
                        else if (progress > 0f) "好的开始，坚持下去！"
                        else "完成第一个任务，开启大学生活！",
                        fontSize = 12.sp,
                        color = colors.textTertiary
                    )
                }
            }

            // ── Task list ──
            Text(
                if (completedCount > 0) "已完成 (${completedCount})" else "待完成",
                fontSize = 12.sp,
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Completed tasks first, then incomplete
                val sortedTasks = tasks.sortedWith(compareBy({ !it.completed }, { it.sortOrder }))
                items(sortedTasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onToggle = {
                            scope.launch {
                                repository.updateTask(task.copy(completed = !task.completed))
                            }
                        },
                        onDelete = {
                            scope.launch {
                                repository.deleteTask(task.id)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                scope.launch {
                    val sortOrder = (tasks.maxOfOrNull { it.sortOrder } ?: -1) + 1
                    repository.insertTask(TaskEntity(name = name.trim(), sortOrder = sortOrder, schemeId = schemeId))
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TaskItem(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed)
                colors.surfaceCard.copy(alpha = 0.5f)
            else
                colors.surfaceCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.accentMain,
                    uncheckedColor = colors.textTertiary
                )
            )
            Text(
                text = task.name,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (task.completed)
                    colors.textTertiary.copy(alpha = 0.6f)
                else
                    colors.textPrimary,
                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Outlined.Close,
                    "删除",
                    tint = colors.textTertiary.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除任务") },
            text = { Text("确定要删除「${task.name}」吗？") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC89098))
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = LocalEggRiceColors.current
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加任务", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 30) name = it },
                label = { Text("任务名称") },
                placeholder = { Text("如：通过普通话考试") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accentMain,
                    cursorColor = colors.accentMain
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentMain)
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
