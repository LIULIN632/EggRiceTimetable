package com.eggrice.timetable.ui.timetable.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.ui.theme.*
import java.time.LocalDate

@Composable
fun WeekHeader(
    currentWeek: Int,
    isCurrentWeek: Boolean = false,
    semesterStart: String = "",
    onDayClick: (Int) -> Unit = {},
    onHomeworkClick: () -> Unit = {}
) {
    val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val today = LocalDate.now()
    val todayDay = today.dayOfWeek.value
    val colors = LocalEggRiceColors.current

    // 计算本周周一日期
    val weekMonday = if (semesterStart.isNotBlank()) {
        try {
            val parts = semesterStart.split("-")
            LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                .plusWeeks((currentWeek - 1).toLong())
        } catch (_: Exception) {
            today.plusDays((-(todayDay - 1) + (currentWeek - 1) * 7).toLong())
        }
    } else {
        today.plusDays((-(todayDay - 1) + (currentWeek - 1) * 7).toLong())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceCard)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Homework button — 32.dp wide to align with grid sidebar
        Column(
            modifier = Modifier
                .width(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onHomeworkClick() }
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.Assignment,
                contentDescription = "作业",
                tint = colors.accentMain,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "作业",
                fontSize = 8.sp,
                color = colors.accentMain,
                fontWeight = FontWeight.Medium
            )
        }

        weekdays.forEachIndexed { index, dayLabel ->
            val dayNum = index + 1
            val isToday = isCurrentWeek && dayNum == todayDay
            val date = weekMonday.plusDays(index.toLong())

            val (bgColor, textColor, dayTextColor) = when {
                isToday && isCurrentWeek -> Triple(colors.accentMain, Color.White, Color.White)
                isToday && !isCurrentWeek -> Triple(colors.surfaceHighlight, colors.accentMain, colors.accentMain)
                else -> Triple(
                    Color.Transparent,
                    colors.textPrimary,
                    colors.textTertiary
                )
            }

            Column(
                modifier = Modifier
                    .width(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(bgColor)
                    .clickable { onDayClick(dayNum) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayLabel,
                    color = dayTextColor,
                    fontSize = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = date.dayOfMonth.toString(),
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
