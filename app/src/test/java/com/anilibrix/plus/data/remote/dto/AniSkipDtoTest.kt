package com.anilibrix.plus.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AniSkipDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun testParseAniSkipResponse() {
        val jsonString = """
            {
                "found": true,
                "results": [
                    {
                        "interval": {
                            "startTime": 85.5,
                            "endTime": 175.5
                        },
                        "skipType": "op",
                        "skipId": "skip-1",
                        "episodeLength": 1420.0
                    },
                    {
                        "interval": {
                            "startTime": 1300.0,
                            "endTime": 1390.0
                        },
                        "skipType": "ed",
                        "skipId": "skip-2",
                        "episodeLength": 1420.0
                    }
                ],
                "message": "Success",
                "statusCode": 200
            }
        """.trimIndent()

        val response = json.decodeFromString<AniSkipResponseDto>(jsonString)
        assertTrue(response.found)
        assertEquals(2, response.results.size)
        assertEquals("op", response.results[0].skipType)
        assertEquals(85.5, response.results[0].interval.startTime, 0.001)
        assertEquals(175.5, response.results[0].interval.endTime, 0.001)
        assertEquals("ed", response.results[1].skipType)
    }
}
