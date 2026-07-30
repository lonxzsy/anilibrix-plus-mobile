package com.anilibrix.plus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiTetheringError
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import kotlinx.coroutines.delay

/** Через сколько ждать, прежде чем признать, что загрузка затянулась. */
private const val SLOW_THRESHOLD_MS = 8_000L

/**
 * Подсказка «загрузка дольше обычного» с кнопкой повтора.
 *
 * Появляется только если ожидание реально затянулось: при быстром ответе
 * пользователь её вообще не увидит. Раньше при медленном API экран показывал
 * скелетон бесконечно и без единого намёка, что что-то идёт не так —
 * а сервер Anilibria временами отвечает около минуты.
 *
 * @param onRetry если `null`, показывается только текст без кнопки.
 */
@Composable
fun SlowLoadingHint(
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(SLOW_THRESHOLD_MS)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(MotionTokens.spatialDefault()) +
            fadeIn(MotionTokens.effectsDefault()),
        exit = shrinkVertically(MotionTokens.spatialFast()) +
            fadeOut(MotionTokens.effectsFast()),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = Elevation.level2,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WifiTetheringError,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Sizing.iconMd),
                    )
                    Text(
                        text = "Загрузка дольше обычного",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "Сервер отвечает медленно. Можно подождать или попробовать снова.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (onRetry != null) {
                    TextButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(Sizing.iconSm),
                        )
                        Text(
                            text = "Повторить",
                            modifier = Modifier.padding(start = Spacing.sm),
                        )
                    }
                }
            }
        }
    }
}
