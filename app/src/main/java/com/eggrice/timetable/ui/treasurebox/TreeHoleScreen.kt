package com.eggrice.timetable.ui.treasurebox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.data.entity.TreeHoleEntity
import com.eggrice.timetable.data.repository.CourseRepository
import com.eggrice.timetable.ui.components.EmptyStatePlaceholder
import com.eggrice.timetable.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeHoleScreen(
    repository: CourseRepository,
    schemeId: Long,
    onBack: () -> Unit
) {
    val isDark = LocalDarkMode.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by repository.getTreeHolesByScheme(schemeId).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("留声树洞", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) DarkSurfaceCard else SurfaceCard
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = accentColor(),
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Edit, "写下心声")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDark) DarkSurfaceAlt else SurfaceAlt)
        ) {
            if (messages.isEmpty()) {
                EmptyStatePlaceholder(
                    icon = Icons.Outlined.Park,
                    message = "这里什么也没有...\n把你的悄悄话放进树洞吧"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        TreeHoleCard(
                            message = msg,
                            isDark = isDark,
                            onDelete = {
                                scope.launch { repository.deleteTreeHole(msg.id) }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTreeHoleDialog(
            isDark = isDark,
            onDismiss = { showAddDialog = false },
            onConfirm = { content ->
                scope.launch {
                    repository.insertTreeHole(TreeHoleEntity(content = content.trim(), schemeId = schemeId))
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TreeHoleCard(
    message: TreeHoleEntity,
    isDark: Boolean,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurfaceCard else SurfaceCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.PersonOutline,
                        null,
                        tint = accentColor().copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "匿名同学",
                        fontSize = 11.sp,
                        color = (if (isDark) DarkTextTertiary else TextTertiary).copy(alpha = 0.6f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dateFormat.format(Date(message.createdAt)),
                        fontSize = 11.sp,
                        color = (if (isDark) DarkTextTertiary else TextTertiary).copy(alpha = 0.4f)
                    )
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            "删除",
                            tint = (if (isDark) DarkTextTertiary else TextTertiary).copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                message.content,
                fontSize = 15.sp,
                color = if (isDark) DarkTextPrimary else TextPrimary,
                lineHeight = 22.sp
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除留言") },
            text = { Text("确定要删除这条留言吗？") },
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
private fun AddTreeHoleDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("写给树洞", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "你的留言将以匿名方式放入树洞",
                    fontSize = 12.sp,
                    color = if (isDark) DarkTextTertiary else TextTertiary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 500) content = it },
                    placeholder = { Text("在这里写下你想说的话...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor(),
                        cursorColor = accentColor()
                    )
                )
                Text(
                    "${content.length}/500",
                    fontSize = 11.sp,
                    color = (if (isDark) DarkTextTertiary else TextTertiary).copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.End
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(content) },
                enabled = content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor())
            ) { Text("放入树洞") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
