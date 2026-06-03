package com.eggrice.timetable.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.ui.theme.*

data class VersionLog(
    val version: String,
    val date: String,
    val features: List<String> = emptyList(),
    val fixes: List<String> = emptyList(),
    val improvements: List<String> = emptyList()
)

val changelog = listOf(
    VersionLog(
        version = "v6.85",
        date = "2026-06-02",
        improvements = listOf(
            "压缩顶部栏和星期栏高度，一屏显示更多课程",
            "双重 clipToBounds 彻底修复课程卡片与星期栏重叠",
            "WebView导入页底部改为双按钮：自动识别课表 / 手动导入课表",
            "JS适配器支持iframe内课表检测，新增800ms动态DOM加载延迟",
            "JS适配器全流程详细日志输出，便于定位解析问题"
        ),
        fixes = listOf(
            "移除导入流程自动去重逻辑"
        )
    ),
    VersionLog(
        version = "v6.84",
        date = "2026-05-30",
        features = listOf(
            "新增「快捷设置当前周次」功能，自动反推学期开始日期",
            "周次选择器与日期选择器双向同步，无反馈循环"
        )
    ),
    VersionLog(
        version = "v6.83",
        date = "2026-05-28",
        fixes = listOf(
            "修复竖排课表只解析到4门课的关键BUG（dayMap列比较逻辑错误）",
            "修复横排/强智/URP解析器未映射列时静默丢弃课程的问题",
            "竖排解析器完全重写为按列优先遍历，支持rowspan跨行"
        ),
        improvements = listOf(
            "dayMap不足3列时自动回退为位置映射",
            "节次标签识别支持纯数字（1-12）"
        )
    ),
    VersionLog(
        version = "v6.82",
        date = "2026-05-26",
        fixes = listOf(
            "修复课程卡片文字未对齐顶部的问题，卡片垂直内边距从8dp减至4dp"
        )
    ),
    VersionLog(
        version = "v6.81",
        date = "2026-05-25",
        fixes = listOf(
            "回退无效的解析变更，恢复稳定版本解析逻辑"
        )
    ),
    VersionLog(
        version = "v6.8",
        date = "2026-05-22",
        features = listOf(
            "完整拖拽换课功能：长按课程卡片拖动到目标格子",
            "拖拽时浮层卡片跟随手指，目标格子高亮提示"
        ),
        fixes = listOf(
            "修复v6.6启动崩溃问题，重构为稳定版本"
        )
    ),
    VersionLog(
        version = "v6.6",
        date = "2026-05-18",
        features = listOf(
            "新增课表方案管理功能，支持多套课表快速切换",
            "新增外观自定义：格子高度、圆角、透明度、文字大小实时预览",
            "新增边框样式：无边框 / 实线 / 虚线"
        ),
        improvements = listOf(
            "Material3主题全面升级",
            "深色/浅色模式完善适配"
        )
    ),
    VersionLog(
        version = "v6.0",
        date = "2026-05-10",
        features = listOf(
            "全新 Material3 + Jetpack Compose 重构",
            "支持正方/强智/青果/超星四大教务系统课表导入",
            "OkHttp原生教务登录 + WebView辅助导入双通道",
            "分享口令导入/导出，一键分享课表给好友",
            "百宝箱：学习资源聚合"
        ),
        improvements = listOf(
            "底部双Tab设计：课程 / 我的",
            "极简清新UI，低饱和马卡龙配色",
            "所有数据仅本地Room存储，不上传任何服务器"
        )
    ),
    VersionLog(
        version = "v5.5",
        date = "2026-04-20",
        features = listOf(
            "新增Excel/HTML文件导入课表",
            "新增CSV导出功能"
        ),
        fixes = listOf(
            "修复部分机型WebView加载白屏问题",
            "修复强智教务Session过期后解析失败"
        )
    ),
    VersionLog(
        version = "v5.4",
        date = "2026-04-10",
        features = listOf(
            "初版发布：蛋炒饭课程表正式上线",
            "正方教务系统课表自动导入",
            "本地Room数据库存储",
            "课程编辑、删除、手动添加"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    val isDark = LocalDarkMode.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("更新日志", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceCard)
            )
        },
        containerColor = if (isDark) DarkSurface else SurfaceCard
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(changelog) { index, log ->
                VersionCard(log, isFirst = index == 0)
            }
        }
    }
}

@Composable
private fun VersionCard(log: VersionLog, isFirst: Boolean) {
    val isDark = LocalDarkMode.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurfaceCard else SurfaceCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFirst) 2.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Version header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isFirst) accentColor() else accentSoftColor()
                ) {
                    Text(
                        text = log.version,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isFirst) SurfaceCard else accentColor(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = log.date,
                    fontSize = 13.sp,
                    color = if (isDark) DarkTextTertiary else TextTertiary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Features
            if (log.features.isNotEmpty()) {
                SectionTag("新增功能", SuccessGreen)
                Spacer(Modifier.height(4.dp))
                log.features.forEach { Bullet(it) }
            }

            // Fixes
            if (log.fixes.isNotEmpty()) {
                if (log.features.isNotEmpty()) Spacer(Modifier.height(6.dp))
                SectionTag("修复BUG", DangerColor)
                Spacer(Modifier.height(4.dp))
                log.fixes.forEach { Bullet(it) }
            }

            // Improvements
            if (log.improvements.isNotEmpty()) {
                if (log.features.isNotEmpty() || log.fixes.isNotEmpty()) Spacer(Modifier.height(6.dp))
                SectionTag("优化改进", OrangeAccent)
                Spacer(Modifier.height(4.dp))
                log.improvements.forEach { Bullet(it) }
            }
        }
    }
}

@Composable
private fun SectionTag(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun Bullet(text: String) {
    val isDark = LocalDarkMode.current
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
    ) {
        Text(
            text = "·",
            fontSize = 14.sp,
            color = if (isDark) DarkTextTertiary else TextTertiary
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = if (isDark) DarkTextSecondary else TextSecondary,
            lineHeight = 18.sp
        )
    }
}
