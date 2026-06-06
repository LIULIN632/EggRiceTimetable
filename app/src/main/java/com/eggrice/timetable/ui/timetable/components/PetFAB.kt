package com.eggrice.timetable.ui.timetable.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.ui.theme.EggRiceSpring
import kotlinx.coroutines.delay

data class PetInfo(val emoji: String, val defaultName: String)

val PetList = listOf(
    PetInfo("🍙", "饭团"),
    PetInfo("🍳", "煎蛋"),
    PetInfo("🐱", "小咪"),
    PetInfo("🐶", "旺财"),
    PetInfo("🐰", "跳跳"),
    PetInfo("🐼", "滚滚"),
)

fun petEmoji(index: Int): String = PetList.getOrElse(index) { PetList[0] }.emoji

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PetFAB(
    petEmoji: String = "🐶",
    badgeCount: Int = 0,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var eating by remember { mutableStateOf(false) }

    // Breathing animation — continuous subtle scale 1.0 ↔ 1.03, 2s cycle
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheScale: Float by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val eatBounce: Float by animateFloatAsState(
        targetValue = if (eating) 1.1f else 1f,
        animationSpec = EggRiceSpring
    )

    val petScale: Float = breatheScale * eatBounce

    LaunchedEffect(eating) {
        if (eating) {
            delay(600)
            eating = false
        }
    }

    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(0.05f))
            .scale(petScale),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF4D8DFF))
                .combinedClickable(
                    onClick = {
                        eating = true
                        onClick()
                    },
                    onLongClick = onLongClick
                )
                .semantics { contentDescription = "蛋炒饭宠物悬浮球" },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (eating) "😋" else petEmoji,
                fontSize = 28.sp
            )
        }

        // Red badge
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE57373)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
