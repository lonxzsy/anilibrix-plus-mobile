package com.anilibrix.plus.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Единая модальная шторка приложения.
 *
 * Раньше каждая шторка настраивалась сама (или не настраивалась вовсе):
 * форма, цвет контейнера и обработка инсетов различались от места к месту,
 * а фильтры каталога вообще не получали `sheetState` — из-за чего их нельзя
 * было закрыть программно с анимацией.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnilibrixBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge.copy(
            bottomStart = ZeroCornerSize,
            bottomEnd = ZeroCornerSize,
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets.navigationBars },
        content = content,
    )
}

/**
 * Закрытие, которое СНАЧАЛА доигрывает анимацию, а потом снимает флаг.
 *
 * Если просто выставить `show = false`, шторка исчезает мгновенно, без выезда вниз.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberSheetDismiss(
    sheetState: SheetState,
    onDismissed: () -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismissed()
        }
    }
}
