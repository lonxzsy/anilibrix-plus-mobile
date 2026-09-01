package com.anilibrix.plus.app.di

import android.content.Context
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Qualifier
import javax.inject.Singleton

/** Кэш потокового воспроизведения: временный, вытесняется по LRU. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StreamingCache

/** Хранилище скачанных серий: постоянное, ничего само не удаляет. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadCache

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideDatabaseProvider(@ApplicationContext context: Context): DatabaseProvider =
        StandaloneDatabaseProvider(context)

    @Provides
    @Singleton
    @StreamingCache
    fun provideStreamingCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
    ): SimpleCache {
        val cacheDir = File(context.cacheDir, "media_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(STREAM_CACHE_BYTES)
        return SimpleCache(cacheDir, evictor, databaseProvider)
    }

    /**
     * Хранилище загрузок.
     *
     * Два отличия от кэша потока, и оба принципиальные:
     *
     *  - лежит в `filesDir`, а не в `cacheDir`. Содержимое `cacheDir` система
     *    вправе удалить в любой момент при нехватке места — скачанные на дорогу
     *    серии исчезали бы без предупреждения;
     *  - [NoOpCacheEvictor] вместо LRU. Загрузку удаляет только пользователь;
     *    вытеснение по размеру означало бы, что новая скачанная серия молча
     *    стирает старую.
     */
    @Provides
    @Singleton
    @DownloadCache
    fun provideDownloadCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
    ): SimpleCache {
        val downloadDir = File(context.filesDir, "downloads")
        return SimpleCache(downloadDir, NoOpCacheEvictor(), databaseProvider)
    }

    @Provides
    @Singleton
    fun provideHttpDataSourceFactory(): DefaultHttpDataSource.Factory =
        DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("AnilibrixPlus/1.0 Android")

    /**
     * Источник данных для плеера: сначала загрузки, потом кэш потока, потом сеть.
     *
     * Благодаря этой цепочке воспроизведение скачанного **не требует отдельной
     * ветки кода**: плеер получает тот же URL, что и при онлайн-просмотре, и
     * сам берёт данные с диска, если они там есть. Альтернатива — подменять
     * URL на локальный путь — означала бы две расходящиеся кодовые дорожки и
     * ошибки на стыке.
     *
     * `FLAG_IGNORE_CACHE_ON_ERROR` снят с загрузок намеренно: для потока
     * «битый кусок кэша — не беда, дочитаем из сети» верно, а для офлайна
     * молча уходить в сеть при повреждённом файле нельзя, иначе в самолёте
     * человек получит непонятную ошибку вместо честного «файл повреждён».
     */
    @Provides
    @Singleton
    fun provideCacheDataSourceFactory(
        @ApplicationContext context: Context,
        @StreamingCache streamingCache: SimpleCache,
        @DownloadCache downloadCache: SimpleCache,
        httpFactory: DefaultHttpDataSource.Factory,
    ): CacheDataSource.Factory {
        val upstreamFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpFactory)
        val streamingLayer = CacheDataSource.Factory()
            .setCache(streamingCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        return CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(streamingLayer)
            .setCacheWriteDataSinkFactory(null)   // читаем, но не пишем: писать сюда — дело DownloadManager
    }

    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
        @DownloadCache downloadCache: SimpleCache,
        httpFactory: DefaultHttpDataSource.Factory,
    ): DownloadManager {
        return DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            httpFactory,
            // Пул на три потока: качать все серии разом бессмысленно —
            // канал один, а параллельные соединения только увеличивают
            // вероятность обрыва на мобильной сети.
            Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS),
        ).apply {
            maxParallelDownloads = MAX_PARALLEL_DOWNLOADS
        }
    }

    private const val STREAM_CACHE_BYTES = 500L * 1024 * 1024
    private const val MAX_PARALLEL_DOWNLOADS = 3
}
