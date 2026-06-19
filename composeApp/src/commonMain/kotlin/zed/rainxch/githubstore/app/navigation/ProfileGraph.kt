package zed.rainxch.githubstore.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import zed.rainxch.profile.presentation.ProfileRoot

fun NavGraphBuilder.profileGraph(
    navController: NavHostController,
    announcementsUnreadCount: Int,
) {
    navigation<GithubStoreGraph.ProfileGraph>(
        startDestination = GithubStoreGraph.ProfileGraph.ProfileScreen,
    ) {
        composable<GithubStoreGraph.ProfileGraph.ProfileScreen> {
            ProfileRoot(
                onNavigateToAuthentication = {
                    navController.navigate(GithubStoreGraph.AuthenticationScreen)
                },
                onNavigateToStarredRepos = {
                    navController.navigate(GithubStoreGraph.StarredReposScreen)
                },
                onNavigateToFavouriteRepos = {
                    navController.navigate(GithubStoreGraph.FavouritesScreen)
                },
                onNavigateToRecentlyViewed = {
                    navController.navigate(GithubStoreGraph.RecentlyViewedScreen)
                },
                onNavigateToDevProfile = { username ->
                    navController.navigate(
                        GithubStoreGraph.DeveloperProfileScreen(
                            username,
                        ),
                    )
                },
                onNavigateToWhatsNew = {
                    navController.navigate(GithubStoreGraph.WhatsNewHistoryScreen)
                },
                onNavigateToAnnouncements = {
                    navController.navigate(GithubStoreGraph.AnnouncementsScreen)
                },
                onNavigateToTweaks = {
                    navController.navigate(GithubStoreGraph.TweaksScreen)
                },
                onNavigateToAbout = {
                    navController.navigate(GithubStoreGraph.AboutScreen)
                },
                hasUnreadAnnouncements = announcementsUnreadCount > 0,
            )
        }
    }
}
