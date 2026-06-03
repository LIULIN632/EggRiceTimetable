package com.eggrice.timetable.ui.treasurebox

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.data.entity.GoodItemEntity
import com.eggrice.timetable.data.repository.CourseRepository
import com.eggrice.timetable.ui.components.EmptyStatePlaceholder
import com.eggrice.timetable.ui.theme.*
import kotlinx.coroutines.launch

private data class PresetGoodItem(
    val name: String,
    val category: String,
    val reason: String,
    val description: String,
    val referencePrice: String
)

private val PRESET_GOOD_ITEMS = listOf(
    // ── 数码 ──
    PresetGoodItem("充电宝", "数码", "手机没电时的救命稻草", "大学外出时间长，上课、图书馆、社团活动，手机一天根本撑不住，早买早踏实", "¥80-150"),
    PresetGoodItem("蓝牙耳机", "数码", "图书馆/宿舍刷课必备", "宿舍集体生活需要安静，听网课、听歌、打电话都方便，降噪款体验翻倍", "¥100-500"),
    PresetGoodItem("平板", "数码", "无纸化学习效率翻倍", "记笔记、看PDF、刷网课比手机舒服太多，配支笔直接告别纸质笔记本", "¥2000-5000"),
    PresetGoodItem("显示器", "数码", "笔记本外接大屏生产力拉满", "写论文、敲代码、看网课多窗口并行，宿舍桌面空间换效率，早用早享受", "¥600-1500"),
    PresetGoodItem("机械键盘", "数码", "打字手感质的飞跃", "大学四年敲键盘时间超乎想象，一把好键盘护手腕、提效率，室友也会感谢你选静音轴", "¥200-600"),
    PresetGoodItem("U盘/移动硬盘", "数码", "资料备份和传输的生命线", "作业、论文、课件随身带，小组作业互相传文件必备，坏了哭都来不及建议买两个", "¥30-300"),
    PresetGoodItem("降噪耳塞/耳机", "数码", "宿舍嘈杂时的救命装备", "室友打游戏、打电话、早起动静大，一副好降噪让你随时进入自己的学习世界", "¥50-800"),
    PresetGoodItem("拓展坞", "数码", "笔记本接口不够用的救星", "轻薄本通常只有1-2个Type-C口，接U盘、显示器、网线全靠它，开会投屏也方便", "¥80-300"),

    // ── 生活 ──
    PresetGoodItem("床帘", "生活", "宿舍里唯一属于你的私密空间", "集体生活最需要的就是隐私感，换衣服、睡觉遮光、深夜开灯不打扰室友", "¥50-150"),
    PresetGoodItem("坐垫", "生活", "拯救你的老腰", "宿舍椅子普遍硬且不舒服，四年坐下来的损伤不可逆，好坐垫是健康投资", "¥30-100"),
    PresetGoodItem("台灯", "生活", "熄灯后的光明自由", "宿舍统一熄灯后想学习或刷手机就靠它，选充电款停电也能用，护眼款更不易疲劳", "¥50-200"),
    PresetGoodItem("洗衣机", "生活", "告别排队洗衣烦恼", "公共洗衣机卫生堪忧且要抢，宿舍合资买个小洗衣机，四年洗衣自由", "¥300-800"),
    PresetGoodItem("小冰箱", "生活", "夏天冷饮自由不是梦", "夏天冰西瓜冰饮料、储存水果酸奶，尤其南方的同学，早买早享受", "¥300-600"),
    PresetGoodItem("保温杯", "生活", "随时喝热水养生又省钱", "教室饮水机排队费时间，自带保温杯随时喝，冬天暖手夏天保冷，告别买矿泉水", "¥50-150"),
    PresetGoodItem("收纳箱/收纳架", "生活", "宿舍空间翻倍的秘密", "宿舍桌子柜子空间有限，合理收纳让物品一目了然，找东西不再翻箱倒柜", "¥20-80"),
    PresetGoodItem("小风扇/挂脖风扇", "生活", "夏天教室没空调的续命神器", "很多大学教室和宿舍没空调或制冷差，一个小风扇让你上课睡觉都舒服", "¥20-100"),
    PresetGoodItem("电蚊香/蚊帐", "生活", "南方同学夏天的护身符", "蚊子多到怀疑人生的时候才知道这有多重要，蚊帐防蚊电蚊香灭蚊双管齐下", "¥15-60"),

    // ── 学习 ──
    PresetGoodItem("打印机", "学习", "打印资料不用跑打印店", "期末复习资料、申请材料、作业报告随时打印，尤其考研党打印量巨大", "¥300-800"),
    PresetGoodItem("护眼灯", "学习", "长时间学习不伤眼", "普通台灯频闪伤眼，好的护眼灯无频闪、可调色温，四年用眼健康值得投资", "¥150-500"),
    PresetGoodItem("书架", "学习", "桌面整洁学习更有动力", "教材教辅越来越多，桌上堆不下时才知道书架有多香，分层收纳一目了然", "¥30-80"),
    PresetGoodItem("错题本", "学习", "学霸标配，复习效率翻倍", "整理错题是最有效的学习方法之一，手写印象更深，纸质版不受设备限制", "¥10-30"),
    PresetGoodItem("笔记本支架", "学习", "颈椎救星，低头党的福音", "长时间低头看笔记本脖子酸痛是通病，支架抬高屏幕平视，坐姿改善立竿见影", "¥30-150"),
    PresetGoodItem("荧光笔/记号笔", "学习", "期末划重点的神器", "教材和笔记用不同颜色标注重点，复习时一目了然，效率提升不止一倍", "¥10-30"),
    PresetGoodItem("计划本/手账本", "学习", "告别拖延症的第一步", "把DDL、考试、社团活动写下来，重要事情不再忘记，自律从记录开始", "¥10-40")
)

private val CATEGORIES = listOf("全部", "数码", "生活", "学习", "自定义")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodItemListScreen(
    repository: CourseRepository,
    schemeId: Long,
    onBack: () -> Unit
) {
    val isDark = LocalDarkMode.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val items by repository.getGoodItemsByScheme(schemeId).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("全部") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    // Initialize preset items on first load
    LaunchedEffect(Unit) {
        if (items.isEmpty()) {
            PRESET_GOOD_ITEMS.forEachIndexed { idx, preset ->
                repository.insertGoodItem(
                    GoodItemEntity(
                        name = preset.name,
                        category = preset.category,
                        reason = preset.reason,
                        description = preset.description,
                        referencePrice = preset.referencePrice,
                        sortOrder = idx,
                        schemeId = schemeId
                    )
                )
            }
        }
    }

    val filteredItems = remember(items, selectedCategory, searchQuery) {
        items.filter { item ->
            val matchCategory = selectedCategory == "全部" || item.category == selectedCategory
            val matchSearch = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.reason.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
    }

    val purchasedCount = items.count { it.purchased }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索好物...", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor(),
                                cursorColor = accentColor(),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                    } else {
                        Text("大学好物清单", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) searchQuery = ""
                    }) {
                        Icon(
                            if (showSearch) Icons.Filled.Close else Icons.Outlined.Search,
                            "搜索",
                            tint = if (showSearch) accentColor() else if (isDark) DarkTextSecondary else TextSecondary
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "添加好物", tint = accentColor())
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) DarkSurfaceCard else SurfaceCard
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
            // ── Progress summary ──
            if (items.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor().copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (purchasedCount >= items.size)
                                Icon(Icons.Filled.CheckCircle, null, tint = accentColor(), modifier = Modifier.size(20.dp))
                            else
                                Text(
                                    "$purchasedCount",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor()
                                )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "已入手 $purchasedCount / ${items.size} 件好物",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) DarkTextPrimary else TextPrimary
                            )
                            Text(
                                if (purchasedCount == 0) "早买早享受，从第一个开始！"
                                else if (purchasedCount >= items.size) "全部入手，大学生活圆满！"
                                else "还有 ${items.size - purchasedCount} 件好物等你解锁",
                                fontSize = 11.sp,
                                color = if (isDark) DarkTextTertiary else TextTertiary
                            )
                        }
                    }
                }
            }

            // ── Category filter chips ──
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(CATEGORIES) { cat ->
                    val count = when (cat) {
                        "全部" -> items.size
                        else -> items.count { it.category == cat }
                    }
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                "$cat ($count)",
                                fontSize = 11.sp,
                                fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor(),
                            selectedLabelColor = Color.White,
                            containerColor = if (isDark) DarkSurfaceCard else SurfaceCard,
                            labelColor = if (isDark) DarkTextSecondary else TextSecondary
                        )
                    )
                }
            }

            // ── Item list ──
            if (filteredItems.isEmpty()) {
                EmptyStatePlaceholder(
                    icon = if (searchQuery.isNotBlank()) Icons.Outlined.SearchOff else Icons.Outlined.Star,
                    message = if (searchQuery.isNotBlank()) "未找到「$searchQuery」相关好物" else "暂无好物"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Unpurchased first, then purchased
                    val sorted = filteredItems.sortedWith(compareBy({ it.purchased }, { it.sortOrder }))
                    items(sorted, key = { it.id }) { item ->
                        GoodItemCard(
                            item = item,
                            isDark = isDark,
                            onToggle = {
                                scope.launch {
                                    repository.updateGoodItem(item.copy(purchased = !item.purchased))
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    repository.deleteGoodItem(item.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddGoodItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, category, reason, description, price ->
                scope.launch {
                    val sortOrder = (items.maxOfOrNull { it.sortOrder } ?: -1) + 1
                    repository.insertGoodItem(
                        GoodItemEntity(
                            name = name.trim(),
                            category = category,
                            reason = reason.trim(),
                            description = description.trim(),
                            referencePrice = price.trim(),
                            sortOrder = sortOrder,
                            schemeId = schemeId
                        )
                    )
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun GoodItemCard(
    item: GoodItemEntity,
    isDark: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.purchased)
                (if (isDark) DarkSurfaceCard else SurfaceCard).copy(alpha = 0.5f)
            else
                if (isDark) DarkSurfaceCard else SurfaceCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.purchased) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.purchased,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = accentColor(),
                        uncheckedColor = if (isDark) DarkTextTertiary else TextTertiary
                    )
                )
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.purchased)
                                (if (isDark) DarkTextTertiary else TextTertiary).copy(alpha = 0.6f)
                            else
                                if (isDark) DarkTextPrimary else TextPrimary,
                            textDecoration = if (item.purchased) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.referencePrice.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (item.purchased)
                                    (accentSoftColor()).copy(alpha = 0.3f)
                                else
                                    accentSoftColor().copy(alpha = 0.5f)
                            ) {
                                Text(
                                    item.referencePrice,
                                    fontSize = 11.sp,
                                    color = accentColor(),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (item.reason.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            item.reason,
                            fontSize = 12.sp,
                            color = accentColor().copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Outlined.Close, "删除",
                        tint = (if (isDark) DarkTextTertiary else TextTertiary).copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Expanded detail section
            if (expanded && item.description.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) DarkSurfaceAlt else SurfaceAlt
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("💡", fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            item.description,
                            fontSize = 12.sp,
                            color = if (isDark) DarkTextSecondary else TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除好物") },
            text = { Text("确定要删除「${item.name}」吗？") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoodItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, reason: String, description: String, price: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("自定义") }
    var reason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var catExpanded by remember { mutableStateOf(false) }
    val categories = listOf("数码", "生活", "学习", "自定义")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加好物", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
                    label = { Text("物品名称 *") },
                    placeholder = { Text("如：电动牙刷") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor(), cursorColor = accentColor())
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
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor())
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { category = c; catExpanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 30) reason = it },
                    label = { Text("推荐理由") },
                    placeholder = { Text("如：宿舍生活幸福感来源") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor(), cursorColor = accentColor())
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 60) description = it },
                    label = { Text("为什么早买早享受") },
                    placeholder = { Text("如：四年下来能省很多时间...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor(), cursorColor = accentColor())
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { if (it.length <= 15) price = it },
                    label = { Text("参考价格") },
                    placeholder = { Text("如：¥100-300") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor(), cursorColor = accentColor())
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, category, reason, description, price) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor())
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
