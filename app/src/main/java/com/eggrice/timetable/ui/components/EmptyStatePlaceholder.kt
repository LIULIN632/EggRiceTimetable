package com.eggrice.timetable.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.ui.theme.*

@Composable
fun EmptyStatePlaceholder(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalDarkMode.current
    val resolvedIconTint = if (isDark) DarkTextTertiary.copy(alpha = 0.3f)
                           else TextTertiary.copy(alpha = 0.3f)
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = resolvedIconTint,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                fontSize = 14.sp,
                color = if (isDark) DarkTextTertiary.copy(alpha = 0.5f)
                        else TextTertiary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}
