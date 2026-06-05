package com.eggrice.timetable.ui.treasurebox

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eggrice.timetable.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPickerPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: TreasureBoxViewModel = viewModel(factory = TreasureBoxViewModel.Factory(app))
    val pickedFood by viewModel.pickedFood.collectAsState()
    val isRolling by viewModel.isRolling.collectAsState()
    val foodOptions by viewModel.foodOptions.collectAsState()
    val colors = LocalEggRiceColors.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<FoodOption?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("全部") }

    val categories = remember(foodOptions) {
        listOf("全部", "一食堂", "二食堂", "三食堂", "外卖") +
            foodOptions.map { it.category }.distinct().filter {
                it.isNotEmpty() && it !in listOf("一食堂", "二食堂", "三食堂", "外卖")
            }
    }

    val filteredFood = remember(foodOptions, selectedCategory) {
        if (selectedCategory == "全部") foodOptions
        else foodOptions.filter { it.category == selectedCategory }
    }

    val scaleAnim by animateFloatAsState(
        targetValue = if (pickedFood != null) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("今天吃什么", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // Export
                    IconButton(onClick = {
                        val json = viewModel.exportFoodOptionsJson(includeDefaults = false)
                        if (json == "[]") {
                            Toast.makeText(context, "暂无自定义菜单可导出", Toast.LENGTH_SHORT).show()
                        } else {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, json)
                                putExtra(Intent.EXTRA_SUBJECT, "食堂菜单分享")
                            }
                            context.startActivity(Intent.createChooser(intent, "分享菜单"))
                        }
                    }) {
                        Icon(Icons.Outlined.Share, "导出", tint = colors.accentMain)
                    }
                    // Import
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Outlined.FileDownload, "导入", tint = colors.accentMain)
                    }
                    // Add
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "添加菜品", tint = colors.accentMain)
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
            // ── Random picker card ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (pickedFood == null) "今天吃什么呢？" else "就决定是你了！",
                        fontSize = 12.sp,
                        color = colors.textTertiary
                    )

                    Spacer(Modifier.height(12.dp))

                    // Food display circle
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(scaleAnim)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFFC8A0A4), Color(0xFFD8C0B8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (pickedFood != null) {
                                Icon(
                                    Icons.Outlined.Restaurant, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    pickedFood!!.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                                if (pickedFood!!.windowName.isNotEmpty()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        pickedFood!!.windowName,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                if (pickedFood!!.price.isNotEmpty()) {
                                    Text(
                                        "¥${pickedFood!!.price}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Outlined.Restaurant, null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("???", fontSize = 16.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Roll button
                    Button(
                        onClick = { viewModel.pickRandomFood() },
                        enabled = !isRolling,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB89498))
                    ) {
                        if (isRolling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("抽选中...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Outlined.Casino, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (pickedFood == null) "开始抽取" else "换一个", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── List header ──
            Text(
                "菜品列表 (${filteredFood.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // ── Category filter ──
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFB89498),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Food list ──
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredFood) { food ->
                    FoodCard(
                        food = food,
                        onEdit = if (food.isCustom) {{ editingFood = food }} else null,
                        onDelete = if (food.isCustom) {{ viewModel.deleteFoodOption(food.id) }} else null
                    )
                }
                // Bottom spacer
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // ── Dialogs ──
    if (showAddDialog) {
        FoodFormDialog(
            title = "添加菜品",
            onDismiss = { showAddDialog = false },
            onConfirm = { food ->
                viewModel.addFoodOption(food)
                showAddDialog = false
                Toast.makeText(context, "已添加「${food.name}」", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (editingFood != null) {
        FoodFormDialog(
            title = "编辑菜品",
            initial = editingFood,
            onDismiss = { editingFood = null },
            onConfirm = { food ->
                viewModel.updateFoodOption(food)
                editingFood = null
                Toast.makeText(context, "已更新「${food.name}」", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showImportDialog) {
        ImportFoodDialog(
            onDismiss = { showImportDialog = false },
            onImport = { json ->
                val count = viewModel.importFoodOptionsJson(json)
                showImportDialog = false
                if (count > 0) {
                    Toast.makeText(context, "成功导入 $count 个菜品", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "未导入新菜品（可能已存在或格式错误）", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

// ═══════════════════════════════════════════
//  Food card item
// ═══════════════════════════════════════════

@Composable
private fun FoodCard(
    food: FoodOption,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val colors = LocalEggRiceColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon
            val emoji = when {
                food.category == "一食堂" -> "🍽️"
                food.category == "二食堂" -> "🍴"
                food.category == "三食堂" -> "🥢"
                food.category == "外卖" -> "🥡"
                food.category.contains("面") -> "🍜"
                food.category.contains("快餐") -> "🍔"
                food.category.contains("小吃") -> "🍢"
                else -> "🍚"
            }
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))

            // Food info
            Column(Modifier.weight(1f)) {
                Text(
                    food.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitleParts = mutableListOf<String>()
                if (food.windowName.isNotEmpty()) subtitleParts.add(food.windowName)
                if (food.price.isNotEmpty()) subtitleParts.add("¥${food.price}")
                if (subtitleParts.isNotEmpty()) {
                    Text(
                        subtitleParts.joinToString(" · "),
                        fontSize = 11.sp,
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (food.category.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFB89498).copy(alpha = 0.12f)
                    ) {
                        Text(
                            food.category,
                            fontSize = 10.sp,
                            color = Color(0xFF9A7075),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Edit/Delete buttons (custom items only)
            if (onEdit != null) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Edit, "编辑", tint = colors.textTertiary, modifier = Modifier.size(16.dp))
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Close, "删除", tint = Color(0xFFC89098), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
//  Add / Edit food dialog
// ═══════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodFormDialog(
    title: String,
    initial: FoodOption? = null,
    onDismiss: () -> Unit,
    onConfirm: (FoodOption) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "一食堂") }
    var windowName by remember { mutableStateOf(initial?.windowName ?: "") }
    var price by remember { mutableStateOf(initial?.price ?: "") }
    var catExpanded by remember { mutableStateOf(false) }

    val categories = listOf("一食堂", "二食堂", "三食堂", "外卖")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("菜品名称") },
                    placeholder = { Text("如：红烧牛肉面") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = windowName,
                        onValueChange = { windowName = it },
                        label = { Text("窗口") },
                        placeholder = { Text("如：二楼6号") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) price = it },
                        label = { Text("价格") },
                        placeholder = { Text("如：15") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { category = c; catExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val food = FoodOption(
                        id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name.trim(),
                        category = category,
                        windowName = windowName.trim(),
                        price = price.trim(),
                        isCustom = true
                    )
                    onConfirm(food)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB89498))
            ) { Text(if (initial != null) "保存" else "添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ═══════════════════════════════════════════
//  Import dialog
// ═══════════════════════════════════════════

@Composable
private fun ImportFoodDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    val colors = LocalEggRiceColors.current
    var jsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入菜单", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "粘贴同学分享的菜单 JSON 文本，重复菜品将自动跳过。",
                    fontSize = 12.sp,
                    color = colors.textTertiary
                )
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    label = { Text("菜单 JSON") },
                    placeholder = { Text("粘贴菜单内容...") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(jsonText.trim()) },
                enabled = jsonText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB89498))
            ) { Text("导入") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
