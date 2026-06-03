package com.eggrice.timetable.ui.timetable.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.data.entity.CourseEntity
import com.eggrice.timetable.ui.theme.CourseColors
import com.eggrice.timetable.ui.theme.CourseTextColors

@Composable
fun CourseCard(
    course: CourseEntity,
    leftPct: Float,
    topPx: Float,
    heightPx: Float,
    widthPct: Float,
    periodH: Float,
    isToday: Boolean,
    showTeacher: Boolean,
    showRoom: Boolean,
    onClick: () -> Unit
) {
    val bgColor = CourseColors[course.colorIndex % CourseColors.size]
    val textColor = CourseTextColors[course.colorIndex % CourseTextColors.size]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (leftPct * 3).dp, top = (topPx / 3).dp)
    ) {
        Column(
            modifier = Modifier
                .width(((widthPct * 3).dp) - 8.dp)
                .height((heightPx / 3).dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor)
                .clickable { onClick() }
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = course.name,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                lineHeight = 13.sp
            )
            if (showTeacher && course.teacher.isNotEmpty()) {
                Text(
                    text = course.teacher,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    lineHeight = 11.sp
                )
            }
            if (showRoom && course.room.isNotEmpty()) {
                Text(
                    text = course.room,
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    lineHeight = 11.sp
                )
            }
        }
    }
}
