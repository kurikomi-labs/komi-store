package zed.rainxch.home.presentation.categorylist

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import zed.rainxch.core.presentation.model.DiscoveryRepositoryUi
import zed.rainxch.home.domain.model.HomeCategory

data class CategoryListState(
    val category: HomeCategory = HomeCategory.HOT_RELEASE,
    val repos: ImmutableList<DiscoveryRepositoryUi> = persistentListOf(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val errorMessage: String? = null,
)
