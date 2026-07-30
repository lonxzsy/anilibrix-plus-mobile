package com.anilibrix.plus.domain.repository

import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.ShikimoriAnime
import com.anilibrix.plus.domain.model.ShikimoriCharacter
import com.anilibrix.plus.domain.model.ShikimoriCharacterSearchResult
import com.anilibrix.plus.domain.model.ShikimoriRelated
import com.anilibrix.plus.domain.model.ShikimoriScreenshot
import kotlinx.coroutines.flow.Flow

interface ShikimoriRepository {
    fun search(query: String, limit: Int = 5): Flow<NetworkResult<List<ShikimoriAnime>>>
    fun getAnime(id: Int): Flow<NetworkResult<ShikimoriAnime>>
    fun getCharacters(id: Int): Flow<NetworkResult<List<ShikimoriCharacter>>>
    fun getRelated(id: Int): Flow<NetworkResult<List<ShikimoriRelated>>>

    /**
     * Кадры из аниме.
     *
     * Эндпоинт был объявлен в [com.anilibrix.plus.data.remote.api.ShikimoriApi]
     * с самого начала, но метода репозитория к нему не существовало — вызвать
     * его было неоткуда.
     */
    fun getScreenshots(id: Int): Flow<NetworkResult<List<ShikimoriScreenshot>>>

    /** Поиск персонажей по имени — для раздела в поисковой панели каталога. */
    fun searchCharacters(query: String, limit: Int = 5): Flow<NetworkResult<List<ShikimoriCharacterSearchResult>>>
}
