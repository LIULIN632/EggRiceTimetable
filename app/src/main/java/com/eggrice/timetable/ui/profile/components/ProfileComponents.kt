package com.eggrice.timetable.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.ui.theme.*

@Composable
internal fun UserProfileArea(
    nickname: String,
    onEditNickname: () -> Unit
) {
    val isDark = LocalDarkMode.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else SurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onEditNickname() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentSoftColor()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (nickname != "同学") nickname.take(1) else "同",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor()
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(nickname, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isDark) DarkTextPrimary else TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.Edit, "编辑昵称", tint = accentColor(), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun ArrowRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        if (subtitle != null) {
            Text(subtitle, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(end = 6.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = IconTertiary, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun SwitchMenuRow(title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    val isDark = LocalDarkMode.current
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (isDark) DarkTextPrimary else TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = if (isDark) DarkTextTertiary else TextTertiary)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor(), uncheckedThumbColor = if (isDark) BorderDark else Color.White, uncheckedTrackColor = if (isDark) BorderDark else BorderLight))
    }
}

@Composable
internal fun SwitchMenuRowSimple(title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor(), uncheckedThumbColor = Color.White, uncheckedTrackColor = BorderLight))
    }
}

@Composable
internal fun MenuRow(icon: ImageVector?, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = accentColor(), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(title, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = IconTertiary, modifier = Modifier.size(20.dp))
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = TextTertiary, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = if (icon != null) 34.dp else 0.dp))
        }
    }
}

@Composable
internal fun DualButton(
    text: String, icon: ImageVector, modifier: Modifier = Modifier,
    onClick: () -> Unit, containerColor: Color = accentSoftColor().copy(alpha = 0.6f), contentColor: Color = accentColor()
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
internal fun SpacerH(height: Int) {
    Spacer(modifier = Modifier.height(height.dp))
}
