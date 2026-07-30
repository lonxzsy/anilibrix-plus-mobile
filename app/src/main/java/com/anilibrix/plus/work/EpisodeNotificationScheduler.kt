package com.anilibrix.plus.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<EpisodeCheckWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        // Напоминание о недосмотренном. Раз в сутки — сам воркер ещё раз
        // проверит, уместно ли сейчас показывать уведомление; сеть ему не
        // нужна, всё считается по локальной базе.
        val reminder = PeriodicWorkRequestBuilder<ResumeReminderWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RESUME_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            reminder
        )
    }

    companion object {
        private const val WORK_NAME = "episode_check_worker"
        private const val RESUME_WORK_NAME = "resume_reminder_worker"
    }
}
