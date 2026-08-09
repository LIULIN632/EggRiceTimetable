package com.eggrice.timetable.ui.timetable.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.eggrice.timetable.util.parseSemesterStart
import java.time.LocalDate

@Composable
fun WeekHeader(
    currentWeek: Int,
    isCurrentWeek: Boolean = false,
    semesterStart: String = "",
    onHomeworkClick: () -> Unit = {}
) {
    val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val today = LocalDate.now()
    val todayDay = today.dayOfWeek.value
    val colors = LocalEggRiceColors.current

    val weekMonday = parseSemesterStart(semesterStart)
        ?.plusWeeks((currentWeek - 1).toLong())
        ?: today.plusDays((-(todayDay - 1) + (currentWeek - 1) * 7).toLong())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceCard)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Homework button
        IconButton(
            onClick = onHomeworkClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Assignment,
                contentDescription = "作业",
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp)
            )
        }

        weekdays.forEachIndexed { index, dayLabel ->
            val dayNum = index + 1
            val isToday = isCurrentWeek && dayNum == todayDay
            val date = weekMonday.plusDays(index.toLong())

            val (bgColor, textColor) = when {
                isToday -> Pair(colors.accentMain, Color.White)
                else -> Pair(Color.Transparent, colors.textPrimary)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(bgColor)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayLabel,
                    color = if (isToday) textColor.copy(alpha = 0.85f) else colors.textTertiary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = date.dayOfMonth.toString(),
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
