package zed.rainxch.core.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.repository.HiddenRepo

interface HiddenReposRepository {
    fun getAllHiddenRepoIds(): Flow<Set<Long>>

    fun getAllHiddenRepos(): Flow<List<HiddenRepo>>

    suspend fun hide(
        repoId: Long,
        repoName: String,
        repoOwner: String,
        repoOwnerAvatarUrl: String,
    )

    suspend fun unhide(repoId: Long)

    suspend fun clearAll()
}
