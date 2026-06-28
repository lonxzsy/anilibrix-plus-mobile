package com.anilibrix.plus.app.di

import com.anilibrix.plus.core.network.AuthInterceptor
import com.anilibrix.plus.core.network.CacheInterceptor
import com.anilibrix.plus.core.network.RetryInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnilibriaApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DecoderApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class JikanApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GitHubApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideJson(): Json = json

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        cacheInterceptor: CacheInterceptor,
        retryInterceptor: RetryInterceptor,
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val userAgentInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "AnilibrixPlus/1.0 Android")
                .build()
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(cacheInterceptor)
            .addInterceptor(retryInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @AnilibriaApi
    fun provideAnilibriaRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://aniliberty.top/api/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    @DecoderApi
    fun provideDecoderRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://anime-cr-production.up.railway.app/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    @JikanApi
    fun provideJikanRetrofit(): Retrofit {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.jikan.moe/v4/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    @GitHubApi
    fun provideGitHubRetrofit(): Retrofit {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    @Provides
    @Singleton
    fun provideAnilibriaApiService(@AnilibriaApi retrofit: Retrofit): com.anilibrix.plus.data.remote.api.AnilibriaApi {
        return retrofit.create(com.anilibrix.plus.data.remote.api.AnilibriaApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDecoderApiService(@DecoderApi retrofit: Retrofit): com.anilibrix.plus.data.remote.api.DecoderApi {
        return retrofit.create(com.anilibrix.plus.data.remote.api.DecoderApi::class.java)
    }

    @Provides
    @Singleton
    fun provideJikanApiService(@JikanApi retrofit: Retrofit): com.anilibrix.plus.data.remote.api.JikanApi {
        return retrofit.create(com.anilibrix.plus.data.remote.api.JikanApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGitHubApiService(@GitHubApi retrofit: Retrofit): com.anilibrix.plus.data.remote.api.GitHubApi {
        return retrofit.create(com.anilibrix.plus.data.remote.api.GitHubApi::class.java)
    }
}
