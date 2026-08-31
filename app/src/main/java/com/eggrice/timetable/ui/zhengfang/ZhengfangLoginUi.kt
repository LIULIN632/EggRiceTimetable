package com.eggrice.timetable.ui.zhengfang

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.data.School
import com.eggrice.timetable.ui.theme.*

/**
 * 正方教务「只登录」流程的共享 UI 组件（选校列表 / 登录表单 / 验证码弹窗）。
 * 状态契约见 ZhengfangLoginViewModel 中的 [ZhengfangLoginState]。
 */

// ── 阶段 1：选择学校 ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZhengfangSchoolList(
    state: ZhengfangLoginState,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null
) {
    val colors = LocalEggRiceColors.current
    val schools by state.filteredSchools.collectAsState()
    val query by state.searchQuery.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        header?.invoke()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { state.updateSearch(it) },
                placeholder = { Text("搜索学校名称...", color = colors.textTertiary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor(),
                    cursorColor = accentColor()
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "正方教务系统学校",
                fontSize = 12.sp,
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(Modifier.height(8.dp))

            if (schools.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "未找到匹配的学校，可先在「教务导入」中添加自定义学校",
                        color = colors.textTertiary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(schools, key = { it.id }) { school ->
                        ZhengfangSchoolRow(school = school, onClick = { state.selectSchool(school) })
                    }
                }
            }
        }
    }
}

@Composable
fun ZhengfangSchoolRow(school: School, onClick: () -> Unit) {
    val colors = LocalEggRiceColors.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
                Text(
                    school.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${school.city} · ${school.jwType.label}",
                    fontSize = 12.sp,
                    color = colors.textTertiary
                )
            }
            Text(
                "选择",
                fontSize = 13.sp,
                color = accentColor(),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── 阶段 2：登录 ──

/**
 * 登录表单（学校卡片 + 学号/密码 + 进度/错误 + 登录按钮）。
 * [hint] 为登录说明文案，[buttonText] 为按钮文案；
 * [topBackLabel] 非空时在顶部显示返回按钮（如「返回列表」）。
 */
@Composable
fun ZhengfangLoginContent(
    state: ZhengfangLoginState,
    hint: String,
    buttonText: String,
    modifier: Modifier = Modifier,
    topBackLabel: String? = null,
    onTopBack: (() -> Unit)? = null
) {
    val colors = LocalEggRiceColors.current
    val school by state.selectedSchool.collectAsState()
    val username by state.username.collectAsState()
    val password by state.password.collectAsState()
    val isLoading by state.isLoading.collectAsState()
    val progress by state.progress.collectAsState()
    val error by state.error.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (topBackLabel != null && onTopBack != null) {
            TextButton(onClick = onTopBack) {
                Text(topBackLabel, fontSize = 13.sp)
            }
        }

        // 当前学校（可换）
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    school?.name ?: "",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { state.backToSchoolList() }) {
                    Text("换学校", fontSize = 13.sp)
                }
            }
        }

        Text(
            hint,
            fontSize = 12.sp,
            color = colors.textTertiary
        )

        OutlinedTextField(
            value = username,
            onValueChange = { state.updateUsername(it) },
            label = { Text("学号") },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor(),
                cursorColor = accentColor()
            ),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = password,
            onValueChange = { state.updatePassword(it) },
            label = { Text("密码") },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor(),
                cursorColor = accentColor()
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (error != null) {
            Text(
                error!!,
                color = Color(0xFFE57373),
                fontSize = 13.sp
            )
        }

        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = accentColor()
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    progress.ifBlank { "登录中..." },
                    fontSize = 13.sp,
                    color = colors.textTertiary
                )
            }
        }

        Button(
            onClick = { state.startLogin() },
            enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor())
        ) {
            Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ── 验证码弹窗 ──

/** 监听 [state] 的验证码状态，需要时弹出验证码弹窗（三个查询页面共用） */
@Composable
fun ZhengfangCaptchaHost(state: ZhengfangLoginState) {
    val showCaptcha by state.showCaptcha.collectAsState()
    val captchaBase64 by state.captchaBase64.collectAsState()
    if (showCaptcha && captchaBase64 != null) {
        ZhengfangCaptchaDialog(
            captchaBase64 = captchaBase64!!,
            onConfirm = { state.submitCaptcha(it) },
            onRefresh = { state.refreshCaptcha() },
            onCancel = { state.cancelCaptcha() }
        )
    }
}

@Composable
fun ZhengfangCaptchaDialog(
    captchaBase64: String,
    onConfirm: (String) -> Unit,
    onRefresh: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    var code by remember { mutableStateOf("") }
    val bitmap = remember(captchaBase64) { decodeBase64Bitmap(captchaBase64) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("输入验证码", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "验证码",
                        modifier = Modifier
                            .height(72.dp)
                            .background(colors.surfaceCard, RoundedCornerShape(6.dp))
                    )
                } else {
                    Text("验证码加载失败，点击刷新重试", fontSize = 12.sp, color = colors.textTertiary)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        placeholder = { Text("请输入验证码") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor(),
                            cursorColor = accentColor()
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, "刷新验证码", tint = colors.textSecondary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code.trim()) },
                enabled = code.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor())
            ) { Text("确认", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("取消") }
        }
    )
}

// ── 公共小工具 ──

/** 成绩 → 展示色（不及格红 / 85+ 绿 / 其余深绿） */
internal fun scoreColor(score: String): Color {
    val v = score.toIntOrNull() ?: return Color(0xFF9E9E9E)
    return when {
        v < 60 -> Color(0xFFE57373)
        v >= 85 -> Color(0xFF66BB6A)
        else -> Color(0xFF43A047)
    }
}

internal fun decodeBase64Bitmap(b64: String): Bitmap? = try {
    val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
    if (bytes.isEmpty()) null else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (e: Exception) {
    null
}
