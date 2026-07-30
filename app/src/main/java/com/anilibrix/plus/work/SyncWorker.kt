package com.anilibrix.plus.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anilibrix.plus.core.sync.SyncProcessor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Доносит до сервера всё, что накопилось в очереди, пока не было сети.
 *
 * `Result.retry()` при неполном разгребании существен: WorkManager сам
 * применит экспоненциальную задержку и дождётся сети по constraint'у, а мы не
 * будем крутить бессмысленный цикл запросов на плохом соединении.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncProcessor: SyncProcessor,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            syncProcessor.drain()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
