package zed.rainxch.githubstore.app.deeplink

sealed interface DeepLinkDestination {
    data class Repository(
        val owner: String,
        val repo: String,
    ) : DeepLinkDestination

    data object Apps : DeepLinkDestination

    data class AuthHandoff(
        val handoffId: String,
        val state: String,
    ) : DeepLinkDestination

    data class AuthError(
        val reason: String,
        val state: String,
    ) : DeepLinkDestination

    data object Tweaks : DeepLinkDestination

    data object About : DeepLinkDestination

    data object TweaksLicenses : DeepLinkDestination

    data object Search : DeepLinkDestination

    data object Favourites : DeepLinkDestination

    data object RecentlyViewed : DeepLinkDestination

    data object None : DeepLinkDestination
}
