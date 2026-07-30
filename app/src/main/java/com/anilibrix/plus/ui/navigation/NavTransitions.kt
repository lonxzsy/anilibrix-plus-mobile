package com.anilibrix.plus.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.navigation.NavBackStackEntry
import com.anilibrix.plus.ui.theme.MotionTokens

/**
 * Единая система переходов между экранами.
 *
 * Раньше во всей навигации стояли голые `tween(300)` / `tween(400)` с
 * подразумеваемым easing, вход не сопровождался проявлением (входящий экран
 * ехал поверх полностью непрозрачного уходящего — тот самый «дешёвый» эффект),
 * а у плеера не были заданы pop-переходы, из-за чего «назад» уезжало ВВЕРХ,
 * повторяя вход.
 *
 * Все спеки берутся из [MotionTokens]: входящий элемент замедляется,
 * уходящий ускоряется.
 */

private typealias Enter = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
private typealias Exit = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

/**
 * Соседние экраны (вкладки нижней навигации): направление не подразумевается,
 * поэтому проявление с лёгким приближением, а не сдвиг.
 */
object FadeThrough {
    val enter: Enter = {
        fadeIn(MotionTokens.effectsDefault()) +
            scaleIn(MotionTokens.spatialDefault(), initialScale = 0.92f)
    }
    val exit: Exit = { fadeOut(MotionTokens.effectsFast()) }
}

/** Движение вглубь иерархии: экран приходит справа, назад — уходит вправо. */
object SharedXAxis {
    val enter: Enter = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = MotionTokens.navEnter(),
        ) + fadeIn(MotionTokens.effectsDefault())
    }
    val exit: Exit = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = MotionTokens.navExit(),
        ) + fadeOut(MotionTokens.effectsFast())
    }
    val popEnter: Enter = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = MotionTokens.navEnter(),
        ) + fadeIn(MotionTokens.effectsDefault())
    }
    val popExit: Exit = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = MotionTokens.navExit(),
        ) + fadeOut(MotionTokens.effectsFast())
    }
}

/**
 * Медиа-поверхности «поднимаются» снизу и симметрично опускаются на «назад».
 * Симметрия здесь — главный фикс: раньше pop повторял вход.
 */
object SharedZAxis {
    val enter: Enter = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Up,
            animationSpec = MotionTokens.navEnter(),
        ) + fadeIn(MotionTokens.effectsDefault())
    }
    val exit: Exit = { fadeOut(MotionTokens.effectsFast()) }
    val popEnter: Enter = { fadeIn(MotionTokens.effectsDefault()) }
    val popExit: Exit = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Down,
            animationSpec = MotionTokens.navExit(),
        ) + fadeOut(MotionTokens.effectsFast())
    }
}
