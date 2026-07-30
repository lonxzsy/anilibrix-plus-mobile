package com.anilibrix.plus.core.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

@Singleton
class RetryInterceptor @Inject constructor() : Interceptor {

    private val maxAttempts = 2

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.isRetryable()) return chain.proceed(request)

        var lastException: IOException? = null
        repeat(maxAttempts) { attempt ->
            try {
                val response = chain.proceed(request)
                if (!response.shouldRetry() || attempt == maxAttempts - 1) {
                    return response
                }
                response.closeQuietly()
            } catch (e: IOException) {
                if (!e.isRetryable() || attempt == maxAttempts - 1) throw e
                lastException = e
            }
        }

        throw lastException ?: IOException("Request failed after $maxAttempts attempts")
    }

    private fun Request.isRetryable(): Boolean {
        if (body?.isOneShot() == true) return false
        return method in RETRYABLE_METHODS
    }

    private fun Response.shouldRetry(): Boolean {
        return code == 408 || code == 429 || code in 500..599
    }

    private fun IOException.isRetryable(): Boolean {
        return when (this) {
            is UnknownHostException -> false
            is SSLException -> false
            is ProtocolException -> false
            is SocketTimeoutException -> true
            else -> true
        }
    }

    private companion object {
        val RETRYABLE_METHODS = setOf("GET", "HEAD", "OPTIONS")
    }
}
