package com.eggrice.timetable.ui.profile.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.ui.theme.*
import com.eggrice.timetable.ui.timetable.components.PetList
import com.eggrice.timetable.ui.timetable.components.petDrawable

// ═══ Design tokens (PRD V3) ═══
internal val PageBg = Color(0xFFFFFBF5)
private val PetCardBlue1 = Color(0xFFD6EAFF)
private val PetCardBlue2 = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF212529)
private val TextGray = Color(0xFF6C757D)
private val PinkBg = Color(0xFFFFF0F3)
private val PinkAccent = Color(0xFFFF9BB3)
private val BlueBg = Color(0xFFF0F4FF)
private val BlueAccent = Color(0xFF6B8EFE)
private val BrownTagBg = Color(0xFF8B6F47)
private val DividerGray = Color(0xFFE9ECEF)

// ═══ V3: PetCard — gradient blue, Shiba Inu scene ═══
@Composable
internal fun PetCard(
    nickname: String,
    school: String,
    petIndex: Int,
    petName: String,
    daysCompanion: Int = 0,
    onEditNickname: () -> Unit,
    onPetClick: () -> Unit
) {
    val isDark = LocalDarkMode.current
    val petDrawableId = petDrawable(petIndex)

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
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark)
                            listOf(Color(0xFF1A2A4A), Color(0xFF1E2430))
                        else
                            listOf(PetCardBlue1, PetCardBlue2)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            // Top-right tag: "我的宠物 旺财 >"
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrownTagBg)
                    .clickable { onPetClick() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("我的宠物", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(2.dp))
                Text(petName, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }

            Column(
                modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 20.dp, end = 120.dp)
            ) {
                // Greeting
                Text("$greeting，", fontSize = 14.sp, color = if (isDark) Color(0xFFB8C4D6) else TextGray)
                Spacer(Modifier.height(2.dp))

                // Nickname row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = nickname,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else TextDark
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Outlined.Edit, "编辑",
                        tint = if (isDark) Color(0xFFB8C4D6) else TextGray,
                        modifier = Modifier.size(14.dp).clickable { onEditNickname() }
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Days companion
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("陪伴${daysCompanion}天", fontSize = 12.sp, color = if (isDark) Color(0xFFB8C4D6) else TextGray)
                }

                // School tag
                if (school.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(school, fontSize = 12.sp, color = if (isDark) Color(0xFFA5C8FF) else BlueAccent, fontWeight = FontWeight.Medium)
                }

                Spacer(Modifier.height(12.dp))

                // Speech bubble
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF2A2A2A) else Color.White,
                    shadowElevation = 1.dp
                ) {
                    Text(
                        "今天也要加油哦！${petName}相信你可以的！",
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFCCCCCC) else TextDark,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Right: pet avatar on grass
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF2E3A4C) else Color(0xFFF5F0E8)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = petDrawableId),
                    contentDescription = petName,
                    modifier = Modifier.size(42.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

// ═══ V3: ReminderCard — pink bg, with icon ═══
@Composable
internal fun ReminderCard(
    unfinishedCount: Int,
    onGoComplete: () -> Unit
) {
    if (unfinishedCount == 0) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PinkBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                Icons.Filled.Notifications, null,
                tint = PinkAccent, modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "你有${unfinishedCount}项未完成",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    "快去看看吧~",
                    fontSize = 12.sp,
                    color = TextGray
                )
            }

            Text(
                "去完成 >",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PinkAccent,
                modifier = Modifier.clickable { onGoComplete() }
            )
        }
    }
}

// ═══ V3: QuickActionGrid 2×2 ═══
@Composable
internal fun QuickActionGrid(
    onImport: () -> Unit,
    onShare: () -> Unit,
    onScheduleSettings: () -> Unit,
    onGeneralSettings: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard(
                icon = Icons.Filled.FileDownload,
                title = "导入课表",
                subtitle = "一键导入课程",
                bgColor = PinkBg,
                accentColor = PinkAccent,
                onClick = onImport,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Filled.Share,
                title = "分享课表",
                subtitle = "分享给好友",
                bgColor = BlueBg,
                accentColor = BlueAccent,
                onClick = onShare,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard(
                icon = Icons.Filled.CalendarMonth,
                title = "课表设置",
                subtitle = "管理显示与样式",
                bgColor = BlueBg,
                accentColor = BlueAccent,
                onClick = onScheduleSettings,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Filled.Settings,
                title = "通用设置",
                subtitle = "系统与偏好设置",
                bgColor = PinkBg,
                accentColor = PinkAccent,
                onClick = onGeneralSettings,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    bgColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = TextGray)
        }
    }
}

// ═══ V3: MoreFeatures section header + list ═══
@Composable
internal fun MoreFeaturesHeader() {
    val isDark = LocalDarkMode.current
    Text(
        "更多功能",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = if (isDark) Color(0xFFCCCCCC) else TextDark,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
internal fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val isDark = LocalDarkMode.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null,
                tint = if (isDark) Color(0xFF9E9E9E) else TextGray,
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(title,
                fontSize = 14.sp,
                color = if (isDark) Color(0xFFE0E0E0) else TextDark,
                modifier = Modifier.weight(1f))
            if (subtitle != null) {
                Text(subtitle,
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF757575) else TextGray,
                    modifier = Modifier.padding(end = 6.dp))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = if (isDark) Color(0xFF555555) else TextGray,
                modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = if (isDark) Color(0xFF2B2B2B) else DividerGray, thickness = 0.5.dp)
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
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = colors.textTertiary, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = if (icon != null) 34.dp else 0.dp))
        }
    }
}
