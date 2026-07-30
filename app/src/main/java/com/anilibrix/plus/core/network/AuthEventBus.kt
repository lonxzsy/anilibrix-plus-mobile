package com.anilibrix.plus.core.network

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** События уровня сессии, которые рождаются в сетевом слое, а обрабатываются в UI. */
sealed interface AuthEvent {
    /** Сервер ответил 401: токен протух или отозван. Локальная сессия уже сброшена. */
    data object SessionExpired : AuthEvent
}

/**
 * Мост из OkHttp в UI.
 *
 * 401 приходит на потоке OkHttp, где нет ни ViewModel, ни Composition. Раньше
 * такой ответ просто превращался в очередную «Ошибка загрузки», и человек
 * видел бесконечно падающие экраны, не понимая, что достаточно войти заново.
 *
 * `replay = 1` намеренный: 401 может прийти в момент, когда оболочка ещё не
 * подписалась (например, во время холодного старта с фоновой синхронизацией),
 * и событие не должно потеряться. `DROP_OLDEST` не даёт очереди расти, если
 * разом упало десять параллельных запросов — показать надо один раз.
 */
@Singleton
class AuthEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<AuthEvent>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun post(event: AuthEvent) {
        _events.tryEmit(event)
    }

    /** Снимает отложенное событие, чтобы оно не показалось повторно после смены экрана. */
    fun consume() {
        _events.resetReplayCache()
    }
}
