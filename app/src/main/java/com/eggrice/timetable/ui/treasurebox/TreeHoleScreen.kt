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
    val colors = LocalEggRiceColors.current
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
                    containerColor = colors.surfaceCard
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colors.accentMain,
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
                .background(colors.surfaceAlt)
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
            onDismiss = { showAddDialog = false },
            onConfirm = { content, author ->
                scope.launch {
                    repository.insertTreeHole(TreeHoleEntity(content = content.trim(), author = author.trim(), schemeId = schemeId))
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TreeHoleCard(
    message: TreeHoleEntity,
    onDelete: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceCard
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
                        tint = colors.accentMain.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        message.author.ifBlank { "匿名同学" },
                        fontSize = 11.sp,
                        color = colors.textTertiary.copy(alpha = 0.6f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dateFormat.format(Date(message.createdAt)),
                        fontSize = 11.sp,
                        color = colors.textTertiary.copy(alpha = 0.4f)
                    )
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            "删除",
                            tint = colors.textTertiary.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                message.content,
                fontSize = 15.sp,
                color = colors.textPrimary,
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC89098))
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTreeHoleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val colors = LocalEggRiceColors.current
    var content by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("写给树洞", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = author,
                    onValueChange = { if (it.length <= 8) author = it },
                    placeholder = { Text("你的昵称（留空则为匿名）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentMain,
                        cursorColor = colors.accentMain
                    )
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 500) content = it },
                    placeholder = { Text("在这里写下你想说的话...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentMain,
                        cursorColor = colors.accentMain
                    )
                )
                Text(
                    "${content.length}/500",
                    fontSize = 11.sp,
                    color = colors.textTertiary.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.End
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(content, author) },
                enabled = content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentMain)
            ) { Text("放入树洞") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
