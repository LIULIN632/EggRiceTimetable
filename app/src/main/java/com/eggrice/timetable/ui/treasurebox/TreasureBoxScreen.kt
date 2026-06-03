package com.eggrice.timetable.ui.treasurebox

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val isDark = LocalDarkMode.current
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
                    containerColor = if (isDark) DarkSurfaceCard else Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDark) DarkSurfaceAlt else SurfaceAlt)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "实用工具",
                fontSize = 13.sp,
                color = if (isDark) DarkTextTertiary else TextTertiary,
                modifier = Modifier.padding(start = 4.dp)
            )

            // ── Card 1: 学习资源 ──
            ToolCard(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                title = "学习资源",
                subtitle = "按学科分类的优质网课资源，一键导入课表",
                gradientColors = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
                onClick = { showLearningPage = true }
            )

            // ── Card 2: 今天吃什么 ──
            ToolCard(
                icon = Icons.Outlined.Restaurant,
                title = "今天吃什么",
                subtitle = "随机抽取美食，解决选择困难症",
                gradientColors = listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
                onClick = { showFoodPage = true }
            )

            // ── Card 3: 大学任务清单 ──
            ToolCard(
                icon = Icons.Outlined.Checklist,
                title = "大学任务清单",
                subtitle = "四六级、考研、驾照... 记录大学目标完成情况",
                gradientColors = listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),
                onClick = { showTaskChecklist = true }
            )

            // ── Card 4: 大学好物清单 ──
            ToolCard(
                icon = Icons.Outlined.Star,
                title = "大学好物清单",
                subtitle = "数码、生活、学习好物推荐，早买早享受",
                gradientColors = listOf(Color(0xFFFF9A56), Color(0xFFFF6B6B)),
                onClick = { showGoodItemList = true }
            )

            // ── Card 5: 留声树洞 ──
            ToolCard(
                icon = Icons.Outlined.Park,
                title = "留声树洞",
                subtitle = "把悄悄话放进树洞，匿名分享你的心事",
                gradientColors = listOf(Color(0xFF8B6914), Color(0xFFC8A25C)),
                onClick = { showTreeHole = true }
            )
        }
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    val isDark = LocalDarkMode.current
    Card(
        modifier = Modifier.fillMaxWidth().height(110.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 2)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }
        }
    }
}
