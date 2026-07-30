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
import androidx.compose.material3.SnackbarResult
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
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import com.anilibrix.plus.ui.theme.extended
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.MotionTokens

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

    /**
     * Показывает тост с действием и ЖДЁТ результат.
     *
     * Нужен для «Отменить» после свайпа в библиотеке: раньше ради этого
     * библиотека держала собственный вложенный [androidx.compose.material3.Scaffold]
     * со своим SnackbarHost, дублируя общий.
     */
    suspend fun showAction(
        message: String,
        actionLabel: String,
        type: ToastType = ToastType.Info,
        duration: SnackbarDuration = SnackbarDuration.Short,
    ): SnackbarResult = snackbarHostState.showSnackbar(
        ToastVisuals(
            message = message,
            type = type,
            actionLabel = actionLabel,
            duration = duration,
        )
    )

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
            val (container, content) = type.colors()

            // Плавающая пилюля, а не полоса во всю ширину: приподнята над
            // краем, скруглена и въезжает снизу с лёгким приближением.
            // Раньше у SnackbarHost вообще не задавались enter/exit.
            val appear = remember {
                MutableTransitionState(false).apply { targetState = true }
            }
            AnimatedVisibility(
                visibleState = appear,
                enter = slideInVertically(MotionTokens.spatialDefault()) { it } +
                    fadeIn(MotionTokens.effectsDefault()) +
                    scaleIn(MotionTokens.spatialDefault(), initialScale = 0.92f),
                exit = slideOutVertically(MotionTokens.spatialFast()) { it } +
                    fadeOut(MotionTokens.effectsFast()),
            ) {
            Snackbar(
                modifier = Modifier.padding(horizontal = Spacing.md),
                containerColor = container,
                contentColor = content,
                actionContentColor = content,
                shape = AnilibrixShapeExtras.pill,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = type.icon(),
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(Sizing.iconSm)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = data.visuals.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            }
        }
    )
}

/**
 * Пара «фон + цвет содержимого».
 *
 * Раньше содержимое было захардкожено в [Color.White] поверх фиксированных
 * оттенков: на янтарном #FFC107 это давало контраст 1.63:1 — провал WCAG на
 * трёх типах из четырёх. Плюс три фиксированных цвета игнорировали схему и
 * конфликтовали с Material You.
 */
@Composable
private fun ToastType.colors(): Pair<Color, Color> {
    val extended = MaterialTheme.extended
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        ToastType.Success -> extended.successContainer to extended.onSuccessContainer
        ToastType.Error -> scheme.errorContainer to scheme.onErrorContainer
        ToastType.Info -> extended.infoContainer to extended.onInfoContainer
        ToastType.Warning -> extended.warningContainer to extended.onWarningContainer
    }
}

private fun ToastType.icon(): ImageVector = when (this) {
    ToastType.Success -> Icons.Default.CheckCircle
    ToastType.Error -> Icons.Default.Error
    ToastType.Info -> Icons.Default.Info
    ToastType.Warning -> Icons.Default.Warning
}
