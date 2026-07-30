package com.anilibrix.plus.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.domain.model.MalCharacterDetail
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
    val character: MalCharacterDetail? = null,
    val error: String? = null
)

@HiltViewModel
class CharacterDetailViewModel @Inject constructor(
    private val jikanRepository: JikanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CharacterDetailState())
    val state: StateFlow<CharacterDetailState> = _state.asStateFlow()

    /**
     * @param characterId собственный MAL-идентификатор персонажа.
     *
     * Раньше здесь вызывался `getCharacters(malId)` — эндпоинт «персонажи
     * аниме», — и в него подставлялся id персонажа. Запрос уходил по адресу
     * `anime/{characterId}/characters`, то есть либо возвращал персонажей
     * случайного аниме с совпавшим номером, либо падал с 404.
     */
    fun load(characterId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            jikanRepository.getCharacter(characterId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> _state.update {
                        it.copy(loading = false, character = result.data, error = null)
                    }
                    is NetworkResult.Error -> _state.update {
                        it.copy(loading = false, error = result.message)
                    }
                    is NetworkResult.Loading -> Unit
                }
            }
        }
    }
}
