package com.anilibrix.plus.data.sync

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.anilibrix.plus.BuildConfig
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.data.remote.api.ShikimoriApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Вход в Shikimori.
 *
 * Авторизация открывается в **системном браузере** (Custom Tabs), а не в
 * собственном WebView. Это принципиально: в чужом WebView человек не может
 * проверить адресную строку и сертификат, то есть не может убедиться, что
 * вводит пароль на настоящем сайте, — а мы получаем техническую возможность
 * этот пароль прочитать. Custom Tabs снимает и то, и другое.
 */
@Singleton
class ShikimoriAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ShikimoriApi,
    private val settings: SettingsDataStore,
) {

    /**
     * Настроены ли ключи приложения.
     *
     * Ключи выдаются владельцу приложения на shikimori.one и хранятся в
     * local.properties. Без них раздел синхронизации показывает, что она
     * недоступна, — это честнее, чем показывать кнопку, которая не работает.
     */
    val isConfigured: Boolean
        get() = BuildConfig.SHIKIMORI_CLIENT_ID.isNotBlank() &&
            BuildConfig.SHIKIMORI_CLIENT_SECRET.isNotBlank()

    suspend fun isLinked(): Boolean = !settings.shikimoriAccessToken.first().isNullOrBlank()

    /** Открывает страницу авторизации. */
    fun startAuthorization() {
        if (!isConfigured) return

        val authUri = Uri.parse(AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("client_id", BuildConfig.SHIKIMORI_CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", SCOPE)
            .build()

        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .apply { intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
            .launchUrl(context, authUri)
    }

    /** Достаёт код авторизации из редиректа `anilibrixplus://shikimori/callback?code=…`. */
    fun extractCode(uri: Uri?): String? {
        if (uri == null) return null
        if (uri.scheme != SCHEME || uri.host != HOST) return null
        return uri.getQueryParameter("code")?.takeIf { it.isNotBlank() }
    }

    /**
     * Меняет код на токен и запоминает пользователя.
     *
     * @return ник привязанного аккаунта или `null`, если обмен не удался.
     */
    suspend fun completeAuthorization(code: String): String? {
        val token = runCatching {
            api.exchangeToken(
                grantType = "authorization_code",
                clientId = BuildConfig.SHIKIMORI_CLIENT_ID,
                clientSecret = BuildConfig.SHIKIMORI_CLIENT_SECRET,
                code = code,
                redirectUri = REDIRECT_URI,
            )
        }.getOrNull() ?: return null

        settings.setShikimoriTokens(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            expiresAt = token.expiresAtMillis(),
        )

        val user = runCatching { api.whoami() }.getOrNull()
        if (user == null) {
            // Токен получили, но кто мы — не знаем. Без user_id работать с
            // user_rates нельзя, поэтому такую половинчатую привязку не
            // сохраняем: она выглядела бы рабочей и молча ничего не делала.
            settings.clearShikimori()
            return null
        }

        settings.setShikimoriUser(
            userId = user.id,
            nickname = user.nickname,
            avatar = user.avatar ?: user.image?.x96,
        )
        return user.nickname
    }

    /**
     * Обновляет токен, если он истёк или вот-вот истечёт.
     *
     * Запас в пять минут: токен, живой на момент проверки, может умереть, пока
     * запрос идёт по сети.
     */
    suspend fun ensureFreshToken(): String? {
        val access = settings.shikimoriAccessToken.first() ?: return null
        val expiresAt = settings.shikimoriExpiresAt.first()
        if (expiresAt == 0L || System.currentTimeMillis() < expiresAt - REFRESH_MARGIN_MS) {
            return access
        }

        val refresh = settings.shikimoriRefreshToken.first() ?: return null
        val token = runCatching {
            api.exchangeToken(
                grantType = "refresh_token",
                clientId = BuildConfig.SHIKIMORI_CLIENT_ID,
                clientSecret = BuildConfig.SHIKIMORI_CLIENT_SECRET,
                refreshToken = refresh,
            )
        }.getOrNull() ?: return null

        settings.setShikimoriTokens(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            expiresAt = token.expiresAtMillis(),
        )
        return token.accessToken
    }

    suspend fun unlink() = settings.clearShikimori()

    companion object {
        const val SCHEME = "anilibrixplus"
        const val HOST = "shikimori"
        const val REDIRECT_URI = "$SCHEME://$HOST/callback"

        private const val AUTHORIZE_URL = "https://shikimori.one/oauth/authorize"

        /** Достаточно для чтения и записи пользовательских списков. */
        private const val SCOPE = "user_rates"

        private const val REFRESH_MARGIN_MS = 5 * 60 * 1000L
    }
}
