package com.anilibrix.plus.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anilibrix.plus.core.notifications.NotificationHelper
import com.anilibrix.plus.domain.usecase.CheckNewEpisodesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EpisodeCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val checkNewEpisodesUseCase: CheckNewEpisodesUseCase,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            notificationHelper.createChannels()
            checkNewEpisodesUseCase().forEach(notificationHelper::showNewEpisode)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
