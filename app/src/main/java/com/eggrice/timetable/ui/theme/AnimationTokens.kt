package com.eggrice.timetable.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/** Motion duration tokens for consistent animation timing. */
object MotionDuration {
    const val Fast = 150
    const val Normal = 300
    const val Slow = 500
}

/** Pre-configured spring animation spec with medium bouncy feel. */
val EggRiceSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

/** Quick press feedback spring — stiffer, less bouncy. */
val EggRiceSpringPress = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)
