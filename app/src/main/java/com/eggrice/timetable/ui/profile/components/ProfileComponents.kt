package com.eggrice.timetable.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
