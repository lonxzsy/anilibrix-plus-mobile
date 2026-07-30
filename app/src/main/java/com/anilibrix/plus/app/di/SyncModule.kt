package com.anilibrix.plus.app.di

import com.anilibrix.plus.core.sync.ShikimoriSyncHandler
import com.anilibrix.plus.data.sync.ShikimoriSyncHandlerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindShikimoriSyncHandler(impl: ShikimoriSyncHandlerImpl): ShikimoriSyncHandler
}
