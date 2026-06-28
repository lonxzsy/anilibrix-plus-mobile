package com.anilibrix.plus.ui.changelog

import com.anilibrix.plus.core.architecture.BaseViewModel
import com.anilibrix.plus.domain.model.ChangelogRelease
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChangelogViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository
) : BaseViewModel<ChangelogUiState, Unit>() {

    override val initialUiState: ChangelogUiState = ChangelogUiState()

    init {
        loadReleases()
    }

    fun handleIntent(intent: ChangelogIntent) {
        when (intent) {
            ChangelogIntent.LoadReleases -> loadReleases()
        }
    }

    private fun loadReleases() {
        load(
            loadingState = { copy(isLoading = true, error = null) },
            successState = { data: List<ChangelogRelease> -> { copy(releases = data, isLoading = false) } },
            errorState = { e -> { copy(error = e.message, isLoading = false) } },
            fetch = {
                var data = listOf<ChangelogRelease>()
                gitHubRepository.getReleases().collect { result ->
                    if (result is NetworkResult.Success) data = result.data
                }
                data
            }
        )
    }
}
