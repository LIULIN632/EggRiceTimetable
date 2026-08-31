package com.eggrice.timetable.ui.treasurebox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eggrice.timetable.data.SchoolRegistry
import com.eggrice.timetable.di.AppContainer
import com.eggrice.timetable.network.AcademicCourseItem
import com.eggrice.timetable.network.AcademicSummary
import com.eggrice.timetable.network.AcademicTypeInfo
import com.eggrice.timetable.network.ZhengfangClient
import com.eggrice.timetable.ui.theme.*
import com.eggrice.timetable.ui.zhengfang.ZhengfangCaptchaHost
import com.eggrice.timetable.ui.zhengfang.ZhengfangLoginContent
import com.eggrice.timetable.ui.zhengfang.ZhengfangSchoolList
import com.eggrice.timetable.ui.zhengfang.scoreColor

/**
 * 修课情况查询（试验阶段）：选学校 → 只登录 → 查看已修课程 / 学分统计 / 平均绩点。
 * 数据来自正方 v9 教务「学生学业情况」页面。选校/登录/验证码共用 ZhengfangLoginUi 组件。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicQueryScreen(
    onBack: () -> Unit,
    client: ZhengfangClient,
    schoolRegistry: SchoolRegistry,
    appContainer: AppContainer
) {
    val viewModel: AcademicQueryViewModel = viewModel(
        factory = AcademicQueryViewModel.Factory(client, schoolRegistry, appContainer)
    )
    val colors = LocalEggRiceColors.current
    val selectedSchool by viewModel.selectedSchool.collectAsState()
    val loggedIn by viewModel.loggedIn.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("修课情况查询", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surfaceCard)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.surfaceAlt)
        ) {
            when {
                selectedSchool == null -> ZhengfangSchoolList(state = viewModel)
                !loggedIn -> ZhengfangLoginContent(
                    state = viewModel,
                    hint = "登录正方教务系统（仅登录查询修课情况，不会导入课表）",
                    buttonText = "登录并查询修课情况"
                )
                else -> ResultContent(viewModel)
            }
        }
    }

    ZhengfangCaptchaHost(state = viewModel)
}

// ── 阶段 3：结果 ──

@Composable
private fun ResultContent(viewModel: AcademicQueryViewModel) {
    val colors = LocalEggRiceColors.current
    val summary by viewModel.summary.collectAsState()
    val types by viewModel.types.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val error by viewModel.error.collectAsState()
    val loaded by viewModel.loaded.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 操作行：重新登录
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "已修课程 · 学分统计",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { viewModel.logout() }) {
                Text("重新登录", fontSize = 13.sp)
            }
        }

        if (error != null && !loaded) {
            Text(
                error!!,
                color = Color(0xFFE57373),
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (isLoading && !loaded) {
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

        if (loaded) {
            val s = summary
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (s != null) {
                    item { SummaryCard(s) }
                    if (s.plannedTotal.isNotBlank() || s.unplannedPassed.isNotBlank()) {
                        item { CourseStatsCard(s) }
                    }
                }
                if (types.isNotEmpty()) {
                    item { CreditStatsHeader() }
                    item { CreditStatsCard(types) }
                }
                if (courses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "暂无课程明细",
                                color = colors.textTertiary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    // 按类型分组展示
                    types.forEach { type ->
                        val typeCourses = courses.filter { it.typeName == type.name }
                        if (typeCourses.isNotEmpty()) {
                            item { TypeHeader(type) }
                            items(typeCourses, key = { "${type.id}_${it.courseId}_${it.courseName}" }) { course ->
                                AcademicCourseCard(course)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 汇总卡片 ──

@Composable
private fun SummaryCard(summary: AcademicSummary) {
    val colors = LocalEggRiceColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("平均学分绩点", fontSize = 12.sp, color = colors.textTertiary)
                Text(
                    summary.gpa.ifBlank { "—" },
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor()
                )
            }
            if (summary.studentId.isNotBlank()) {
                Text(
                    "学号 ${summary.studentId}",
                    fontSize = 12.sp,
                    color = colors.textTertiary
                )
            }
        }
    }
}

@Composable
private fun CourseStatsCard(summary: AcademicSummary) {
    val colors = LocalEggRiceColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("培养方案完成情况", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Spacer(Modifier.height(10.dp))
            if (summary.plannedTotal.isNotBlank()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCell("计划内总数", summary.plannedTotal, Modifier.weight(1f))
                    StatCell("通过", summary.plannedPassed, Modifier.weight(1f))
                    StatCell("未通过", summary.plannedFailed, Modifier.weight(1f))
                    StatCell("未修", summary.plannedMissed, Modifier.weight(1f))
                    StatCell("在读", summary.plannedIn, Modifier.weight(1f))
                }
            }
            if (summary.unplannedPassed.isNotBlank() || summary.unplannedFailed.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCell("计划外通过", summary.unplannedPassed, Modifier.weight(1f))
                    StatCell("计划外未通过", summary.unplannedFailed, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalEggRiceColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value.ifBlank { "—" },
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = colors.textTertiary)
    }
}

// ── 学分汇总（各类型）──

@Composable
private fun CreditStatsHeader() {
    val colors = LocalEggRiceColors.current
    Text(
        "课程类型学分",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun CreditStatsCard(types: List<AcademicTypeInfo>) {
    val colors = LocalEggRiceColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            types.forEachIndexed { index, type ->
                if (index > 0) HorizontalDivider(color = Divider, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        type.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "已获 " + type.earnedCredit.ifBlank { "—" } +
                            " / 要求 " + type.requiredCredit.ifBlank { "—" } +
                            " / 缺 " + type.missedCredit.ifBlank { "—" },
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

// ── 课程列表 ──

@Composable
private fun TypeHeader(type: AcademicTypeInfo) {
    val colors = LocalEggRiceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            type.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (type.earnedCredit.isNotBlank()) {
            Text(
                "已获 ${type.earnedCredit} 学分",
                fontSize = 12.sp,
                color = colors.textTertiary
            )
        }
    }
}

@Composable
private fun AcademicCourseCard(item: AcademicCourseItem) {
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (item.maxGrade.isNotBlank()) {
                    Text(
                        item.maxGrade,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = scoreColor(item.maxGrade)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Divider, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                buildString {
                    if (item.credit.isNotBlank()) append("学分 ").append(item.credit)
                    if (item.gradePoint.isNotBlank()) append("  ·  绩点 ").append(item.gradePoint)
                    if (item.nature.isNotBlank()) append("  ·  ").append(item.nature)
                    if (item.status.isNotBlank()) append("  ·  ").append(item.status)
                    if (item.term.isNotBlank()) append("  ·  ").append(item.term)
                    if (isEmpty()) append("暂无详细信息")
                },
                fontSize = 12.sp,
                color = colors.textTertiary
            )
        }
    }
}

