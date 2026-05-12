package zed.rainxch.core.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.GithubRepoSummary
import zed.rainxch.core.domain.model.SeenRepo

interface SeenReposRepository {
    fun getAllSeenRepoIds(): Flow<Set<Long>>

    fun getAllSeenRepos(): Flow<List<SeenRepo>>

    suspend fun markAsSeen(repo: GithubRepoSummary)

    /**
     * Primitive-arg overload for surfaces that only hold the UI model
     * (Home / Search cards) and would otherwise have to reconstitute a
     * full [GithubRepoSummary] just to mark a repo seen.
     */
    suspend fun markAsSeen(
        repoId: Long,
        repoName: String,
        repoOwner: String,
        repoOwnerAvatarUrl: String,
        repoDescription: String?,
        primaryLanguage: String?,
        repoUrl: String,
    )

    suspend fun removeFromHistory(repoId: Long)

    suspend fun clearAll()
}
