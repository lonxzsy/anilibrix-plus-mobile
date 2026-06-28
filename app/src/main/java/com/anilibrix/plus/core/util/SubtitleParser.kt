package com.anilibrix.plus.core.util

import java.util.regex.Pattern

data class SubtitleCue(
    val startTime: Long,
    val endTime: Long,
    val text: String
)

object SubtitleParser {

    fun parseSrt(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = content.trim().split(Regex("\\r?\\n\\r?\\n"))
        val timePattern = Pattern.compile(
            "(\\d{2}):(\\d{2}):(\\d{2})[,\\.](\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2})[,\\.](\\d{3})"
        )

        for (block in blocks) {
            val lines = block.lines().filter { it.isNotBlank() }
            if (lines.size < 2) continue

            val timeLine = lines.firstOrNull { it.contains("-->") } ?: continue
            val matcher = timePattern.matcher(timeLine)
            if (!matcher.find()) continue

            val startTime = parseSrtTime(
                matcher.group(1)!!.toInt(),
                matcher.group(2)!!.toInt(),
                matcher.group(3)!!.toInt(),
                matcher.group(4)!!.toInt()
            )
            val endTime = parseSrtTime(
                matcher.group(5)!!.toInt(),
                matcher.group(6)!!.toInt(),
                matcher.group(7)!!.toInt(),
                matcher.group(8)!!.toInt()
            )

            val textLines = lines.dropWhile { it.contains("-->") || it.all { c -> c.isDigit() } }
            val text = textLines.joinToString("\n").trim()
            if (text.isNotBlank()) {
                cues.add(SubtitleCue(startTime, endTime, text))
            }
        }

        return cues
    }

    fun parseVtt(content: String): List<SubtitleCue> {
        val cleaned = content.replace(Regex("WEBVTT\\s*"), "")
            .replace(Regex("Kind:.*"), "")
            .replace(Regex("Language:.*"), "")
            .trim()
        return parseSrt(cleaned)
    }

    private fun parseSrtTime(hours: Int, minutes: Int, seconds: Int, millis: Int): Long {
        return hours.toLong() * 3600000L + minutes.toLong() * 60000L + seconds.toLong() * 1000L + millis.toLong()
    }
}
