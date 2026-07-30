package com.anilibrix.plus.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anilibrix.plus.ui.theme.MotionTokens
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import com.anilibrix.plus.ui.theme.Spacing

enum class ScreenState { Loading, Empty, Error, Content }

/**
 * Единая точка переключения «загрузка / пусто / ошибка / контент».
 *
 * До этого каждый экран решал сам, и получалось вразнобой: главная показывала
 * скелетоны, каталог и расписание — голый спиннер, библиотека не показывала
 * ничего (её флаг загрузки вообще никогда не выставлялся), а пустых состояний
 * местами не было. Здесь консистентность обеспечена структурно, а не
 * договорённостью.
 *
 * Переход — fade through: уходящее гаснет быстро, входящее проявляется
 * с лёгким приближением, поэтому подмена не выглядит рывком.
 */
@Composable
fun ScreenStateHost(
    state: ScreenState,
    modifier: Modifier = Modifier,
    /**
     * Если передан, при затянувшемся ожидании поверх скелетона появится
     * [SlowLoadingHint] с кнопкой повтора.
     */
    onRetry: (() -> Unit)? = null,
    loading: @Composable () -> Unit,
    empty: @Composable () -> Unit,
    error: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    AnimatedContent(
        targetState = state,
        modifier = modifier,
        transitionSpec = {
            (
                fadeIn(MotionTokens.effectsDefault()) +
                    scaleIn(MotionTokens.spatialDefault(), initialScale = 0.96f)
                ) togetherWith fadeOut(MotionTokens.effectsFast())
        },
        label = "screenState",
    ) { target ->
        when (target) {
            ScreenState.Loading -> {
                Box(Modifier.fillMaxSize()) {
                    loading()
                    SlowLoadingHint(
                        onRetry = onRetry,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = Spacing.xxl),
                    )
                }
            }
            ScreenState.Empty -> empty()
            ScreenState.Error -> error()
            ScreenState.Content -> content()
        }
    }
}
