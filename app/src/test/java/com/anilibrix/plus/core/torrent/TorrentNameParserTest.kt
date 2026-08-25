package com.anilibrix.plus.core.torrent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TorrentNameParserTest {

    @Test
    fun parse_singleEpisodeSubsPlease() {
        val raw = "[SubsPlease] Sousou no Frieren - 05 (1080p) [9A1B2C3D].mkv"
        val info = TorrentNameParser.parse(raw)

        assertEquals("SubsPlease", info.releaseGroup)
        assertEquals("Sousou no Frieren", info.cleanTitle)
        assertEquals("1080p", info.quality)
        assertEquals(listOf(5), info.episodeNumbers)
        assertEquals("Серия 05", info.episodeLabel)
        assertFalse(info.isBatch)
    }

    @Test
    fun parse_batchEraiRaws() {
        val raw = "[Erai-raws] One Piece - 01-12 [1080p][Multiple Subtitle][ENG][SPA][POR].mkv"
        val info = TorrentNameParser.parse(raw)

        assertEquals("Erai-raws", info.releaseGroup)
        assertEquals("1080p", info.quality)
        assertTrue(info.isBatch)
        assertEquals((1..12).toList(), info.episodeNumbers)
        assertEquals("Серии 01-12 (Пакет)", info.episodeLabel)
        assertEquals("Multi-Sub", info.subtitleInfo)
    }

    @Test
    fun parse_fullSeasonJudas() {
        val raw = "[Judas] Shingeki no Kyojin (Season 4) [1080p][HEVC x265 10bit][Dual-Audio AAC] (Batch)"
        val info = TorrentNameParser.parse(raw)

        assertEquals("Judas", info.releaseGroup)
        assertEquals("1080p", info.quality)
        assertTrue(info.isBatch)
        assertEquals("HEVC X265 10BIT", info.videoCodec)
        assertEquals("Dual-Audio AAC", info.audioInfo)
    }

    @Test
    fun parse_anilibriaTorrent() {
        val raw = "[AniLibria.TV] Frieren - 01-28 [WEBRip 1080p]"
        val info = TorrentNameParser.parse(raw)

        assertEquals("AniLibria.TV", info.releaseGroup)
        assertEquals("1080p", info.quality)
        assertTrue(info.isBatch)
        assertEquals((1..28).toList(), info.episodeNumbers)
    }
}
