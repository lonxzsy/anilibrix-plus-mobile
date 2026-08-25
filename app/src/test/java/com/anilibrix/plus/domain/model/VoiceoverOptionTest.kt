package com.anilibrix.plus.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceoverOptionTest {

    @Test
    fun testVoiceoverOptionCreation() {
        val option = VoiceoverOption(
            id = "kodik_609",
            name = "Студийная Банда",
            provider = VoiceoverProvider.KODIK,
            type = VoiceoverType.VOICE,
            episodesCount = 24,
            translationId = 609L
        )

        assertEquals("kodik_609", option.id)
        assertEquals("Студийная Банда", option.name)
        assertEquals(VoiceoverProvider.KODIK, option.provider)
        assertEquals(VoiceoverType.VOICE, option.type)
        assertEquals(24, option.episodesCount)
        assertEquals(609L, option.translationId)
    }

    @Test
    fun testDefaultOption() {
        val anilibria = VoiceoverOption(
            id = "anilibria",
            name = "AniLibria",
            provider = VoiceoverProvider.ANILIBRIA,
            type = VoiceoverType.VOICE,
            isDefault = true
        )

        assertTrue(anilibria.isDefault)
        assertEquals(VoiceoverProvider.ANILIBRIA, anilibria.provider)
    }
}
