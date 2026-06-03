package com.eggrice.timetable.ui.import_

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eggrice.timetable.TimetableApplication
import com.eggrice.timetable.data.JwSystemType
import com.eggrice.timetable.data.isJwSystemAvailable
import com.eggrice.timetable.network.ZhengfangSchool
import com.eggrice.timetable.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as TimetableApplication
    val viewModel: ImportViewModel = viewModel(
        factory = ImportViewModel.Factory(app.repository, app.appContainer.zhengfangClient, app.appContainer.schoolRegistry)
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

    val filteredSchools by viewModel.filteredSchools.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入课表", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancelCaptcha()
                        viewModel.reset()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceCard)
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
                    Text("选择教务系统", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                item {
                    JwSystemGrid(
                        onSelect = { type ->
                            if (isJwSystemAvailable(type)) {
                                viewModel.selectSystem(type)
                            } else {
                                Toast.makeText(context, "该教务系统即将支持，敬请期待", Toast.LENGTH_SHORT).show()
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
                        color = TextPrimary
                    )
                }
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearch(it) },
                        placeholder = { Text("搜索学校名称或城市") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TextTertiary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor(),
                            unfocusedBorderColor = CardBorder
                        )
                    )
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
                                color = TextTertiary
                            )
                        }
                    }
                } else {
                    items(filteredSchools.size) { idx ->
                        val school = filteredSchools[idx]
                        val isSelected = selectedSchool == school
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) accentSoftColor().copy(alpha = 0.5f) else Color.Transparent)
                                .clickable { viewModel.selectSchool(school) }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.School,
                                null,
                                tint = if (isSelected) accentColor() else TextTertiary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(school.name, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("${school.city} · ${school.baseUrl}", fontSize = 12.sp, color = TextTertiary)
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    "已选",
                                    tint = accentColor(),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = Divider, thickness = 0.5.dp)
                    }
                }
                return@LazyColumn
            }

            // Step 3: Login
            item {
                Text("登录教务系统", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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
                            Text(selectedSchool!!.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text(selectedSchool!!.baseUrl, fontSize = 11.sp, color = TextTertiary)
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
                    shape = RoundedCornerShape(8.dp)
                )
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
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (result!!.success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
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
                                Text(result!!.error!!, fontSize = 13.sp, color = TextSecondary)
                            }
                            if (result!!.success) {
                                Text(
                                    "已导入 ${result!!.courses?.size ?: 0} 门课程",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
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

    // Captcha dialog
    if (showCaptcha && captchaBase64 != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelCaptcha() },
            title = { Text("安全验证", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("请输入图片中的验证码", fontSize = 13.sp, color = TextTertiary)
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
                            modifier = Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("验证码加载失败", fontSize = 12.sp, color = TextTertiary)
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
    val types = JwSystemType.values().toList()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in types.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (type in row) {
                    val available = isJwSystemAvailable(type)
                    val colors = systemCardColors(type)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clickable { onSelect(type) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (available) colors.bg else colors.bg.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, if (available) colors.border else CardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(type.label, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                color = if (available) TextPrimary else TextTertiary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (available) type.description else "即将支持",
                                fontSize = 11.sp,
                                color = if (available) TextSecondary else TextTertiary
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

private data class SystemCardColors(val bg: Color, val border: Color)

private fun systemCardColors(type: JwSystemType): SystemCardColors = when (type) {
    JwSystemType.ZHENGFANG -> SystemCardColors(Color(0xFFEEF0F4), Color(0xFFCDD2DC))
    JwSystemType.QIANGZHI -> SystemCardColors(Color(0xFFF5F0EB), Color(0xFFE0D5C8))
    JwSystemType.QINGGUO -> SystemCardColors(Color(0xFFEBF3EE), Color(0xFFC8DCD0))
    JwSystemType.CHAOXING -> SystemCardColors(Color(0xFFF0EBF3), Color(0xFFD0C8E0))
    JwSystemType.URP -> SystemCardColors(Color(0xFFEBEFF5), Color(0xFFC8D0E0))
}
