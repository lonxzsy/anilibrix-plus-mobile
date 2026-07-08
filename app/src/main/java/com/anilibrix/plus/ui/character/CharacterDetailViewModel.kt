package com.anilibrix.plus.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.domain.model.MalCharacter
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.JikanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CharacterDetailState(
    val loading: Boolean = true,
    val character: MalCharacter? = null,
    val error: String? = null
)

@HiltViewModel
class CharacterDetailViewModel @Inject constructor(
    private val jikanRepository: JikanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CharacterDetailState())
    val state: StateFlow<CharacterDetailState> = _state.asStateFlow()

    fun load(malId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            jikanRepository.getCharacters(malId)
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            val character = result.data.firstOrNull()
                            _state.update {
                                it.copy(
                                    loading = false,
                                    character = character
                                )
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
}
