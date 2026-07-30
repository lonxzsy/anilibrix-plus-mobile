package com.anilibrix.plus.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.ui.theme.Spacing

/**
 * # Контракт владения инсетами
 *
 * От него зависит вся раскладка приложения, поэтому он записан здесь один раз.
 *
 * **Корневой `Scaffold`** (MainActivity) владеет нижним баром и снекбаром —
 * и больше ничем. Он объявляет `contentWindowInsets = WindowInsets(0)`,
 * то есть НЕ претендует на системные бары, и публикует высоту нижнего бара
 * через [LocalBottomBarHeight].
 *
 * **Каждый экран** владеет собственным `Scaffold` для своего топбара и FAB,
 * объявляет `contentWindowInsets = WindowInsets.safeDrawing.only(Top + Horizontal)`
 * и НЕ применяет `innerPadding` как `Modifier.padding`, а передаёт
 * `innerPadding.calculateTopPadding()` в `contentPadding.top` своего
 * скролл-контейнера, а [LocalBottomBarHeight] — в `contentPadding.bottom`.
 *
 * Итог: контент реально скроллится ПОД барами (настоящий edge-to-edge),
 * ничто не западдинжено дважды, и сворачивающиеся топбары работают,
 * потому что список действительно во весь экран.
 *
 * **Полноэкранные маршруты** (оба плеера) не участвуют: у них нет `Scaffold`,
 * нижний бар не скомпонован, [LocalBottomBarHeight] равен 0.
 *
 * До этого `MainActivity` применял `Modifier.padding(innerPadding)`, который
 * ничего не консюмит: edge-to-edge был включён и тут же обесценен, а восемь
 * вложенных `Scaffold` добавляли системные отступы повторно.
 */
val LocalBottomBarHeight = compositionLocalOf { 0.dp }

/**
 * Отступы для скролл-контейнера экрана: верх приходит от собственного
 * `Scaffold` экрана, низ — от нижнего бара, чтобы последний элемент списка
 * можно было домотать.
 */
@Composable
fun screenContentPadding(
    scaffoldPadding: PaddingValues,
    horizontal: Dp = Spacing.screenHorizontal,
    extraBottom: Dp = Spacing.lg,
): PaddingValues = PaddingValues(
    start = horizontal,
    end = horizontal,
    top = scaffoldPadding.calculateTopPadding(),
    bottom = LocalBottomBarHeight.current + extraBottom,
)

/**
 * Вариант для контейнеров, у которых верхний инсет уже применён родителем.
 *
 * Так устроены страницы внутри пейджера: `HorizontalPager` получает
 * `padding(top = ...)` целиком, а каждая страница отвечает только за
 * горизонтальные поля и за то, чтобы последний элемент можно было домотать
 * из-под нижнего бара.
 */
@Composable
fun screenContentPadding(
    horizontal: Dp = Spacing.screenHorizontal,
    top: Dp = Spacing.sm,
    extraBottom: Dp = Spacing.lg,
): PaddingValues = PaddingValues(
    start = horizontal,
    end = horizontal,
    top = top,
    bottom = LocalBottomBarHeight.current + extraBottom,
)
