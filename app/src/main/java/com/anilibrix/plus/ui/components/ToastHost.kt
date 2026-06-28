package com.anilibrix.plus.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class ToastHostState(
    private val scope: CoroutineScope
) {
    val snackbarHostState = SnackbarHostState()

    fun showSuccess(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    fun showError(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    fun showInfo(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }
}

@Composable
fun rememberToastHostState(): ToastHostState {
    val scope = rememberCoroutineScope()
    return remember { ToastHostState(scope) }
}

@Composable
fun ToastHost(
    toastHostState: ToastHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = toastHostState.snackbarHostState,
        modifier = modifier,
        snackbar = { data: SnackbarData ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    )
}
