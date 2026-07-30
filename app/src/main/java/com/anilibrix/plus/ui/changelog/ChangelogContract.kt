package com.anilibrix.plus.ui.changelog

import com.anilibrix.plus.domain.model.ChangelogRelease

data class ChangelogUiState(
    val releases: List<ChangelogRelease> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class ChangelogIntent {
    data object LoadReleases : ChangelogIntent()
}
