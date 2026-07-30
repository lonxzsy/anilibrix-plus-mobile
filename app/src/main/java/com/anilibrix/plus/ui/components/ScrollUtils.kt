package com.anilibrix.plus.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember

/**
 * `true`, пока пользователь скроллит вверх (к началу списка) — этим управляется
 * разворачивание расширенного FAB и показ чрома.
 *
 * Вычисляется через [derivedStateOf], поэтому пересчёт идёт только при реальной
 * смене направления, а не на каждый кадр скролла.
 */
@Composable
fun rememberIsScrollingUp(state: LazyListState): State<Boolean> {
    val previousIndex = remember(state) { mutableIntStateOf(state.firstVisibleItemIndex) }
    val previousOffset = remember(state) { mutableIntStateOf(state.firstVisibleItemScrollOffset) }
    return remember(state) {
        derivedStateOf {
            val index = state.firstVisibleItemIndex
            val offset = state.firstVisibleItemScrollOffset
            val scrollingUp = if (previousIndex.intValue != index) {
                previousIndex.intValue > index
            } else {
                previousOffset.intValue >= offset
            }
            previousIndex.intValue = index
            previousOffset.intValue = offset
            scrollingUp
        }
    }
}

@Composable
fun rememberIsScrollingUp(state: LazyGridState): State<Boolean> {
    val previousIndex = remember(state) { mutableIntStateOf(state.firstVisibleItemIndex) }
    val previousOffset = remember(state) { mutableIntStateOf(state.firstVisibleItemScrollOffset) }
    return remember(state) {
        derivedStateOf {
            val index = state.firstVisibleItemIndex
            val offset = state.firstVisibleItemScrollOffset
            val scrollingUp = if (previousIndex.intValue != index) {
                previousIndex.intValue > index
            } else {
                previousOffset.intValue >= offset
            }
            previousIndex.intValue = index
            previousOffset.intValue = offset
            scrollingUp
        }
    }
}

/** `true`, если список прокручен хоть немного — для тени/цвета топбара. */
@Composable
fun rememberIsScrolled(state: LazyListState): State<Boolean> = remember(state) {
    derivedStateOf { state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0 }
}

@Composable
fun rememberIsScrolled(state: LazyGridState): State<Boolean> = remember(state) {
    derivedStateOf { state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0 }
}
