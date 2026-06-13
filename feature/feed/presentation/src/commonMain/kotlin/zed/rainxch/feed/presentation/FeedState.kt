package zed.rainxch.feed.presentation

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.core.presentation.model.DiscoveryRepositoryUi

@Immutable
data class FeedState(
    val repos: ImmutableList<DiscoveryRepositoryUi> = persistentListOf(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val selectedPlatform: DiscoveryPlatform = DiscoveryPlatform.All,
)
