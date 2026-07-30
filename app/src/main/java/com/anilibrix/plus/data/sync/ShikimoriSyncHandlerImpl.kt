package com.anilibrix.plus.data.sync

import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.core.sync.PendingOperation
import com.anilibrix.plus.core.sync.ShikimoriSyncHandler
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Отправка статуса и прогресса в Shikimori.
 *
 * Пока аккаунт не привязан, операция считается закрытой: держать её в очереди
 * незачем — отправлять некуда, а очередь она бы забивала бесконечно.
 */
@Singleton
class ShikimoriSyncHandlerImpl @Inject constructor(
    private val settings: SettingsDataStore,
    private val shikimoriRates: ShikimoriRatesGateway,
    private val authManager: ShikimoriAuthManager,
) : ShikimoriSyncHandler {

    override suspend fun push(operation: PendingOperation): Boolean {
        if (!settings.shikimoriPushStatus.first()) return true
        // Освежаем токен ДО запроса: истёкший даёт 401, который выглядел бы
        // как сетевая ошибка и заставлял бы очередь повторяться впустую.
        val token = authManager.ensureFreshToken()
        if (token.isNullOrBlank()) return true
        return shikimoriRates.push(operation)
    }
}
