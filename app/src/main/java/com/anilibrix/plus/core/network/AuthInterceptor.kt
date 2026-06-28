package com.anilibrix.plus.core.network

import com.anilibrix.plus.core.datastore.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            settingsDataStore.authToken.first()
        }
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
