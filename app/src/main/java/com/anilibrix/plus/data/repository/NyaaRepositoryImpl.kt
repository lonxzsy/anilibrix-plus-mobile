package com.anilibrix.plus.data.repository

import com.anilibrix.plus.data.remote.api.NyaaApi
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.Torrent
import com.anilibrix.plus.domain.repository.NyaaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NyaaRepositoryImpl @Inject constructor(
    private val api: NyaaApi
) : NyaaRepository {

    override fun searchTorrents(query: String): Flow<NetworkResult<List<Torrent>>> = flow {
        emit(NetworkResult.Loading)
        if (query.isBlank()) {
            emit(NetworkResult.Success(emptyList()))
            return@flow
        }
        try {
            val responseBody = api.searchRss(query = query)
            val xmlString = responseBody.string()
            val torrents = parseNyaaRss(xmlString)
            emit(NetworkResult.Success(torrents))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Ошибка поиска торрентов Nyaa", e))
        }
    }

    private fun parseNyaaRss(xml: String): List<Torrent> {
        val torrents = mutableListOf<Torrent>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inItem = false
            var currentTitle: String? = null
            var currentSeeders: Int? = null
            var currentLeechers: Int? = null
            var currentSize: String? = null
            var currentMagnet: String? = null
            var currentGuid: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            inItem = true
                            currentTitle = null
                            currentSeeders = null
                            currentLeechers = null
                            currentSize = null
                            currentMagnet = null
                            currentGuid = null
                        } else if (inItem) {
                            when {
                                tagName.equals("title", ignoreCase = true) -> currentTitle = parser.nextText()
                                tagName.equals("seeders", ignoreCase = true) -> currentSeeders = parser.nextText().toIntOrNull()
                                tagName.equals("leechers", ignoreCase = true) -> currentLeechers = parser.nextText().toIntOrNull()
                                tagName.equals("size", ignoreCase = true) -> currentSize = parser.nextText()
                                tagName.equals("guid", ignoreCase = true) -> currentGuid = parser.nextText()
                                tagName.equals("link", ignoreCase = true) -> {
                                    val text = parser.nextText()
                                    if (text.startsWith("magnet:?")) {
                                        currentMagnet = text
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            inItem = false
                            if (currentTitle != null) {
                                val quality = extractQuality(currentTitle)
                                val id = (currentGuid ?: currentTitle).hashCode().toLong().let { if (it < 0) -it else it }
                                torrents.add(
                                    Torrent(
                                        id = id,
                                        quality = quality,
                                        series = currentTitle,
                                        size = parseSizeToBytes(currentSize),
                                        magnet = currentMagnet ?: "",
                                        seeders = currentSeeders ?: 0,
                                        leechers = currentLeechers ?: 0
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return torrents
    }

    private fun extractQuality(title: String): String {
        return when {
            title.contains("1080p", ignoreCase = true) -> "1080p"
            title.contains("720p", ignoreCase = true) -> "720p"
            title.contains("480p", ignoreCase = true) -> "480p"
            title.contains("2160p", ignoreCase = true) || title.contains("4K", ignoreCase = true) -> "4K"
            else -> "HD"
        }
    }

    private fun parseSizeToBytes(sizeStr: String?): Long {
        if (sizeStr.isNullOrBlank()) return 0L
        val parts = sizeStr.trim().split(" ")
        if (parts.size < 2) return 0L
        val num = parts[0].toDoubleOrNull() ?: return 0L
        val unit = parts[1].uppercase()
        return when {
            unit.startsWith("G") -> (num * 1024 * 1024 * 1024).toLong()
            unit.startsWith("M") -> (num * 1024 * 1024).toLong()
            unit.startsWith("K") -> (num * 1024).toLong()
            else -> num.toLong()
        }
    }
}
