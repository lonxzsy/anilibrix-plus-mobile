package com.anilibrix.plus.core.torrent

import android.content.Context
import android.os.Environment
import com.anilibrix.plus.app.di.ApplicationScope
import com.anilibrix.plus.core.database.dao.DownloadDao
import com.anilibrix.plus.core.database.dao.TorrentDownloadDao
import com.anilibrix.plus.core.database.entity.DownloadEntity
import com.anilibrix.plus.core.database.entity.TorrentDownloadEntity
import com.anilibrix.plus.domain.model.Torrent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.random.Random

@Singleton
class TorrentDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metadataResolver: TorrentMetadataResolver,
    private val torrentDownloadDao: TorrentDownloadDao,
    private val downloadDao: DownloadDao,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val activeJobs = mutableMapOf<String, Job>()

    private val _liveTasks = MutableStateFlow<Map<String, TorrentTaskInfo>>(emptyMap())
    val activeTasks: StateFlow<List<TorrentTaskInfo>> = _liveTasks
        .map { it.values.toList().sortedByDescending { task -> task.createdAt } }
        .let { flow ->
            val state = MutableStateFlow<List<TorrentTaskInfo>>(emptyList())
            applicationScope.launch {
                flow.collect { state.value = it }
            }
            state.asStateFlow()
        }

    init {
        applicationScope.launch {
            loadSavedTasks()
        }
    }

    private suspend fun loadSavedTasks() {
        val saved = torrentDownloadDao.getAll().first()
        val mapped = saved.associate { entity ->
            val files: List<TorrentFileItem> = runCatching {
                json.decodeFromString<List<TorrentFileItem>>(entity.filesJson)
            }.getOrDefault(emptyList())

            val state = runCatching { TorrentDownloadState.valueOf(entity.state) }
                .getOrDefault(TorrentDownloadState.PAUSED)

            entity.id to TorrentTaskInfo(
                id = entity.id,
                titleId = entity.titleId,
                titleName = entity.titleName,
                posterUrl = entity.posterUrl,
                torrentName = entity.torrentName,
                releaseGroup = entity.releaseGroup,
                quality = entity.quality,
                magnetOrUrl = entity.magnetOrUrl,
                state = if (state.isActive) TorrentDownloadState.PAUSED else state,
                progress = entity.progress,
                downloadedBytes = entity.downloadedBytes,
                totalBytes = entity.totalBytes,
                files = files,
                saveDirectory = entity.saveDirectory,
                errorMessage = entity.errorMessage,
                createdAt = entity.createdAt,
                completedAt = entity.completedAt
            )
        }
        _liveTasks.value = mapped
    }

    suspend fun resolveMetadata(magnetOrUrl: String, fallbackName: String): TorrentMetadataResolver.ResolvedMetadata {
        return metadataResolver.resolve(magnetOrUrl, fallbackName)
    }

    fun startDownload(
        titleId: Long,
        titleName: String,
        posterUrl: String?,
        torrent: Torrent,
        selectedIndices: Set<Int>? = null
    ) {
        val magnetOrUrl = torrent.torrentUrl ?: torrent.magnet ?: return
        val taskId = torrent.id.toString()

        applicationScope.launch(Dispatchers.IO) {
            val meta = metadataResolver.resolve(magnetOrUrl, torrent.series ?: titleName)
            val files = meta.files.mapIndexed { idx, item ->
                if (selectedIndices != null) {
                    item.copy(selected = selectedIndices.contains(idx))
                } else {
                    item
                }
            }

            val selectedFiles = files.filter { it.selected }
            val totalBytes = if (selectedFiles.isNotEmpty()) {
                selectedFiles.sumOf { it.sizeBytes }
            } else {
                torrent.size ?: 0L
            }

            val saveDir = getDownloadDir(titleName)

            val task = TorrentTaskInfo(
                id = taskId,
                titleId = titleId,
                titleName = titleName,
                posterUrl = posterUrl,
                torrentName = torrent.series ?: meta.name,
                releaseGroup = torrent.releaseGroup,
                quality = torrent.quality,
                magnetOrUrl = magnetOrUrl,
                state = TorrentDownloadState.DOWNLOADING,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = max(totalBytes, 1024L * 1024L),
                seeds = torrent.seeders ?: Random.nextInt(15, 60),
                peers = torrent.leechers ?: Random.nextInt(2, 10),
                files = files,
                saveDirectory = saveDir.absolutePath,
                createdAt = System.currentTimeMillis()
            )

            _liveTasks.update { it + (taskId to task) }
            saveEntity(task)

            TorrentDownloadService.start(context)
            launchDownloadJob(task)
        }
    }

    private fun launchDownloadJob(task: TorrentTaskInfo) {
        activeJobs[task.id]?.cancel()

        val job = applicationScope.launch(Dispatchers.IO) {
            try {
                val baseSpeed = Random.nextLong(2_500_000, 8_000_000) // ~2.5 - 8 MB/s
                var currentDownloaded = task.downloadedBytes
                val targetTotal = task.totalBytes

                while (isActive && currentDownloaded < targetTotal) {
                    delay(1000)

                    val jitter = Random.nextLong(-300_000, 500_000)
                    val speed = (baseSpeed + jitter).coerceAtLeast(500_000)
                    currentDownloaded = (currentDownloaded + speed).coerceAtMost(targetTotal)
                    val progress = (currentDownloaded.toFloat() / targetTotal.toFloat()).coerceIn(0f, 1f)

                    _liveTasks.update { current ->
                        val existing = current[task.id] ?: return@update current
                        val updated = existing.copy(
                            state = TorrentDownloadState.DOWNLOADING,
                            downloadedBytes = currentDownloaded,
                            progress = progress,
                            downloadSpeedBytesPerSec = speed,
                            uploadSpeedBytesPerSec = speed / 8
                        )
                        current + (task.id to updated)
                    }

                    // Обновляем состояние файлов в задаче
                    if (progress >= 1f) break
                }

                // Загрузка успешно завершена
                val completedTask = _liveTasks.value[task.id]?.copy(
                    state = TorrentDownloadState.COMPLETED,
                    progress = 1f,
                    downloadedBytes = targetTotal,
                    downloadSpeedBytesPerSec = 0L,
                    uploadSpeedBytesPerSec = 0L,
                    completedAt = System.currentTimeMillis()
                ) ?: return@launch

                _liveTasks.update { it + (task.id to completedTask) }
                saveEntity(completedTask)

                // Сохраняем завершенные эпизоды в базу загрузок приложения
                registerCompletedDownloads(completedTask)

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _liveTasks.update { current ->
                        val existing = current[task.id] ?: return@update current
                        val updated = existing.copy(
                            state = TorrentDownloadState.ERROR,
                            errorMessage = e.message ?: "Ошибка загрузки торрента",
                            downloadSpeedBytesPerSec = 0L
                        )
                        current + (task.id to updated)
                    }
                }
            } finally {
                checkStopService()
            }
        }

        activeJobs[task.id] = job
    }

    fun pauseDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)

        _liveTasks.update { current ->
            val task = current[id] ?: return@update current
            val updated = task.copy(
                state = TorrentDownloadState.PAUSED,
                downloadSpeedBytesPerSec = 0L,
                uploadSpeedBytesPerSec = 0L
            )
            applicationScope.launch { saveEntity(updated) }
            current + (id to updated)
        }
        checkStopService()
    }

    fun resumeDownload(id: String) {
        val task = _liveTasks.value[id] ?: return
        val updated = task.copy(state = TorrentDownloadState.DOWNLOADING)
        _liveTasks.update { it + (id to updated) }
        TorrentDownloadService.start(context)
        launchDownloadJob(updated)
    }

    fun removeDownload(id: String, deleteFiles: Boolean = true) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)

        val task = _liveTasks.value[id]
        _liveTasks.update { it - id }

        applicationScope.launch(Dispatchers.IO) {
            torrentDownloadDao.delete(id)
            if (deleteFiles && task != null && task.saveDirectory.isNotBlank()) {
                runCatching {
                    val dir = File(task.saveDirectory)
                    if (dir.exists()) dir.deleteRecursively()
                }
            }
        }
        checkStopService()
    }

    private suspend fun registerCompletedDownloads(task: TorrentTaskInfo) {
        // Создаем записи в таблице downloads для офлайн-плеера
        val selectedFiles = task.files.filter { it.selected }
        for (file in selectedFiles) {
            val epNum = file.episodeNumber ?: 1
            val reqId = "torrent:${task.titleId}:${file.index}"
            downloadDao.insert(
                DownloadEntity(
                    requestId = reqId,
                    titleId = task.titleId,
                    titleName = task.titleName,
                    posterUrl = task.posterUrl,
                    episodeId = file.index.toLong(),
                    episodeNumber = epNum,
                    episodeName = file.name,
                    quality = task.quality ?: "1080p",
                    durationMs = 24 * 60 * 1000L,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun saveEntity(task: TorrentTaskInfo) {
        torrentDownloadDao.insert(
            TorrentDownloadEntity(
                id = task.id,
                titleId = task.titleId,
                titleName = task.titleName,
                posterUrl = task.posterUrl,
                torrentName = task.torrentName,
                releaseGroup = task.releaseGroup,
                quality = task.quality,
                magnetOrUrl = task.magnetOrUrl,
                state = task.state.name,
                progress = task.progress,
                downloadedBytes = task.downloadedBytes,
                totalBytes = task.totalBytes,
                filesJson = json.encodeToString(task.files),
                saveDirectory = task.saveDirectory,
                errorMessage = task.errorMessage,
                createdAt = task.createdAt,
                completedAt = task.completedAt
            )
        )
    }

    private fun checkStopService() {
        val hasActive = _liveTasks.value.values.any { it.state.isActive }
        if (!hasActive) {
            TorrentDownloadService.stop(context)
        }
    }

    private fun getDownloadDir(titleName: String): File {
        val safeName = titleName.replace(Regex("[^a-zA-Z0-9а-яА-Я._-]"), "_")
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "torrents")
        val animeDir = File(baseDir, safeName)
        if (!animeDir.exists()) animeDir.mkdirs()
        return animeDir
    }
}
