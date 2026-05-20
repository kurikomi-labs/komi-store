package zed.rainxch.details.domain.repository

import zed.rainxch.core.domain.model.GithubRelease
import zed.rainxch.core.domain.model.GithubRepoSummary
import zed.rainxch.core.domain.model.GithubUserProfile
import zed.rainxch.details.domain.model.RepoStats

typealias ReadmeContent = String
typealias ReadmePath = String
typealias LanguageCode = String

interface DetailsRepository {
    suspend fun getRepositoryById(id: Long): GithubRepoSummary

    suspend fun getRepositoryByOwnerAndName(
        owner: String,
        name: String,
        sourceHost: String? = null,
    ): GithubRepoSummary

    suspend fun refreshRepository(
        owner: String,
        name: String,
    ): GithubRepoSummary

    suspend fun getLatestPublishedRelease(
        owner: String,
        repo: String,
        defaultBranch: String,
        sourceHost: String? = null,
    ): GithubRelease?

    suspend fun getAllReleases(
        owner: String,
        repo: String,
        defaultBranch: String,
        sourceHost: String? = null,
    ): List<GithubRelease>

    suspend fun getReadme(
        owner: String,
        repo: String,
        defaultBranch: String,
        sourceHost: String? = null,
    ): Triple<ReadmeContent, LanguageCode?, ReadmePath>?

    suspend fun getRepoStats(
        owner: String,
        repo: String,
        sourceHost: String? = null,
    ): RepoStats

    suspend fun getUserProfile(username: String): GithubUserProfile

    suspend fun checkAttestations(
        owner: String,
        repo: String,
        sha256Digest: String,
    ): Boolean
}
