package com.anilibrix.plus.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.ui.theme.infoBlue
import com.anilibrix.plus.ui.theme.successGreen
import com.anilibrix.plus.ui.theme.warningAmber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val LocalToastHostState: ProvidableCompositionLocal<ToastHostState?> = compositionLocalOf { null }

enum class ToastType {
    Success,
    Error,
    Info,
    Warning
}

private data class ToastVisuals(
    override val message: String,
    val type: ToastType,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short
) : SnackbarVisuals

@Stable
class ToastHostState(
    private val scope: CoroutineScope
) {
    val snackbarHostState = SnackbarHostState()

    fun showSuccess(message: String, actionLabel: String? = null) {
        showToast(message = message, type = ToastType.Success, actionLabel = actionLabel)
    }

    fun showError(message: String, actionLabel: String? = null) {
        showToast(message = message, type = ToastType.Error, actionLabel = actionLabel, duration = SnackbarDuration.Long)
    }

    fun showInfo(message: String, actionLabel: String? = null) {
        showToast(message = message, type = ToastType.Info, actionLabel = actionLabel)
    }

    fun showWarning(message: String, actionLabel: String? = null) {
        showToast(message = message, type = ToastType.Warning, actionLabel = actionLabel, duration = SnackbarDuration.Long)
    }

    fun showToast(
        message: String,
        type: ToastType = ToastType.Info,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        scope.launch {
            snackbarHostState.showSnackbar(
                ToastVisuals(
                    message = message,
                    type = type,
                    actionLabel = actionLabel,
                    duration = duration
                )
            )
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
            val visuals = data.visuals as? ToastVisuals
            val type = visuals?.type ?: ToastType.Info
            Snackbar(
                containerColor = type.containerColor(),
                contentColor = Color.White
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = type.icon(),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = data.visuals.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    )
}

@Composable
private fun ToastType.containerColor(): Color = when (this) {
    ToastType.Success -> successGreen
    ToastType.Error -> MaterialTheme.colorScheme.error
    ToastType.Info -> infoBlue
    ToastType.Warning -> warningAmber
}

private fun ToastType.icon(): ImageVector = when (this) {
    ToastType.Success -> Icons.Default.CheckCircle
    ToastType.Error -> Icons.Default.Error
    ToastType.Info -> Icons.Default.Info
    ToastType.Warning -> Icons.Default.Warning
}
