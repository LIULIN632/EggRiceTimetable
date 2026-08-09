package com.eggrice.timetable.ui.timetable.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.data.entity.HomeworkEntity
import com.eggrice.timetable.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeworkListDialog(
    homework: List<HomeworkEntity>,
    onDismiss: () -> Unit,
    onToggleComplete: (HomeworkEntity) -> Unit,
    onDelete: (HomeworkEntity) -> Unit,
    onAdd: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    val fmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.CHINA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("作业管理", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, "添加作业", tint = colors.accentMain)
                }
            }
        },
        text = {
            if (homework.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Assignment,
                            null,
                            tint = colors.textTertiary.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("暂无作业", color = colors.textTertiary.copy(alpha = 0.5f), fontSize = 14.sp)
                        Text("点击右上角 + 添加", color = colors.textTertiary.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(homework, key = { it.id }) { hw ->
                        val bg = colors.surfaceAlt
                        HomeworkItem(
                            hw = hw,
                            formattedDate = fmt.format(Date(hw.createdAt)),
                            bgColor = bg,
                            onToggle = { onToggleComplete(hw) },
                            onDelete = { onDelete(hw) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun HomeworkItem(
    hw: HomeworkEntity,
    formattedDate: String,
    bgColor: Color,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    val textColor = colors.textPrimary
    val secondaryColor = colors.textSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Complete checkbox
        Icon(
            imageVector = if (hw.completed) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
            contentDescription = if (hw.completed) "已完成" else "未完成",
            tint = if (hw.completed) SuccessGreen else colors.textTertiary,
            modifier = Modifier.size(22.dp).clickable { onToggle() }
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = hw.courseName,
                fontSize = 12.sp,
                color = colors.accentMain,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            if (hw.content.isNotEmpty()) {
                Text(
                    text = hw.content,
                    fontSize = 14.sp,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (hw.completed) TextDecoration.LineThrough else TextDecoration.None
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hw.dueDate.isNotEmpty()) {
                    Icon(Icons.Outlined.Event, null, tint = secondaryColor, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(hw.dueDate, fontSize = 11.sp, color = secondaryColor, maxLines = 1)
                    Spacer(Modifier.width(12.dp))
                }
                Text(formattedDate, fontSize = 11.sp, color = colors.textTertiary)
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Delete, "删除", tint = colors.textTertiary, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHomeworkDialog(
    onDismiss: () -> Unit,
    onSave: (courseName: String, content: String, dueDate: String) -> Unit,
    existingCourses: List<String> = emptyList(),
    initialCourseName: String = ""
) {
    val colors = LocalEggRiceColors.current
    val allCourses = remember(existingCourses) {
        val set = linkedSetOf<String>()
        if (initialCourseName.isNotBlank()) set.add(initialCourseName)
        set.addAll(existingCourses)
        set.toList()
    }

    var selectedCourse by remember { mutableStateOf(if (initialCourseName.isNotBlank()) initialCourseName else "") }
    var customCourse by remember { mutableStateOf("") }
    var isCustomEntry by remember { mutableStateOf(allCourses.isEmpty() || (initialCourseName.isNotBlank() && initialCourseName !in existingCourses)) }
    var content by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }

    var showCourseError by remember { mutableStateOf(false) }
    var courseExpanded by remember { mutableStateOf(false) }

    val courseName = if (isCustomEntry) customCourse else selectedCourse

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("添加作业", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Course selector: dropdown or text field
                if (allCourses.isNotEmpty() && !isCustomEntry) {
                    ExposedDropdownMenuBox(
                        expanded = courseExpanded,
                        onExpandedChange = { courseExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCourse.ifEmpty { "选择课程" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("课程名称 *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded) },
                            isError = showCourseError,
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = courseExpanded,
                            onDismissRequest = { courseExpanded = false }
                        ) {
                            allCourses.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        selectedCourse = name
                                        courseExpanded = false
                                        showCourseError = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("+ 自定义输入", color = colors.accentMain, fontSize = 13.sp) },
                                onClick = {
                                    isCustomEntry = true
                                    customCourse = ""
                                    courseExpanded = false
                                }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = customCourse,
                        onValueChange = { customCourse = it; showCourseError = false },
                        label = { Text("课程名称 *") },
                        placeholder = { Text("例如：高等数学") },
                        singleLine = true,
                        isError = showCourseError,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    // Switch back to dropdown if courses available
                    if (allCourses.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                isCustomEntry = false
                                selectedCourse = allCourses.firstOrNull() ?: ""
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("从已有课程选择 →", color = colors.accentMain, fontSize = 12.sp)
                        }
                    }
                }

                if (showCourseError) {
                    Text("请选择或输入课程名称", color = colors.danger, fontSize = 11.sp)
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("作业内容") },
                    placeholder = { Text("例如：P45 习题3-5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("截止日期 (可选)") },
                    placeholder = { Text("例如：下周一 / 6月15日") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = courseName.trim()
                    if (finalName.isBlank()) { showCourseError = true; return@Button }
                    onSave(finalName, content.trim(), dueDate.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentMain),
                shape = RoundedCornerShape(10.dp)
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
