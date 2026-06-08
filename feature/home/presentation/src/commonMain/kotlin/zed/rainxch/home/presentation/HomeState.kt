package zed.rainxch.home.presentation

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.home.presentation.model.HomeRepoCardUi

@Stable
data class HomeState(
    val lead: HomeRepoCardUi? = null,
    val hot: ImmutableList<HomeRepoCardUi> = persistentListOf(),
    val trending: ImmutableList<HomeRepoCardUi> = persistentListOf(),
    val popular: ImmutableList<HomeRepoCardUi> = persistentListOf(),
    val starred: ImmutableList<HomeRepoCardUi> = persistentListOf(),
    val isHotLoading: Boolean = false,
    val isTrendingLoading: Boolean = false,
    val isPopularLoading: Boolean = false,
    val isStarredLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedPlatforms: Set<DiscoveryPlatform> = emptySet(),
    val isPlatformPopupVisible: Boolean = false,
    val isAppsSectionVisible: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val isHideSeenEnabled: Boolean = false,
    val isUserSignedIn: Boolean = false,
    val actionSheetCard: HomeRepoCardUi? = null,
)
