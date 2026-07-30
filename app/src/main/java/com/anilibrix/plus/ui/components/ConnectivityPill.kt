package com.anilibrix.plus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anilibrix.plus.core.util.NetworkMonitor
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing

/**
 * Индикатор отсутствия сети.
 *
 * Заменил OfflineBanner — полосу во всю ширину, которая жила в `Column` над
 * NavGraph: её появление сжимало и переверстывало весь nav-host (заметный
 * скачок и потеря позиции скролла). Теперь это плавающая пилюля-оверлей,
 * которая ничего не двигает.
 */
@Composable
fun ConnectivityPill(
    networkMonitor: NetworkMonitor,
    modifier: Modifier = Modifier,
) {
    val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle(initialValue = true)

    AnimatedVisibility(
        visible = !isOnline,
        enter = fadeIn(MotionTokens.effectsDefault()) +
            slideInVertically(MotionTokens.spatialDefault()) { it / 2 } +
            scaleIn(MotionTokens.spatialDefault(), initialScale = 0.9f),
        exit = fadeOut(MotionTokens.effectsFast()) +
            slideOutVertically(MotionTokens.spatialFast()) { it / 2 } +
            scaleOut(MotionTokens.spatialFast(), targetScale = 0.9f),
        modifier = modifier,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            tonalElevation = Elevation.level3,
            shadowElevation = Elevation.level2,
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = Spacing.lg,
                    vertical = Spacing.sm,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(Sizing.iconSm),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "Нет подключения",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
