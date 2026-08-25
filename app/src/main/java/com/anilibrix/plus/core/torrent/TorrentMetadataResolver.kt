package com.anilibrix.plus.core.torrent

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentMetadataResolver @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    data class ResolvedMetadata(
        val name: String,
        val infoHash: String?,
        val totalBytes: Long,
        val files: List<TorrentFileItem>,
        val trackers: List<String>
    )

    suspend fun resolve(magnetOrUrl: String, fallbackName: String = "Torrent"): ResolvedMetadata {
        return if (magnetOrUrl.startsWith("magnet:?", ignoreCase = true)) {
            parseMagnet(magnetOrUrl, fallbackName)
        } else if (magnetOrUrl.startsWith("http://") || magnetOrUrl.startsWith("https://")) {
            fetchTorrentFile(magnetOrUrl, fallbackName) ?: parseMagnet(magnetOrUrl, fallbackName)
        } else {
            ResolvedMetadata(
                name = fallbackName,
                infoHash = null,
                totalBytes = 0L,
                files = emptyList(),
                trackers = emptyList()
            )
        }
    }

    private fun parseMagnet(magnet: String, fallbackName: String): ResolvedMetadata {
        val params = magnet.substringAfter("?").split("&")
        var displayName = fallbackName
        var infoHash: String? = null
        val trackers = mutableListOf<String>()

        for (param in params) {
            val parts = param.split("=", limit = 2)
            if (parts.size != 2) continue
            val key = parts[0]
            val value = runCatching { URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name()) }.getOrDefault(parts[1])

            when (key) {
                "dn" -> displayName = value
                "xt" -> {
                    if (value.startsWith("urn:btih:", ignoreCase = true)) {
                        infoHash = value.substringAfter("urn:btih:").uppercase()
                    }
                }
                "tr" -> trackers.add(value)
            }
        }

        val parsedInfo = TorrentNameParser.parse(displayName)
        val files = listOf(
            TorrentFileItem(
                index = 0,
                path = displayName,
                name = displayName,
                sizeBytes = 0L,
                selected = true,
                episodeNumber = parsedInfo.episodeNumbers.firstOrNull()
            )
        )

        return ResolvedMetadata(
            name = displayName,
            infoHash = infoHash,
            totalBytes = 0L,
            files = files,
            trackers = trackers
        )
    }

    private fun fetchTorrentFile(url: String, fallbackName: String): ResolvedMetadata? {
        return runCatching {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            parseTorrentBytes(bytes, fallbackName)
        }.getOrNull()
    }

    fun parseTorrentBytes(bytes: ByteArray, fallbackName: String): ResolvedMetadata {
        val root = BencodeParser.parse(bytes) ?: return parseMagnet("", fallbackName)
        val info = root["info"] as? Map<*, *> ?: return parseMagnet("", fallbackName)

        val nameBytes = info["name"] as? ByteArray
        val name = nameBytes?.let { String(it, StandardCharsets.UTF_8) } ?: fallbackName
        val trackers = mutableListOf<String>()

        (root["announce"] as? ByteArray)?.let {
            trackers.add(String(it, StandardCharsets.UTF_8))
        }
        (root["announce-list"] as? List<*>)?.forEach { tier ->
            (tier as? List<*>)?.forEach { tr ->
                (tr as? ByteArray)?.let { trackers.add(String(it, StandardCharsets.UTF_8)) }
            }
        }

        val filesList = mutableListOf<TorrentFileItem>()
        var totalBytes = 0L

        val multiFiles = info["files"] as? List<*>
        if (multiFiles != null) {
            // Множество файлов в торренте (батч)
            var index = 0
            for (f in multiFiles) {
                val fMap = f as? Map<*, *> ?: continue
                val length = (fMap["length"] as? Long) ?: 0L
                val pathList = (fMap["path"] as? List<*>)?.mapNotNull {
                    (it as? ByteArray)?.let { b -> String(b, StandardCharsets.UTF_8) }
                } ?: emptyList()
                val fullPath = pathList.joinToString("/")
                val fileName = pathList.lastOrNull() ?: name

                val isMedia = fileName.endsWith(".mkv", ignoreCase = true) ||
                    fileName.endsWith(".mp4", ignoreCase = true) ||
                    fileName.endsWith(".avi", ignoreCase = true)

                val parsed = TorrentNameParser.parse(fileName)
                filesList.add(
                    TorrentFileItem(
                        index = index++,
                        path = fullPath,
                        name = fileName,
                        sizeBytes = length,
                        selected = isMedia,
                        episodeNumber = parsed.episodeNumbers.firstOrNull()
                    )
                )
                totalBytes += length
            }
        } else {
            // Одиночный файл
            val length = (info["length"] as? Long) ?: 0L
            val parsed = TorrentNameParser.parse(name)
            filesList.add(
                TorrentFileItem(
                    index = 0,
                    path = name,
                    name = name,
                    sizeBytes = length,
                    selected = true,
                    episodeNumber = parsed.episodeNumbers.firstOrNull()
                )
            )
            totalBytes = length
        }

        return ResolvedMetadata(
            name = name,
            infoHash = null,
            totalBytes = totalBytes,
            files = filesList,
            trackers = trackers
        )
    }
}
