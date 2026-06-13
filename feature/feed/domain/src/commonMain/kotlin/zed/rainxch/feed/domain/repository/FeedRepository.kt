package zed.rainxch.feed.domain.repository

import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.feed.domain.model.FeedPage

interface FeedRepository {
    suspend fun getFeed(
        platform: DiscoveryPlatform,
        page: Int,
        forceRefresh: Boolean = false,
    ): Result<FeedPage>
}
