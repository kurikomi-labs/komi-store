package zed.rainxch.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.domain.model.PaginatedDiscoveryRepositories
import zed.rainxch.domain.model.ExploreResult
import zed.rainxch.domain.model.ProgrammingLanguage
import zed.rainxch.domain.model.SearchSource
import zed.rainxch.domain.model.SortBy
import zed.rainxch.domain.model.SortOrder

interface SearchRepository {
    fun searchRepositories(
        query: String,
        platform: DiscoveryPlatform,
        language: ProgrammingLanguage,
        sortBy: SortBy,
        sortOrder: SortOrder,
        page: Int,
        source: SearchSource = SearchSource.GitHub,
    ): Flow<PaginatedDiscoveryRepositories>

    suspend fun exploreFromGithub(
        query: String,
        platform: DiscoveryPlatform,
        page: Int,
    ): ExploreResult
}
