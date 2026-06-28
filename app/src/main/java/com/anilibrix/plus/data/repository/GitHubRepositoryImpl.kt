package com.anilibrix.plus.data.repository

import com.anilibrix.plus.data.remote.api.GitHubApi
import com.anilibrix.plus.domain.model.ChangelogRelease
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.GitHubRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubRepositoryImpl @Inject constructor(
    private val api: GitHubApi
) : GitHubRepository {

    override fun getReleases(): Flow<NetworkResult<List<ChangelogRelease>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getReleases()
            emit(NetworkResult.Success(response.map { dto ->
                ChangelogRelease(
                    tagName = dto.tagName,
                    name = dto.name,
                    body = dto.body,
                    publishedAt = dto.publishedAt,
                    htmlUrl = dto.htmlUrl
                )
            }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }
}
