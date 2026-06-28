package com.anilibrix.plus.core.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheInterceptor @Inject constructor() : Interceptor {

    private data class CacheEntry(val bodyBytes: ByteArray, val contentType: String, val timestamp: Long)

    private val cache = object : LinkedHashMap<String, CacheEntry>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
            return size > 100
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET") return chain.proceed(request)

        val cacheKey = request.url.toString()
        val cached = cache[cacheKey]
        val now = System.currentTimeMillis()

        if (cached != null && (now - cached.timestamp) < TimeUnit.MINUTES.toMillis(5)) {
            val mediaType = cached.contentType.toMediaTypeOrNull()
            val body = cached.bodyBytes.toResponseBody(mediaType)
            return Response.Builder()
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("X-Cache", "HIT")
                .body(body)
                .build()
        }

        val response = chain.proceed(request)

        if (response.isSuccessful) {
            val responseBody = response.body
            val bodyBytes = responseBody?.bytes() ?: byteArrayOf()
            val contentType = responseBody?.contentType()?.toString() ?: "application/json"
            cache[cacheKey] = CacheEntry(bodyBytes, contentType, now)
            val newBody = bodyBytes.toResponseBody(contentType.toMediaTypeOrNull())
            return response.newBuilder()
                .header("X-Cache", "MISS")
                .body(newBody)
                .build()
        }

        return response
    }
}
