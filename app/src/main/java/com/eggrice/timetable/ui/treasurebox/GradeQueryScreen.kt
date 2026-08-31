package com.eggrice.timetable.ui.treasurebox

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.network.ZfGradeItem
import com.eggrice.timetable.network.ZfTerm
import com.eggrice.timetable.ui.theme.*
import com.eggrice.timetable.ui.zhengfang.ZhengfangCaptchaHost
import com.eggrice.timetable.ui.zhengfang.ZhengfangLoginContent
import com.eggrice.timetable.ui.zhengfang.ZhengfangSchoolList
import com.eggrice.timetable.ui.zhengfang.scoreColor

/**
 * 教务成绩查询内容（合并页「课程与成绩」Tab 2）：选学校 → 只登录 → 选学期 → 查看总评/平时/期末/期中，
 * 可导出成绩到「课程管理」。学校接口未下发分项时对应字段为空，页面显示 —。
 * 选校/登录/验证码共用 ZhengfangLoginUi 组件。
 */
@Composable
fun GradeQueryContent(
    viewModel: GradeQueryViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalEggRiceColors.current
    val selectedSchool by viewModel.selectedSchool.collectAsState()
    val loggedIn by viewModel.loggedIn.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceAlt)
    ) {
        when {
            selectedSchool == null -> ZhengfangSchoolList(state = viewModel)
            !loggedIn -> ZhengfangLoginContent(
                state = viewModel,
                hint = "登录正方教务系统（仅登录查询成绩，不会导入课表）",
                buttonText = "登录并查询成绩"
            )
            else -> GradesContent(viewModel)
        }
    }

    ZhengfangCaptchaHost(state = viewModel)
}

// ── 阶段 3：成绩列表 ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradesContent(viewModel: GradeQueryViewModel) {
    val colors = LocalEggRiceColors.current
    val terms by viewModel.terms.collectAsState()
    val selectedTerm by viewModel.selectedTerm.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val error by viewModel.error.collectAsState()
    val gradesLoaded by viewModel.gradesLoaded.collectAsState()
    val savedKeys by viewModel.savedKeys.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 操作行：学期下拉 + 导出本页 + 重新登录
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TermDropdown(
                terms = terms,
                selected = selectedTerm,
                onSelect = { viewModel.selectTerm(it) },
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
                viewModel.saveAllGrades()
                Toast.makeText(context, "已导出当前学期到课程管理", Toast.LENGTH_SHORT).show()
            }) {
                Text("导出本页", fontSize = 12.sp)
            }
            TextButton(onClick = { viewModel.logout() }) {
                Text("重新登录", fontSize = 12.sp)
            }
        }

        // 当前学期汇总统计
        if (grades.isNotEmpty()) {
            val scores = grades.mapNotNull { it.totalScore.toFloatOrNull() }
            val gpas = grades.mapNotNull { it.gpa.toFloatOrNull() }
            Text(
                buildString {
                    append("共 ").append(grades.size).append(" 门")
                    if (scores.isNotEmpty()) append("  ·  平均分 ").append("%.1f".format(scores.average()))
                    if (gpas.isNotEmpty()) append("  ·  平均绩点 ").append("%.2f".format(gpas.average()))
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        if (error != null && grades.isEmpty()) {
            Text(
                error!!,
                color = Color(0xFFE57373),
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (isLoading && grades.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = accentColor()
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    progress.ifBlank { "加载中..." },
                    fontSize = 13.sp,
                    color = colors.textTertiary
                )
            }
        }

        when {
            grades.isEmpty() && gradesLoaded -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "${selectedTerm?.label ?: ""} 暂无成绩记录",
                        color = colors.textTertiary,
                        fontSize = 13.sp
                    )
                }
            }
            grades.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(grades) { item ->
                        val key = "${item.courseName}|${item.termLabel}|${item.totalScore}"
                        GradeCard(
                            item = item,
                            isSaved = key in savedKeys,
                            onSave = {
                                viewModel.saveGrade(item)
                                Toast.makeText(context, "已导出到课程管理", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermDropdown(
    terms: List<ZfTerm>,
    selected: ZfTerm?,
    onSelect: (ZfTerm) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.label ?: "请选择学期",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            terms.forEach { term ->
                DropdownMenuItem(
                    text = { Text(term.label, fontSize = 13.sp) },
                    onClick = {
                        onSelect(term)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GradeCard(item: ZfGradeItem, isSaved: Boolean, onSave: () -> Unit) {
    val colors = LocalEggRiceColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.courseName.ifBlank { "未知课程" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    item.totalScore.ifBlank { "—" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = scoreColor(item.totalScore)
                )
                Spacer(Modifier.width(4.dp))
                // 保存到已修课程（成绩存档）
                IconButton(onClick = onSave, modifier = Modifier.size(34.dp)) {
                    Icon(
                        if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "已保存" else "保存成绩",
                        tint = if (isSaved) colors.textTertiary else accentColor(),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            // 分项行：平时 / 期末 / 期中
            Row(modifier = Modifier.fillMaxWidth()) {
                GradeCell("平时", item.regular, Modifier.weight(1f))
                GradeCell("期末", item.final, Modifier.weight(1f))
                GradeCell("期中", item.midterm, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Divider, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                buildString {
                    append("绩点 ").append(item.gpa.ifBlank { "—" })
                    append("  ·  学分 ").append(item.credits.ifBlank { "—" })
                    if (item.examType.isNotBlank()) append("  ·  ").append(item.examType)
                    append("  ·  ").append(item.termLabel)
                },
                fontSize = 12.sp,
                color = colors.textTertiary
            )
        }
    }
}

@Composable
private fun GradeCell(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalEggRiceColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = colors.textTertiary)
        Spacer(Modifier.height(2.dp))
        Text(
            value.ifBlank { "—" },
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
    }
}
