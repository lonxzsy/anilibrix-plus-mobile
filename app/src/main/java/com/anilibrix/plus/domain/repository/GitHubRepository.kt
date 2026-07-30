package com.anilibrix.plus.domain.repository

import com.anilibrix.plus.domain.model.ChangelogRelease
import com.anilibrix.plus.domain.model.GitHubIssue
import com.anilibrix.plus.domain.model.NetworkResult
import kotlinx.coroutines.flow.Flow

interface GitHubRepository {
    fun getReleases(): Flow<NetworkResult<List<ChangelogRelease>>>
    fun getIssues(): Flow<NetworkResult<List<GitHubIssue>>>
    fun createIssue(title: String, body: String?): Flow<NetworkResult<GitHubIssue>>
}
