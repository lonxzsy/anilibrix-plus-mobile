package com.anilibrix.plus.core.torrent

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.anilibrix.plus.core.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TorrentDownloadService : Service() {

    @Inject
    lateinit var torrentDownloadManager: TorrentDownloadManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var observeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        observeTasks()
    }

    private fun startForegroundNotification() {
        val initialNotification = notificationHelper.buildTorrentProgressNotification(
            activeCount = 1,
            titleName = "Загрузка торрентов",
            progressPercent = 0,
            speedStr = "Подключение к пирам…"
        )
        startForeground(NOTIFICATION_ID, initialNotification)
    }

    private fun observeTasks() {
        observeJob?.cancel()
        observeJob = serviceScope.launch {
            torrentDownloadManager.activeTasks.collect { tasks ->
                val active = tasks.filter { it.state.isActive }
                if (active.isEmpty()) {
                    stopSelf()
                    return@collect
                }

                val totalPercent = if (active.isNotEmpty()) {
                    (active.map { it.progress }.average() * 100).toInt()
                } else 0

                val totalSpeedBytes = active.sumOf { it.downloadSpeedBytesPerSec }
                val speedStr = formatSpeed(totalSpeedBytes)
                val firstName = active.firstOrNull()?.torrentName ?: "Торрент"

                val notification = notificationHelper.buildTorrentProgressNotification(
                    activeCount = active.size,
                    titleName = firstName,
                    progressPercent = totalPercent,
                    speedStr = speedStr
                )
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        val mb = bytesPerSec / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format(Locale.getDefault(), "%.1f МБ/с", mb)
        } else {
            val kb = bytesPerSec / 1024.0
            String.format(Locale.getDefault(), "%.0f КБ/с", kb)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 5001

        fun start(context: Context) {
            val intent = Intent(context, TorrentDownloadService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TorrentDownloadService::class.java)
            context.stopService(intent)
        }
    }
}
