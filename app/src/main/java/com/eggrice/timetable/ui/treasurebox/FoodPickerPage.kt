package com.eggrice.timetable.ui.treasurebox

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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eggrice.timetable.ui.theme.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPickerPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: TreasureBoxViewModel = viewModel(factory = TreasureBoxViewModel.Factory(app))
    val pickedFood by viewModel.pickedFood.collectAsState()
    val isRolling by viewModel.isRolling.collectAsState()
    val foodOptions by viewModel.foodOptions.collectAsState()
    val isDark = LocalDarkMode.current

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("全部") }

    val categories = remember(foodOptions) {
        listOf("全部") + foodOptions.map { it.category }.distinct().filter { it.isNotEmpty() }
    }

    val filteredFood = remember(foodOptions, selectedCategory) {
        if (selectedCategory == "全部") foodOptions
        else foodOptions.filter { it.category == selectedCategory }
    }

    // Scale animation for picked food
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
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "添加美食", tint = accentColor())
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
            // ── Picker area ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (pickedFood == null) "今天吃什么呢？" else "就决定是你了！",
                        fontSize = 13.sp,
                        color = if (isDark) DarkTextTertiary else TextTertiary
                    )

                    Spacer(Modifier.height(16.dp))

                    // Food display circle
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(scaleAnim)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFFFF9A9E), Color(0xFFFAD0C4))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (pickedFood != null) {
                                Icon(
                                    Icons.Outlined.Restaurant, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    pickedFood!!.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                                if (pickedFood!!.category.isNotEmpty()) {
                                    Text(
                                        pickedFood!!.category,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Outlined.HelpOutline, null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("???", fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Roll button
                    Button(
                        onClick = { viewModel.pickRandomFood() },
                        enabled = !isRolling,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5576C))
                    ) {
                        if (isRolling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("抽选中...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (pickedFood == null) "开始抽取" else "换一个", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Food list ──
            Text(
                "美食列表 (${filteredFood.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) DarkTextSecondary else TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Category filter
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
                            selectedContainerColor = Color(0xFFF5576C),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredFood) { food ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Emoji based on category
                            val emoji = when {
                                food.category.contains("面") -> "🍜"
                                food.category.contains("快餐") -> "🍔"
                                food.category.contains("小吃") -> "🍢"
                                food.category.contains("外卖") -> "🥡"
                                else -> "🍚"
                            }
                            Text(emoji, fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(food.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (isDark) DarkTextPrimary else TextPrimary)
                                if (food.category.isNotEmpty()) {
                                    Text(food.category, fontSize = 11.sp, color = TextTertiary)
                                }
                            }
                            if (food.isCustom) {
                                IconButton(
                                    onClick = { viewModel.deleteFoodOption(food.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Outlined.Close, "删除", tint = Color(0xFFE57373), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFoodDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { food ->
                viewModel.addFoodOption(food)
                showAddDialog = false
                Toast.makeText(context, "已添加「${food.name}」", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodDialog(
    onDismiss: () -> Unit,
    onConfirm: (FoodOption) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("自定义") }
    var catExpanded by remember { mutableStateOf(false) }
    val categories = listOf("食堂", "外卖", "面食", "快餐", "小吃", "自定义")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加美食", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("美食名称") },
                    placeholder = { Text("如：红烧牛肉面") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                onClick = { onConfirm(FoodOption(name = name.trim(), category = category)) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor())
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
