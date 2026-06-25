package zed.rainxch.githubstore.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import zed.rainxch.auth.presentation.AuthDeepLinkBus
import zed.rainxch.auth.presentation.AuthDeepLinkEvent
import zed.rainxch.githubstore.app.deeplink.DeepLinkDestination
import zed.rainxch.githubstore.app.deeplink.DeepLinkParser
import zed.rainxch.githubstore.app.navigation.GithubStoreGraph
import zed.rainxch.githubstore.app.navigation.getCurrentScreen
import zed.rainxch.tweaks.presentation.utils.TweaksDeepLinkBus

@Composable
fun HandleDesktopToolbarDeeplinks(
    deepLinkUri: String?,
    onDeepLinkConsumed: () -> Unit,
    navController: NavHostController,
) {
    val currentScreen = navController.currentBackStackEntryAsState().value.getCurrentScreen()

    LaunchedEffect(deepLinkUri) {
        deepLinkUri?.let { uri ->
            when (val destination = DeepLinkParser.parse(uri)) {
                is DeepLinkDestination.Repository -> {
                    navController.navigate(
                        GithubStoreGraph.DetailsScreen(
                            owner = destination.owner,
                            repo = destination.repo,
                        ),
                    )
                }

                DeepLinkDestination.Home -> {
                    if (currentScreen !is GithubStoreGraph.HomeScreen) {
                        navController.navigate(GithubStoreGraph.HomeScreen) {
                            popUpTo(GithubStoreGraph.HomeScreen) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                DeepLinkDestination.Apps -> {
                    navController.navigate(GithubStoreGraph.AppsScreen) {
                        popUpTo(GithubStoreGraph.HomeScreen) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                is DeepLinkDestination.AuthHandoff -> {
                    AuthDeepLinkBus.publish(
                        AuthDeepLinkEvent.Handoff(destination.handoffId, destination.state),
                    )
                    if (currentScreen !is GithubStoreGraph.AuthenticationScreen) {
                        navController.navigate(GithubStoreGraph.AuthenticationScreen) {
                            launchSingleTop = true
                        }
                    }
                }

                is DeepLinkDestination.AuthError -> {
                    AuthDeepLinkBus.publish(
                        AuthDeepLinkEvent.Error(destination.reason, destination.state),
                    )
                    if (currentScreen !is GithubStoreGraph.AuthenticationScreen) {
                        navController.navigate(GithubStoreGraph.AuthenticationScreen) {
                            launchSingleTop = true
                        }
                    }
                }

                DeepLinkDestination.Tweaks -> {
                    navController.navigate(GithubStoreGraph.TweaksScreen) {
                        launchSingleTop = true
                    }
                }

                DeepLinkDestination.Feedback -> {
                    navController.navigate(GithubStoreGraph.TweaksScreen) {
                        launchSingleTop = true
                    }
                    TweaksDeepLinkBus.requestOpenFeedback()
                }

                DeepLinkDestination.About -> {
                    navController.navigate(GithubStoreGraph.AboutScreen) {
                        launchSingleTop = true
                    }
                }

                DeepLinkDestination.TweaksLicenses -> {
                    navController.navigate(GithubStoreGraph.LicensesScreen) {
                        launchSingleTop = true
                    }
                }

                DeepLinkDestination.Search -> {
                    if (currentScreen !is GithubStoreGraph.SearchScreen) {
                        navController.navigate(GithubStoreGraph.SearchScreen()) {
                            popUpTo(GithubStoreGraph.HomeScreen) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }

                DeepLinkDestination.Favourites -> {
                    navController.navigate(GithubStoreGraph.FavouritesScreen) {
                        popUpTo(GithubStoreGraph.HomeScreen) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                DeepLinkDestination.RecentlyViewed -> {
                    navController.navigate(GithubStoreGraph.RecentlyViewedScreen) {
                        popUpTo(GithubStoreGraph.HomeScreen) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                DeepLinkDestination.None -> {
                }
            }
            onDeepLinkConsumed()
        }
    }
}
