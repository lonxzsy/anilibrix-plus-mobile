package com.anilibrix.plus.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.anilibrix.plus.app.di.ApplicationScope
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.core.notifications.NotificationHelper
import com.anilibrix.plus.work.EpisodeNotificationScheduler
import com.anilibrix.plus.work.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AnilibrixApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var episodeNotificationScheduler: EpisodeNotificationScheduler

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
        episodeNotificationScheduler.schedule()
        syncScheduler.schedulePeriodic()
        // Разгребаем очередь сразу на старте: действия, сделанные офлайн в
        // прошлой сессии, доедут до сервера, как только появится сеть.
        syncScheduler.syncNow()

        applicationScope.launch {
            // Токен, записанный предыдущими версиями, лежит открытым текстом.
            // Перешифровываем один раз, молча — для пользователя ничего не
            // меняется, сессия не рвётся.
            settingsDataStore.migrateAuthTokenIfNeeded()
        }
    }
}
