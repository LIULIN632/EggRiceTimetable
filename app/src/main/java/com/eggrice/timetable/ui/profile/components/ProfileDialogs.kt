package com.eggrice.timetable.ui.profile.components

import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.TimetableApplication
import com.eggrice.timetable.data.entity.CourseEntity
import com.eggrice.timetable.ui.theme.*
import com.eggrice.timetable.ui.timetable.components.PetList
import com.eggrice.timetable.ui.timetable.components.petEmoji
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// Data transfer object for share/backup
internal data class CourseBackup(
    val version: Int = 1,
    val exportTime: String = "",
    val school: String = "",
    val courses: List<CourseEntity> = emptyList()
)

// ═══════════════════════════════════════════
//  Menu Dialogs
// ═══════════════════════════════════════════

@Composable
internal fun ImportMenuDialog(
    onDismiss: () -> Unit,
    onJwImport: () -> Unit,
    onShareCodeImport: () -> Unit,
    onFileImport: () -> Unit,
    onBackupRestore: () -> Unit,
    onSchoolRequest: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入课表", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                MenuRow(Icons.Outlined.Language, "教务系统导入", "推荐 · WebView + OkHttp 混合方案", onClick = onJwImport)
                HorizontalDivider(color = colors.borderDivider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                MenuRow(Icons.Outlined.QrCode, "分享口令导入", "通过口令快速导入", onClick = onShareCodeImport)
                MenuRow(Icons.Outlined.Description, "文件导入", "Excel / HTML / CSV / ICS", onClick = onFileImport)
                MenuRow(Icons.Outlined.Restore, "从备份恢复", "导入备份文件", onClick = onBackupRestore)
                HorizontalDivider(color = colors.borderDivider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                MenuRow(Icons.Outlined.AddTask, "申请学校适配", "未找到你的学校？", onClick = onSchoolRequest)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
internal fun JwImportSubDialog(
    onDismiss: () -> Unit,
    onWebImport: () -> Unit,
    onNativeImport: () -> Unit,
    onFreeImport: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = colors.textPrimary,
                    modifier = Modifier.clickable(onClick = onDismiss).size(28.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("教务系统导入", fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column {
                MenuRow(Icons.Outlined.Public, "WebView教务导入（推荐）", "正方/强智/青果/超星 · Cookie持久化", onClick = onWebImport)
                MenuRow(Icons.Outlined.Language, "通用导入（自定义地址）", "输入任意教务网址 · 自动适配", onClick = onFreeImport)
                MenuRow(Icons.Outlined.CloudSync, "原生教务导入（实验性）", "OkHttp直连RSA加密登录 · 部分学校", onClick = onNativeImport)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("返回") } }
    )
}

@Composable
internal fun FileImportSubDialog(
    onDismiss: () -> Unit,
    onExcel: () -> Unit,
    onHtml: () -> Unit,
    onCsv: () -> Unit,
    onIcs: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = colors.textPrimary,
                    modifier = Modifier.clickable(onClick = onDismiss).size(28.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("文件导入", fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column {
                MenuRow(Icons.Outlined.TableChart, "Excel 文件", ".xlsx / .xls", onClick = onExcel)
                MenuRow(Icons.Outlined.Code, "HTML 文件", ".html / .htm", onClick = onHtml)
                MenuRow(Icons.Outlined.GridOn, "CSV 文件", ".csv", onClick = onCsv)
                MenuRow(Icons.Outlined.CalendarMonth, "ICS 文件", ".ics · 即将推出", onClick = onIcs)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("返回") } }
    )
}

@Composable
internal fun ShareMenuDialog(
    onDismiss: () -> Unit,
    onGenerateCode: () -> Unit,
    onExportBackup: () -> Unit,
    onExportCsv: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享与导出", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                MenuRow(Icons.Outlined.Tag, "生成分享口令") { onGenerateCode() }
                MenuRow(Icons.Outlined.Backup, "导出备份文件") { onExportBackup() }
                MenuRow(Icons.Outlined.TableChart, "导出Excel/CSV") { onExportCsv() }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ═══════════════════════════════════════════
//  General Dialogs
// ═══════════════════════════════════════════

@Composable
internal fun EditTextDialog(
    title: String,
    currentValue: String,
    hint: String,
    maxLength: Int = 20,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentValue) }
    val colors = LocalEggRiceColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Badge,
                    contentDescription = null,
                    tint = colors.accentMain,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= maxLength) text = it },
                    placeholder = { Text(hint, color = colors.textTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentMain,
                        cursorColor = colors.accentMain,
                        focusedContainerColor = colors.surfaceBase.copy(alpha = 0.5f),
                        unfocusedContainerColor = colors.surfaceBase.copy(alpha = 0.5f)
                    )
                )
                if (maxLength < Int.MAX_VALUE) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${text.length}/$maxLength",
                        fontSize = 11.sp,
                        color = colors.textTertiary,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentMain),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("确定", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = colors.textSecondary) }
        }
    )
}

@Composable
internal fun <T> OptionsDialog(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value); onDismiss() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == value,
                            onClick = { onSelect(value); onDismiss() },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.accentMain)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, fontSize = 15.sp, color = colors.textPrimary)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
internal fun ShareCodeDialog(gson: Gson, app: TimetableApplication, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val colors = LocalEggRiceColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享口令导入") },
        text = {
            Column {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; error = null },
                    placeholder = { Text("粘贴分享口令", color = colors.textTertiary) },
                    singleLine = false,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentMain,
                        cursorColor = colors.accentMain
                    )
                )
                if (error != null) {
                    Text(error!!, color = colors.danger, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                val json = String(Base64.decode(code.trim(), Base64.DEFAULT), Charsets.UTF_8)
                                val backup = gson.fromJson(json, CourseBackup::class.java)
                                if (backup.courses.isEmpty()) {
                                    withContext(Dispatchers.Main) { error = "口令中没有课程数据" }
                                    return@withContext
                                }
                                app.repository.deleteAll()
                                backup.courses.forEach { app.repository.insert(it) }
                                withContext(Dispatchers.Main) {
                                    onDismiss()
                                    Toast.makeText(context, "成功导入 ${backup.courses.size} 门课程", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { error = "口令无效，请检查后重试" }
                            }
                        }
                    }
                }
            ) {
                Text("导入", color = colors.accentMain)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
internal fun FaqDialog(onDismiss: () -> Unit) {
    val faqs = listOf(
        "如何导入课表？" to "通过「教务系统导入」连接学校教务系统即可自动导入课表。也支持分享口令、Excel文件、HTML文件等多种方式。",
        "支持哪些学校？" to "目前支持使用正方、强智、青果、超星教务系统的高校。如果您的学校不在列表中，可以申请适配。",
        "课表数据存储在哪里？" to "所有数据仅存储在手机本地，不上传任何服务器，完全保护您的隐私。",
        "如何分享课表？" to "在课程页面长按课程即可生成分享口令，好友可通过口令导入您的课表。",
        "如何切换深色模式？" to "在「通用设置」中可以选择浅色、深色或跟随系统三种模式。"
    )
    val colors = LocalEggRiceColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("常见问题") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                faqs.forEach { (q, a) ->
                    Text(q, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(a, fontSize = 13.sp, color = colors.textSecondary, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
internal fun FeedbackDialog(context: android.content.Context, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    val colors = LocalEggRiceColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("意见反馈") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("请输入您的反馈意见", color = colors.textTertiary) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentMain,
                        cursorColor = colors.accentMain
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    placeholder = { Text("联系方式（选填）", color = colors.textTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentMain,
                        cursorColor = colors.accentMain
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:eggrice@outlook.com")
                                putExtra(Intent.EXTRA_SUBJECT, "蛋炒饭课程表 - 意见反馈")
                                putExtra(Intent.EXTRA_TEXT, "${text}\n\n联系方式：${contact.ifBlank { "未填写" }}")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "反馈已记录，感谢您的支持！", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }
                }
            ) {
                Text("提交", color = colors.accentMain)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
internal fun SchoolRequestDialog(context: android.content.Context, onDismiss: () -> Unit) {
    var schoolName by remember { mutableStateOf("") }
    var systemType by remember { mutableStateOf("未知") }
    var expanded by remember { mutableStateOf(false) }
    val systems = listOf("未知", "正方", "强智", "青果", "超星", "其他")
    val colors = LocalEggRiceColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("申请学校适配") },
        text = {
            Column {
                OutlinedTextField(
                    value = schoolName,
                    onValueChange = { schoolName = it },
                    placeholder = { Text("学校全称", color = colors.textTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentMain,
                        cursorColor = colors.accentMain
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedTextField(
                        value = systemType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("教务系统类型") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Filled.ArrowDropDown, "选择")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentMain
                        )
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        systems.forEach { sys ->
                            DropdownMenuItem(
                                text = { Text(sys) },
                                onClick = { systemType = sys; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (schoolName.isNotBlank()) {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:eggrice@outlook.com")
                                putExtra(Intent.EXTRA_SUBJECT, "蛋炒饭课程表 - 申请学校适配")
                                putExtra(Intent.EXTRA_TEXT, "学校：${schoolName}\n教务系统：${systemType}\n请协助适配该校教务系统")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "申请已提交，感谢您的支持！", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }
                }
            ) {
                Text("提交申请", color = colors.accentMain)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
internal fun ShareExportDialog(gson: Gson, app: TimetableApplication, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var shareCode by remember { mutableStateOf("") }
    var generated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val courses = app.repository.allCourses.firstOrNull() ?: emptyList()
            val backup = CourseBackup(
                exportTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                courses = courses
            )
            val json = gson.toJson(backup)
            shareCode = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            generated = true
        }
    }

    val colors = LocalEggRiceColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享口令") },
        text = {
            Column {
                if (!generated) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.accentMain, strokeWidth = 2.dp)
                } else {
                    Text("复制以下口令分享给好友：", fontSize = 13.sp, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = shareCode,
                        onValueChange = {},
                        readOnly = true,
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("share_code", shareCode))
                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentMain),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("复制口令") }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
internal fun PetManageDialog(
    petIndex: Int,
    petName: String,
    onSelectPet: (Int) -> Unit,
    onRenamePet: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    var editedName by remember { mutableStateOf(petName) }
    var selectedIndex by remember { mutableIntStateOf(petIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Pets, null, tint = colors.accentMain, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("我的宠物", fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // Current pet preview
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(colors.surfaceHighlight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(petEmoji(selectedIndex), fontSize = 38.sp)
                }

                Spacer(Modifier.height(16.dp))

                // Pet grid
                Text("选择宠物", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary,
                    modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PetList.forEachIndexed { idx, pet ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (selectedIndex == idx) Modifier.background(colors.accentMain.copy(alpha = 0.15f)) else Modifier.background(colors.surfaceAlt)
                                )
                                .clickable { selectedIndex = idx; editedName = pet.defaultName }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(pet.emoji, fontSize = 28.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(pet.defaultName, fontSize = 10.sp, color = if (selectedIndex == idx) colors.accentMain else colors.textTertiary,
                                fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Name edit
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { if (it.length <= 8) editedName = it },
                    label = { Text("宠物名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentMain,
                        cursorColor = colors.accentMain,
                        focusedContainerColor = colors.surfaceBase.copy(alpha = 0.5f),
                        unfocusedContainerColor = colors.surfaceBase.copy(alpha = 0.5f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelectPet(selectedIndex)
                    onRenamePet(editedName.ifBlank { PetList[selectedIndex].defaultName })
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentMain),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("确定", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = colors.textSecondary) }
        }
    )
}

@Composable
internal fun AboutDialog(
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    val versionName = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知" } catch (_: Exception) { "未知" }
    val colors = LocalEggRiceColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于蛋炒饭课程表") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.surfaceHighlight), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.School, null, tint = colors.accentMain, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("蛋炒饭课程表", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                Text("v$versionName", fontSize = 14.sp, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("纯净无广告 · 开源 · 免费", fontSize = 13.sp, color = colors.textTertiary)
                Text("支持正方/强智/青果/超星教务系统", fontSize = 12.sp, color = colors.textTertiary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("所有数据仅存储在手机本地", fontSize = 12.sp, color = colors.textTertiary)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}
