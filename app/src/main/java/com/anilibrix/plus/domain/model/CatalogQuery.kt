package com.anilibrix.plus.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class CatalogQuery(
    val page: Int = 1,
    val limit: Int = 20,
    val search: String? = null,
    val genres: Set<String> = emptySet(),
    val year: Int? = null,
    val type: ReleaseType? = null,
    val season: SeasonName? = null,
    val status: CatalogStatus? = null,
    val sort: CatalogSort = CatalogSort.UPDATED
)

enum class CatalogStatus(val displayName: String, val apiValue: String) {
    ONGOING("Онгоинг", "ongoing"),
    COMPLETED("Завершён", "completed")
}

enum class CatalogSort(val displayName: String, val apiValue: String) {
    UPDATED("По обновлению", "-updated_at"),
    YEAR("По году", "-year"),
    RATING("По оценке", "-score"),
    TITLE("По названию", "name")
}
