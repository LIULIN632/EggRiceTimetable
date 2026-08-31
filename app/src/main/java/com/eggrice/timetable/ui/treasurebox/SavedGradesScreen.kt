package com.eggrice.timetable.ui.treasurebox

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.data.entity.SavedGradeEntity
import com.eggrice.timetable.di.AppContainer
import com.eggrice.timetable.network.ZhengfangClient
import com.eggrice.timetable.ui.theme.*
import com.eggrice.timetable.ui.zhengfang.ZhengfangCaptchaHost
import com.eggrice.timetable.ui.zhengfang.ZhengfangLoginContent
import com.eggrice.timetable.ui.zhengfang.ZhengfangSchoolList
import com.eggrice.timetable.ui.zhengfang.scoreColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 课程管理内容（合并页「课程与成绩」Tab 1）：按学期分组展示已保存的课程成绩；
 * 可从教务一键拉取全部学期成绩保存，离线可查看。同步流程的选校/登录/验证码共用 ZhengfangLoginUi 组件。
 */
@Composable
fun SavedGradesContent(
    viewModel: SavedGradesViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalEggRiceColors.current
    val context = LocalContext.current
    val syncing by viewModel.syncing.collectAsState()
    val selectedSchool by viewModel.selectedSchool.collectAsState()
    val syncResult by viewModel.syncResult.collectAsState()

    // 同步完成提示
    LaunchedEffect(syncResult) {
        if (syncResult != null) {
            Toast.makeText(context, syncResult, Toast.LENGTH_LONG).show()
            viewModel.consumeSyncResult()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceAlt)
    ) {
        when {
            // 同步流程：先选学校
            syncing && selectedSchool == null -> ZhengfangSchoolList(
                state = viewModel,
                header = {
                    val c = LocalEggRiceColors.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.backToSchoolList() }) {
                            Text("返回列表", fontSize = 13.sp)
                        }
                        Text(
                            "选择学校以同步成绩",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = c.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            )
            // 同步流程：登录 + 拉取进度
            syncing -> ZhengfangLoginContent(
                state = viewModel,
                hint = "登录正方教务系统，自动拉取全部学期成绩并保存（不会导入课表）",
                buttonText = "登录并同步全部成绩",
                topBackLabel = "返回列表",
                onTopBack = { viewModel.backToSchoolList() }
            )
            // 默认：本地成绩列表
            else -> SavedListContent(viewModel)
        }
    }

    ZhengfangCaptchaHost(state = viewModel)
}

// ── 本地成绩列表 ──

@Composable
private fun SavedListContent(viewModel: SavedGradesViewModel) {
    val colors = LocalEggRiceColors.current
    val grades by viewModel.savedGrades.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部：同步入口
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "保存后可离线查看",
                fontSize = 12.sp,
                color = colors.textTertiary,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { viewModel.startSync() },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor())
            ) {
                Icon(Icons.Outlined.Sync, null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("从教务同步", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 汇总统计卡
        if (grades.isNotEmpty()) {
            SavedStatsCard(grades)
        }

        if (grades.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有保存的课程成绩", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "登录教务系统后，自动拉取所有学期成绩并保存",
                        fontSize = 12.sp,
                        color = colors.textTertiary
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.startSync() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor())
                    ) {
                        Text("从教务同步成绩", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // 按学期分组展示（学期倒序，新在前）
            val grouped = grades.groupBy { it.termLabel.ifBlank { "未分学期" } }
            val terms = grouped.keys.sortedDescending()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                terms.forEach { term ->
                    val list = grouped.getValue(term)
                    item(key = "term_$term") {
                        TermHeader(term, list)
                    }
                    items(list, key = { it.id }) { grade ->
                        SavedGradeCard(grade, onDelete = { viewModel.deleteGrade(grade.id) })
                    }
                }
            }
        }
    }
}

// ── 学期分组标题 ──

@Composable
private fun TermHeader(term: String, grades: List<SavedGradeEntity>) {
    val colors = LocalEggRiceColors.current
    val totalCredit = grades.mapNotNull { it.credits.toFloatOrNull() }.sum()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            term,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            buildString {
                append(grades.size).append(" 门")
                if (totalCredit > 0) append("  ·  学分 ").append("%.1f".format(totalCredit))
            },
            fontSize = 12.sp,
            color = colors.textTertiary
        )
    }
}

// ── 汇总统计卡 ──

@Composable
private fun SavedStatsCard(grades: List<SavedGradeEntity>) {
    val colors = LocalEggRiceColors.current
    val scores = grades.mapNotNull { it.totalScore.toFloatOrNull() }
    val gpas = grades.mapNotNull { it.gpa.toFloatOrNull() }
    val passRate = if (scores.isNotEmpty()) scores.count { it >= 60 } * 100.0 / scores.size else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell("总门数", grades.size.toString(), Modifier.weight(1f))
            StatCell("平均分", if (scores.isNotEmpty()) "%.1f".format(scores.average()) else "—", Modifier.weight(1f))
            StatCell("平均绩点", if (gpas.isNotEmpty()) "%.2f".format(gpas.average()) else "—", Modifier.weight(1f))
            StatCell("通过率", passRate?.let { "%.0f%%".format(it) } ?: "—", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalEggRiceColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = colors.textTertiary)
    }
}

@Composable
private fun SavedGradeCard(grade: SavedGradeEntity, onDelete: () -> Unit) {
    val colors = LocalEggRiceColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        grade.courseName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (grade.totalScore.isNotBlank()) {
                        Text(
                            grade.totalScore,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = scoreColor(grade.totalScore)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    buildString {
                        if (grade.credits.isNotBlank()) append("学分 ").append(grade.credits)
                        if (grade.gpa.isNotBlank()) append("  ·  绩点 ").append(grade.gpa)
                        if (grade.termLabel.isNotBlank()) append("  ·  ").append(grade.termLabel)
                        if (grade.schoolName.isNotBlank()) append("  ·  ").append(grade.schoolName)
                    }.ifBlank { "暂无详细信息" },
                    fontSize = 12.sp,
                    color = colors.textTertiary
                )
                if (grade.savedAt > 0) {
                    Text(
                        "保存于 " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(grade.savedAt)),
                        fontSize = 11.sp,
                        color = colors.textTertiary.copy(alpha = 0.8f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    "删除",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
