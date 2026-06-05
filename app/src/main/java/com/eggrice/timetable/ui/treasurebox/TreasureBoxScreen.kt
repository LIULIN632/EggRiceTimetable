package com.eggrice.timetable.ui.treasurebox

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.data.repository.CourseRepository
import com.eggrice.timetable.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasureBoxScreen(
    onBack: () -> Unit,
    onImportCourse: (courseName: String, teacher: String, room: String, day: Int, startSlot: Int, endSlot: Int) -> Unit,
    repository: CourseRepository,
    schemeId: Long
) {
    val colors = LocalEggRiceColors.current
    var showLearningPage by remember { mutableStateOf(false) }
    var showFoodPage by remember { mutableStateOf(false) }
    var showTaskChecklist by remember { mutableStateOf(false) }
    var showGoodItemList by remember { mutableStateOf(false) }
    var showTreeHole by remember { mutableStateOf(false) }

    if (showLearningPage) {
        LearningResourcePage(
            onBack = { showLearningPage = false },
            onImportCourse = onImportCourse
        )
        return
    }
    if (showFoodPage) {
        FoodPickerPage(onBack = { showFoodPage = false })
        return
    }
    if (showTaskChecklist) {
        TaskChecklistScreen(
            repository = repository,
            schemeId = schemeId,
            onBack = { showTaskChecklist = false }
        )
        return
    }
    if (showGoodItemList) {
        GoodItemListScreen(
            repository = repository,
            schemeId = schemeId,
            onBack = { showGoodItemList = false }
        )
        return
    }
    if (showTreeHole) {
        TreeHoleScreen(
            repository = repository,
            schemeId = schemeId,
            onBack = { showTreeHole = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("百宝箱", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "实用工具",
                fontSize = 13.sp,
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 4.dp)
            )

            // ── Card 1: 学习资源 ──
            ToolCard(
                title = "学习资源",
                subtitle = "按学科分类的优质网课资源，一键导入课表",
                bgColor = Color(0xFF8EA0BC),
                onClick = { showLearningPage = true }
            )

            // ── Card 2: 今天吃什么 ──
            ToolCard(
                title = "今天吃什么",
                subtitle = "食堂菜单管理，随机抽取解决选择困难",
                bgColor = Color(0xFFC098A0),
                onClick = { showFoodPage = true }
            )

            // ── Card 3: 大学任务清单 ──
            ToolCard(
                title = "大学任务清单",
                subtitle = "四六级、考研、驾照... 记录大学目标完成情况",
                bgColor = Color(0xFF7AAC94),
                onClick = { showTaskChecklist = true }
            )

            // ── Card 4: 大学好物清单 ──
            ToolCard(
                title = "大学好物清单",
                subtitle = "数码、生活、学习好物推荐，早买早享受",
                bgColor = Color(0xFFB09074),
                onClick = { showGoodItemList = true }
            )

            // ── Card 5: 留声树洞 ──
            ToolCard(
                title = "留声树洞",
                subtitle = "把悄悄话放进树洞，匿名分享你的心事",
                bgColor = Color(0xFF9A9080),
                onClick = { showTreeHole = true }
            )
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
