package com.eggrice.timetable.ui.import_

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eggrice.timetable.TimetableApplication
import com.eggrice.timetable.data.JwSystemType
import com.eggrice.timetable.data.School
import com.eggrice.timetable.data.isJwSystemAvailable
import com.eggrice.timetable.network.ZhengfangSchool
import com.eggrice.timetable.ui.components.SchoolFavoriteButton
import com.eggrice.timetable.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onGoSettings: (() -> Unit)? = null
) {
    val colors = LocalEggRiceColors.current
    val isDark = LocalDarkMode.current
    val context = LocalContext.current
    val app = context.applicationContext as TimetableApplication
    val viewModel: ImportViewModel = viewModel(
        factory = ImportViewModel.Factory(
            app.repository,
            app.appContainer.zhengfangClient,
            app.appContainer.qiangzhiClient,
            app.appContainer.schoolRegistry,
            app.appContainer
        )
    )
    val selectedSystem by viewModel.selectedSystem.collectAsState()
    val selectedSchool by viewModel.selectedSchool.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val result by viewModel.result.collectAsState()
    val captchaBase64 by viewModel.captchaBase64.collectAsState()
    val showCaptcha by viewModel.showCaptcha.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var captchaCode by remember { mutableStateOf("") }
    var rememberPassword by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showAddCustomSchool by remember { mutableStateOf(false) }
    var customSchoolName by remember { mutableStateOf("") }
    var customSchoolUrl by remember { mutableStateOf("") }
    var pendingCustomSchool by remember { mutableStateOf<School?>(null) }
    var customJwType by remember { mutableStateOf<JwSystemType?>(null) }

    val filteredSchools by viewModel.filteredSchools.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    // 逐级返回：成功卡片 → 退出；失败卡片 → 回登录表单；表单/选学校 → 选学校/选系统；初始页 → 退出
    fun handleBack() {
        val res = result
        if (isLoading) viewModel.cancelCaptcha()
        when {
            res?.success == true -> {
                // 导入成功：直接退出导入页
                viewModel.reset()
                onBack()
            }
            res != null -> {
                // 导入失败：回到登录表单，保留已选学校
                viewModel.reset()
            }
            selectedSystem != null || selectedSchool != null -> {
                viewModel.goBack()
            }
            else -> {
                viewModel.reset()
                onBack()
            }
        }
    }
    BackHandler { handleBack() }

    // 点击学校：内置未收录地址的学校（baseUrl 空）→ 弹窗完善信息
    fun onSchoolClick(school: School) {
        if (school.baseUrl.isBlank()) {
            pendingCustomSchool = school
            customSchoolName = school.name
            customSchoolUrl = ""
            customJwType = selectedSystem
            showAddCustomSchool = true
        } else {
            viewModel.selectSchool(school)
        }
    }

    // Auto-load saved credentials when school changes
    LaunchedEffect(selectedSchool) {
        if (selectedSchool != null) {
            val baseUrl = selectedSchool!!.baseUrl.trimEnd('/')
            val saved = viewModel.loadSavedCredentials(baseUrl)
            if (saved != null) {
                username = saved.first
                password = saved.second
                rememberPassword = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入课表", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surfaceCard)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Step 1: Select教务 system
            if (selectedSystem == null) {
                item {
                    Text("选择教务系统", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
                item {
                    JwSystemGrid(
                        onSelect = { type ->
                            if (isJwSystemAvailable(type)) {
                                viewModel.selectSystem(type)
                            } else {
                                Toast.makeText(context, "该教务系统暂不支持原生导入，请使用「Web导入」方式", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
                return@LazyColumn
            }

            // Step 2: Select school
            if (selectedSchool == null) {
                val system = selectedSystem ?: return@LazyColumn
                item {
                    Text(
                        "选择学校 · ${system.label}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearch(it) },
                        placeholder = { Text("搜索学校名称或城市") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = colors.textTertiary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor(),
                            unfocusedBorderColor = colors.borderDivider
                        )
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { showAddCustomSchool = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, accentColor())
                    ) {
                        Icon(Icons.Outlined.Add, null, tint = accentColor(), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("添加自定义学校", color = accentColor(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                if (filteredSchools.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (searchQuery.isEmpty()) "暂无可用学校" else "未找到学校，请尝试其他关键词",
                                fontSize = 13.sp,
                                color = colors.textTertiary
                            )
                        }
                    }
                } else {
                    val favSchools = filteredSchools.filter { it.id in favoriteIds }
                    val otherSchools = filteredSchools.filter { it.id !in favoriteIds }

                    // ── Favorites section ──
                    if (favSchools.isNotEmpty()) {
                        item {
                            Text(
                                "⭐ 我的收藏",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }
                        items(favSchools.size) { idx ->
                            val school = favSchools[idx]
                            val isSelected = selectedSchool == school
                            SchoolRow(
                                school = school,
                                isSelected = isSelected,
                                isFavorite = true,
                                onSelect = { onSchoolClick(school) },
                                onToggleFavorite = { viewModel.toggleFavorite(school) }
                            )
                            HorizontalDivider(color = colors.borderDivider, thickness = 0.5.dp)
                        }
                        item {
                            HorizontalDivider(
                                color = colors.textTertiary.copy(alpha = 0.3f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        item {
                            Text(
                                "全部学校",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }
                    }

                    // ── All schools section ──
                    items(otherSchools.size) { idx ->
                        val school = otherSchools[idx]
                        val isSelected = selectedSchool == school
                        SchoolRow(
                            school = school,
                            isSelected = isSelected,
                            isFavorite = school.id in favoriteIds,
                            onSelect = { onSchoolClick(school) },
                            onToggleFavorite = { viewModel.toggleFavorite(school) }
                        )
                        HorizontalDivider(color = colors.borderDivider, thickness = 0.5.dp)
                    }
                }
                return@LazyColumn
            }

            // Step 3: Login
            item {
                Text("登录教务系统", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = accentSoftColor()),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.School, null, tint = accentColor(), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(selectedSchool!!.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                            Text(selectedSchool!!.baseUrl, fontSize = 11.sp, color = colors.textTertiary)
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("学号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(8.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(8.dp),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showPassword) "隐藏密码" else "显示密码",
                                tint = colors.textTertiary
                            )
                        }
                    }
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { rememberPassword = !rememberPassword }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberPassword,
                        onCheckedChange = { rememberPassword = it },
                        colors = CheckboxDefaults.colors(checkedColor = accentColor())
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("记住密码", fontSize = 14.sp, color = colors.textSecondary)
                }
            }
            if (isLoading && progress.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = accentSoftColor()),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = accentColor(),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(progress, fontSize = 13.sp, color = accentColor())
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        if (username.isNotBlank() && password.isNotBlank()) {
                            viewModel.startLogin(username, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor()),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (isLoading) "导入中..." else "登录并导入",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
            if (result != null) {
                // 登录完成后处理密码记忆
                item {
                    LaunchedEffect(result) {
                        val school = viewModel.selectedSchool.value
                        if (school != null) {
                            val baseUrl = school.baseUrl.trimEnd('/')
                            if (result?.success == true && rememberPassword) {
                                viewModel.saveCredentials(baseUrl, username, password)
                            } else if (result?.success == true && !rememberPassword) {
                                viewModel.deleteCredentials(baseUrl)
                            }
                        }
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (result!!.success) (if (isDark) Color(0xFF1B3B23) else Color(0xFFE8F5E9)) else (if (isDark) Color(0xFF3B2323) else Color(0xFFFFEBEE))
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                if (result!!.success) "导入成功！" else "导入失败",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            if (result!!.error != null) {
                                Text(result!!.error!!, fontSize = 13.sp, color = colors.textSecondary)
                            }
                            if (result!!.success) {
                                Text(
                                    "已导入 ${result!!.courses?.size ?: 0} 门课程",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary
                                )
                                if (onGoSettings != null) {
                                    Text(
                                        "上课提醒与桌面小组件默认关闭，可在通用设置中开启",
                                        fontSize = 12.sp,
                                        color = colors.textTertiary,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                    Row {
                                        TextButton(
                                            onClick = { viewModel.reset(); onGoSettings() },
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) { Text("开启提醒/小组件", color = accentColor()) }
                                        TextButton(
                                            onClick = { viewModel.reset(); onBack() },
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) { Text("返回课表", color = accentColor()) }
                                    }
                                } else {
                                    TextButton(
                                        onClick = { viewModel.reset(); onBack() },
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) { Text("返回课表", color = accentColor()) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add-custom-school dialog（手动添加 / 完善内置未收录地址的学校）
    if (showAddCustomSchool) {
        val system = selectedSystem
        AlertDialog(
            onDismissRequest = {
                showAddCustomSchool = false
                pendingCustomSchool = null
            },
            title = {
                Text(
                    if (pendingCustomSchool != null) "完善学校信息" else "添加自定义学校",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column {
                    Text(
                        if (pendingCustomSchool != null)
                            "该学校暂未收录教务地址，填写后即可导入。"
                        else
                            "学校未收录？手动填写教务系统地址即可添加。",
                        fontSize = 12.sp,
                        color = colors.textTertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customSchoolName,
                        onValueChange = { customSchoolName = it },
                        label = { Text("学校名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customSchoolUrl,
                        onValueChange = { customSchoolUrl = it },
                        label = { Text("教务系统地址") },
                        placeholder = { Text("例如 jw.example.edu.cn") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("教务系统类型", fontSize = 12.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        JwSystemType.values().forEach { t ->
                            val selected = (customJwType ?: system) == t
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) accentColor() else colors.surfaceAlt,
                                modifier = Modifier.clickable { customJwType = t }
                            ) {
                                Text(
                                    t.label,
                                    fontSize = 11.sp,
                                    color = if (selected) Color.White else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val type = customJwType ?: system ?: JwSystemType.ZHENGFANG
                        val newSchool = viewModel.addCustomSchool(customSchoolName, customSchoolUrl, type)
                        if (newSchool != null) {
                            viewModel.selectSchool(newSchool)
                            Toast.makeText(context, "已添加自定义学校", Toast.LENGTH_SHORT).show()
                        }
                        customSchoolName = ""
                        customSchoolUrl = ""
                        customJwType = null
                        pendingCustomSchool = null
                        showAddCustomSchool = false
                    },
                    enabled = customSchoolName.isNotBlank() && customSchoolUrl.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor())
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCustomSchool = false
                    pendingCustomSchool = null
                }) { Text("取消") }
            }
        )
    }

    // Captcha dialog
    if (showCaptcha && captchaBase64 != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelCaptcha() },
            title = { Text("安全验证", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("请输入图片中的验证码", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(modifier = Modifier.height(12.dp))
                    val captchaBitmap = remember(captchaBase64) {
                        try {
                            val bytes = Base64.decode(captchaBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (e: Exception) { null }
                    }
                    if (captchaBitmap != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                bitmap = captchaBitmap.asImageBitmap(),
                                contentDescription = "验证码",
                                modifier = Modifier.weight(1f).height(70.dp).clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Fit
                            )
                            IconButton(
                                onClick = { viewModel.refreshCaptcha() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Filled.SyncAlt,
                                    "刷新验证码",
                                    tint = accentColor(),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(6.dp)).background(colors.surfaceAlt),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("验证码加载失败", fontSize = 12.sp, color = colors.textTertiary)
                                TextButton(onClick = { viewModel.refreshCaptcha() }) {
                                    Text("点击重试", fontSize = 12.sp, color = accentColor())
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = captchaCode,
                        onValueChange = { captchaCode = it },
                        label = { Text("验证码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitCaptcha(captchaCode); captchaCode = "" },
                    enabled = captchaCode.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor())
                ) { Text("确认登录") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelCaptcha() }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun JwSystemGrid(onSelect: (JwSystemType) -> Unit) {
    val colors = LocalEggRiceColors.current
    val types = JwSystemType.values().toList()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in types.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (type in row) {
                    val available = isJwSystemAvailable(type)
                    val sysColors = systemCardColors(type)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clickable { onSelect(type) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (available) sysColors.bg else sysColors.bg.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, if (available) sysColors.border else colors.borderDivider)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(type.label, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                color = if (available) colors.textPrimary else colors.textTertiary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (available) type.description else "请用WebView方式导入",
                                fontSize = 11.sp,
                                color = if (available) colors.textSecondary else colors.textTertiary
                            )
                        }
                    }
                }
                // Pad uneven row
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SchoolRow(
    school: School,
    isSelected: Boolean,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) accentSoftColor().copy(alpha = 0.5f) else Color.Transparent)
            .clickable { onSelect() }
            .padding(start = 12.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.School,
            null,
            tint = if (isSelected) accentColor() else colors.textTertiary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(school.name, fontSize = 15.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
            Text(
                if (school.baseUrl.isBlank()) "未收录教务地址 · 点击完善" else "${school.city} · ${school.baseUrl}",
                fontSize = 12.sp,
                color = colors.textTertiary
            )
        }
        SchoolFavoriteButton(
            isFavorite = isFavorite,
            onToggle = onToggleFavorite
        )
        if (isSelected) {
            Icon(
                Icons.Outlined.CheckCircle,
                "已选",
                tint = accentColor(),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private data class SystemCardColors(val bg: Color, val border: Color)

private fun systemCardColors(type: JwSystemType): SystemCardColors = when (type) {
    JwSystemType.ZHENGFANG -> SystemCardColors(Color(0xFFEEF0F4), Color(0xFFCDD2DC))
    JwSystemType.QIANGZHI -> SystemCardColors(Color(0xFFF5F0EB), Color(0xFFE0D5C8))
    JwSystemType.QINGGUO -> SystemCardColors(Color(0xFFEBF3EE), Color(0xFFC8DCD0))
    JwSystemType.CHAOXING -> SystemCardColors(Color(0xFFF0EBF3), Color(0xFFD0C8E0))
    JwSystemType.URP -> SystemCardColors(Color(0xFFEBEFF5), Color(0xFFC8D0E0))
}
