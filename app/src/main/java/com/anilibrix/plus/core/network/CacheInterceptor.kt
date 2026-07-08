package com.anilibrix.plus.core.network

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheInterceptor @Inject constructor() : Interceptor {

    private val defaultCacheControl = CacheControl.Builder()
        .maxAge(10, TimeUnit.MINUTES)
        .build()
        .toString()

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val request = response.request

        if (request.method != "GET" || !response.isSuccessful || response.header("Cache-Control") != null) {
            return response
        }

        return response.newBuilder()
            .header("Cache-Control", defaultCacheControl)
            .build()
    }
}
