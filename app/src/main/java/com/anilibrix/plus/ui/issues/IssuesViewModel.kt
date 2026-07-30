package com.anilibrix.plus.ui.issues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.domain.model.GitHubIssue
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IssuesState(
    val loading: Boolean = true,
    val issues: List<GitHubIssue> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class IssuesViewModel @Inject constructor(
    private val githubRepository: GitHubRepository
) : ViewModel() {

    private val _state = MutableStateFlow(IssuesState())
    val state: StateFlow<IssuesState> = _state.asStateFlow()

    fun loadIssues() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            githubRepository.getIssues()
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            _state.update {
                                it.copy(loading = false, issues = result.data)
                            }
                        }
                        is NetworkResult.Error -> {
                            _state.update {
                                it.copy(loading = false, error = result.message)
                            }
                        }
                        is NetworkResult.Loading -> {}
                    }
                }
        }
    }

    fun createIssue(title: String, body: String?) {
        viewModelScope.launch {
            githubRepository.createIssue(title, body)
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            loadIssues()
                        }
                        is NetworkResult.Error -> {
                            _state.update { it.copy(error = result.message) }
                        }
                        is NetworkResult.Loading -> {}
                    }
                }
        }
    }
}
