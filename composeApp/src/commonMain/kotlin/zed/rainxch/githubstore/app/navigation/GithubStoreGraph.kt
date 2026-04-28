package zed.rainxch.githubstore.app.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface GithubStoreGraph {
    @Serializable
    data object HomeScreen : GithubStoreGraph

    @Serializable
    data object SearchScreen : GithubStoreGraph

    @Serializable
    data object AuthenticationScreen : GithubStoreGraph

    @Serializable
    data class DetailsScreen(
        val repositoryId: Long = -1L,
        val owner: String = "",
        val repo: String = "",
        val isComingFromUpdate: Boolean = false,
        // E6 telemetry: where this nav originated. Categorical only —
        // "search" / "category" / "library" / "link". Drives the FROM
        // prop on DETAILS_VIEWED.
        val from: String = "link",
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
}
