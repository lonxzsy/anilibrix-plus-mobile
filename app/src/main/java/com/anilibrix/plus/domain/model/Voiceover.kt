package com.anilibrix.plus.domain.model

import androidx.compose.runtime.Immutable

/**
 * Провайдер видеопотоков и озвучек.
 */
enum class VoiceoverProvider(val displayName: String) {
    ANILIBRIA("AniLibria"),
    KODIK("Kodik"),
    CONSUMET("Consumet"),
    ANIFY("Anify"),
    DECODER("Studio"),
    TORRENT("Торрент / Локальный")
}

/**
 * Тип перевода: многоголосый/дубляж, субтитры или оригинал.
 */
enum class VoiceoverType(val displayName: String) {
    VOICE("Озвучка"),
    SUBTITLES("Субтитры"),
    ORIGINAL("Оригинал")
}

/**
 * Доступный вариант озвучки тайтла для выбора пользователем.
 */
@Immutable
data class VoiceoverOption(
    val id: String,
    val name: String,
    val provider: VoiceoverProvider,
    val type: VoiceoverType = VoiceoverType.VOICE,
    val episodesCount: Int? = null,
    val isDefault: Boolean = false,
    val translationId: Long? = null,
    val link: String? = null,
    val quality: String? = null,
)

/**
 * Информация о прямом видеопотоке для плеера.
 */
@Immutable
data class StreamSourceInfo(
    val url: String,
    val quality: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val subtitlesUrl: String? = null,
    val isHls: Boolean = true
)
