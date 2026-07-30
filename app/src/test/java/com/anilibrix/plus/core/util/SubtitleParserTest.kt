package com.anilibrix.plus.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleParserTest {

    @Test
    fun parseSrt_parsesMultiLineCuesAndMillisecondTimes() {
        val content = """
            1
            00:00:01,250 --> 00:00:03,500
            First line
            Second line

            2
            00:01:00,000 --> 00:01:02,125
            Another cue
        """.trimIndent()

        val cues = SubtitleParser.parseSrt(content)

        assertEquals(2, cues.size)
        assertEquals(SubtitleCue(1_250L, 3_500L, "First line\nSecond line"), cues[0])
        assertEquals(SubtitleCue(60_000L, 62_125L, "Another cue"), cues[1])
    }

    @Test
    fun parseSrt_ignoresMalformedAndBlankBlocks() {
        val content = """
            1
            Not a time range
            Ignored

            2
            00:00:04.000 --> 00:00:05.000
            Kept

            3
            00:00:06,000 --> 00:00:07,000
        """.trimIndent()

        val cues = SubtitleParser.parseSrt(content)

        assertEquals(listOf(SubtitleCue(4_000L, 5_000L, "Kept")), cues)
    }

    @Test
    fun parseVtt_stripsHeaderMetadataAndParsesCue() {
        val content = """
            WEBVTT
            Kind: captions
            Language: ru

            00:00:10.000 --> 00:00:12.500
            Привет
        """.trimIndent()

        val cues = SubtitleParser.parseVtt(content)

        assertEquals(listOf(SubtitleCue(10_000L, 12_500L, "Привет")), cues)
    }

    @Test
    fun parseSrt_emptyInputReturnsNoCues() {
        assertTrue(SubtitleParser.parseSrt("   ").isEmpty())
    }
}
