package com.anilibrix.plus.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.util.Rational
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player

/**
 * Вход в «картинку в картинке».
 *
 * Раньше кнопка PiP была заглушкой: она меняла флаг в состоянии, но
 * `enterPictureInPictureMode` не вызывался, а в манифесте не было
 * `supportsPictureInPicture` — то есть система в любом случае отказала бы.
 *
 * Пропорции берём из **реального** размера видео. Значение по умолчанию 16:9
 * применяется, только пока размер ещё неизвестен: с неверной пропорцией
 * система обрежет картинку по краям, и это заметно.
 */
fun Activity.enterPictureInPicture(player: Player?) {
    val videoSize = player?.videoSize
    val ratio = if (videoSize != null && videoSize.width > 0 && videoSize.height > 0) {
        Rational(videoSize.width, videoSize.height)
    } else {
        Rational(16, 9)
    }

    val params = PictureInPictureParams.Builder()
        .setAspectRatio(ratio.coerceToSupportedRange())
        .build()

    runCatching { enterPictureInPictureMode(params) }
}

/**
 * Система принимает пропорции только в диапазоне примерно от 1:2.39 до 2.39:1
 * и отклоняет запрос целиком, если выйти за него. Вертикальные видео и
 * сверхширокие кадры встречаются, поэтому зажимаем.
 */
private fun Rational.coerceToSupportedRange(): Rational {
    val value = numerator.toFloat() / denominator.toFloat()
    return when {
        value > MAX_RATIO -> Rational(239, 100)
        value < MIN_RATIO -> Rational(100, 239)
        else -> this
    }
}

private const val MAX_RATIO = 2.39f
private const val MIN_RATIO = 1f / 2.39f

@Composable
fun rememberActivity(): Activity? {
    var context: Context? = LocalContext.current
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Находится ли приложение в «картинке в картинке».
 *
 * Значение приходит из Activity: выйти из PiP можно жестом системы, о котором
 * ViewModel плеера не узнает. В PiP собственные контролы не рисуются — система
 * показывает свои, и два набора кнопок поверх крошечного окна нечитаемы.
 */
val LocalIsInPictureInPicture = androidx.compose.runtime.compositionLocalOf { false }
