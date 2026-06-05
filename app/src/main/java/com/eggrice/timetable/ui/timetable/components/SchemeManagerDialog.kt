package com.eggrice.timetable.ui.timetable.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.data.entity.SchemeEntity
import com.eggrice.timetable.ui.theme.*

@Composable
fun SchemeManagerDialog(
    schemes: List<SchemeEntity>,
    activeSchemeId: Long,
    onSwitchScheme: (SchemeEntity) -> Unit,
    onCreateScheme: (String) -> Unit,
    onRenameScheme: (SchemeEntity, String) -> Unit,
    onDeleteScheme: (SchemeEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<SchemeEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<SchemeEntity?>(null) }
    val isDark = LocalDarkMode.current
    val colors = LocalEggRiceColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("课表管理", fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, "新建方案", tint = colors.accentMain)
                }
            }
        },
        text = {
            Column {
                if (schemes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.FolderOpen, null, tint = colors.textTertiary.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("暂无方案", color = colors.textTertiary, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp)
                    ) {
                        items(schemes) { scheme ->
                            val isActive = scheme.id == activeSchemeId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isActive) colors.accentMain.copy(alpha = if (isDark) 0.18f else 0.10f)
                                        else colors.surfaceCard
                                    )
                                    .clickable { onSwitchScheme(scheme) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isActive) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                                    null,
                                    tint = if (isActive) colors.accentMain else colors.textTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        scheme.name,
                                        fontSize = 14.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = colors.textPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { showRenameDialog = scheme },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Edit, "重命名",
                                        tint = colors.textTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (scheme.id != 0L) {
                                    IconButton(
                                        onClick = { showDeleteConfirm = scheme },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Delete, "删除",
                                            tint = colors.danger,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("完成", fontWeight = FontWeight.Bold, color = colors.accentMain) }
        }
    )

    // Create dialog
    if (showCreateDialog) {
        SchemeEditDialog(
            title = "新建方案",
            initialName = "",
            onConfirm = { name ->
                onCreateScheme(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    // Rename dialog
    showRenameDialog?.let { scheme ->
        SchemeEditDialog(
            title = "重命名方案",
            initialName = scheme.name,
            onConfirm = { name ->
                onRenameScheme(scheme, name)
                showRenameDialog = null
            },
            onDismiss = { showRenameDialog = null }
        )
    }

    // Delete confirmation
    showDeleteConfirm?.let { scheme ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除方案", fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除「${scheme.name}」吗？该方案下的所有课程数据将被永久删除，此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteScheme(scheme)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SchemeEditDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val colors = LocalEggRiceColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("方案名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accentMain,
                    cursorColor = colors.accentMain
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentMain)
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
