package com.eggrice.timetable.ui.treasurebox

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eggrice.timetable.ui.components.EmptyStatePlaceholder
import com.eggrice.timetable.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningResourcePage(
    onBack: () -> Unit,
    onImportCourse: (courseName: String, teacher: String, room: String, day: Int, startSlot: Int, endSlot: Int) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: TreasureBoxViewModel = viewModel(factory = TreasureBoxViewModel.Factory(app))
    val resources by viewModel.resources.collectAsState()
    val favorites by viewModel.favoriteIds.collectAsState()
    val isDark = LocalDarkMode.current

    var selectedSubject by remember { mutableStateOf("全部") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showFavoritesOnly by remember { mutableStateOf(false) }

    val subjects = remember(resources) {
        listOf("全部") + (resources.map { it.subject }.distinct().filter { it != "自定义" })
    }

    val filteredResources = remember(resources, selectedSubject, showFavoritesOnly, favorites) {
        val base = if (selectedSubject == "全部") resources
        else resources.filter { it.subject == selectedSubject }
        if (showFavoritesOnly) base.filter { it.id in favorites } else base
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学习资源", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "添加资源", tint = accentColor())
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
        ) {
            // Subject + favorite filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) DarkSurfaceCard else Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subjects) { subject ->
                        FilterChip(
                            selected = selectedSubject == subject,
                            onClick = { selectedSubject = subject },
                            label = { Text(subject, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor(),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                FilterChip(
                    selected = showFavoritesOnly,
                    onClick = { showFavoritesOnly = !showFavoritesOnly },
                    label = { Text("已收藏", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            if (showFavoritesOnly) Icons.Filled.Star else Icons.Outlined.Star,
                            null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFB300),
                        selectedLabelColor = Color.White
                    )
                )
            }

            if (filteredResources.isEmpty()) {
                EmptyStatePlaceholder(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    message = "暂无学习资源"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredResources) { resource ->
                        val isFav = resource.id in favorites
                        ResourceCard(
                            resource = resource,
                            isDark = isDark,
                            isFavorite = isFav,
                            onOpenUrl = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resource.videoUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(resource.id) },
                            onImport = {
                                onImportCourse(
                                    resource.courseName,
                                    resource.blogger,
                                    "${resource.subject}课",
                                    resource.dayOfWeek,
                                    resource.startSlot,
                                    resource.endSlot
                                )
                                Toast.makeText(context, "已添加到课表！", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val shareText = "【学习资源推荐】\n课程：${resource.courseName}\n博主：${resource.blogger}\n简介：${resource.description}\n链接：${resource.videoUrl}\n\n—— 来自「蛋炒饭课程表」百宝箱"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "分享学习资源"))
                            },
                            onDelete = if (resource.isCustom) {
                                { viewModel.deleteResource(resource.id) }
                            } else null
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddResourceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { resource ->
                viewModel.addResource(resource)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ResourceCard(
    resource: LearningResource,
    isDark: Boolean,
    isFavorite: Boolean,
    onOpenUrl: () -> Unit,
    onToggleFavorite: () -> Unit,
    onImport: () -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val subjectColor = when (resource.subject) {
        "数学" -> Color(0xFF667EEA)
        "编程" -> Color(0xFF43A047)
        "英语" -> Color(0xFFEF5350)
        "考研" -> Color(0xFFFF7043)
        "设计" -> Color(0xFFAB47BC)
        "通识" -> Color(0xFF26A69A)
        else -> accentColor()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Subject badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = subjectColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        resource.subject,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = subjectColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    resource.courseName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkTextPrimary else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Favorite indicator
                if (isFavorite) {
                    Icon(
                        Icons.Filled.Star, "已收藏",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Close, "删除", tint = TextTertiary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Person, null, tint = accentColor(), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(resource.blogger, fontSize = 12.sp, color = accentColor(), fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(4.dp))
            Text(resource.description, fontSize = 12.sp, color = if (isDark) DarkTextSecondary else TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Jump to URL button
                OutlinedButton(
                    onClick = onOpenUrl,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("跳转", fontSize = 11.sp)
                }
                Spacer(Modifier.width(6.dp))
                // Favorite button
                FilledTonalButton(
                    onClick = onToggleFavorite,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isFavorite) Color(0xFFFFF3CD) else accentSoftColor().copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        null,
                        tint = if (isFavorite) Color(0xFFFFB300) else accentColor(),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isFavorite) "已收藏" else "收藏", fontSize = 11.sp, color = if (isFavorite) Color(0xFFE6A000) else accentColor())
                }
                Spacer(Modifier.width(6.dp))
                // Import to timetable button
                FilledTonalButton(
                    onClick = onImport,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = accentSoftColor())
                ) {
                    Icon(Icons.Outlined.AddCircle, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("导入", fontSize = 11.sp)
                }
                Spacer(Modifier.width(6.dp))
                // Share button
                FilledTonalButton(
                    onClick = onShare,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = accentSoftColor())
                ) {
                    Icon(Icons.Outlined.Share, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("分享", fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddResourceDialog(
    onDismiss: () -> Unit,
    onConfirm: (LearningResource) -> Unit
) {
    var subject by remember { mutableStateOf("自定义") }
    var courseName by remember { mutableStateOf("") }
    var blogger by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableIntStateOf(1) }
    var startSlot by remember { mutableIntStateOf(1) }
    var endSlot by remember { mutableIntStateOf(2) }

    var subjectExpanded by remember { mutableStateOf(false) }
    val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加学习资源", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Subject dropdown
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = it }
                ) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("学科分类") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                        SUBJECTS.filter { it != "自定义" }.forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = { subject = s; subjectExpanded = false })
                        }
                        DropdownMenuItem(text = { Text("自定义") }, onClick = { subject = "自定义"; subjectExpanded = false })
                    }
                }

                OutlinedTextField(value = courseName, onValueChange = { courseName = it }, label = { Text("课程名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = blogger, onValueChange = { blogger = it }, label = { Text("推荐博主") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("视频简介") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("视频链接") }, placeholder = { Text("https://b23.tv/...") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var dayExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = dayExpanded, onExpandedChange = { dayExpanded = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = weekdays[dayOfWeek - 1], onValueChange = {}, readOnly = true,
                            label = { Text("星期") }, singleLine = true,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        )
                        ExposedDropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                            weekdays.forEachIndexed { i, d -> DropdownMenuItem(text = { Text(d) }, onClick = { dayOfWeek = i + 1; dayExpanded = false }) }
                        }
                    }
                    var sExp by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = sExp, onExpandedChange = { sExp = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = "第${startSlot}节", onValueChange = {}, readOnly = true,
                            label = { Text("开始节") }, singleLine = true,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        )
                        ExposedDropdownMenu(expanded = sExp, onDismissRequest = { sExp = false }) {
                            (1..12).forEach { s -> DropdownMenuItem(text = { Text("第${s}节") }, onClick = { startSlot = s; if (endSlot < s) endSlot = s; sExp = false }) }
                        }
                    }
                    var eExp by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = eExp, onExpandedChange = { eExp = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = "第${endSlot}节", onValueChange = {}, readOnly = true,
                            label = { Text("结束节") }, singleLine = true,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        )
                        ExposedDropdownMenu(expanded = eExp, onDismissRequest = { eExp = false }) {
                            (startSlot..12).forEach { s -> DropdownMenuItem(text = { Text("第${s}节") }, onClick = { endSlot = s; eExp = false }) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(LearningResource(
                        subject = subject, courseName = courseName, blogger = blogger,
                        description = description, videoUrl = videoUrl,
                        dayOfWeek = dayOfWeek, startSlot = startSlot, endSlot = endSlot
                    ))
                },
                enabled = courseName.isNotBlank() && blogger.isNotBlank() && videoUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor())
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
