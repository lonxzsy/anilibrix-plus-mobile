package com.anilibrix.plus.core.network

import com.anilibrix.plus.core.datastore.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Подписывает запросы к Shikimori токеном пользователя.
 *
 * Публичные эндпоинты (поиск, персонажи, кадры) работают и без токена,
 * поэтому его отсутствие — не ошибка: запрос просто уходит анонимным.
 * Подписываются только те, что этого требуют, — `/api/v2/user_rates` и
 * `whoami`, — и им токен уже будет.
 *
 * `runBlocking` здесь допустим: интерцептор и так вызывается на фоновом
 * потоке OkHttp, а чтение одного ключа из DataStore занимает микросекунды.
 * Альтернатива — держать токен в volatile-поле — потребовала бы отдельной
 * синхронизации с логаутом.
 */
@Singleton
class ShikimoriAuthInterceptor @Inject constructor(
    private val settings: SettingsDataStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // Обмен кода на токен подписывать нечем и незачем.
        if (request.url.encodedPath.startsWith("/oauth/")) {
            return chain.proceed(request)
        }

        val token = runCatching {
            runBlocking { settings.shikimoriAccessToken.first() }
        }.getOrNull()

        val signed = if (token.isNullOrBlank()) {
            request
        } else {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        }
        return chain.proceed(signed)
    }
}
