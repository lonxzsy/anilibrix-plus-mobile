package com.anilibrix.plus.core.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.anilibrix.plus.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Плеер, который переживает уход приложения в фон.
 *
 * Раньше воспроизведение обрывалось на любом сворачивании: `ExoPlayer`
 * создавался прямо в composable и умирал вместе с экраном. Отсюда же не было
 * ни контролов на экране блокировки, ни реакции на кнопки гарнитуры, ни
 * корректной обработки аудиофокуса — звонок или чужое видео просто играли
 * поверх.
 *
 * `MediaSessionService` решает всё это разом: система сама рисует уведомление
 * с обложкой и перемоткой, сама доставляет команды с наушников и Android Auto.
 */
@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var cacheDataSourceFactory: CacheDataSource.Factory

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                // handleAudioFocus = true: на входящий звонок плеер сам встанет
                // на паузу и сам продолжит после. Раньше звук шёл поверх звонка.
                /* handleAudioFocus = */ true,
            )
            // Выдернули наушники — пауза. Иначе звук резко уходит в динамик,
            // и это самый неприятный способ узнать, что ты в общественном месте.
            .setHandleAudioBecomingNoisy(true)
            .build()

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * Если пользователь смахнул приложение из недавних, а плеер стоит на паузе —
     * останавливаем сервис. Оставлять висеть уведомление паузы после того, как
     * приложение закрыли, — это мусор в шторке, который нечем убрать.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
