package com.eggrice.timetable.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.di.AppContainer
import com.eggrice.timetable.ui.theme.*
import java.time.LocalDate

/**
 * 学期周次校准引导（导入课表后弹出）：
 * 用户选「当前是第几周」，系统反推开学日期 = 本周一 − (周数−1)×7，一键对齐课表与真实日期。
 * 不依赖教务是否返回学期信息，任何导入方式（正方/强智/Web/分享码）都适用。
 */
@Composable
fun SemesterCalibrateDialog(
    container: AppContainer,
    onDismiss: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    val context = LocalContext.current
    val semesterWeeks by container.semesterWeeks.collectAsState()

    // 本周一（dayOfWeek: 1=周一 … 7=周日）
    val today = LocalDate.now()
    val thisMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())

    var week by remember { mutableIntStateOf(1) }
    val computedStart = remember(week) { thisMonday.minusWeeks((week - 1).toLong()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("学期周次校准", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                Text(
                    "刚导入的课表按周次显示。确认「当前是第几周」后，" +
                        "将自动设置开学日期，课表与真实日期就能对齐（今天 = 第 $week 周）。",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(18.dp))
                // 周次选择：− / 数字 / +
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WeekStepButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, enabled = week > 1) {
                        week--
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    ) {
                        Text("当前是第几周？", fontSize = 12.sp, color = colors.textTertiary)
                        Text(
                            "第 $week 周",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accentMain
                        )
                    }
                    WeekStepButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, enabled = week < semesterWeeks.coerceAtLeast(1)) {
                        week++
                    }
                }
                Spacer(Modifier.height(16.dp))
                // 反推结果预览
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceHighlight
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("开学日期将设为", fontSize = 12.sp, color = colors.textTertiary)
                        Text(
                            computedStart.toString() + "（第1周周一）",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            "若仍不对，可稍后在「设置 → 学期设置」中手动调整",
                            fontSize = 11.sp,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    container.setSemesterStart(computedStart.toString())
                    Toast.makeText(context, "已校准：开学日期 $computedStart，今天是第 $week 周", Toast.LENGTH_LONG).show()
                    container.clearSemesterCalibration()
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor())
            ) { Text("应用校准", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = {
                container.clearSemesterCalibration()
                onDismiss()
            }) { Text("跳过", color = colors.textSecondary) }
        }
    )
}

@Composable
private fun WeekStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (enabled) colors.surfaceHighlight else colors.surfaceCard.copy(alpha = 0.6f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = if (enabled) "调整周次" else null,
            tint = if (enabled) colors.accentMain else colors.textTertiary.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp)
        )
    }
}
