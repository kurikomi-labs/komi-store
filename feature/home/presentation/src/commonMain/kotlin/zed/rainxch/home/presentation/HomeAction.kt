package zed.rainxch.home.presentation

import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.core.presentation.model.GithubRepoSummaryUi

sealed interface HomeAction {
    data object OnRefreshClick : HomeAction

    data object OnRetry : HomeAction

    data object OnSearchClick : HomeAction

    data object OnSettingsClick : HomeAction

    data object OnAppsClick : HomeAction

    data object OnPlatformPopupOpen : HomeAction

    data object OnPlatformPopupDismiss : HomeAction

    data class OnRepoClick(
        val repo: GithubRepoSummaryUi,
    ) : HomeAction

    data class OnRepoLongClick(
        val repoId: Long,
    ) : HomeAction

    data object OnActionSheetDismiss : HomeAction

    data class OnDeveloperClick(
        val username: String,
    ) : HomeAction

    data class OnShareClick(
        val repo: GithubRepoSummaryUi,
    ) : HomeAction

    data class OnHideRepository(
        val repo: GithubRepoSummaryUi,
    ) : HomeAction

    data class OnUndoHideRepository(
        val repoId: Long,
    ) : HomeAction

    data class OnMarkAsSeen(
        val repo: GithubRepoSummaryUi,
    ) : HomeAction

    data class OnMarkAsUnseen(
        val repoId: Long,
    ) : HomeAction

    data object OnSeeAllHot : HomeAction

    data object OnSeeAllTrending : HomeAction

    data object OnSeeAllPopular : HomeAction

    data object OnSeeAllStarred : HomeAction
}
