package com.anilibrix.plus.domain.usecase

import com.anilibrix.plus.domain.model.CatalogQuery
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Подборка «вам может понравиться» по локальной истории.
 *
 * Считается **на устройстве**: никакие данные о просмотрах никуда не уходят.
 * Логика намеренно простая и объяснимая — вес жанра складывается из того,
 * сколько тайтлов с этим жанром человек смотрел, и насколько высоко он их
 * оценил. Сложнее здесь не нужно: сервер не даёт ни эмбеддингов, ни
 * коллаборативной фильтрации, а угадывать вслепую хуже, чем честно
 * предложить «ещё про то же самое».
 *
 * Подпись «потому что вы смотрели …» возвращается вместе с результатом:
 * непрозрачная подборка вызывает недоверие, а объяснённая — проверяема.
 */
class GetPersonalRecommendationsUseCase @Inject constructor(
    private val localRepository: LocalRepository,
    private val anilibriaRepository: AnilibriaRepository,
) {

    suspend operator fun invoke(limit: Int = 12): PersonalRecommendations {
        val history = localRepository.getHistory().first()
        val ratings = localRepository.getAllRatings().first()
        val watching = localRepository.getCollections(CollectionType.WATCHING).first()
        val completed = localRepository.getCollections(CollectionType.COMPLETED).first()

        val seedIds = (history.map { it.titleId } + watching.map { it.titleId } + completed.map { it.titleId })
            .distinct()
        if (seedIds.isEmpty()) return PersonalRecommendations()

        // Берём не больше десяти тайтлов-источников: каждый — это запрос к
        // API, а точность подборки после первого десятка почти не растёт.
        val seeds = seedIds
            .sortedByDescending { ratings[it] ?: 0f }
            .take(MAX_SEEDS)
            .mapNotNull { titleId ->
                val result = anilibriaRepository.getRelease(titleId.toString())
                    .first { it !is NetworkResult.Loading }
                (result as? NetworkResult.Success)?.data
            }
        if (seeds.isEmpty()) return PersonalRecommendations()

        val genreWeights = mutableMapOf<String, Float>()
        seeds.forEach { title ->
            // Оценка усиливает вклад: то, что человек оценил на пятёрку,
            // должно влиять сильнее, чем случайно открытая серия.
            val weight = 1f + (ratings[title.id] ?: 0f) / 2.5f
            title.genres.forEach { genre ->
                genreWeights[genre.name] = (genreWeights[genre.name] ?: 0f) + weight
            }
        }

        val topGenres = genreWeights.entries
            .sortedByDescending { it.value }
            .take(TOP_GENRES)
            .map { it.key }
        if (topGenres.isEmpty()) return PersonalRecommendations()

        val exclude = seedIds.toMutableSet()
        exclude += localRepository.getCollections(CollectionType.WATCH_LATER).first().map { it.titleId }
        exclude += localRepository.getCollections(CollectionType.DROPPED).first().map { it.titleId }

        val result = anilibriaRepository
            .getCatalog(CatalogQuery(page = 1, limit = limit * 3, genres = topGenres.toSet()))
            .first { it !is NetworkResult.Loading }

        val candidates = (result as? NetworkResult.Success)?.data.orEmpty()
            .filter { it.id !in exclude }
            // Ранжируем по совпадению жанров: тайтл, попавший сразу в два
            // любимых жанра, интереснее того, что совпал одним.
            .sortedByDescending { candidate ->
                candidate.genres.count { it.name in topGenres }
            }
            .take(limit)

        return PersonalRecommendations(
            titles = candidates,
            reasonGenres = topGenres,
            basedOnCount = seeds.size,
        )
    }

    private companion object {
        const val MAX_SEEDS = 10
        const val TOP_GENRES = 3
    }
}

data class PersonalRecommendations(
    val titles: List<Title> = emptyList(),
    val reasonGenres: List<String> = emptyList(),
    val basedOnCount: Int = 0,
) {
    val isEmpty: Boolean get() = titles.isEmpty()

    /** «потому что вы смотрели фэнтези и приключения» — объяснение подборки. */
    fun reason(): String = when {
        reasonGenres.isEmpty() -> ""
        reasonGenres.size == 1 -> "потому что вы смотрите ${reasonGenres.first().lowercase()}"
        else -> "потому что вы смотрите " +
            reasonGenres.dropLast(1).joinToString(", ") { it.lowercase() } +
            " и ${reasonGenres.last().lowercase()}"
    }
}
