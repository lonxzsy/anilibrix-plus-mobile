package com.anilibrix.plus.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class KodikDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun testParseKodikSearchResponse() {
        val jsonString = """
            {
                "time": "2ms",
                "total": 2,
                "results": [
                    {
                        "id": "anime-123",
                        "title": "Attack on Titan",
                        "title_orig": "Shingeki no Kyojin",
                        "translation": {
                            "id": 609,
                            "title": "Студийная Банда",
                            "type": "voice"
                        },
                        "link": "//kodik.cc/video/123",
                        "last_episode": 24,
                        "seasons": {
                            "1": {
                                "episodes": {
                                    "1": "//kodik.cc/video/123/1",
                                    "2": "//kodik.cc/video/123/2"
                                }
                            }
                        }
                    },
                    {
                        "id": "anime-124",
                        "title": "Attack on Titan",
                        "translation": {
                            "id": 610,
                            "title": "Dream Cast",
                            "type": "voice"
                        },
                        "link": "//kodik.cc/video/124",
                        "last_episode": 24
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<KodikSearchResponse>(jsonString)
        assertEquals(2, response.total)
        assertEquals(2, response.results.size)

        val first = response.results[0]
        assertEquals("Attack on Titan", first.title)
        assertNotNull(first.translation)
        assertEquals(609L, first.translation?.id)
        assertEquals("Студийная Банда", first.translation?.title)
        assertEquals(2, first.seasons?.get("1")?.episodes?.size)
    }
}
