package com.anilibrix.plus.core.torrent

import java.util.regex.Pattern

/**
 * Структурированные данные о торрент-раздаче после умного парсинга.
 */
data class ParsedTorrentInfo(
    val rawTitle: String,
    val releaseGroup: String?,
    val cleanTitle: String,
    val episodeLabel: String,
    val episodeNumbers: List<Int>,
    val isBatch: Boolean,
    val quality: String,
    val videoCodec: String?,
    val audioInfo: String?,
    val subtitleInfo: String?
)

/**
 * Умный парсер названий торрентов с Nyaa.si, AniLibria и других трекеров.
 */
object TorrentNameParser {

    private val GROUP_REGEX = Pattern.compile("^\\[([^]]+)]|^\\(([^)]+)\\)")
    private val QUALITY_REGEX = Pattern.compile("(?i)\\b(2160p|4k|1080p|720p|480p|360p|576p)\\b")
    private val CODEC_REGEX = Pattern.compile("(?i)\\b(hevc|x265|h265|x264|h264|av1|xvid|10-?bit|8-?bit)\\b")
    private val AUDIO_REGEX = Pattern.compile("(?i)\\b(dual-?audio|multi-?audio|flac|aac|ddp?5\\.1|ac3|e-ac3|opus|dts|mp3)\\b")
    private val SUB_REGEX = Pattern.compile("(?i)\\b(multi-?sub|softsubs?|hardsubs?|eng|rus|spa|por|ita|ger|fre|ara)\\b")
    private val BATCH_KEYWORDS = listOf("batch", "complete", "season", "сезон", "полный", "vol", "vols", "box")

    // Регулярные выражения для поиска серий
    private val EPISODE_RANGE_REGEX = Pattern.compile("(?i)(?:[-_\\s]|ep|e|eps|series|сери[ия])\\s*(\\d{1,4})\\s*(?:-|~|to|—|–)\\s*(\\d{1,4})")
    private val SINGLE_EPISODE_REGEX = Pattern.compile("(?i)(?:[-_\\s]|^)(?:ep|e|eps|series|серия|#)?\\s*(\\d{1,4})(?:v\\d+)?(?=\\s|\\(|\\[|\\.|\$|_)")
    private val SEASON_REGEX = Pattern.compile("(?i)(?:season|s|сезон)\\s*(\\d{1,2})")

    fun parse(rawTitle: String): ParsedTorrentInfo {
        var text = rawTitle.trim()

        // 1. Извлечение релиз-группы
        var releaseGroup: String? = null
        val groupMatcher = GROUP_REGEX.matcher(text)
        if (groupMatcher.find()) {
            val groupCandidate = (groupMatcher.group(1) ?: groupMatcher.group(2))?.trim()
            if (!groupCandidate.isNullOrBlank() && !isQualityOrCodec(groupCandidate)) {
                releaseGroup = groupCandidate
                text = text.substring(groupMatcher.end()).trim()
            }
        }

        // 2. Определение качества
        val quality = extractQuality(rawTitle)

        // 3. Определение видеокодека
        val videoCodec = extractVideoCodec(rawTitle)

        // 4. Определение аудио
        val audioInfo = extractAudio(rawTitle)

        // 5. Определение субтитров
        val subtitleInfo = extractSubtitles(rawTitle)

        // 6. Определение серий / батча
        val isBatchWordPresent = BATCH_KEYWORDS.any { rawTitle.contains(it, ignoreCase = true) }
        val rangeMatcher = EPISODE_RANGE_REGEX.matcher(rawTitle)
        val episodeNumbers = mutableListOf<Int>()
        var episodeLabel = "Полный релиз"
        var isBatch = isBatchWordPresent

        if (rangeMatcher.find()) {
            val start = rangeMatcher.group(1)?.toIntOrNull() ?: 1
            val end = rangeMatcher.group(2)?.toIntOrNull() ?: start
            if (start <= end && end - start < 2000) {
                for (i in start..end) episodeNumbers.add(i)
                isBatch = true
                episodeLabel = "Серии ${formatNumber(start)}-${formatNumber(end)} (Пакет)"
            }
        } else {
            // Поиск одиночной серии
            val singleMatcher = SINGLE_EPISODE_REGEX.matcher(text)
            var foundEp: Int? = null
            while (singleMatcher.find()) {
                val numStr = singleMatcher.group(1)
                val num = numStr?.toIntOrNull()
                // Фильтрация годов (например, 2024, 2025), разрешений (1080, 720, 480) и битрейтов
                if (num != null && num !in 1970..2040 && num !in listOf(1080, 720, 480, 2160, 360, 576, 264, 265)) {
                    foundEp = num
                    break
                }
            }

            if (foundEp != null) {
                episodeNumbers.add(foundEp)
                episodeLabel = "Серия ${formatNumber(foundEp)}"
            } else if (isBatchWordPresent) {
                isBatch = true
                val seasonMatch = SEASON_REGEX.matcher(rawTitle)
                episodeLabel = if (seasonMatch.find()) {
                    "Сезон ${seasonMatch.group(1)} (Пакет)"
                } else {
                    "Весь сезон (Пакет)"
                }
            } else if (rawTitle.contains("Movie", ignoreCase = true) || rawTitle.contains("Фильм", ignoreCase = true)) {
                episodeLabel = "Фильм"
            } else if (rawTitle.contains("OVA", ignoreCase = true) || rawTitle.contains("OAD", ignoreCase = true)) {
                episodeLabel = "OVA"
            }
        }

        // 7. Очистка названия тайтла
        val cleanTitle = cleanAnimeTitle(rawTitle, releaseGroup)

        return ParsedTorrentInfo(
            rawTitle = rawTitle,
            releaseGroup = releaseGroup,
            cleanTitle = cleanTitle,
            episodeLabel = episodeLabel,
            episodeNumbers = episodeNumbers,
            isBatch = isBatch,
            quality = quality,
            videoCodec = videoCodec,
            audioInfo = audioInfo,
            subtitleInfo = subtitleInfo
        )
    }

    private fun extractQuality(title: String): String {
        val matcher = QUALITY_REGEX.matcher(title)
        if (matcher.find()) {
            val q = matcher.group(1)?.lowercase().orEmpty()
            return when {
                q == "2160p" || q == "4k" -> "4K"
                q == "1080p" -> "1080p"
                q == "720p" -> "720p"
                q == "480p" -> "480p"
                q.isNotBlank() -> q.uppercase()
                else -> "HD"
            }
        }
        return if (title.contains("BD", ignoreCase = true) || title.contains("BDRip", ignoreCase = true)) "1080p (BD)" else "HD"
    }

    private fun extractVideoCodec(title: String): String? {
        val found = mutableListOf<String>()
        val matcher = CODEC_REGEX.matcher(title)
        while (matcher.find()) {
            val c = matcher.group(1)?.uppercase()
            if (c != null && !found.contains(c)) found.add(c)
        }
        return if (found.isNotEmpty()) found.joinToString(" ") else null
    }

    private fun extractAudio(title: String): String? {
        val found = mutableListOf<String>()
        val matcher = AUDIO_REGEX.matcher(title)
        while (matcher.find()) {
            val a = matcher.group(1) ?: continue
            val normalized = when {
                a.contains("dual", ignoreCase = true) -> "Dual-Audio"
                a.contains("multi", ignoreCase = true) -> "Multi-Audio"
                else -> a.uppercase()
            }
            if (!found.contains(normalized)) found.add(normalized)
        }
        return if (found.isNotEmpty()) found.joinToString(" ") else null
    }

    private fun extractSubtitles(title: String): String? {
        val found = mutableListOf<String>()
        val matcher = SUB_REGEX.matcher(title)
        while (matcher.find()) {
            val s = matcher.group(1) ?: continue
            val normalized = when {
                s.contains("multi", ignoreCase = true) -> "Multi-Sub"
                s.contains("soft", ignoreCase = true) -> "SoftSub"
                s.contains("hard", ignoreCase = true) -> "HardSub"
                else -> s.uppercase()
            }
            if (!found.contains(normalized)) found.add(normalized)
        }
        return if (found.isNotEmpty()) found.joinToString(" ") else null
    }

    private fun cleanAnimeTitle(title: String, releaseGroup: String?): String {
        var t = title
        // Убираем группу
        if (releaseGroup != null) {
            t = t.replace("[$releaseGroup]", "")
                .replace("($releaseGroup)", "")
        }
        // Убираем расширение файла
        t = t.replace(Regex("(?i)\\.(mkv|mp4|avi|ts)$"), "")
        // Убираем контрольные суммы в скобках [ABCD1234]
        t = t.replace(Regex("\\[[0-9A-Fa-f]{8}]"), "")
        // Убираем квадратные скобки с метаданными
        t = t.replace(Regex("\\[[^]]*]"), "")
        // Убираем круглые скобки с качеством/кодеками
        t = t.replace(Regex("(?i)\\((?:1080p|720p|480p|4k|hevc|x265|x264|batch|dual-audio|flac|aac)[^)]*\\)"), "")
        // Убираем номера серий на конце
        t = t.replace(Regex("(?i)\\s*-\\s*(?:ep|e|eps)?\\s*\\d+(?:-\\d+)?\\s*$"), "")
        t = t.replace(Regex("(?i)\\s+s\\d+\\s*$"), "")
        return t.trim().trim('-', '_', ' ')
    }

    private fun isQualityOrCodec(str: String): Boolean {
        return QUALITY_REGEX.matcher(str).matches() || CODEC_REGEX.matcher(str).matches()
    }

    private fun formatNumber(num: Int): String {
        return if (num in 1..9) "0$num" else num.toString()
    }
}
