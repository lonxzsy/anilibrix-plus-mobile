package com.anilibrix.plus.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TimecodeDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Test
    fun decodesDocumentedArrayResponseShape() {
        val payload = """
            [
              ["68d4d5c5-e3d5-419f-a21c-c511b6b251f5", 743.5, true],
              ["9c5f39e1-56dd-4d44-950a-e1a4201426a4", 12, false]
            ]
        """.trimIndent()

        val timecodes = json.decodeFromString<List<TimecodeDto>>(payload)

        assertEquals(2, timecodes.size)
        assertEquals("68d4d5c5-e3d5-419f-a21c-c511b6b251f5", timecodes[0].releaseEpisodeId)
        assertEquals(743.5, timecodes[0].time, 0.0)
        assertEquals(true, timecodes[0].isWatched)
        assertEquals("9c5f39e1-56dd-4d44-950a-e1a4201426a4", timecodes[1].releaseEpisodeId)
        assertEquals(12.0, timecodes[1].time, 0.0)
        assertEquals(false, timecodes[1].isWatched)
    }

    @Test
    fun decodesObjectResponseShapeForCompatibility() {
        val payload = """
            [
              {
                "release_episode_id": "cf31fe87-fad8-11eb-b2fa-0242ac120004",
                "time": 127.45,
                "is_watched": false,
                "ignored": "value"
              }
            ]
        """.trimIndent()

        val timecodes = json.decodeFromString<List<TimecodeDto>>(payload)

        assertEquals(1, timecodes.size)
        assertEquals("cf31fe87-fad8-11eb-b2fa-0242ac120004", timecodes.single().releaseEpisodeId)
        assertEquals(127.45, timecodes.single().time, 0.0)
        assertEquals(false, timecodes.single().isWatched)
    }

    @Test
    fun encodesUpdateRequestAsApiBodyItem() {
        val encoded = json.encodeToString(
            listOf(
                TimecodeRequest(
                    time = 42.25,
                    isWatched = false,
                    releaseEpisodeId = "release-episode-id"
                )
            )
        )

        assertEquals(
            "[{\"time\":42.25,\"is_watched\":false,\"release_episode_id\":\"release-episode-id\"}]",
            encoded
        )
    }

    @Test
    fun encodesDeleteRequestAsApiBodyItem() {
        val encoded = json.encodeToString(listOf(DeleteTimecodeRequest("release-episode-id")))

        assertEquals(
            "[{\"release_episode_id\":\"release-episode-id\"}]",
            encoded
        )
    }
}
