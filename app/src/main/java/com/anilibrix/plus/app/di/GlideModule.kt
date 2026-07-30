package com.anilibrix.plus.app.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.signature.ObjectKey
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class GlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val memoryCacheSize = (Runtime.getRuntime().maxMemory() / 8).toInt()
        builder.setMemoryCache(LruResourceCache(memoryCacheSize.toLong()))

        val diskCacheSize = 250L * 1024 * 1024
        builder.setDiskCache(
            InternalCacheDiskCacheFactory(context, "glide_cache", diskCacheSize)
        )

        builder.setDefaultRequestOptions(
            com.bumptech.glide.request.RequestOptions().signature(ObjectKey("1"))
        )
    }
}
