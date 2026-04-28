package zed.rainxch.githubstore.app.navigation

import kotlinx.serialization.Serializable

// E6 telemetry: where a Details nav originated. Closed set so the
// FROM prop on DETAILS_VIEWED can never carry an arbitrary string.
@Serializable
enum class DetailsFrom(
    val slug: String,
) {
    Search("search"),
    Category("category"),
    Library("library"),
    Link("link"),
}

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
        // Drives the FROM prop on DETAILS_VIEWED. Typed so callers
        // can't accidentally introduce off-allowlist values.
        val from: DetailsFrom = DetailsFrom.Link,
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
