package com.anilibrix.plus.core.network

import com.anilibrix.plus.app.di.ApplicationScope
import com.anilibrix.plus.core.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ловит 401 и превращает протухшую сессию в понятное пользователю событие.
 *
 * Раньше 401 никак не обрабатывался: токен оставался в хранилище, каждый
 * следующий запрос уходил с ним же и снова получал 401, а на экранах
 * бесконечно висела «Ошибка загрузки». Выйти и войти заново приходилось
 * догадываться самому.
 *
 * Здесь используется обычный `Interceptor`, а не `okhttp3.Authenticator`:
 * `Authenticator` предназначен для **повторной** авторизации по refresh-токену,
 * а у Anilibria его нет — обновить сессию нечем, единственный корректный
 * исход — сбросить её.
 *
 * Запрос логина исключён: на нём 401 означает «неверный пароль», а не
 * «сессия истекла», и сбрасывать в этот момент нечего.
 */
@Singleton
class UnauthorizedInterceptor @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val authEventBus: AuthEventBus,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 401 && !request.url.encodedPath.contains(LOGIN_PATH)) {
            // Запись в DataStore — suspend, а мы на потоке OkHttp; уводим в
            // applicationScope, чтобы не блокировать сетевой поток.
            applicationScope.launch {
                settingsDataStore.setAuthToken(null)
                settingsDataStore.setAuthLogin(null)
            }
            authEventBus.post(AuthEvent.SessionExpired)
        }

        return response
    }

    private companion object {
        const val LOGIN_PATH = "auth/login"
    }
}
