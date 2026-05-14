package zed.rainxch.githubstore.app.navigation

import kotlinx.serialization.Serializable
import zed.rainxch.search.presentation.model.SearchPlatformUi

@Serializable
sealed interface GithubStoreGraph {
    @Serializable
    data object HomeScreen : GithubStoreGraph

    @Serializable
    data class SearchScreen(
        val initialPlatform: SearchPlatformUi? = null,
    ) : GithubStoreGraph

    @Serializable
    data object AuthenticationScreen : GithubStoreGraph

    @Serializable
    data class DetailsScreen(
        val repositoryId: Long = -1L,
        val owner: String = "",
        val repo: String = "",
        val isComingFromUpdate: Boolean = false,
    ) : GithubStoreGraph

    @Serializable
    data class DeveloperProfileScreen(
        val username: String,
    ) : GithubStoreGraph

    @Serializable
    data object ProfileScreen : GithubStoreGraph

    @Serializable
    data object TweaksScreen : GithubStoreGraph

    @Serializable
    data object FavouritesScreen : GithubStoreGraph

    @Serializable
    data object StarredReposScreen : GithubStoreGraph

    @Serializable
    data object RecentlyViewedScreen : GithubStoreGraph

    @Serializable
    data object AppsScreen : GithubStoreGraph

    @Serializable
    data object SponsorScreen : GithubStoreGraph

    @Serializable
    data object ExternalImportScreen : GithubStoreGraph

    @Serializable
    data object MirrorPickerScreen : GithubStoreGraph

    @Serializable
    data object SkippedUpdatesScreen : GithubStoreGraph

    @Serializable
    data object HiddenRepositoriesScreen : GithubStoreGraph

    @Serializable
    data object WhatsNewHistoryScreen : GithubStoreGraph

    @Serializable
    data object AnnouncementsScreen : GithubStoreGraph

    @Serializable
    data object StarredPickerScreen : GithubStoreGraph
}
