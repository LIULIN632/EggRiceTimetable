package com.eggrice.timetable.ui.timetable.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.ui.theme.EggRiceSpring
import com.eggrice.timetable.ui.theme.LocalEggRiceColors
import kotlinx.coroutines.delay

data class PetInfo(val emoji: String, val defaultName: String)

val PetList = listOf(
    PetInfo("🍙", "饭团"),  // 🍙
    PetInfo("🍳", "煎蛋"),  // 🍳
    PetInfo("🐱", "小咪"),  // 🐱
    PetInfo("🐶", "旺财"),  // 🐶
    PetInfo("🐰", "跳跳"),  // 🐰
    PetInfo("🐼", "滚滚"),  // 🐼
)

fun petEmoji(index: Int): String = PetList.getOrElse(index) { PetList[0] }.emoji

@Composable
fun PetFAB(
    petEmoji: String = "🍳",
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var eating by remember { mutableStateOf(false) }

    val petScale by animateFloatAsState(
        targetValue = if (eating) 1.15f else 1f,
        animationSpec = EggRiceSpring
    )

    LaunchedEffect(eating) {
        if (eating) {
            delay(600)
            eating = false
        }
    }

    val colors = LocalEggRiceColors.current

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Button(
            onClick = {
                eating = true
                onClick()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accentMain,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .scale(petScale)
                .semantics { contentDescription = "蛋炒饭悬浮球，点击触发趣味动画" }
        ) {
            Text(if (eating) "😋" else petEmoji, fontSize = 24.sp)
        }
    }
}
