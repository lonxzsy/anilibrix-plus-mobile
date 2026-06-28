package com.anilibrix.plus.core.architecture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<S, E> : ViewModel() {

    protected abstract val initialUiState: S

    protected val _uiState: MutableStateFlow<S> by lazy { MutableStateFlow(initialUiState) }
    val uiState: StateFlow<S> by lazy { _uiState.asStateFlow() }

    protected val _effect: MutableStateFlow<E?> = MutableStateFlow(null)
    val effect: StateFlow<E?> = _effect.asStateFlow()

    protected val scope: CoroutineScope = viewModelScope

    protected fun updateState(reducer: S.() -> S) {
        _uiState.value = _uiState.value.reducer()
    }

    protected fun sendEffect(effect: E) {
        _effect.value = effect
    }

    protected fun clearEffect() {
        _effect.value = null
    }

    protected fun launch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch { block() }
    }

    protected fun <T> load(
        loadingState: S.() -> S,
        successState: (T) -> S.() -> S,
        errorState: (Throwable) -> S.() -> S,
        fetch: suspend () -> T
    ) {
        viewModelScope.launch {
            try {
                updateState { loadingState() }
                val result = fetch()
                updateState { successState(result)() }
            } catch (e: Exception) {
                updateState { errorState(e)() }
            }
        }
    }
}
