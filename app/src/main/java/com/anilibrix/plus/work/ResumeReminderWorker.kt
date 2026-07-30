package com.anilibrix.plus.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.core.notifications.NotificationHelper
import com.anilibrix.plus.domain.model.EpisodeProgress
import com.anilibrix.plus.domain.repository.LocalRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Напоминание вернуться к недосмотренной серии.
 *
 * Смысл — не в том, чтобы вернуть человека любой ценой, а в том, чтобы он не
 * забыл про сериал, который сам начал. Поэтому ограничений три и все жёсткие:
 * не чаще раза в сутки, только если серия брошена больше суток назад, и не
 * ночью. Всё это отключается одним тумблером в профиле.
 */
@HiltWorker
class ResumeReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val localRepository: LocalRepository,
    private val settings: SettingsDataStore,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!settings.notificationsResumeEnabled.first()) return Result.success()
            if (settings.notificationsQuietHours.first() && isQuietHour()) return Result.success()

            val now = System.currentTimeMillis()
            val lastReminder = settings.lastResumeReminderAt.first()
            if (now - lastReminder < MIN_INTERVAL_MS) return Result.success()

            val candidate = localRepository.getContinueWatching().first()
                .filter { entry ->
                    val fraction = if (entry.duration > 0) {
                        entry.timestamp.toFloat() / entry.duration
                    } else {
                        0f
                    }
                    fraction > EpisodeProgress.STARTED_THRESHOLD &&
                        fraction < EpisodeProgress.WATCHED_THRESHOLD &&
                        // Сутки без возврата: напоминать через час после того,
                        // как человек отложил серию, — навязчиво.
                        now - entry.watchedAt > STALE_AFTER_MS
                }
                .maxByOrNull { it.watchedAt }
                ?: return Result.success()

            notificationHelper.showResumeReminder(
                titleId = candidate.titleId,
                episodeId = candidate.episodeId,
                titleName = candidate.titleName.ifBlank { "Тайтл #${candidate.titleId}" },
                episodeNumber = candidate.episodeNumber,
                remainingMinutes = (candidate.duration - candidate.timestamp).coerceAtLeast(0L) / 60_000L,
            )
            settings.setLastResumeReminderAt(now)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun isQuietHour(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= QUIET_FROM_HOUR || hour < QUIET_TO_HOUR
    }

    private companion object {
        val MIN_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)
        val STALE_AFTER_MS = TimeUnit.DAYS.toMillis(1)
        const val QUIET_FROM_HOUR = 23
        const val QUIET_TO_HOUR = 8
    }
}
