package zed.rainxch.home.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.domain.model.PaginatedDiscoveryRepositories
import zed.rainxch.home.domain.model.TopicCategory

interface HomeRepository {
    fun getTrendingRepositories(
        platforms: Set<DiscoveryPlatform>,
        page: Int,
    ): Flow<PaginatedDiscoveryRepositories>

    fun getHotReleaseRepositories(
        platforms: Set<DiscoveryPlatform>,
        page: Int,
    ): Flow<PaginatedDiscoveryRepositories>

    fun getMostPopular(
        platforms: Set<DiscoveryPlatform>,
        page: Int,
    ): Flow<PaginatedDiscoveryRepositories>

    fun searchByTopic(
        searchKeywords: String,
        platforms: Set<DiscoveryPlatform>,
        page: Int,
    ): Flow<PaginatedDiscoveryRepositories>

    fun getTopicRepositories(
        topic: TopicCategory,
        platforms: Set<DiscoveryPlatform>,
    ): Flow<PaginatedDiscoveryRepositories>

    /**
     * Fetch a single repo summary by id. Used by the Home starred section to
     * hydrate stale local cache rows with fresh topics / updatedAt / platforms.
     * Returns `null` on failure (network / 404) so callers can drop that one
     * item without failing the whole section.
     */
    suspend fun getRepositoryById(id: Long): zed.rainxch.core.domain.model.GithubRepoSummary?
}
