package com.anilibrix.plus.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.composed
import com.anilibrix.plus.ui.theme.AnilibrixAnimation

fun Modifier.pressScale(
    pressedScale: Float = 0.96f
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = AnilibrixAnimation.SpringSoft,
        label = "pressScale"
    )

    scale(scale)
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

fun Modifier.hapticOnPress(
    feedbackConstant: Int = HapticFeedbackConstants.CLOCK_TICK
): Modifier = composed {
    val view = LocalView.current
    pointerInput(feedbackConstant) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            view.performHapticFeedback(feedbackConstant)
            waitForUpOrCancellation()
        }
    }
}
