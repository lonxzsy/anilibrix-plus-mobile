package com.anilibrix.plus.app.di

import android.content.Context
import com.anilibrix.plus.core.network.AuthInterceptor
import com.anilibrix.plus.core.network.CacheInterceptor
import com.anilibrix.plus.core.network.RetryInterceptor
import com.anilibrix.plus.core.network.ShikimoriAuthInterceptor
import com.anilibrix.plus.core.network.UnauthorizedInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
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

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ShikimoriApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KodikApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AniSkipApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ConsumetApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NyaaApi

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
    fun provideHttpCache(@ApplicationContext context: Context): Cache {
        val cacheDir = File(context.cacheDir, "http_cache")
        return Cache(cacheDir, 20L * 1024L * 1024L)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        cacheInterceptor: CacheInterceptor,
        retryInterceptor: RetryInterceptor,
        unauthorizedInterceptor: UnauthorizedInterceptor,
        cache: Cache
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        val userAgentInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "AnilibrixPlus/1.0 Android")
                .build()
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(authInterceptor)
            // Строго после authInterceptor и до retryInterceptor: смотрит на
            // ответ на запрос, который УЖЕ подписан токеном, и делает это один
            // раз, а не на каждой повторной попытке.
            .addInterceptor(unauthorizedInterceptor)
            .addInterceptor(retryInterceptor)
            .addNetworkInterceptor(cacheInterceptor)
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
    @ShikimoriApi
    fun provideShikimoriRetrofit(shikimoriAuthInterceptor: ShikimoriAuthInterceptor): Retrofit {
        // Shikimori требует осмысленный User-Agent и отвечает 429 на запросы
        // без него. Раньше интерцептор стоял только на клиенте Anilibria, а
        // клиент Shikimori создавался вообще без интерцепторов.
        val headers = Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "AnilibrixPlus")
                    .header("Accept", "application/json")
                    .build()
            )
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(headers)
            .addInterceptor(shikimoriAuthInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl("https://shikimori.one/")
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

    @Provides
    @Singleton
    fun provideShikimoriApiService(@ShikimoriApi retrofit: Retrofit): com.anilibrix.plus.data.remote.api.ShikimoriApi {
        return retrofit.create(com.anilibrix.plus.data.remote.api.ShikimoriApi::class.java)
    }

    @Provides
    @Singleton
    @KodikApi
    fun provideKodikRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://kodik-api.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideKodikApiService(@KodikApi retrofit: Retrofit): com.anilibrix.plus.data.remote.api.KodikApi {
        return retrofit.create(com.anilibrix.plus.data.remote.api.KodikApi::class.java)
    }

    @Provides
    @Singleton
    @AniSkipApi
    fun provideAniSkipRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.aniskip.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAniSkipApiService(@AniSkipApi retrofit: Retrofit): com.anilibrix.plus.data.remote.api.AniSkipApi {
        return retrofit.create(com.anilibrix.plus.data.remote.api.AniSkipApi::class.java)
    }

    @Provides
    @Singleton
    @ConsumetApi
    fun provideConsumetRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.consumet.org/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideConsumetApiService(@ConsumetApi retrofit: Retrofit): com.anilibrix.plus.data.remote.api.ConsumetApi {
        return retrofit.create(com.anilibrix.plus.data.remote.api.ConsumetApi::class.java)
    }

    @Provides
    @Singleton
    @NyaaApi
    fun provideNyaaRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://nyaa.si/")
            .client(client)
            .build()
    }

    @Provides
    @Singleton
    fun provideNyaaApiService(@NyaaApi retrofit: Retrofit): com.anilibrix.plus.data.remote.api.NyaaApi {
        return retrofit.create(com.anilibrix.plus.data.remote.api.NyaaApi::class.java)
    }
}
