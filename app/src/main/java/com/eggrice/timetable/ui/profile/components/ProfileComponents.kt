package com.eggrice.timetable.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.ui.theme.*
import com.eggrice.timetable.ui.timetable.components.PetList
import com.eggrice.timetable.ui.timetable.components.petEmoji

@Composable
internal fun UserProfileArea(
    nickname: String,
    school: String,
    onEditNickname: () -> Unit
) {
    val colors = LocalEggRiceColors.current

    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour in 6..11 -> "早上好"
            hour in 12..13 -> "中午好"
            hour in 14..17 -> "下午好"
            else -> "晚上好"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditNickname() }
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (nickname != "同学") nickname.take(1) else "同",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accentMain
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                // Greeting
                Text(
                    "$greeting，",
                    fontSize = 13.sp,
                    color = colors.textTertiary
                )
                Spacer(Modifier.height(2.dp))
                // Nickname row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        nickname,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Outlined.Edit,
                        "编辑昵称",
                        tint = colors.accentMain.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                // School tag
                if (school.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.surfaceHighlight
                    ) {
                        Text(
                            school,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.accentMain,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ArrowRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    val colors = LocalEggRiceColors.current
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
        if (subtitle != null) {
            Text(subtitle, fontSize = 13.sp, color = colors.textSecondary, modifier = Modifier.padding(end = 6.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "进入", tint = colors.textTertiary, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun SwitchMenuRow(title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    val colors = LocalEggRiceColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            Text(subtitle, fontSize = 11.sp, color = colors.textTertiary)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.accentMain, uncheckedThumbColor = colors.borderDivider, uncheckedTrackColor = colors.borderDivider))
    }
}

@Composable
internal fun SwitchMenuRowSimple(title: String, checked: Boolean, onToggle: () -> Unit) {
    val colors = LocalEggRiceColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.accentMain, uncheckedThumbColor = Color.White, uncheckedTrackColor = colors.borderDivider))
    }
}

@Composable
internal fun MenuRow(icon: ImageVector?, title: String, subtitle: String? = null, onClick: () -> Unit) {
    val colors = LocalEggRiceColors.current
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = colors.accentMain, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(title, fontSize = 15.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "进入", tint = colors.textTertiary, modifier = Modifier.size(20.dp))
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = colors.textTertiary, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = if (icon != null) 34.dp else 0.dp))
        }
    }
}

@Composable
internal fun DualButton(
    text: String, icon: ImageVector, modifier: Modifier = Modifier,
    onClick: () -> Unit, containerColor: Color = LocalEggRiceColors.current.surfaceHighlight, contentColor: Color = LocalEggRiceColors.current.accentMain
) {
    Card(modifier = modifier.height(72.dp).clickable(onClick = onClick), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
        }
    }
}

@Composable
internal fun PetArea(
    petIndex: Int,
    petName: String,
    onClick: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    val emoji = petEmoji(petIndex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pet emoji avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 30.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "我的宠物",
                    fontSize = 13.sp,
                    color = colors.textTertiary
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        petName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Outlined.Edit,
                        "编辑",
                        tint = colors.accentMain.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                "切换宠物",
                tint = colors.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun SpacerH(height: Int) {
    Spacer(modifier = Modifier.height(height.dp))
}

// ═══ V2: 宠物卡片 (220dp, 浅蓝渐变) ═══
@Composable
internal fun PetCard(
    nickname: String,
    school: String,
    petIndex: Int,
    petName: String,
    onEditNickname: () -> Unit,
    onPetClick: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    val isDark = LocalDarkMode.current
    val emoji = petEmoji(petIndex)

    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour in 6..11 -> "早上好"
            hour in 12..13 -> "中午好"
            hour in 14..17 -> "下午好"
            else -> "晚上好"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark)
                            listOf(Color(0xFF1A2A4A), Color(0xFF1E1E1E))
                        else
                            listOf(Color(0xFFEAF4FF), Color(0xFFFFFFFF))
                    )
                )
        ) {
            // Left content
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp, top = 24.dp, bottom = 24.dp)
            ) {
                Text(greeting + "，", fontSize = 14.sp, color = colors.textSecondary)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (nickname != "同学") nickname else "同学",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Outlined.Edit, "编辑",
                        tint = colors.accentMain.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp).clickable { onEditNickname() }
                    )
                }

                if (school.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = colors.surfaceHighlight) {
                        Text(
                            school, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                            color = colors.accentMain,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Lv.12", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.accentMain)
                    Text(" · ", fontSize = 11.sp, color = colors.textTertiary)
                    Text("陪伴86天", fontSize = 11.sp, color = colors.textTertiary)
                }

                Spacer(Modifier.height(12.dp))

                // Pet info row
                Row(
                    modifier = Modifier.clickable { onPetClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("我的宠物", fontSize = 12.sp, color = colors.textTertiary)
                    Spacer(Modifier.width(4.dp))
                    Text(petName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, "切换",
                        tint = colors.textTertiary, modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Right: large dog emoji
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 8.dp)
            ) {
                Text(emoji, fontSize = 80.sp)
            }

            // Decorative elements
            Text(
                "🌿",
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 30.dp)
            )

            // Speech bubble
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 20.dp),
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceCard.copy(alpha = 0.85f),
                shadowElevation = 2.dp
            ) {
                Text(
                    "今天也要加油哦！",
                    fontSize = 11.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ═══ V2: 未完成提醒卡 (浅粉背景) ═══
@Composable
internal fun ReminderCard(
    unfinishedCount: Int,
    onGoComplete: () -> Unit
) {
    if (unfinishedCount == 0) return

    val colors = LocalEggRiceColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4EC).copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📋", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "你有${unfinishedCount}项未完成",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8B5A5E)
                )
                Text(
                    "快去完成它们吧～",
                    fontSize = 11.sp,
                    color = Color(0xFF8B5A5E).copy(alpha = 0.6f)
                )
            }
            Button(
                onClick = onGoComplete,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE57373).copy(alpha = 0.12f),
                    contentColor = Color(0xFFE57373)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("去完成 →", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ═══ V2: 快捷功能 2×2 网格 ═══
@Composable
internal fun QuickActionGrid(
    onImport: () -> Unit,
    onShare: () -> Unit,
    onScheduleSettings: () -> Unit,
    onGeneralSettings: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                text = "导入课表",
                icon = Icons.Filled.FileDownload,
                bgColor = Color(0xFFFFE4EC).copy(alpha = 0.6f),
                contentColor = Color(0xFFE57373),
                onClick = onImport,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                text = "分享课表",
                icon = Icons.Filled.Share,
                bgColor = Color(0xFFEAF4FF).copy(alpha = 0.8f),
                contentColor = Color(0xFF4D8DFF),
                onClick = onShare,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                text = "课表设置",
                icon = Icons.Filled.Palette,
                bgColor = Color(0xFFEAF4FF).copy(alpha = 0.8f),
                contentColor = Color(0xFF4D8DFF),
                onClick = onScheduleSettings,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                text = "通用设置",
                icon = Icons.Filled.Settings,
                bgColor = Color(0xFFFFE4EC).copy(alpha = 0.6f),
                contentColor = Color(0xFFE57373),
                onClick = onGeneralSettings,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ═══ V2: 皮肤切换卡片 ═══
@Composable
internal fun SkinSwitcherCard(
    currentSkin: String,
    onSelectSkin: (String) -> Unit
) {
    val colors = LocalEggRiceColors.current
    val isDark = LocalDarkMode.current

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            "主题皮肤",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkinOption(
                emoji = "🐶",
                name = "旺财",
                desc = "海盐蓝",
                isSelected = currentSkin == "wangcai",
                accentColor = if (isDark) Color(0xFF6AA8FF) else Color(0xFF4D8DFF),
                onClick = { onSelectSkin("wangcai") },
                modifier = Modifier.weight(1f)
            )
            SkinOption(
                emoji = "🍳",
                name = "蛋炒饭",
                desc = "金黄琥珀",
                isSelected = currentSkin == "fried_rice",
                accentColor = if (isDark) Color(0xFFF5B840) else Color(0xFFF0A030),
                onClick = { onSelectSkin("fried_rice") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SkinOption(
    emoji: String,
    name: String,
    desc: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalEggRiceColors.current

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.1f) else colors.surfaceCard
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, accentColor)
        } else {
            androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderDivider)
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 36.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                name,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accentColor else colors.textPrimary
            )
            Text(
                desc,
                fontSize = 11.sp,
                color = if (isSelected) accentColor.copy(alpha = 0.7f) else colors.textTertiary
            )
            if (isSelected) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor
                ) {
                    Text(
                        "当前",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    text: String,
    icon: ImageVector,
    bgColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(88.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(8.dp))
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
        }
    }
}
