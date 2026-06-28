package com.anilibrix.plus.core.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetryInterceptor @Inject constructor() : Interceptor {

    private val delays = listOf(1000L, 2000L, 3000L)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null

        for (attempt in 0..delays.size) {
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful) return response
                response.closeQuietly()
            } catch (e: SocketTimeoutException) {
                lastException = e
            } catch (e: UnknownHostException) {
                throw e
            } catch (e: SSLException) {
                throw e
            } catch (e: IOException) {
                lastException = e
            }

            if (attempt < delays.size) {
                try {
                    Thread.sleep(delays[attempt])
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }

        throw lastException ?: IOException("Request failed after ${delays.size + 1} attempts")
    }
}
