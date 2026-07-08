package com.anilibrix.plus.core.util

object Constants {

    const val APP_NAME = "Anilibrix Plus"
    const val PACKAGE_NAME = "com.anilibrix.plus"

    const val ANILIBRIA_BASE_URL = "https://aniliberty.top/api/v1/"
    const val DECODER_BASE_URL = "https://anime-cr-production.up.railway.app/"
    const val JIKAN_BASE_URL = "https://api.jikan.moe/v4/"
    const val GITHUB_BASE_URL = "https://api.github.com/"

    const val SOURCE_ANILIBRIA = "anilibria"
    const val SOURCE_KODIK = "kodik"
    const val SOURCE_SHIKIMORI = "shikimori"

    const val QUALITY_360 = "360"
    const val QUALITY_480 = "480"
    const val QUALITY_720 = "720"
    const val QUALITY_1080 = "1080"
    const val QUALITY_SOURCE = "source"

    val QUALITY_OPTIONS = listOf(QUALITY_360, QUALITY_480, QUALITY_720, QUALITY_1080, QUALITY_SOURCE)

    const val SPEED_0_5 = 0.5f
    const val SPEED_0_75 = 0.75f
    const val SPEED_1_0 = 1.0f
    const val SPEED_1_25 = 1.25f
    const val SPEED_1_5 = 1.5f
    const val SPEED_2_0 = 2.0f

    val SPEED_OPTIONS = listOf(SPEED_0_5, SPEED_0_75, SPEED_1_0, SPEED_1_25, SPEED_1_5, SPEED_2_0)

    const val HISTORY_CAP = 500
    const val CACHE_MAX_SIZE = 100L
    const val CACHE_TTL_MINUTES = 5L
    const val LRU_CACHE_SIZE = 10 * 1024 * 1024L

    const val RETRY_MAX_ATTEMPTS = 4
    val RETRY_DELAYS_MS = listOf(1000L, 2000L, 3000L)

    const val DEFAULT_TIMEOUT_SECONDS = 30L
    const val DATABASE_NAME = "anilibrix.db"
    const val DATASTORE_NAME = "settings"

    const val THEME_DARK = "dark"
    const val THEME_LIGHT = "light"
    const val THEME_SYSTEM = "system"
}
