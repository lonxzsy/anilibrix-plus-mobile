package com.anilibrix.plus.core.download

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.anilibrix.plus.app.di.ApplicationScope
import com.anilibrix.plus.app.di.DownloadCache
import com.anilibrix.plus.core.database.dao.DownloadDao
import com.anilibrix.plus.core.database.entity.DownloadEntity
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.domain.model.Episode
import com.anilibrix.plus.domain.model.Title
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Загрузки как одно целое.
 *
 * Источник правды по прогрессу и статусу — `DownloadIndex` внутри Media3.
 * Room хранит только то, чего Media3 знать не может: название тайтла, постер,
 * номер серии. Дублировать сюда состояние — верный способ получить
 * рассинхрон, а рассинхрон в загрузках человек читает как «файл пропал».
 */
@UnstableApi
@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: DownloadManager,
    private val downloadDao: DownloadDao,
    private val settings: SettingsDataStore,
    @DownloadCache private val downloadCache: SimpleCache,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    /**
     * Состояние всех загрузок из Media3.
     *
     * Пересобирается по событиям менеджера, а не по таймеру: опрос раз в
     * секунду грел бы процессор, когда ничего не качается, и всё равно
     * отставал бы во время активной загрузки.
     */
    private val media3Downloads: Flow<List<Download>> = callbackFlow {
        // Слушатель DownloadManager обязан регистрироваться на том же потоке,
        // на котором менеджер создан, — то есть на главном. Без flowOn(Main)
        // подписка происходила бы на диспетчере собирающего, и открытие
        // экрана «Загрузки» падало бы или молча теряло события.
        val listener = object : DownloadManager.Listener {
            override fun onInitialized(manager: DownloadManager) = emitFullIndex()

            override fun onDownloadChanged(
                manager: DownloadManager,
                download: Download,
                finalException: Exception?,
            ) = emitFullIndex()

            override fun onDownloadRemoved(manager: DownloadManager, download: Download) {
                emitFullIndex()
                // Осиротевшие метаданные чистим здесь же: иначе экран
                // «Загрузки» показывал бы строки без файлов.
                applicationScope.launch { downloadDao.delete(download.request.id) }
            }

            override fun onIdle(manager: DownloadManager) = emitFullIndex()

            /**
             * Полный список, а не только активные: завершённые загрузки в
             * `currentDownloads` не попадают, а показать их надо.
             *
             * Чтение индекса — обращение к базе, поэтому уводим его с главного
             * потока.
             */
            fun emitFullIndex() {
                applicationScope.launch(Dispatchers.IO) {
                    val downloads = runCatching {
                        downloadManager.downloadIndex.getDownloads().use { cursor ->
                            buildList { while (cursor.moveToNext()) add(cursor.download) }
                        }
                    }.getOrElse { emptyList() }
                    trySend(downloads)
                }
            }
        }

        listener.emitFullIndex()
        downloadManager.addListener(listener)
        awaitClose { downloadManager.removeListener(listener) }
    }.flowOn(Dispatchers.Main)

    val summary: Flow<DownloadSummary> = combine(
        media3Downloads,
        downloadDao.getAll(),
    ) { downloads, metadata ->
        val byId = metadata.associateBy { it.requestId }
        val items = downloads.mapNotNull { download ->
            val meta = byId[download.request.id] ?: return@mapNotNull null
            download.toItem(meta)
        }.sortedByDescending { it.createdAt }

        DownloadSummary(items = items, usedBytes = downloadCache.cacheSpace)
    }

    fun observeForTitle(titleId: Long): Flow<Map<Long, DownloadItem>> =
        summary.map { s -> s.items.filter { it.titleId == titleId }.associateBy { it.episodeId } }

    /**
     * Ставит серию в очередь.
     *
     * Требования к сети берём из настроек прямо здесь: если человек попросил
     * качать только по Wi-Fi, а мы поставим загрузку без ограничения, она
     * съест мобильный трафик молча.
     */
    suspend fun enqueue(title: Title, episode: Episode, quality: String? = null) {
        val preferredQuality = quality ?: settings.downloadQuality.first()
        val url = episode.urlFor(preferredQuality) ?: return
        val requestId = requestId(title.id, episode.id)

        downloadDao.insert(
            DownloadEntity(
                requestId = requestId,
                titleId = title.id,
                titleName = title.name.main,
                posterUrl = title.poster?.cardUrl,
                episodeId = episode.id,
                releaseEpisodeId = episode.releaseEpisodeId,
                episodeNumber = episode.ordinal,
                episodeName = episode.name,
                quality = preferredQuality,
                durationMs = episode.duration * 1000L,
            )
        )

        applyRequirements()

        val request = DownloadRequest.Builder(requestId, android.net.Uri.parse(url))
            .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            .build()

        DownloadService.sendAddDownload(
            context,
            AnilibrixDownloadService::class.java,
            request,
            /* foreground = */ false,
        )
    }

    suspend fun enqueueAll(title: Title, episodes: List<Episode>) {
        episodes.forEach { enqueue(title, it) }
    }

    fun remove(requestId: String) {
        DownloadService.sendRemoveDownload(
            context,
            AnilibrixDownloadService::class.java,
            requestId,
            /* foreground = */ false,
        )
    }

    fun removeAll() {
        DownloadService.sendRemoveAllDownloads(
            context,
            AnilibrixDownloadService::class.java,
            /* foreground = */ false,
        )
    }

    /** Пауза и возобновление всех загрузок разом. */
    fun setPaused(paused: Boolean) {
        DownloadService.sendSetStopReason(
            context,
            AnilibrixDownloadService::class.java,
            /* id = */ null,
            if (paused) STOP_REASON_MANUAL else Download.STOP_REASON_NONE,
            /* foreground = */ false,
        )
    }

    fun resume(requestId: String) {
        DownloadService.sendSetStopReason(
            context,
            AnilibrixDownloadService::class.java,
            requestId,
            Download.STOP_REASON_NONE,
            /* foreground = */ false,
        )
    }

    /**
     * Переносит ограничение «только Wi-Fi» в сам DownloadManager.
     *
     * Через `Requirements`, а не собственной проверкой сети: тогда система
     * сама приостановит загрузку при переходе на мобильную сеть и сама же
     * возобновит по возвращении Wi-Fi, в том числе когда приложение закрыто.
     */
    suspend fun applyRequirements() {
        val wifiOnly = settings.downloadWifiOnly.first()
        DownloadService.sendSetRequirements(
            context,
            AnilibrixDownloadService::class.java,
            AnilibrixDownloadService.requirements(wifiOnly),
            /* foreground = */ false,
        )
    }

    suspend fun usedBytes(): Long = downloadCache.cacheSpace

    private fun Download.toItem(meta: DownloadEntity): DownloadItem {
        val percent = percentDownloaded.takeIf { it >= 0f } ?: 0f
        return DownloadItem(
            requestId = request.id,
            titleId = meta.titleId,
            titleName = meta.titleName,
            posterUrl = meta.posterUrl,
            episodeId = meta.episodeId,
            episodeNumber = meta.episodeNumber,
            episodeName = meta.episodeName,
            quality = meta.quality,
            state = DownloadState.fromMedia3(state, stopReason),
            progress = (percent / 100f).coerceIn(0f, 1f),
            downloadedBytes = bytesDownloaded,
            totalBytes = contentLength.takeIf { it > 0L },
            createdAt = meta.createdAt,
        )
    }

    private fun Episode.urlFor(quality: String): String? = when (quality) {
        "480" -> hls480 ?: hls720 ?: hls1080
        "1080" -> hls1080 ?: hls720 ?: hls480
        else -> hls720 ?: hls1080 ?: hls480
    }

    companion object {
        /** Стабильный id: повторная постановка той же серии не плодит дубли. */
        fun requestId(titleId: Long, episodeId: Long): String = "$titleId:$episodeId"

        /** Причина остановки «пользователь нажал паузу» — любое ненулевое значение. */
        const val STOP_REASON_MANUAL = 1
    }
}
