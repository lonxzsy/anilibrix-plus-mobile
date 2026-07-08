package com.anilibrix.plus.data.repository

import com.anilibrix.plus.data.remote.api.GitHubApi
import com.anilibrix.plus.data.remote.dto.GitHubCreateIssueRequest
import com.anilibrix.plus.domain.model.ChangelogRelease
import com.anilibrix.plus.domain.model.GitHubIssue
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

    override fun getIssues(): Flow<NetworkResult<List<GitHubIssue>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getIssues()
            emit(NetworkResult.Success(response.map { dto ->
                GitHubIssue(
                    number = dto.number,
                    title = dto.title,
                    body = dto.body,
                    state = dto.state,
                    createdAt = dto.createdAt,
                    htmlUrl = dto.htmlUrl,
                    userLogin = dto.user?.login,
                    userAvatarUrl = dto.user?.avatarUrl
                )
            }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun createIssue(title: String, body: String?): Flow<NetworkResult<GitHubIssue>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.createIssue(GitHubCreateIssueRequest(title = title, body = body))
            emit(NetworkResult.Success(GitHubIssue(
                number = response.number,
                title = response.title,
                body = response.body,
                state = response.state,
                createdAt = response.createdAt,
                htmlUrl = response.htmlUrl,
                userLogin = response.user?.login,
                userAvatarUrl = response.user?.avatarUrl
            )))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }
}
