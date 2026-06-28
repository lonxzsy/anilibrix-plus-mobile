package com.anilibrix.plus.app.di

import com.anilibrix.plus.data.repository.AnilibriaRepositoryImpl
import com.anilibrix.plus.data.repository.DecoderRepositoryImpl
import com.anilibrix.plus.data.repository.GitHubRepositoryImpl
import com.anilibrix.plus.data.repository.JikanRepositoryImpl
import com.anilibrix.plus.data.repository.LocalRepositoryImpl
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.DecoderRepository
import com.anilibrix.plus.domain.repository.GitHubRepository
import com.anilibrix.plus.domain.repository.JikanRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAnilibriaRepository(impl: AnilibriaRepositoryImpl): AnilibriaRepository

    @Binds
    @Singleton
    abstract fun bindDecoderRepository(impl: DecoderRepositoryImpl): DecoderRepository

    @Binds
    @Singleton
    abstract fun bindJikanRepository(impl: JikanRepositoryImpl): JikanRepository

    @Binds
    @Singleton
    abstract fun bindGitHubRepository(impl: GitHubRepositoryImpl): GitHubRepository

    @Binds
    @Singleton
    abstract fun bindLocalRepository(impl: LocalRepositoryImpl): LocalRepository
}
