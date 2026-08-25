package com.anilibrix.plus.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val authTokenProvider: AuthTokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val host = chain.request().url.host
        val isAnilibriaHost = host.contains("anilib", ignoreCase = true)
        val token = authTokenProvider.currentToken()
        val request = if (token.isNullOrBlank() || !isAnilibriaHost) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
