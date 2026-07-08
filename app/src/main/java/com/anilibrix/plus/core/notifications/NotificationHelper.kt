package com.anilibrix.plus.core.notifications

import android.Manifest
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
                description = "Уведомления о выходе новых серий избранных аниме"
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
            }
        )
        channels.forEach(notificationManager::createNotificationChannel)
    }

    fun showNewEpisode(notification: NewEpisodeNotification) {
        if (!canPostNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TITLE_ID, notification.titleId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.titleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val androidNotification = NotificationCompat.Builder(context, NEW_EPISODES_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Новая серия вышла")
            .setContentText("${notification.titleName}: серия ${notification.episodeNumber}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${notification.titleName}: доступна серия ${notification.episodeNumber}")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(
            NEW_EPISODES_NOTIFICATION_BASE_ID + notification.titleId.toInt(),
            androidNotification
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
        const val EXTRA_TITLE_ID = "title_id"
        private const val NEW_EPISODES_NOTIFICATION_BASE_ID = 20_000
    }
}
