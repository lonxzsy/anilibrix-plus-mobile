package com.anilibrix.plus.core.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.anilibrix.plus.R
import com.anilibrix.plus.app.MainActivity
import com.anilibrix.plus.domain.usecase.NewEpisodeNotification
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun createChannels() {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel(
                NEW_EPISODES_CHANNEL_ID,
                "Новые серии",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о выходе новых серий отслеживаемых аниме"
            },
            NotificationChannel(
                APP_UPDATES_CHANNEL_ID,
                "Обновления приложения",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о новых версиях приложения"
            },
            NotificationChannel(
                SYNC_STATUS_CHANNEL_ID,
                "Синхронизация",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Статус фонового обновления базы и кэша"
            },
            NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Загрузки",
                // LOW: прогресс загрузки не должен звенеть и всплывать поверх
                // всего — это фоновая работа, а не событие.
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ход скачивания серий для просмотра офлайн"
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_TORRENTS,
                "Торренты",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ход скачивания торрент-раздач и серий"
                setShowBadge(false)
            },
            NotificationChannel(
                RESUME_CHANNEL_ID,
                "Напоминания о просмотре",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Напоминание о недосмотренной серии"
            },
        )
        channels.forEach(notificationManager::createNotificationChannel)
    }

    /**
     * Уведомление о новой серии.
     *
     * Ведёт **прямо в плеер** нужной серии, а не на главный экран, как раньше:
     * пройти от главной до конкретной серии — четыре тапа, и половина смысла
     * уведомления по дороге теряется.
     */
    fun showNewEpisode(notification: NewEpisodeNotification) {
        if (!canPostNotifications()) return

        val openTitle = contentIntent(
            titleId = notification.titleId,
            episodeId = null,
            requestCode = notification.titleId.toInt(),
        )
        val watchNow = notification.episodeId?.let { episodeId ->
            contentIntent(
                titleId = notification.titleId,
                episodeId = episodeId,
                requestCode = (WATCH_REQUEST_OFFSET + notification.titleId).toInt(),
            )
        }

        val builder = NotificationCompat.Builder(context, NEW_EPISODES_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notification.titleName)
            .setContentText("Вышла серия ${notification.episodeNumber}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${notification.titleName}: доступна серия ${notification.episodeNumber}")
            )
            .setContentIntent(watchNow ?: openTitle)
            .setAutoCancel(true)
            // Группировка: за ночь может выйти пять серий, и пять отдельных
            // строк в шторке читаются как спам.
            .setGroup(NEW_EPISODES_GROUP)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (watchNow != null) {
            builder.addAction(R.mipmap.ic_launcher, "Смотреть", watchNow)
        }

        NotificationManagerCompat.from(context).apply {
            notify(
                NEW_EPISODES_NOTIFICATION_BASE_ID + notification.titleId.toInt(),
                builder.build(),
            )
            notify(NEW_EPISODES_SUMMARY_ID, buildEpisodesSummary())
        }
    }

    /** Напоминание вернуться к недосмотренной серии. */
    fun showResumeReminder(titleId: Long, episodeId: Long, titleName: String, episodeNumber: Int, remainingMinutes: Long) {
        if (!canPostNotifications()) return

        val intent = contentIntent(
            titleId = titleId,
            episodeId = episodeId,
            requestCode = (RESUME_REQUEST_OFFSET + titleId).toInt(),
        )
        val notification = NotificationCompat.Builder(context, RESUME_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Продолжить «$titleName»")
            .setContentText(
                if (remainingMinutes > 0) {
                    "Серия $episodeNumber — осталось $remainingMinutes мин"
                } else {
                    "Серия $episodeNumber"
                }
            )
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(RESUME_NOTIFICATION_ID, notification)
    }

    /** Уведомление о доступной новой версии приложения. */
    fun showUpdateAvailable(version: String, notes: String?) {
        if (!canPostNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_UPDATE, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            UPDATE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, APP_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Доступна версия $version")
            .setContentText(notes?.lineSequence()?.firstOrNull()?.take(80) ?: "Нажмите, чтобы обновить")
            .setContentIntent(pending)
            .setAutoCancel(true)

        if (!notes.isNullOrBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(notes.take(500)))
        }

        NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, builder.build())
    }

    /** Итог фоновой синхронизации; показывается только когда есть что сказать. */
    fun showSyncResult(message: String) {
        if (!canPostNotifications()) return
        val notification = NotificationCompat.Builder(context, SYNC_STATUS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Синхронизация")
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(context).notify(SYNC_NOTIFICATION_ID, notification)
    }

    /**
     * Уведомление foreground-сервиса загрузок.
     *
     * Собирается вручную, а не через `DownloadNotificationHelper`: тот выдаёт
     * англоязычные строки и не умеет объяснить, почему загрузка стоит.
     * «Ожидает Wi-Fi» — единственное, что человеку в этот момент нужно знать.
     */
    @UnstableApi
    fun buildDownloadProgressNotification(downloads: List<Download>, notMetRequirements: Int): Notification {
        val active = downloads.filter {
            it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED
        }
        val totalPercent = active
            .map { it.percentDownloaded }
            .filter { it >= 0f }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toInt()

        val waiting = notMetRequirements != 0
        val text = when {
            waiting -> "Ожидание сети — загрузка продолжится автоматически"
            active.isEmpty() -> "Завершение…"
            active.size == 1 -> "Скачивается 1 серия"
            else -> "Скачивается серий: ${active.size}"
        }

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_DOWNLOADS, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            DOWNLOADS_REQUEST_CODE,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Загрузки")
            .setContentText(text)
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .apply {
                if (!waiting && totalPercent != null) {
                    setProgress(100, totalPercent, totalPercent <= 0)
                } else {
                    setProgress(0, 0, !waiting)
                }
            }
            .build()
    }

    fun buildTorrentProgressNotification(
        activeCount: Int,
        titleName: String,
        progressPercent: Int,
        speedStr: String
    ): Notification {
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_DOWNLOADS, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            TORRENT_REQUEST_CODE,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (activeCount > 1) "Скачивание торрентов ($activeCount)" else "Скачивание: $titleName"
        val subtitle = "$progressPercent% · $speedStr"

        return NotificationCompat.Builder(context, CHANNEL_TORRENTS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, progressPercent, progressPercent <= 0)
            .build()
    }

    private fun buildEpisodesSummary(): Notification =
        NotificationCompat.Builder(context, NEW_EPISODES_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Новые серии")
            .setGroup(NEW_EPISODES_GROUP)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

    private fun contentIntent(titleId: Long, episodeId: Long?, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TITLE_ID, titleId)
            if (episodeId != null) putExtra(EXTRA_EPISODE_ID, episodeId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val NEW_EPISODES_CHANNEL_ID = "new_episodes"
        const val APP_UPDATES_CHANNEL_ID = "app_updates"
        const val SYNC_STATUS_CHANNEL_ID = "sync_status"
        const val CHANNEL_DOWNLOADS = "downloads"
        const val CHANNEL_TORRENTS = "torrent_downloads"
        const val RESUME_CHANNEL_ID = "resume_reminder"

        const val EXTRA_TITLE_ID = "title_id"
        const val EXTRA_EPISODE_ID = "episode_id"
        const val EXTRA_OPEN_DOWNLOADS = "open_downloads"
        const val EXTRA_OPEN_UPDATE = "open_update"

        private const val NEW_EPISODES_GROUP = "anilibrix_new_episodes"
        private const val NEW_EPISODES_NOTIFICATION_BASE_ID = 20_000
        private const val NEW_EPISODES_SUMMARY_ID = 19_999
        private const val RESUME_NOTIFICATION_ID = 30_001
        private const val UPDATE_NOTIFICATION_ID = 30_002
        private const val SYNC_NOTIFICATION_ID = 30_003
        private const val UPDATE_REQUEST_CODE = 41_001
        private const val DOWNLOADS_REQUEST_CODE = 41_002
        private const val TORRENT_REQUEST_CODE = 41_003
        private const val WATCH_REQUEST_OFFSET = 100_000L
        private const val RESUME_REQUEST_OFFSET = 200_000L
    }
}
