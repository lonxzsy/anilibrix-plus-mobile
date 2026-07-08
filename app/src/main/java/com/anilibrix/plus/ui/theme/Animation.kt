package com.anilibrix.plus.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object AnilibrixAnimation {
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Fast = tween<Float>(durationMillis = 160, easing = Emphasized)
    val Medium = tween<Float>(durationMillis = 260, easing = Emphasized)
    val Slow = tween<Float>(durationMillis = 420, easing = Emphasized)

    val SpringSoft = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val SpringBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
