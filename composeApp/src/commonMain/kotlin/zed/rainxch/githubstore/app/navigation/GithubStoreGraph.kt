package zed.rainxch.githubstore.app.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface GithubStoreGraph {
    @Serializable
    data object HomeScreen : GithubStoreGraph

    @Serializable
    data class SearchScreen(
        // String over enum: Compose Navigation's Desktop (`nonAndroid.kt`)
        // serializer needs an explicit NavType for non-primitive nav args,
        // which enums don't have out of the box. Enum-as-name string keeps
        // the contract type-safe at the caller / VM boundary while letting
        // the route serialize on every target with no typeMap.
        val initialPlatform: String? = null,
    ) : GithubStoreGraph

    @Serializable
    data object AuthenticationScreen : GithubStoreGraph

    @Serializable
    data class DetailsScreen(
        val repositoryId: Long = -1L,
        val owner: String = "",
        val repo: String = "",
        val isComingFromUpdate: Boolean = false,
        // Non-null when the repo lives on a non-GitHub forge (Codeberg /
        // Forgejo / Gitea / custom). Drives the foreign-source branch in
        // DetailsViewModel so we hit the Forgejo API instead of GitHub.
        val sourceHost: String? = null,
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
    data object OnboardingScreen : GithubStoreGraph

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

    @Serializable
    data object HostTokensScreen : GithubStoreGraph
}
