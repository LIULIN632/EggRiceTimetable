package com.eggrice.timetable.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggrice.timetable.ui.theme.LocalEggRiceColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PetBubble(
    message: String,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalEggRiceColors.current

    // Gentle float animation
    val floatTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
    ) {
        Column(
            modifier = modifier.offset { IntOffset(0, floatOffset.roundToInt()) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bubble card
            Box(
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(0.08f))
                    .background(colors.surfaceCard, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    lineHeight = 18.sp
                )
            }
            // Triangle pointer
            Box(
                modifier = Modifier
                    .offset(y = (-1).dp)
                    .background(colors.surfaceCard)
            ) {
                Text(
                    "▲",
                    fontSize = 10.sp,
                    color = colors.surfaceCard,
                    modifier = Modifier.offset(y = (-3).dp)
                )
            }
        }
    }
}

@Composable
fun rememberPetBubbleState(): PetBubbleState {
    return remember { PetBubbleState() }
}

class PetBubbleState {
    var message by mutableStateOf<String?>(null)
    private var dismissJob: Job? = null

    fun show(msg: String, scope: CoroutineScope) {
        message = msg
        dismissJob?.cancel()
        dismissJob = scope.launch {
            delay(4000)
            message = null
        }
    }

    fun dismiss() {
        message = null
        dismissJob?.cancel()
    }
}
