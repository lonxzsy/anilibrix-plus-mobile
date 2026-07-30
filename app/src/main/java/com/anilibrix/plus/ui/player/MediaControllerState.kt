package com.anilibrix.plus.ui.player

import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.anilibrix.plus.core.playback.PlaybackService
import com.google.common.util.concurrent.MoreExecutors

/**
 * Подключение экрана к плееру, который живёт в сервисе.
 *
 * Раньше `ExoPlayer` создавался прямо в composable и умирал вместе с ним:
 * сворачивание приложения обрывало воспроизведение, а системных контролов не
 * было вовсе. Теперь плеер один на всё приложение и находится в
 * [PlaybackService], а экран получает к нему пульт.
 *
 * До завершения подключения возвращается `null` — это нормальное состояние
 * первых кадров, а не ошибка.
 */
@UnstableApi
@Composable
fun rememberMediaController(): State<MediaController?> {
    val context = LocalContext.current
    val controllerState = remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(context) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                // Подключение может завершиться уже после ухода с экрана —
                // тогда пульт надо сразу отпустить, иначе он удержит сервис.
                controllerState.value = runCatching { future.get() }.getOrNull()
            },
            MoreExecutors.directExecutor(),
        )

        onDispose {
            controllerState.value?.release()
            controllerState.value = null
            MediaController.releaseFuture(future)
        }
    }

    return controllerState
}
