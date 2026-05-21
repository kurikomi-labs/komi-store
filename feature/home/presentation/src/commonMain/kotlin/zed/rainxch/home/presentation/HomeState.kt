package zed.rainxch.home.presentation

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.domain.model.InstalledApp
import zed.rainxch.core.presentation.model.DiscoveryRepositoryUi

@Stable
data class HomeState(
    val hot: ImmutableList<DiscoveryRepositoryUi> = persistentListOf(),
    val trending: ImmutableList<DiscoveryRepositoryUi> = persistentListOf(),
    val popular: ImmutableList<DiscoveryRepositoryUi> = persistentListOf(),
    val starred: ImmutableList<DiscoveryRepositoryUi> = persistentListOf(),
    val installedApps: ImmutableList<InstalledApp> = persistentListOf(),
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
    val seenRepoIds: Set<Long> = emptySet(),
    val hiddenRepoIds: Set<Long> = emptySet(),
    val actionSheetRepoId: Long? = null,
)
