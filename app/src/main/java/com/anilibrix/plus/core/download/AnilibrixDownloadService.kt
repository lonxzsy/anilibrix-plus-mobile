package com.anilibrix.plus.core.download

import android.app.Notification
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.media3.exoplayer.scheduler.Scheduler
import androidx.media3.exoplayer.workmanager.WorkManagerScheduler
import com.anilibrix.plus.R
import com.anilibrix.plus.core.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground-сервис загрузок.
 *
 * Media3 требует именно сервис: без него система убьёт процесс, как только
 * приложение уйдёт в фон, и загрузка оборвётся на середине — ровно тогда,
 * когда человек её и оставляет.
 *
 * [WorkManagerScheduler] возобновляет прерванные загрузки после перезагрузки
 * телефона и при появлении подходящей сети.
 */
@UnstableApi
@AndroidEntryPoint
class AnilibrixDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    NotificationHelper.CHANNEL_DOWNLOADS,
    R.string.download_channel_name,
    0,
) {

    // Имя намеренно не `downloadManager`: геттер поля столкнулся бы по JVM-
    // сигнатуре с `getDownloadManager()` из DownloadService.
    @Inject
    lateinit var injectedDownloadManager: DownloadManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun getDownloadManager(): DownloadManager = injectedDownloadManager

    override fun getScheduler(): Scheduler = WorkManagerScheduler(this, WORK_NAME)

    override fun getForegroundNotification(
        downloads: List<Download>,
        notMetRequirements: Int,
    ): Notification = notificationHelper.buildDownloadProgressNotification(downloads, notMetRequirements)

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 4001
        private const val WORK_NAME = "anilibrix_downloads"

        /** Требования к сети: только Wi-Fi или любая сеть. */
        fun requirements(wifiOnly: Boolean): Requirements = Requirements(
            if (wifiOnly) {
                Requirements.NETWORK_UNMETERED
            } else {
                Requirements.NETWORK
            }
        )

        const val NOTIFICATION_IMPORTANCE = NotificationUtil.IMPORTANCE_LOW
    }
}
